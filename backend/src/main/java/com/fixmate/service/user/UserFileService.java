package com.fixmate.service.user;

import com.fixmate.dto.user.FileAssetVO;
import com.fixmate.service.common.FileValidationService;
import org.springframework.web.multipart.MultipartFile;

public interface UserFileService {

    /** 流式校验并落到受限临时文件；调用方负责删除临时文件。 */
    FileValidationService.PreparedUpload prepare(MultipartFile file, String purpose);

    FileAssetVO upload(Long userId, FileValidationService.PreparedUpload prepared, String originalName,
                       String declaredType, String purpose);
}
