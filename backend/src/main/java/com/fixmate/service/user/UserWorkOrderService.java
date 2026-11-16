package com.fixmate.service.user;

import com.fixmate.common.api.PageVO;
import com.fixmate.dto.user.WorkOrderAcceptanceDTO;
import com.fixmate.dto.user.WorkOrderDetailVO;
import com.fixmate.dto.user.WorkOrderListItemVO;
import com.fixmate.entity.WorkOrderEntity;

public interface UserWorkOrderService {

    PageVO<WorkOrderListItemVO> list(Long userId, int page, int size, String status);

    WorkOrderDetailVO detail(Long userId, Long workOrderId);

    WorkOrderDetailVO accept(Long userId, Long workOrderId, WorkOrderAcceptanceDTO dto);

    /** 供同模块的转换服务复用详情装配。 */
    WorkOrderDetailVO buildDetail(WorkOrderEntity order);
}
