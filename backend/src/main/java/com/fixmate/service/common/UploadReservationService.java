package com.fixmate.service.common;

import com.fixmate.entity.FileAssetEntity;
import com.fixmate.mapper.FileAssetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** OSS 写入前持久化回收凭据；外层失败或进程退出后仍可由 TEMP 清理回收。 */
@Service
public class UploadReservationService {
    private final FileAssetMapper mapper;

    public UploadReservationService(FileAssetMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(FileAssetEntity asset) {
        mapper.insert(asset);
    }
}
