package com.fixmate.security;

import com.fixmate.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_FAMILY = "fid";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        byte[] raw = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.properties = properties;
    }

    public String issueAccessToken(Long userId, String familyId) {
        return issue(userId, familyId, TYPE_ACCESS, properties.getAccessTtlSeconds());
    }

    public String issueRefreshToken(Long userId, String familyId) {
        return issue(userId, familyId, TYPE_REFRESH, properties.getRefreshTtlSeconds());
    }

    private String issue(Long userId, String familyId, String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("fixmate").audience().add("fixmate-miniapp").and()
                .claim("principalType","CUSTOMER")
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_FAMILY, familyId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public JwtPrincipal parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public JwtPrincipal parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private JwtPrincipal parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String principalType=claims.get("principalType",String.class);
            if(principalType!=null&&(!"CUSTOMER".equals(principalType)||!"fixmate".equals(claims.getIssuer())
                ||claims.getAudience()==null||!claims.getAudience().contains("fixmate-miniapp")))throw new JwtException("unexpected principal domain");
            // 原客户令牌仅在其已签发有效期内兼容；STAFF令牌从不进入客户解析器。
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new JwtException("unexpected token type");
            }
            return new JwtPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_FAMILY, String.class),
                    claims.getId());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtException("invalid token", ex);
        }
    }

    public long getAccessTtlSeconds() {
        return properties.getAccessTtlSeconds();
    }
}
