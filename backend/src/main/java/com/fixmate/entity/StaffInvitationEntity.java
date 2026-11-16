package com.fixmate.entity;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
@TableName("staff_invitation")
public class StaffInvitationEntity {
    @TableId(type=IdType.AUTO)
    private Long id;
    private Long staffId;
    private String codeHash;
    private String status;
    private LocalDateTime expiresAt;
    private Long createdBy;
    private LocalDateTime consumedAt;
    private Long consumedBindingId;
    private Long revokedBy;
    private LocalDateTime revokedAt;
    private String revocationReason;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long value) { this.staffId=value; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String value) { this.codeHash=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { this.expiresAt=value; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long value) { this.createdBy=value; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime value) { this.consumedAt=value; }
    public Long getConsumedBindingId() { return consumedBindingId; }
    public void setConsumedBindingId(Long value) { this.consumedBindingId=value; }
    public Long getRevokedBy() { return revokedBy; }
    public void setRevokedBy(Long value) { this.revokedBy=value; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime value) { this.revokedAt=value; }
    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String value) { this.revocationReason=value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { this.version=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt=value; }
}
