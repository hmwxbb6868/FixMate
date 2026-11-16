package com.fixmate.service.user;

import com.fixmate.dto.user.WorkOrderDetailVO;

public interface UserRepairConvertService {

    WorkOrderDetailVO convertToWorkOrder(Long userId, Long repairRequestId, Integer expectedVersion);
}
