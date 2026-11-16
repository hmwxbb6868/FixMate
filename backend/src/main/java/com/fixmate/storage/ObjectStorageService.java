package com.fixmate.storage;

import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorageService {

    /** 写入对象，返回 ETag。Key 由服务端生成，禁止使用用户原始文件名。 */
    String putObject(String objectKey, InputStream content, long contentLength, String contentType);

    /** 生成短时签名下载 URL。 */
    String generateSignedUrl(String objectKey, Duration ttl);

    void deleteObject(String objectKey);
}
