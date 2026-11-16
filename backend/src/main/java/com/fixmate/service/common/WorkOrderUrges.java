package com.fixmate.service.common;

import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.mapper.JobPoolEntryMapper;
import com.fixmate.mapper.UrgeRecordMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderUrges {
    private final UrgeRecordMapper urges;
    private final JobPoolEntryMapper publications;

    public WorkOrderUrges(UrgeRecordMapper urges,JobPoolEntryMapper publications) {
        this.urges=urges;
        this.publications=publications;
    }

    public boolean awaitingRedispatchAfterWithdrawal(WorkOrderEntity order) {
        if(!"WAITING_ASSIGNMENT".equals(order.getStatus())||order.getCurrentAssignmentId()!=null
                ||order.getTechnicianId()!=null||order.getTakeoverBy()!=null)return false;
        var publication=publications.latestForOrder(order.getId());
        return publication!=null&&"WITHDRAWN".equals(publication.getStatus());
    }

    /** Call within the transaction that closes the work order or withdraws its publication. */
    public void invalidatePending(Long orderId,LocalDateTime at,String conclusion) {
        urges.invalidatePending(orderId,at,conclusion);
    }
}
