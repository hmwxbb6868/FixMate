package com.fixmate.service.user;

import com.fixmate.dto.user.QuoteAcceptDTO;
import com.fixmate.dto.user.QuoteRejectDTO;
import com.fixmate.dto.user.QuoteVO;

public interface UserQuoteService {

    QuoteVO accept(Long userId, Long quoteId, QuoteAcceptDTO dto);

    QuoteVO reject(Long userId, Long quoteId, QuoteRejectDTO dto);
}
