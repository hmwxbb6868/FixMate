package com.fixmate.service.admin;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;
public interface AdminSafetyService {
    SafetyEventStaffVOPageVO list(StaffPrincipal actor,int page,int size,String status);
    SafetyEventStaffVO acknowledge(StaffPrincipal actor,Long id,ReasonCommandDTO input);
    SafetyEventStaffVO resolve(StaffPrincipal actor,Long id,ReasonCommandDTO input);
}
