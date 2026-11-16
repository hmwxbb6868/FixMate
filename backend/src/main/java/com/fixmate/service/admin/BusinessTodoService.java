package com.fixmate.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.common.ServiceContactProjection;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class BusinessTodoService {
    private final UrgeRecordMapper urges;private final HumanHandoffRecordMapper handoffs;private final WorkOrderMapper orders;
    private final RepairRequestMapper repairs;private final StaffAccessService access;private final ServiceContactProjection contacts;
    private final DatabaseClockMapper clock;private final AuditLogService audit;
    public BusinessTodoService(UrgeRecordMapper urges,HumanHandoffRecordMapper handoffs,WorkOrderMapper orders,RepairRequestMapper repairs,
            StaffAccessService access,ServiceContactProjection contacts,DatabaseClockMapper clock,AuditLogService audit){this.urges=urges;this.handoffs=handoffs;this.orders=orders;this.repairs=repairs;this.access=access;this.contacts=contacts;this.clock=clock;this.audit=audit;}
    public void guard(StaffPrincipal actor){access.require(actor,"ADMIN","DISPATCHER");}
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public BusinessTodoVOPageVO list(StaffPrincipal actor,boolean handoff,int page,int size,String status){
        access.require(actor,"ADMIN","DISPATCHER","AUDITOR");paging(page,size);
        if(!"PENDING".equals(status)&&!"HANDLED".equals(status)&&!(handoff?"ALL":"INVALIDATED").equals(status))
            throw BusinessException.validation("待办状态不正确");
        if(handoff){
            var filter=new QueryWrapper<HumanHandoffRecordEntity>().eq(!"ALL".equals(status),"status",status)
                    .orderByDesc("HANDLED".equals(status),"handled_at").orderByDesc("id");
            var result=handoffs.selectPage(new Page<>(page,size),filter);
            return new BusinessTodoVOPageVO(result.getRecords().stream().map(this::view).toList(),page,size,result.getTotal());
        }
        var result=urges.selectPage(new Page<>(page,size),new QueryWrapper<UrgeRecordEntity>().eq("status",status).orderByDesc("id"));return new BusinessTodoVOPageVO(result.getRecords().stream().map(this::view).toList(),page,size,result.getTotal());
    }
    public BusinessTodoVO handle(StaffPrincipal actor,boolean handoff,Long id,ReasonCommandDTO input){
        access.requireLocked(actor,"ADMIN","DISPATCHER");
        Long orderId=handoff?handoff(id,false).getWorkOrderId():urge(id,false).getWorkOrderId();
        var w=orders.selectOne(new QueryWrapper<WorkOrderEntity>().eq("id",orderId).last("FOR UPDATE"));if(w==null)throw BusinessException.notFound("关联工单不存在");
        var now=clock.now();String conclusion=redact(w.getId(),StaffIdentityGuard.text(input.reason(),2,1000,"处理结论"));Integer version;
        if(handoff){var row=handoff(id,true);if(!orderId.equals(row.getWorkOrderId()))throw BusinessException.stateConflict("待办关联已变化");version=row.getVersion();check(row.getStatus(),version,input.expectedVersion());
            if(handoffs.update(null,new UpdateWrapper<HumanHandoffRecordEntity>().eq("id",id).eq("status","PENDING").eq("version",version).set("status","HANDLED").set("handled_by",actor.staffId()).set("handled_at",now).set("handling_conclusion",conclusion).set("version",version+1).set("updated_at",now))!=1)throw BusinessException.versionConflict("待办已更新");
        }else{var row=urge(id,true);if(!orderId.equals(row.getWorkOrderId()))throw BusinessException.stateConflict("待办关联已变化");version=row.getVersion();check(row.getStatus(),version,input.expectedVersion());
            if(urges.update(null,new UpdateWrapper<UrgeRecordEntity>().eq("id",id).eq("status","PENDING").eq("version",version).set("status","HANDLED").set("handled_by",actor.staffId()).set("handled_at",now).set("handling_conclusion",conclusion).set("version",version+1).set("updated_at",now))!=1)throw BusinessException.versionConflict("待办已更新");
        }
        audit.record("STAFF",actor.staffId(),"BUSINESS_TODO_HANDLED",handoff?"HUMAN_HANDOFF_RECORD":"URGE_RECORD",id,Map.of("status","PENDING","version",version),Map.of("status","HANDLED","version",version+1,"workOrderId",orderId,"handlingConclusionRecorded",true));
        return handoff?view(handoff(id,false)):view(urge(id,false));
    }
    private void check(String status,Integer actual,Integer expected){
        if("INVALIDATED".equals(status))throw BusinessException.stateConflict("该催办已失效，请刷新列表");
        StaffIdentityGuard.version(actual,expected);if(!"PENDING".equals(status))throw BusinessException.stateConflict("该待办已处理");
    }
    private UrgeRecordEntity urge(Long id,boolean lock){var row=urges.selectOne(new QueryWrapper<UrgeRecordEntity>().eq("id",id).last(lock?"FOR UPDATE":""));if(row==null)throw BusinessException.notFound("催单待办不存在");return row;}
    private HumanHandoffRecordEntity handoff(Long id,boolean lock){var row=handoffs.selectOne(new QueryWrapper<HumanHandoffRecordEntity>().eq("id",id).last(lock?"FOR UPDATE":""));if(row==null)throw BusinessException.notFound("转人工待办不存在");return row;}
    private BusinessTodoVO view(UrgeRecordEntity r){return view(r.getId(),r.getWorkOrderId(),"URGE",r.getStatus(),r.getReason(),r.getCreatedAt(),r.getVersion(),r.getHandledBy(),r.getHandledAt(),r.getHandlingConclusion());}
    private BusinessTodoVO view(HumanHandoffRecordEntity r){return view(r.getId(),r.getWorkOrderId(),"HUMAN_HANDOFF",r.getStatus(),r.getReason(),r.getCreatedAt(),r.getVersion(),r.getHandledBy(),r.getHandledAt(),r.getHandlingConclusion());}
    private BusinessTodoVO view(Long id,Long order,String kind,String status,String reason,LocalDateTime created,Integer version,Long by,LocalDateTime at,String conclusion){boolean pending="PENDING".equals(status);return new BusinessTodoVO(id,order,kind,status,redact(order,Objects.toString(reason,"")),offset(created),version,pending?null:access.actor(by),pending?null:offset(at),pending||conclusion==null?null:redact(order,conclusion));}
    private String redact(Long orderId,String value){var w=orders.selectById(orderId);var r=w==null?null:repairs.selectById(w.getRepairRequestId());if(r!=null){var contact=contacts.view(r,true);if(contact.contactName()!=null&&!contact.contactName().isBlank())value=value.replace(contact.contactName(),"＊");if(contact.addressText()!=null&&!contact.addressText().isBlank())value=value.replace(contact.addressText(),"＊");return contacts.redactNote(value,r);}return value.replaceAll("(?<![0-9])([0-9]{3})[0-9]{4}([0-9]{4})(?![0-9])","$1****$2");}
    private OffsetDateTime offset(LocalDateTime value){return value==null?null:value.atOffset(ZoneOffset.UTC);}
    private void paging(int page,int size){if(page<1||size<1||size>100)throw BusinessException.validation("分页参数不正确");}
}
