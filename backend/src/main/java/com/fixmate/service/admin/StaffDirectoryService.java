package com.fixmate.service.admin;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;
import java.util.List;

public interface StaffDirectoryService {
    StaffProfileVOPageVO list(StaffPrincipal actor,int page,int size);
    AdminCustomerVOPageVO customers(StaffPrincipal actor,int page,int size,String keyword,String status);
    List<StaffOptionVO> options(StaffPrincipal actor);
    StaffProfileVO create(StaffPrincipal actor,StaffCreateDTO input);
    StaffProfileVO status(StaffPrincipal actor,Long id,StaffStatusDTO input);
    void resetPassword(StaffPrincipal actor,Long id,StaffPasswordResetDTO input);
}
