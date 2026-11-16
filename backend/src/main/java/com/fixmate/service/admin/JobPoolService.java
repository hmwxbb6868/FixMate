package com.fixmate.service.admin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.common.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class JobPoolService {
 private final JobPoolEntryMapper entries;private final JobPoolQueryMapper query;private final WorkOrderMapper orders;private final RepairRequestMapper repairs;
 private final WorkOrderIdentityLocker locker;private final WorkAssignmentService assignments;private final EmployeeWechatAccessService employees;
 private final WorkOrderSafetyGate safety;private final ServiceContactProjection contacts;private final TaskSummaryProjection summaries;
 private final DatabaseClockMapper clock;private final AuditLogService audit;private final ObjectMapper json;private final StaffAccessService access;private final VisitSchedulingService visits;
 private final WorkOrderUrges urges;
 public JobPoolService(JobPoolEntryMapper entries,JobPoolQueryMapper query,WorkOrderMapper orders,RepairRequestMapper repairs,WorkOrderIdentityLocker locker,WorkAssignmentService assignments,EmployeeWechatAccessService employees,WorkOrderSafetyGate safety,ServiceContactProjection contacts,TaskSummaryProjection summaries,DatabaseClockMapper clock,AuditLogService audit,ObjectMapper json,StaffAccessService access,VisitSchedulingService visits,WorkOrderUrges urges){this.visits=visits;this.access=access;this.entries=entries;this.query=query;this.orders=orders;this.repairs=repairs;this.locker=locker;this.assignments=assignments;this.employees=employees;this.safety=safety;this.contacts=contacts;this.summaries=summaries;this.clock=clock;this.audit=audit;this.json=json;this.urges=urges;}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public JobPoolPublicationVO publish(StaffPrincipal actor,Long orderId,JobPoolPublishDTO input){
  var order=locker.lock(actor,orderId,List.of(),"ADMIN","DISPATCHER").order();
  StaffIdentityGuard.version(order.getVersion(),input.expectedWorkOrderVersion());
  boolean fromReturn=input.expectedAssignmentVersion()!=null;
  String reason=input.reason()==null?(fromReturn?"批准师傅退回并发布到接单大厅":"管理端确认发布到接单大厅"):
          contacts.redactPersonalText(StaffIdentityGuard.text(input.reason(),2,1000,"发布依据"),repairs.selectById(order.getRepairRequestId()));
  if(fromReturn)assignments.releaseReturnForPublication(order,actor.staffId(),input.expectedAssignmentVersion(),reason);
  eligible(order,true);
  if(entries.selectCount(new LambdaQueryWrapper<JobPoolEntryEntity>().eq(JobPoolEntryEntity::getWorkOrderId,orderId).eq(JobPoolEntryEntity::getStatus,"PUBLISHED"))>0)throw BusinessException.stateConflict("订单已发布，请刷新");
  var e=new JobPoolEntryEntity();var now=clock.now();e.setWorkOrderId(orderId);e.setStatus("PUBLISHED");e.setPublishedBy(actor.staffId());e.setPublishedAt(now);e.setVersion(0);e.setCreatedAt(now);e.setUpdatedAt(now);
  e.setReason(reason);
  changed(entries.insert(e));touch(order);event(actor.staffId(),e,"PUBLISHED",null);return publication(e,order);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public JobPoolPublicationVO withdraw(StaffPrincipal actor,Long id,JobPoolWithdrawDTO input){
  var candidate=entry(id,false);var order=locker.lock(actor,candidate.getWorkOrderId(),List.of(),"ADMIN","DISPATCHER").order();var e=entry(id,true);
  if(com.fixmate.common.enums.WorkOrderStatus.valueOf(order.getStatus()).isTerminal())throw BusinessException.stateConflict("工单已结束");
  StaffIdentityGuard.version(order.getVersion(),input.expectedWorkOrderVersion());StaffIdentityGuard.version(e.getVersion(),input.expectedPublicationVersion());
  if(!"PUBLISHED".equals(e.getStatus()))throw BusinessException.stateConflict("订单已被接走或已撤回");
  var reason=contacts.redactPersonalText(StaffIdentityGuard.text(input.reason(),2,1000,"撤回依据"),repairs.selectById(order.getRepairRequestId()));var now=clock.now();
  changed(entries.update(null,new UpdateWrapper<JobPoolEntryEntity>().eq("id",id).eq("version",e.getVersion()).eq("status","PUBLISHED").set("status","WITHDRAWN").set("withdrawn_by",actor.staffId()).set("withdrawn_at",now).set("reason",reason).set("updated_at",now).set("version",e.getVersion()+1)));
  urges.invalidatePending(order.getId(),now,"接单大厅发布已撤回，催办自动失效");
  e.setStatus("WITHDRAWN");e.setVersion(e.getVersion()+1);touch(order);event(actor.staffId(),e,"WITHDRAWN",null);return publication(e,order);
 }
 public JobPoolPublicationVO latest(StaffPrincipal actor,Long orderId){access.require(actor,"ADMIN","DISPATCHER","AUDITOR");var order=order(orderId,false);var e=entries.latestForOrder(orderId);if(e==null)throw BusinessException.notFound("工单尚无接单池发布记录");return publication(e,order);}
 @Transactional(readOnly=true)
 public JobPoolEntryVOPageVO list(Long customerId,int page,int size){employees.require(customerId);com.fixmate.service.admin.serviceImpl.StaffDirectoryServiceImpl.page(page,size);return new JobPoolEntryVOPageVO(query.page((long)(page-1)*size,size).stream().map(e->summary(e,order(e.getWorkOrderId(),false))).toList(),page,size,query.count());}
 @Transactional(readOnly=true)
 public JobPoolEntryVO detail(Long customerId,Long id){employees.require(customerId);var e=entry(id,false);if(!"PUBLISHED".equals(e.getStatus()))throw BusinessException.stateConflict("订单已被接走或已撤回");var order=order(e.getWorkOrderId(),false);eligible(order,false);return summary(e,order);}
 /** A replay receipt is authorized only for its still-current ACTIVE assignee. */
 public void requireClaimAccess(Long customerId,Long id){var person=employees.require(customerId);var e=entry(id,false);if("PUBLISHED".equals(e.getStatus()))return;
  if("CLAIMED".equals(e.getStatus())&&person.staffId().equals(e.getClaimedBy())&&person.bindingId().equals(e.getClaimedBindingId())&&person.bindingVersion().equals(e.getClaimedBindingVersion())){var order=order(e.getWorkOrderId(),false);var a=assignments.currentAssignment(order,false);if(a!=null&&"ACTIVE".equals(a.getStatus())&&a.getId().equals(e.getAssignmentId())&&person.staffId().equals(a.getTechnicianId())&&order.getTakeoverBy()==null&&!Set.of("COMPLETED","CANCELLED","CLOSED_UNRESOLVED").contains(order.getStatus()))return;}
  throw BusinessException.stateConflict("订单已被接走、撤回或当前接单权限已变化");
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public AssignmentVO claim(Long customerId,Long id,JobPoolClaimDTO input){var person=employees.requireLocked(customerId);var candidate=entry(id,false);var order=order(candidate.getWorkOrderId(),true);var e=entry(id,true);
  if(!"PUBLISHED".equals(e.getStatus()))throw BusinessException.stateConflict("订单已被接走或已撤回");StaffIdentityGuard.version(order.getVersion(),input.expectedWorkOrderVersion());StaffIdentityGuard.version(e.getVersion(),input.expectedPublicationVersion());eligible(order,true);
  var a=assignments.claimLocked(person,order);var now=clock.now();changed(entries.update(null,new UpdateWrapper<JobPoolEntryEntity>().eq("id",id).eq("version",e.getVersion()).eq("status","PUBLISHED").set("status","CLAIMED").set("claimed_by",person.staffId()).set("claimed_binding_id",person.bindingId()).set("claimed_binding_version",person.bindingVersion()).set("assignment_id",a.id()).set("claimed_at",now).set("version",e.getVersion()+1).set("updated_at",now)));
  e.setStatus("CLAIMED");e.setVersion(e.getVersion()+1);event(person.staffId(),e,"CLAIMED",person);return a;
 }
 private void eligible(WorkOrderEntity order,boolean lock){
  if(!"WAITING_ASSIGNMENT".equals(order.getStatus())||order.getCurrentAssignmentId()!=null||order.getTechnicianId()!=null||order.getTakeoverBy()!=null||assignments.currentAssignment(order,lock)!=null)throw BusinessException.stateConflict("订单已被接走或不具备发布条件");
  if(!contacts.confirmedForOrder(repairs.selectById(order.getRepairRequestId()),order))throw BusinessException.stateConflict("服务联系尚未确认或不完整");
  if(!"CUSTOMER_SLOT".equals(order.getVisitPolicy()))throw BusinessException.stateConflict("请用户先选择期望上门时段，再发布接单");
  visits.requirePreference(order);
  if(lock)safety.requireClear(order.getId());else if(!safety.clear(order.getId()))throw BusinessException.stateConflict("订单安全事件尚未解除");
 }
 private JobPoolEntryVO summary(JobPoolEntryEntity e,WorkOrderEntity order){return new JobPoolEntryVO(VisitSchedulingService.preferred(order),VisitSchedulingService.policy(order),List.of("CLAIM_JOB"),e.getId(),order.getId(),order.getWorkOrderNo(),order.getApplianceType(),summaries.summary(order,repairs.selectById(order.getRepairRequestId())),CoarseServiceArea.fromSnapshot(json,order.getAddressSnapshot()),offset(e.getPublishedAt()),e.getVersion(),order.getVersion());}
 private JobPoolPublicationVO publication(JobPoolEntryEntity e,WorkOrderEntity order){return new JobPoolPublicationVO(e.getId(),e.getWorkOrderId(),e.getStatus(),e.getVersion(),order.getVersion(),offset(e.getPublishedAt()));}
 private JobPoolEntryEntity entry(Long id,boolean lock){var q=new LambdaQueryWrapper<JobPoolEntryEntity>().eq(JobPoolEntryEntity::getId,id);if(lock)q.last("FOR UPDATE");var e=entries.selectOne(q);if(e==null)throw BusinessException.notFound("接单发布不存在");return e;}
 private WorkOrderEntity order(Long id,boolean lock){var q=new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,id);if(lock)q.last("FOR UPDATE");var o=orders.selectOne(q);if(o==null)throw BusinessException.notFound("工单不存在");return o;}
 private void touch(WorkOrderEntity order){changed(orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",order.getId()).eq("version",order.getVersion()).set("version",order.getVersion()+1).set("updated_at",clock.now())));order.setVersion(order.getVersion()+1);}
 private void changed(int rows){if(rows!=1)throw BusinessException.versionConflict("订单发布已变化，请刷新");}
 private OffsetDateTime offset(LocalDateTime time){return time.atOffset(ZoneOffset.UTC);}
 private void event(Long actor,JobPoolEntryEntity e,String type,EmployeeWechatAccessService.EmployeeIdentity person){var data=new LinkedHashMap<String,Object>();data.put("eventType",type);data.put("workOrderId",e.getWorkOrderId());data.put("version",e.getVersion());if(person!=null){data.put("customerId",person.customerId());data.put("bindingId",person.bindingId());data.put("bindingVersion",person.bindingVersion());}audit.record("STAFF",actor,"JOB_POOL_"+type,"JOB_POOL_ENTRY",e.getId(),null,data);}
}
