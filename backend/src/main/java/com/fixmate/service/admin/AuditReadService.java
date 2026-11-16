package com.fixmate.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.AuditLogEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.common.AuditMetadataProjection;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(isolation=Isolation.REPEATABLE_READ)
public class AuditReadService {
    private final AuditLogMapper rows;private final WorkOrderMapper orders;private final StaffAccessService access;private final AuditMetadataProjection projection;
    public AuditReadService(AuditLogMapper rows,WorkOrderMapper orders,StaffAccessService access,AuditMetadataProjection projection){this.rows=rows;this.orders=orders;this.access=access;this.projection=projection;}
    public AuditSummaryVOPageVO list(StaffPrincipal actor,int page,int size,Long workOrderId){
        var staff=access.require(actor,"ADMIN","AUDITOR","DISPATCHER");if(page<1||size<1||size>100||workOrderId!=null&&workOrderId<=0)throw BusinessException.validation("审计筛选参数不正确");
        if(workOrderId!=null&&orders.selectById(workOrderId)==null)throw BusinessException.notFound("工单不存在");
        var q=new QueryWrapper<AuditLogEntity>();if(workOrderId!=null||"DISPATCHER".equals(staff.getRole()))q.apply(scopeSql(workOrderId),workOrderId==null?new Object[]{}:new Object[]{workOrderId});
        var result=rows.selectPage(new Page<>(page,size),q.orderByDesc("id"));return new AuditSummaryVOPageVO(result.getRecords().stream().map(this::summary).toList(),page,size,result.getTotal());
    }
    public AuditDetailVO detail(StaffPrincipal actor,Long id){
        var staff=access.require(actor,"ADMIN","AUDITOR","DISPATCHER");var q=new QueryWrapper<AuditLogEntity>().eq("id",id);
        if("DISPATCHER".equals(staff.getRole()))q.apply(scopeSql(null));var row=rows.selectOne(q);if(row==null)throw BusinessException.notFound("授权范围内未找到审计记录");
        return new AuditDetailVO(summary(row),projection.summary(row.getBeforeState()),projection.summary(row.getAfterState()),proxy(row));
    }
    private String proxy(AuditLogEntity row){
        if(!"STAFF".equals(row.getActorType())||!Set.of("SERVICE_RECORD_SUBMITTED","WORK_ORDER_COMPLETION_SUBMITTED","QUOTE_DRAFT_CREATED","QUOTE_DRAFT_EDITED","QUOTE_SUBMITTED").contains(row.getAction()))return null;
        var id=projection.proxy(row.getBeforeState(),row.getAfterState());return Objects.equals(id,row.getActorId()==null?null:row.getActorId().toString())?null:id;
    }
    private AuditSummaryVO summary(AuditLogEntity row){
        if(!Set.of("CUSTOMER","STAFF","SYSTEM").contains(row.getActorType()))throw BusinessException.stateConflict("历史审计操作者类型异常，请核对");
        return new AuditSummaryVO(row.getId(),row.getActorType(),row.getActorId()==null?null:row.getActorId().toString(),row.getAction(),row.getTargetType(),projection.idText(row.getTargetId()),projection.reason(row.getBeforeState(),row.getAfterState()),Objects.toString(row.getTraceId(),""),row.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
    /** 精确字符串ID关联，禁止MySQL隐式数值转换将1xxx误匹配成1；不从审计JSON猜资源关系。 */
    private String scopeSql(Long order){
        var parts=new ArrayList<String>();String filter=order==null?"":" AND w.id={0}";
        parts.add("(target_type='WORK_ORDER' AND EXISTS (SELECT 1 FROM work_order w WHERE CAST(w.id AS CHAR)=audit_log.target_id"+filter+"))");
        parts.add("(target_type='REPAIR_REQUEST' AND EXISTS (SELECT 1 FROM repair_request r WHERE CAST(r.id AS CHAR)=audit_log.target_id"+(order==null?"":" AND r.work_order_id={0}")+"))");
        var tables=Map.of("QUOTE","quote","WORK_ASSIGNMENT","work_order_assignment","JOB_POOL_ENTRY","job_pool_entry",
                "APPOINTMENT","appointment","SERVICE_RECORD","service_record","SAFETY_EVENT","safety_event",
                "URGE_RECORD","urge_record","HUMAN_HANDOFF_RECORD","human_handoff_record");
        for(var entry:new TreeMap<>(tables).entrySet())parts.add("(target_type='"+entry.getKey()+"' AND EXISTS (SELECT 1 FROM "+entry.getValue()+" child JOIN work_order w ON w.id=child.work_order_id WHERE CAST(child.id AS CHAR)=audit_log.target_id"+filter+"))");
        return "("+String.join(" OR ",parts)+")";
    }
}
