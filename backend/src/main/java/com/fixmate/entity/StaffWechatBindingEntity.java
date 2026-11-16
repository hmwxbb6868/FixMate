package com.fixmate.entity;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
@TableName("staff_wechat_binding")
public class StaffWechatBindingEntity {
    @TableId(type=IdType.AUTO)
    private Long id;
    private Long customerId;
    private Long staffId;
    private Long invitationId;
    private String status;
    private String applicantName;
    private String applicantContactPhone;
    private Boolean contactPhoneVerified;
    private LocalDateTime appliedAt;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private Long revokedBy;
    private LocalDateTime revokedAt;
    private String decisionReason;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long value) { this.customerId=value; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long value) { this.staffId=value; }
    public Long getInvitationId() { return invitationId; }
    public void setInvitationId(Long value) { this.invitationId=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String value) { this.applicantName=value; }
    public String getApplicantContactPhone() { return applicantContactPhone; }
    public void setApplicantContactPhone(String value) { this.applicantContactPhone=value; }
    public Boolean getContactPhoneVerified() { return contactPhoneVerified; }
    public void setContactPhoneVerified(Boolean value) { this.contactPhoneVerified=value; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime value) { this.appliedAt=value; }
    public Long getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Long value) { this.confirmedBy=value; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime value) { this.confirmedAt=value; }
    public Long getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(Long value) { this.rejectedBy=value; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime value) { this.rejectedAt=value; }
    public Long getRevokedBy() { return revokedBy; }
    public void setRevokedBy(Long value) { this.revokedBy=value; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime value) { this.revokedAt=value; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String value) { this.decisionReason=value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { this.version=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt=value; }
}
