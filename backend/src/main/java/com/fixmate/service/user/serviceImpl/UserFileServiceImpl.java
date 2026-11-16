package com.fixmate.service.user.serviceImpl;

import com.fixmate.common.api.ErrorCode;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.config.OssProperties;
import com.fixmate.dto.user.FileAssetVO;
import com.fixmate.entity.FileAssetEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.FileAssetMapper;
import com.fixmate.service.common.FileValidationService;
import com.fixmate.service.common.UploadReservationService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.service.user.UserFileService;
import com.fixmate.storage.ObjectStorageService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserFileServiceImpl implements UserFileService {

    private final FileValidationService fileValidationService;
    private final ObjectStorageService storageService;
    private final FileAssetMapper fileAssetMapper;
    private final OssProperties ossProperties;
    private final AuditLogService auditLogService;
    private final UploadReservationService reservationService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserFileServiceImpl.class);

    public UserFileServiceImpl(FileValidationService fileValidationService,
                               ObjectStorageService storageService,
                               FileAssetMapper fileAssetMapper,
                               OssProperties ossProperties,
                               AuditLogService auditLogService, UploadReservationService reservationService) {
        this.fileValidationService = fileValidationService;
        this.storageService = storageService;
        this.fileAssetMapper = fileAssetMapper;
        this.ossProperties = ossProperties;
        this.auditLogService = auditLogService;
        this.reservationService = reservationService;
    }

    @Override
    public FileValidationService.PreparedUpload prepare(MultipartFile file, String purpose) {
        return fileValidationService.prepare(file, purpose);
    }

    @Override
    public FileAssetVO upload(Long userId, FileValidationService.PreparedUpload prepared, String originalName,
                              String declaredType, String purpose) {
        FileValidationService.ValidatedFile validated = prepared.file();

        if (ossProperties.getEndpoint() == null || ossProperties.getEndpoint().isBlank()) {
            throw new BusinessException(ErrorCode.Codes.STORAGE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "文件存储未配置");
        }

        String objectKey = "uploads/" + userId + "/" + UUID.randomUUID() + "." + validated.extension();
        FileAssetEntity asset = new FileAssetEntity();
        asset.setUploaderType("CUSTOMER");
        asset.setUploaderId(userId);
        asset.setPurpose(purpose);
        asset.setBucket(ossProperties.getBucket());
        asset.setStorageKey(objectKey);
        asset.setOriginalName(originalName);
        asset.setMimeType(validated.mimeType());
        asset.setSizeBytes(validated.size());
        asset.setSha256(validated.sha256());
        asset.setStatus("TEMP");
        asset.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        asset.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        // 独立提交，先有回收凭据再写 OSS；etag 为空时禁止业务绑定。
        reservationService.reserve(asset);
        String etag;
        try (java.io.InputStream content = java.nio.file.Files.newInputStream(prepared.tempPath())) {
            etag = storageService.putObject(objectKey, content, validated.size(), validated.mimeType());
        } catch (java.io.IOException ex) {
            throw new BusinessException(ErrorCode.Codes.STORAGE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "文件读取失败");
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.Codes.STORAGE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "文件存储不可用");
        }
        // 幂等执行器把上传包在事务里：后续 complete/审计失败导致回滚时，OSS 对象必须一起清掉
        deleteObjectOnRollback(objectKey);

        try {
            int updated = fileAssetMapper.update(null, new UpdateWrapper<FileAssetEntity>()
                    .eq("id", asset.getId()).eq("status", "TEMP").isNull("etag")
                    .set("etag", etag).set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
            if (updated != 1) {
                throw new IllegalStateException("upload reservation changed");
            }
        } catch (RuntimeException ex) {
            // 数据库失败时补偿删除刚写入的对象，避免孤儿文件
            try {
                storageService.deleteObject(objectKey);
            } catch (RuntimeException ignored) {
                    // 已持久化的 TEMP 凭据交由定时任务回收。
            }
            throw ex;
        }

        auditLogService.recordCustomer(userId, "FILE_UPLOADED", "FILE_ASSET", asset.getId(), null,
                Map.of("purpose", purpose, "mimeType", validated.mimeType(), "sizeBytes", validated.size()));

        String url = storageService.generateSignedUrl(objectKey, Duration.ofSeconds(300));
        return new FileAssetVO(asset.getId(), url, validated.mimeType(), validated.size(), validated.sha256());
    }

    private void deleteObjectOnRollback(String objectKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_ROLLED_BACK) {
                    // UNKNOWN 不能认定数据库已回滚。保留对象，由持久化记录及引用状态决定回收。
                    return;
                }
                try {
                    storageService.deleteObject(objectKey);
                } catch (RuntimeException ex) {
                    log.warn("upload rollback cleanup deferred for object reservation");
                    // 独立事务提交的 TEMP 回收凭据仍在，不依赖路径生命周期误删 ACTIVE 图片。
                }
            }
        });
    }
}
