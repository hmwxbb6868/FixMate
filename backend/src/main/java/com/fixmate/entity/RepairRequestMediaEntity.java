package com.fixmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("repair_request_media")
public class RepairRequestMediaEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long repairRequestId;
    private Long fileId;
    private String mediaPurpose;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepairRequestId() {
        return repairRequestId;
    }

    public void setRepairRequestId(Long repairRequestId) {
        this.repairRequestId = repairRequestId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getMediaPurpose() {
        return mediaPurpose;
    }

    public void setMediaPurpose(String mediaPurpose) {
        this.mediaPurpose = mediaPurpose;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
