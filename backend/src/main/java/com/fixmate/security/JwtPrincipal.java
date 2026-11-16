package com.fixmate.security;

public record JwtPrincipal(Long userId, String familyId, String tokenId) {
}
