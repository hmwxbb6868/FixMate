package com.fixmate.service.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 报价金额计算唯一入口。冻结规则 10.4：
 * lineAmount = round(unitPrice × quantity, 2, HALF_UP)
 * subtotal   = Σ lineAmount
 * total      = confirmedAmountBefore + subtotal
 */
@Service
public class PricingCalculationService {

    public BigDecimal lineAmount(BigDecimal unitPrice, BigDecimal quantity) {
        if (unitPrice == null || quantity == null) {
            throw new IllegalArgumentException("unitPrice/quantity required");
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        return unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal subtotal(List<BigDecimal> lineAmounts) {
        return lineAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal total(BigDecimal confirmedAmountBefore, BigDecimal subtotal) {
        BigDecimal before = confirmedAmountBefore == null ? BigDecimal.ZERO : confirmedAmountBefore;
        return before.add(subtotal).setScale(2, RoundingMode.HALF_UP);
    }
}
