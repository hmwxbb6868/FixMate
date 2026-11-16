package com.fixmate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fixmate.wechat")
public class WechatProperties {

    private String appId = "";

    private String appSecret = "";

    public boolean isConfigured() {
        return !appId.isBlank() && !appSecret.isBlank();
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }
}
