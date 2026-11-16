package com.fixmate.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.entity.FileAssetEntity;
import com.fixmate.entity.RepairRequestMediaEntity;
import com.fixmate.mapper.FileAssetMapper;
import com.fixmate.mapper.RepairRequestMediaMapper;
import com.fixmate.storage.ObjectStorageService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 临时文件清理：超过 24 小时仍未绑定业务的 TEMP 文件先删除 OSS 对象，再把数据库状态置为 DELETED。
 * 逻辑停用，不物理删除记录。
 *
 * 清理与提交激活会并发，因此：
 * 1. 删除前先用条件更新把 TEMP 原子认领成 DELETING，认领失败说明已被业务激活或别的实例拿走；
 * 2. 仍被业务表引用的文件一律跳过，避免删掉刚绑定但状态尚未回写的对象。
 */
@Component
public class TempFileCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(TempFileCleanupTask.class);
    private static final int TEMP_RETENTION_HOURS = 24;

    private final FileAssetMapper fileAssetMapper;
    private final RepairRequestMediaMapper mediaMapper;
    private final ObjectStorageService storageService;
    private final com.fixmate.mapper.CustomerUserMapper customers;

    public TempFileCleanupTask(FileAssetMapper fileAssetMapper,
                               RepairRequestMediaMapper mediaMapper,
                               ObjectStorageService storageService, com.fixmate.mapper.CustomerUserMapper customers) {
        this.fileAssetMapper = fileAssetMapper;
        this.mediaMapper = mediaMapper;
        this.storageService = storageService;
        this.customers = customers;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    public void cleanupExpiredTempFiles() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime threshold = now.minusHours(TEMP_RETENTION_HOURS);
        List<FileAssetEntity> expired = fileAssetMapper.selectList(new LambdaQueryWrapper<FileAssetEntity>()
                .and(q -> q.eq(FileAssetEntity::getStatus, "TEMP").le(FileAssetEntity::getCreatedAt, threshold)
                        .or(r -> r.eq(FileAssetEntity::getStatus, "DELETING")
                                .le(FileAssetEntity::getUpdatedAt, now.minusHours(1))))
                .last("limit 200"));
        for (FileAssetEntity file : expired) {
            if (isReferenced(file.getId())) {
                continue;
            }
            int claimed = fileAssetMapper.update(null, new UpdateWrapper<FileAssetEntity>()
                    .eq("id", file.getId())
                    .eq("status", file.getStatus())
                    .eq("updated_at", file.getUpdatedAt())
                    .set("status", "DELETING")
                    .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
            if (claimed == 0) {
                continue;
            }
            try {
                storageService.deleteObject(file.getStorageKey());
            } catch (RuntimeException ex) {
                log.warn("delete oss object failed for file {}, revert to TEMP", file.getId(), ex);
                fileAssetMapper.update(null, new UpdateWrapper<FileAssetEntity>()
                        .eq("id", file.getId())
                        .eq("status", "DELETING")
                        .set("status", "TEMP")
                        .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
                continue;
            }
            fileAssetMapper.update(null, new UpdateWrapper<FileAssetEntity>()
                    .eq("id", file.getId())
                    .eq("status", "DELETING")
                    .set("status", "DELETED")
                    .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
        }
    }

    private boolean isReferenced(Long fileId) {
        Long references = mediaMapper.selectCount(new LambdaQueryWrapper<RepairRequestMediaEntity>()
                .eq(RepairRequestMediaEntity::getFileId, fileId));
        if (references != null && references > 0) return true;
        return customers.selectCount(new LambdaQueryWrapper<com.fixmate.entity.CustomerUserEntity>()
            .eq(com.fixmate.entity.CustomerUserEntity::getAvatarFileId,fileId)) > 0;
    }
}
