package com.fixmate.service.user;

import com.fixmate.dto.user.HumanHandoffCreateDTO;
import com.fixmate.dto.user.UrgeCreateDTO;

public interface UserWorkOrderActionService {

    void urge(Long userId, Long workOrderId, UrgeCreateDTO dto);
    void cancel(Long userId,Long workOrderId,Integer expectedVersion,String expectedVisitFee);

    void requestHumanHandoff(Long userId, Long workOrderId, HumanHandoffCreateDTO dto);
}
