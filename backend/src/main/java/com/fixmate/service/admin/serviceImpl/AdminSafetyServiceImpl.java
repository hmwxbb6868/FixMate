package com.fixmate.service.admin.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.AdminSafetyService;
import com.fixmate.service.common.ServiceContactProjection;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSafetyServiceImpl implements AdminSafetyService {
    private final SafetyEventMapper events;
    private final WorkOrderMapper orders;
    private final RepairRequestMapper repairs;
    private final StaffAccessService access;
    private final DatabaseClockMapper clock;
    private final AuditLogService audit;
    private final ObjectMapper json;
    private final ServiceContactProjection contacts;
    public AdminSafetyServiceImpl(SafetyEventMapper events,WorkOrderMapper orders,RepairRequestMapper repairs,StaffAccessService access,
            DatabaseClockMapper clock,AuditLogService audit,ObjectMapper json,ServiceContactProjection contacts) {
        this.events=events;this.orders=orders;this.repairs=repairs;this.access=access;this.clock=clock;this.audit=audit;this.json=json;this.contacts=contacts;
    }
    @Override public SafetyEventStaffVOPageVO list(StaffPrincipal actor,int page,int size,String status) {
        access.require(actor,"ADMIN","DISPATCHER","AUDITOR");StaffDirectoryServiceImpl.page(page,size);
        if(!Set.of("ALL","OPEN","RESOLVED").contains(status))throw BusinessException.validation("安全状态不正确");
        var filter=new LambdaQueryWrapper<SafetyEventEntity>().eq(!"ALL".equals(status),SafetyEventEntity::getStatus,status);
        var result=events.selectPage(new Page<>(page,size),filter.orderByDesc(SafetyEventEntity::getId));
        return new SafetyEventStaffVOPageVO(result.getRecords().stream().map(this::view).toList(),page,size,result.getTotal());
    }
    @Override @Transactional public SafetyEventStaffVO acknowledge(StaffPrincipal actor,Long id,ReasonCommandDTO input){return mutate(actor,id,input,false);}
    @Override @Transactional public SafetyEventStaffVO resolve(StaffPrincipal actor,Long id,ReasonCommandDTO input){return mutate(actor,id,input,true);}
    private SafetyEventStaffVO mutate(StaffPrincipal actor,Long id,ReasonCommandDTO input,boolean resolve) {
        access.requireLocked(actor,"ADMIN","DISPATCHER");var candidate=events.selectById(id);
        if(candidate==null)throw BusinessException.notFound("安全事件不存在");
        if(candidate.getWorkOrderId()!=null) {
            var order=orders.selectOne(new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,candidate.getWorkOrderId()).last("FOR UPDATE"));
            if(order==null||!order.getRepairRequestId().equals(candidate.getRepairRequestId()))throw BusinessException.stateConflict("安全事件关联异常");
        } else {
            if(repairs.selectOne(new LambdaQueryWrapper<RepairRequestEntity>().eq(RepairRequestEntity::getId,candidate.getRepairRequestId()).last("FOR UPDATE"))==null)
                throw BusinessException.stateConflict("安全报修关联异常");
        }
        var event=events.selectOne(new LambdaQueryWrapper<SafetyEventEntity>().eq(SafetyEventEntity::getId,id).last("FOR UPDATE"));
        if(!Objects.equals(event.getWorkOrderId(),candidate.getWorkOrderId()))throw BusinessException.stateConflict("事件关联已变化，请刷新");
        StaffIdentityGuard.version(event.getVersion(),input.expectedVersion());
        if(!"OPEN".equals(event.getStatus()))throw BusinessException.stateConflict("安全事件已处理");
        if(resolve&&event.getAcknowledgedBy()==null)throw BusinessException.stateConflict("请先完成人工安全确认");
        if(!resolve&&event.getAcknowledgedBy()!=null)throw BusinessException.stateConflict("安全事件已确认");
        var repair=repairs.selectById(event.getRepairRequestId());
        if(repair==null)throw BusinessException.stateConflict("安全报修已不可用");
        String reason=contacts.redactNote(StaffIdentityGuard.text(input.reason(),2,1000,"安全处理依据"),repair);var at=clock.now();
        var update=new UpdateWrapper<SafetyEventEntity>().eq("id",id).eq("version",input.expectedVersion()).eq("status","OPEN")
                .set("version",input.expectedVersion()+1).set("updated_at",at);
        if(resolve){update.set("status","RESOLVED").set("resolved_by",actor.staffId()).set("resolved_at",at).set("resolution_basis",reason);
            event.setStatus("RESOLVED");event.setResolvedBy(actor.staffId());event.setResolvedAt(at);event.setResolutionBasis(reason);}
        else {update.set("acknowledged_by",actor.staffId()).set("acknowledged_at",at).set("acknowledgement_note",reason);
            event.setAcknowledgedBy(actor.staffId());event.setAcknowledgedAt(at);event.setAcknowledgementNote(reason);}
        if(events.update(null,update)!=1)throw BusinessException.versionConflict("安全事件版本已变化");event.setVersion(input.expectedVersion()+1);
        audit.record("STAFF",actor.staffId(),resolve?"SAFETY_RESOLVED":"SAFETY_ACKNOWLEDGED","SAFETY_EVENT",id,
                Map.of("version",input.expectedVersion()),Map.of("version",event.getVersion(),"reason",reason));
        return view(event);
    }
    private SafetyEventStaffVO view(SafetyEventEntity e) {
        List<SafetyRuleDetail> rules;
        try{rules=json.readValue(e.getMatchedRules(),new TypeReference<List<SafetyRuleDetail>>(){});if(rules==null)throw new IllegalArgumentException();}
        catch(Exception ex){throw new IllegalStateException("安全指引存储异常，请人工核查");}
        return new SafetyEventStaffVO(e.getId(),e.getWorkOrderId()==null?null:e.getWorkOrderId().toString(),e.getStatus(),rules,e.getVersion(),offset(e.getCreatedAt()),
                offset(e.getAcknowledgedAt()),access.actor(e.getAcknowledgedBy()),e.getAcknowledgementNote(),access.actor(e.getResolvedBy()),offset(e.getResolvedAt()),e.getResolutionBasis());
    }
    private OffsetDateTime offset(LocalDateTime value){return value==null?null:value.atOffset(ZoneOffset.UTC);}
}
