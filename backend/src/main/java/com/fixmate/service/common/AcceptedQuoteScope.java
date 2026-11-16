package com.fixmate.service.common;

import com.fixmate.entity.QuoteEntity;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;

/** 只校验已接受范围，不改价、不恢复未接受报价；金额仍交给唯一计价服务。 */
@Service
public class AcceptedQuoteScope {
    private final PricingCalculationService pricing;
    public AcceptedQuoteScope(PricingCalculationService pricing){this.pricing=pricing;}
    public boolean valid(List<QuoteEntity> quotes) {
        var all=new HashMap<Long,QuoteEntity>();
        for(var q:quotes)if(q.getId()==null||all.put(q.getId(),q)!=null)return false;
        var accepted=quotes.stream().filter(q->"ACCEPTED".equals(q.getStatus()))
                .sorted(Comparator.comparing(QuoteEntity::getVersionNo,Comparator.nullsFirst(Comparator.naturalOrder()))).toList();
        if(accepted.isEmpty())return false;
        QuoteEntity latestAccepted=null;BigDecimal total=BigDecimal.ZERO;Integer lastVersion=null;
        for(var q:accepted) {
            if(q.getVersionNo()==null||q.getVersionNo()<1||Objects.equals(lastVersion,q.getVersionNo())
                    ||!"CNY".equals(q.getCurrency())
                    ||q.getSubtotalAmount()==null||q.getSubtotalAmount().signum()<0||q.getTotalAmount()==null
                    ||q.getConfirmedAmountBefore()==null||q.getConfirmedAmountBefore().compareTo(total)!=0)return false;
            if(!continuous(q,all))return false;
            if(latestAccepted==null) {
                if(!"INITIAL".equals(q.getQuoteType())||q.getBaseQuoteId()!=null)return false;
            } else if(!"ADDITIONAL".equals(q.getQuoteType())||!latestAccepted.getId().equals(q.getBaseQuoteId())
                    ||!latestAccepted.getWorkOrderId().equals(q.getWorkOrderId()))return false;
            total=pricing.total(total,q.getSubtotalAmount());
            if(total.compareTo(q.getTotalAmount())!=0)return false;
            lastVersion=q.getVersionNo();
            latestAccepted=q;
        }
        return true;
    }
    public boolean canAbandonLatestAdditional(List<QuoteEntity> quotes){
        if(!valid(quotes))return false;
        var last=quotes.stream().max(Comparator.comparing(QuoteEntity::getVersionNo)).orElseThrow();
        if(!"ADDITIONAL".equals(last.getQuoteType())||!Set.of("DRAFT","SUBMITTED","REJECTED","EXPIRED","WITHDRAWN").contains(last.getStatus()))return false;
        var accepted=quotes.stream().filter(q->"ACCEPTED".equals(q.getStatus())).max(Comparator.comparing(QuoteEntity::getVersionNo)).orElseThrow();
        var all=new HashMap<Long,QuoteEntity>();for(var q:quotes)all.put(q.getId(),q);
        return "CNY".equals(last.getCurrency())&&accepted.getId().equals(last.getBaseQuoteId())&&continuous(last,all)
                &&last.getConfirmedAmountBefore()!=null&&last.getConfirmedAmountBefore().compareTo(accepted.getTotalAmount())==0;
    }
    /** 冻结10.5，与原用户接受报价的连续链规则一致，不把“更早版本”当作“上一版本”。 */
    private boolean continuous(QuoteEntity quote,Map<Long,QuoteEntity> all){
        if(quote.getVersionNo()==null||quote.getVersionNo()<1)return false;
        var cursor=quote;int version=quote.getVersionNo();var visited=new HashSet<Long>();
        while(version>1){
            if(cursor.getPreviousQuoteId()==null||!visited.add(cursor.getPreviousQuoteId()))return false;
            cursor=all.get(cursor.getPreviousQuoteId());
            if(cursor==null||!Objects.equals(cursor.getWorkOrderId(),quote.getWorkOrderId())||cursor.getVersionNo()==null||cursor.getVersionNo()!=version-1)return false;
            version--;
        }
        return cursor.getPreviousQuoteId()==null;
    }
}
