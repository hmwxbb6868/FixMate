package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.entity.*;
import com.fixmate.mapper.*;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CustomerCancellationPolicy {
    public static final BigDecimal STANDARD_VISIT_FEE = new BigDecimal("50.00");
    private static final Set<String> INSPECTION_STATES=Set.of("IN_INSPECTION","WAITING_QUOTE","WAITING_QUOTE_CONFIRMATION");
    private final QuoteMapper quotes;
    private final ServiceRecordMapper records;
    private final WorkOrderStatusLogMapper logs;

    public CustomerCancellationPolicy(QuoteMapper quotes,ServiceRecordMapper records,
                                      WorkOrderStatusLogMapper logs) {
        this.quotes=quotes;this.records=records;this.logs=logs;
    }

    public boolean canCancelNow(WorkOrderEntity order) {
        return eligible(order)&&!started(order);
    }

    private boolean eligible(WorkOrderEntity order) {
        return order!=null&&WorkOrderStatus.valueOf(order.getStatus()).canTransitionTo(WorkOrderStatus.CANCELLED)
                &&quotes.selectCount(new QueryWrapper<QuoteEntity>().eq("work_order_id",order.getId()).eq("status","ACCEPTED"))==0;
    }

    private boolean started(WorkOrderEntity order) {
        return order.getVisitFeeAmount()!=null||INSPECTION_STATES.contains(order.getStatus())
                ||inspection(order.getId())!=null
                ||records.selectCount(new QueryWrapper<ServiceRecordEntity>().eq("work_order_id",order.getId()).isNotNull("submitted_at"))>0;
    }

    private WorkOrderStatusLogEntity inspection(Long orderId) {
        return logs.selectOne(new QueryWrapper<WorkOrderStatusLogEntity>().eq("work_order_id",orderId)
                .eq("to_status","IN_INSPECTION").orderByAsc("id").last("LIMIT 1"));
    }

    public BigDecimal visitFee(WorkOrderEntity order) {
        return order.getVisitFeeAmount()!=null?order.getVisitFeeAmount():STANDARD_VISIT_FEE;
    }

    public String action(WorkOrderEntity order) {
        if(!eligible(order))return null;
        return started(order)?"CANCEL_WITH_VISIT_FEE":"CANCEL_WORK_ORDER";
    }

    public String cancellationAmount(WorkOrderEntity order,String action) {
        if(order==null)return null;
        BigDecimal amount=order.getCancellationFeeAmount();
        if(amount==null&&"CANCEL_WITH_VISIT_FEE".equals(action))amount=visitFee(order);
        return amount==null?null:amount.toPlainString();
    }
}
