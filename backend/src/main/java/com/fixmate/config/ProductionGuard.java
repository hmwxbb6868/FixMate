package com.fixmate.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 生产启动门禁：违反冻结规则第 18.2 条时必须直接启动失败。
 */
@Component
@Profile("prod")
public class ProductionGuard {

    private static final Logger log = LoggerFactory.getLogger(ProductionGuard.class);

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${fixmate.jwt.secret:}")
    private String jwtSecret;

    @Value("${fixmate.wechat.app-id:}")
    private String wechatAppId;

    @Value("${fixmate.wechat.app-secret:}")
    private String wechatAppSecret;

    @Value("${fixmate.oss.endpoint:}")
    private String ossEndpoint;

    @Value("${fixmate.oss.bucket:}")
    private String ossBucket;

    @Value("${fixmate.oss.access-key-id:}")
    private String ossAccessKeyId;

    @Value("${fixmate.oss.access-key-secret:}")
    private String ossAccessKeySecret;

    @PostConstruct
    public void verify() {
        requireNonBlank(redisPassword, "REDIS_PASSWORD");
        requireNonBlank(jwtSecret, "JWT_SECRET");
        requireNonBlank(wechatAppId, "WECHAT_APP_ID");
        requireNonBlank(wechatAppSecret, "WECHAT_APP_SECRET");
        requireNonBlank(ossEndpoint, "ALIYUN_OSS_ENDPOINT");
        requireNonBlank(ossBucket, "ALIYUN_OSS_BUCKET");
        // OSS 客户端实际依赖这两项；缺失时应用虽然能启动，但任何上传都会失败
        requireNonBlank(ossAccessKeyId, "ALIYUN_OSS_ACCESS_KEY_ID");
        requireNonBlank(ossAccessKeySecret, "ALIYUN_OSS_ACCESS_KEY_SECRET");

        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes in prod");
        }
        if (jwtSecret.startsWith("fixmate-dev") || jwtSecret.contains("change-me")) {
            throw new IllegalStateException("JWT_SECRET looks like a development default");
        }
        if (wechatAppId.toLowerCase().contains("test")) {
            throw new IllegalStateException("WECHAT_APP_ID looks like a test app id");
        }
        String bucket = ossBucket.toLowerCase();
        if (bucket.contains("-dev") || bucket.startsWith("dev-")) {
            throw new IllegalStateException("ALIYUN_OSS_BUCKET looks like a development bucket");
        }
        log.info("production guard passed");
    }

    private void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must not be empty in prod");
        }
    }
}
