package com.fixmate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fixmate.oss")
public class OssProperties {

    private String endpoint = "";

    private String bucket = "";

    private String accessKeyId = "";

    private String accessKeySecret = "";

    private int signedUrlSeconds = 300;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public int getSignedUrlSeconds() {
        return signedUrlSeconds;
    }

    public void setSignedUrlSeconds(int signedUrlSeconds) {
        this.signedUrlSeconds = signedUrlSeconds;
    }
}
