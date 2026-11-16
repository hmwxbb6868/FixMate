package com.fixmate.service.admin;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;
public interface StaffAuthService {
    StaffPrincipal prepareLogin(StaffLoginDTO dto,String key,String ip);
    StaffTokenVO login(StaffPrincipal permit);
    StaffPrincipal refreshPrincipal(String token,boolean requireActiveFamily);
    StaffTokenVO refresh(StaffPrincipal principal,String key);
    void logout(StaffPrincipal principal,String key);
    void changePassword(StaffPrincipal principal,StaffPasswordChangeDTO dto);
    String secretHash(Object dto);
}
