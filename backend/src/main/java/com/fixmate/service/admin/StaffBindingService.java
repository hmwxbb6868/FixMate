package com.fixmate.service.admin;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;

public interface StaffBindingService {
    StaffInvitationIssuedVO issue(StaffPrincipal actor,Long staffId,StaffInvitationCreateDTO input);
    NewTechnicianInvitationIssuedVO issueNewTechnician(StaffPrincipal actor);
    StaffInvitationVOPageVO invitations(StaffPrincipal actor,int page,int size,Long staffId,String status);
    StaffInvitationVO invitation(StaffPrincipal actor,Long id);
    StaffInvitationVO revokeInvitation(StaffPrincipal actor,Long id,ReasonCommandDTO input);
    StaffBindingVO apply(Long customerId,StaffBindingApplyDTO input,String ip);
    StaffBindingVO current(Long customerId);
    WorkspaceAccessVO workspace(Long customerId);
    StaffBindingVOPageVO bindings(StaffPrincipal actor,int page,int size,String status);
    StaffBindingVO binding(StaffPrincipal actor,Long id);
    StaffBindingVO decide(StaffPrincipal actor,Long id,StaffBindingDecisionDTO input,String decision);
    void requireCustomer(Long customerId);
}
