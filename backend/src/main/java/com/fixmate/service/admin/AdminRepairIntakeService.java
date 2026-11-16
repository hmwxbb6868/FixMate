package com.fixmate.service.admin;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;

public interface AdminRepairIntakeService {
    RepairRecordListItemVOPageVO list(StaffPrincipal actor,int page,int size,String status,boolean pendingReview,String repairNo,boolean pendingCustomerConfirmation);
    AdminRepairRequestDetailVO detail(StaffPrincipal actor,Long id);
    AdminRepairRequestDetailVO triage(StaffPrincipal actor,Long id,ManualTriageDTO input);
    AdminRepairRequestDetailVO confirmContact(StaffPrincipal actor,Long id,ServiceContactConfirmationDTO input);
}
