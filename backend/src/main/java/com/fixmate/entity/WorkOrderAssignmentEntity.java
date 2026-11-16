package com.fixmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("work_order_assignment")
public class WorkOrderAssignmentEntity {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String allocationSource;
    public String getAllocationSource(){return allocationSource;}
    public void setAllocationSource(String v){allocationSource=v;}
    private Long workOrderId;
    private Long technicianId;
    private String status;
    private Long assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private Long endedBy;
    private LocalDateTime endedAt;
    private String endReason;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId(){return id;}
    public void setId(Long value){id=value;}
    public Long getWorkOrderId(){return workOrderId;}
    public void setWorkOrderId(Long value){workOrderId=value;}
    public Long getTechnicianId(){return technicianId;}
    public void setTechnicianId(Long value){technicianId=value;}
    public String getStatus(){return status;}
    public void setStatus(String value){status=value;}
    public Long getAssignedBy(){return assignedBy;}
    public void setAssignedBy(Long value){assignedBy=value;}
    public LocalDateTime getAssignedAt(){return assignedAt;}
    public void setAssignedAt(LocalDateTime value){assignedAt=value;}
    public LocalDateTime getAcceptedAt(){return acceptedAt;}
    public void setAcceptedAt(LocalDateTime value){acceptedAt=value;}
    public Long getEndedBy(){return endedBy;}
    public void setEndedBy(Long value){endedBy=value;}
    public LocalDateTime getEndedAt(){return endedAt;}
    public void setEndedAt(LocalDateTime value){endedAt=value;}
    public String getEndReason(){return endReason;}
    public void setEndReason(String value){endReason=value;}
    public Integer getVersion(){return version;}
    public void setVersion(Integer value){version=value;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
