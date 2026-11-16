package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.IdempotencyContext;
import com.fixmate.common.idempotency.IdempotentExecutor;
import com.fixmate.dto.user.FileAssetVO;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.user.UserFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/user/files")
public class UserFileController {

    private static final long IDEMPOTENCY_TTL_SECONDS = 86400;

    private final UserFileService fileService;
    private final IdempotentExecutor executor;
    private final com.fixmate.common.ratelimit.RateLimitService rateLimitService;

    public UserFileController(UserFileService fileService, IdempotentExecutor executor,
                              com.fixmate.common.ratelimit.RateLimitService rateLimitService) {
        this.fileService = fileService;
        this.executor = executor;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public ResponseEntity<Object> upload(@RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @RequestPart("file") MultipartFile file,
                                         @RequestParam("purpose") String purpose,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        rateLimitService.check("file-upload", String.valueOf(principal.userId()), 30,
                java.time.Duration.ofMinutes(1));
        // 流式校验落到临时文件，幂等哈希用内容 SHA-256：仅凭文件名与大小会把不同内容判为同一请求
        com.fixmate.service.common.FileValidationService.PreparedUpload prepared =
                fileService.prepare(file, purpose);
        try {
            String hash = executor.hash(purpose + "|" + prepared.file().sha256());
            IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "userUploadFile", key,
                    hash, IDEMPOTENCY_TTL_SECONDS);
            return executor.execute(ctx, () -> {
                FileAssetVO vo = fileService.upload(principal.userId(), prepared, file.getOriginalFilename(),
                        file.getContentType(), purpose);
                return ResponseEntity.ok(ApiResponse.ok(vo));
            });
        } finally {
            prepared.delete();
        }
    }
}
