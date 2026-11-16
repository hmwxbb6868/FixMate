package com.fixmate.storage.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.fixmate.common.api.ErrorCode;
import com.fixmate.config.OssProperties;
import com.fixmate.exception.BusinessException;
import com.fixmate.storage.ObjectStorageService;
import java.io.InputStream;
import java.time.Duration;
import java.util.Date;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
@ConditionalOnProperty(prefix = "fixmate.oss", name = "endpoint", matchIfMissing = false)
public class AliyunOssStorageService implements ObjectStorageService {

    private final OssProperties properties;
    private final OSS client;

    public AliyunOssStorageService(OssProperties properties) {
        this.properties = properties;
        // 本地未配置 OSS 时不建立客户端，使用时再报 STORAGE_UNAVAILABLE，避免启动失败
        this.client = (properties.getEndpoint() == null || properties.getEndpoint().isBlank())
                ? null
                : new OSSClientBuilder().build(
                        secureEndpoint(properties.getEndpoint()),
                        properties.getAccessKeyId(), properties.getAccessKeySecret());
    }

    /**
     * 微信小程序不再允许加载 http 图片，签名地址必须走 https。
     * OSS endpoint 常被配置成不带协议头的域名，SDK 会默认用 http，这里统一补成 https。
     */
    static String secureEndpoint(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        String trimmed = endpoint.trim();
        if (trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("http://")) {
            return "https://" + trimmed.substring("http://".length());
        }
        return "https://" + trimmed;
    }

    private void requireClient() {
        if (client == null) {
            throw new BusinessException(ErrorCode.Codes.STORAGE_UNAVAILABLE,
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "文件存储未配置");
        }
    }

    @Override
    public String putObject(String objectKey, InputStream content, long contentLength, String contentType) {
        requireClient();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(contentLength);
            return client.putObject(properties.getBucket(), objectKey, content, metadata).getETag();
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.Codes.STORAGE_UNAVAILABLE,
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "文件存储不可用");
        }
    }

    @Override
    public String generateSignedUrl(String objectKey, Duration ttl) {
        requireClient();
        Date expiry = new Date(System.currentTimeMillis() + ttl.toMillis());
        return client.generatePresignedUrl(properties.getBucket(), objectKey, expiry).toString();
    }

    @Override
    public void deleteObject(String objectKey) {
        requireClient();
        client.deleteObject(properties.getBucket(), objectKey);
    }
}
