package com.fixmate.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("job_pool_entry")
public class JobPoolEntryEntity {
 @TableId(type=IdType.AUTO)
 private Long id;
 public Long getId(){return id;}
 public void setId(Long v){id=v;}
 private Long workOrderId;
 public Long getWorkOrderId(){return workOrderId;}
 public void setWorkOrderId(Long v){workOrderId=v;}
 private String status;
 public String getStatus(){return status;}
 public void setStatus(String v){status=v;}
 private Long publishedBy;
 public Long getPublishedBy(){return publishedBy;}
 public void setPublishedBy(Long v){publishedBy=v;}
 private LocalDateTime publishedAt;
 public LocalDateTime getPublishedAt(){return publishedAt;}
 public void setPublishedAt(LocalDateTime v){publishedAt=v;}
 private Long claimedBindingId;
 public Long getClaimedBindingId(){return claimedBindingId;}
 public void setClaimedBindingId(Long v){claimedBindingId=v;}
 private Integer claimedBindingVersion;
 public Integer getClaimedBindingVersion(){return claimedBindingVersion;}
 public void setClaimedBindingVersion(Integer v){claimedBindingVersion=v;}
 private Long claimedBy;
 public Long getClaimedBy(){return claimedBy;}
 public void setClaimedBy(Long v){claimedBy=v;}
 private Long assignmentId;
 public Long getAssignmentId(){return assignmentId;}
 public void setAssignmentId(Long v){assignmentId=v;}
 private LocalDateTime claimedAt;
 public LocalDateTime getClaimedAt(){return claimedAt;}
 public void setClaimedAt(LocalDateTime v){claimedAt=v;}
 private Long withdrawnBy;
 public Long getWithdrawnBy(){return withdrawnBy;}
 public void setWithdrawnBy(Long v){withdrawnBy=v;}
 private LocalDateTime withdrawnAt;
 public LocalDateTime getWithdrawnAt(){return withdrawnAt;}
 public void setWithdrawnAt(LocalDateTime v){withdrawnAt=v;}
 private String reason;
 public String getReason(){return reason;}
 public void setReason(String v){reason=v;}
 private Integer version;
 public Integer getVersion(){return version;}
 public void setVersion(Integer v){version=v;}
 private LocalDateTime createdAt;
 public LocalDateTime getCreatedAt(){return createdAt;}
 public void setCreatedAt(LocalDateTime v){createdAt=v;}
 private LocalDateTime updatedAt;
 public LocalDateTime getUpdatedAt(){return updatedAt;}
 public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
