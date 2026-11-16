package com.fixmate.schedule;

import com.fixmate.entity.QuoteEntity;
import com.fixmate.service.common.QuoteExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 报价过期扫描：每 10 分钟一次，逐条在独立事务中处理。
 */
@Component
public class QuoteExpirationTask {

    private static final Logger log = LoggerFactory.getLogger(QuoteExpirationTask.class);

    private final QuoteExpirationService quoteExpirationService;

    public QuoteExpirationTask(QuoteExpirationService quoteExpirationService) {
        this.quoteExpirationService = quoteExpirationService;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT10M")
    public void expireQuotes() {
        for (QuoteEntity quote : quoteExpirationService.findExpiredCandidates()) {
            try {
                quoteExpirationService.expireOne(quote.getId());
            } catch (RuntimeException ex) {
                log.warn("expire quote failed for quote {}", quote.getId(), ex);
            }
        }
    }
}
