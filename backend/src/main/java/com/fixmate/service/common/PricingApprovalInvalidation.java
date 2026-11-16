package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.entity.*;
import com.fixmate.mapper.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** 配置写已持员工目录锁；先锁受影响根→报价→审批，再允许配置行修改。 */
@Service
public class PricingApprovalInvalidation {
    private final WorkOrderMapper orders;private final QuoteMapper quotes;private final QuoteOverrideApprovalMapper approvals;
    private final DatabaseClockMapper clock;private final AuditLogService audit;
    public PricingApprovalInvalidation(WorkOrderMapper orders,QuoteMapper quotes,QuoteOverrideApprovalMapper approvals,DatabaseClockMapper clock,AuditLogService audit){this.orders=orders;this.quotes=quotes;this.approvals=approvals;this.clock=clock;this.audit=audit;}
    public void invalidate(Long serviceId,Long ruleId){
        String filter=ruleId!=null?"qi.pricing_rule_id={0}":"qi.service_item_id={0}";Long id=ruleId!=null?ruleId:serviceId;
        var roots=orders.selectList(new QueryWrapper<WorkOrderEntity>().apply("id IN (SELECT q.work_order_id FROM quote q JOIN quote_item qi ON qi.quote_id=q.id AND qi.content_revision=q.item_revision WHERE q.status='DRAFT' AND "+filter+")",id).orderByAsc("id").last("FOR UPDATE"));
        for(var root:roots)for(var q:quotes.selectList(new QueryWrapper<QuoteEntity>().eq("work_order_id",root.getId()).eq("status","DRAFT").orderByAsc("id").last("FOR UPDATE"))){
            for(var a:approvals.selectList(new QueryWrapper<QuoteOverrideApprovalEntity>().eq("quote_id",q.getId()).in("status",List.of("PENDING","APPROVED")).orderByAsc("id").last("FOR UPDATE"))){
                a.setStatus("INVALIDATED");a.setInvalidatedAt(clock.now());a.setUpdatedAt(clock.now());a.setVersion(a.getVersion()+1);approvals.updateById(a);
                audit.recordSystem("QUOTE_OVERRIDE_INVALIDATED","QUOTE",q.getId(),Map.of("approvalId",a.getId()),Map.of("reason","引用服务或价格配置变更"));
            }
        }
    }
}
