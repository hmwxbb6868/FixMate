package com.fixmate.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** 自动化测试使用，不访问真实 OSS。 */
@Service
@Profile("test")
public class FakeObjectStorageService implements ObjectStorageService {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public String putObject(String objectKey, InputStream content, long contentLength, String contentType) {
        try {
            store.put(objectKey, content.readAllBytes());
            return "fake-etag-" + objectKey.hashCode();
        } catch (IOException ex) {
            throw new IllegalStateException("fake storage write failed", ex);
        }
    }

    @Override
    public String generateSignedUrl(String objectKey, Duration ttl) {
        return "fake://" + objectKey;
    }

    @Override
    public void deleteObject(String objectKey) {
        store.remove(objectKey);
    }

    public InputStream read(String objectKey) {
        return new ByteArrayInputStream(store.getOrDefault(objectKey, new byte[0]));
    }

    public int size() {
        return store.size();
    }
}
