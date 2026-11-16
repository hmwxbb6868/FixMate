package com.fixmate.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.dto.admin.StaffIdentityContracts.DashboardVO;
import com.fixmate.dto.admin.StaffIdentityContracts.AdminFollowUpVO;
import com.fixmate.entity.*;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 同一只读业务快照统计，generatedAt取数据库UTC时间；无AI、无隐式业务迁移。 */
@Service
public class AdminDashboardService {
    private final StaffAccessService access;private final WorkOrderMapper orders;private final RepairRequestMapper repairs;
    private final SafetyEventMapper safety;private final HumanHandoffRecordMapper handoffs;private final UrgeRecordMapper urges;
    private final QuoteMapper quotes;private final QuoteOverrideApprovalMapper approvals;private final AppointmentMapper appointments;
    private final WorkOrderAssignmentMapper assignments;private final DatabaseClockMapper clock;
    private final AdminFollowUpQueryMapper followUps;
    public AdminDashboardService(StaffAccessService access,WorkOrderMapper orders,RepairRequestMapper repairs,SafetyEventMapper safety,HumanHandoffRecordMapper handoffs,UrgeRecordMapper urges,QuoteMapper quotes,QuoteOverrideApprovalMapper approvals,AppointmentMapper appointments,WorkOrderAssignmentMapper assignments,DatabaseClockMapper clock,AdminFollowUpQueryMapper followUps){this.access=access;this.orders=orders;this.repairs=repairs;this.safety=safety;this.handoffs=handoffs;this.urges=urges;this.quotes=quotes;this.approvals=approvals;this.appointments=appointments;this.assignments=assignments;this.clock=clock;this.followUps=followUps;}
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public DashboardVO get(StaffPrincipal actor){
        access.require(actor,"ADMIN","DISPATCHER","AUDITOR");var now=clock.now();var distribution=new LinkedHashMap<String,Integer>();
        for(var status:WorkOrderStatus.values())distribution.put(status.name(),number(orders.selectCount(new QueryWrapper<WorkOrderEntity>().eq("status",status.name()))));
        int manual=number((long)distribution.get("WAITING_MANUAL_REVIEW")+repairs.selectCount(new QueryWrapper<RepairRequestEntity>().isNull("work_order_id").in("status",List.of("SUBMITTED","HUMAN_HANDOFF"))));
        int unresolved=number(safety.selectCount(new QueryWrapper<SafetyEventEntity>().ne("status","RESOLVED")));
        int handoff=number(handoffs.selectCount(new QueryWrapper<HumanHandoffRecordEntity>().eq("status","PENDING")));
        int urge=number(urges.selectCount(new QueryWrapper<UrgeRecordEntity>().eq("status","PENDING")));
        int confirmation=number(quotes.selectCount(new QueryWrapper<QuoteEntity>().eq("status","SUBMITTED").apply("EXISTS (SELECT 1 FROM work_order w WHERE w.id=quote.work_order_id AND w.status IN ('WAITING_QUOTE_CONFIRMATION','PAUSED_FOR_REQUOTE'))")));
        int approval=number(approvals.selectCount(new QueryWrapper<QuoteOverrideApprovalEntity>().eq("status","PENDING").apply("EXISTS (SELECT 1 FROM quote q WHERE q.id=quote_override_approval.quote_id AND q.status='DRAFT' AND q.expires_at>{0})",now)));
        int upcoming=number(appointments.selectCount(new QueryWrapper<AppointmentEntity>().eq("status","CONFIRMED").ge("scheduled_start_at",now).lt("scheduled_start_at",now.plusDays(1)).apply("EXISTS (SELECT 1 FROM work_order w WHERE w.id=appointment.work_order_id AND w.status='APPOINTED')")));
        int missed=number(appointments.selectCount(new QueryWrapper<AppointmentEntity>().eq("status","MISSED").apply("EXISTS (SELECT 1 FROM work_order w WHERE w.id=appointment.work_order_id AND w.status='WAITING_APPOINTMENT')").apply("id=(SELECT MAX(ap.id) FROM appointment ap WHERE ap.work_order_id=appointment.work_order_id)")));
        int returned=number(assignments.selectCount(new QueryWrapper<WorkOrderAssignmentEntity>().eq("status","RETURN_REQUESTED").apply("EXISTS (SELECT 1 FROM work_order w WHERE w.id=work_order_assignment.work_order_id AND w.current_assignment_id=work_order_assignment.id AND w.status NOT IN ('COMPLETED','CANCELLED','CLOSED_UNRESOLVED'))")));
        var overdue=followUps.overdue(now,now.minusHours(2),now.minusHours(12),now.minusHours(24)).stream()
                .map(row->new AdminFollowUpVO(row.kind(),row.targetId(),row.referenceNo(),row.waitingFor(),
                        row.waitingSince().atOffset(ZoneOffset.UTC),row.categoryTotal())).toList();
        return new DashboardVO(manual,unresolved,handoff,urge,distribution.get("WAITING_ASSIGNMENT"),confirmation,approval,upcoming,missed,returned,distribution.get("PAUSED_FOR_REQUOTE"),distribution,now.atOffset(ZoneOffset.UTC),false,overdue);
    }
    private int number(Long value){return Math.toIntExact(value);}
}
