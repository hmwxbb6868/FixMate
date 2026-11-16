package com.fixmate.dto.user;

public record FileAssetVO(
        Long id,
        String url,
        String mimeType,
        long sizeBytes,
        String sha256) {
}
