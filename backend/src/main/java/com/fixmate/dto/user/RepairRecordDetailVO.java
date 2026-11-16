package com.fixmate.dto.user;

import java.util.List;

public record RepairRecordDetailVO(com.fixmate.dto.admin.StaffIdentityContracts.PreferredVisitVO preferredVisit,String visitPolicy,RepairRequestSnapshotVO repairRequest, Long workOrderId,
        String workOrderStatus, Integer workOrderVersion, List<String> allowedActions,String cancellationFeeAmount) {}
