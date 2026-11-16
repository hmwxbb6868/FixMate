package com.fixmate.service.admin;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;
public interface WorkAssignmentService {
    AssignmentVO claimLocked(com.fixmate.service.common.EmployeeWechatAccessService.EmployeeIdentity employee,com.fixmate.entity.WorkOrderEntity order);
    AssignmentVO assign(StaffPrincipal actor,Long orderId,AssignmentCreateDTO input);
    AssignmentVOPageVO history(StaffPrincipal actor,Long orderId,int page,int size);
    AssignmentHistoryDetailVO historyDetail(StaffPrincipal actor,Long id);
    AssignmentVO decideReturn(StaffPrincipal actor,Long id,AssignmentReturnDecisionDTO input);
    boolean canReleaseReturnForPublication(com.fixmate.entity.WorkOrderEntity order);
    void releaseReturnForPublication(com.fixmate.entity.WorkOrderEntity order,Long actor,Integer expectedAssignmentVersion,String reason);
    StaffTaskListItemVOPageVO tasks(Long customerId,int page,int size);
    StaffTaskListItemVO task(Long customerId,Long orderId);
    AssignmentVO act(Long customerId,Long id,AssignmentActionDTO input,String action);
    void requireOwn(Long customerId,Long assignmentId,String action);
    void revokeDisabledStaff(StaffPrincipal actor,Long staffId,String reason);
    com.fixmate.entity.WorkOrderAssignmentEntity currentAssignment(com.fixmate.entity.WorkOrderEntity order,boolean lock);
    com.fixmate.entity.WorkOrderAssignmentEntity displayAssignment(com.fixmate.entity.WorkOrderEntity order,boolean lock);
    boolean canAssignManual(com.fixmate.entity.WorkOrderEntity order);
    void finishForClosure(com.fixmate.entity.WorkOrderEntity order,Long actor,String reason);
    void releaseForCustomerCancellation(com.fixmate.entity.WorkOrderEntity order,Long customerId,String reason);
    AssignmentVO assignmentView(com.fixmate.entity.WorkOrderAssignmentEntity assignment);
    void endForCoordination(com.fixmate.entity.WorkOrderEntity order,Long actor,String reason);
}
