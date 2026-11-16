package com.fixmate.security;

import java.time.Duration;
import java.util.UUID;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Refresh Token 家族状态。
 * family 撤销后，任何持有旧 Refresh Token 的请求都必须被拒绝。
 */
@Service
public class TokenFamilyStore {

    private static final String FAMILY_KEY = "fixmate:token:family:";
    private static final String USED_KEY = "fixmate:token:refresh:used:";
    private static final DefaultRedisScript<Long> REVOKE = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current then return 0 end
            if current == ARGV[1] then return 1 end
            if string.sub(current, 1, 7) ~= 'ACTIVE:' then return 0 end
            redis.call('SET', KEYS[1], ARGV[1], 'KEEPTTL')
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    public TokenFamilyStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String createFamily(Long userId, Duration ttl) {
        String familyId = UUID.randomUUID().toString();
        redis.opsForValue().set(FAMILY_KEY + familyId, "ACTIVE:" + userId, ttl);
        return familyId;
    }

    public boolean isFamilyActive(String familyId) {
        String value = redis.opsForValue().get(FAMILY_KEY + familyId);
        return value != null && value.startsWith("ACTIVE:");
    }

    public boolean revokeFamily(String familyId, String idempotencyKey, Duration ttl) {
        // 原子转换且保留首个撤销者与原 TTL；并发请求不能覆盖恢复归属。
        return Long.valueOf(1).equals(redis.execute(REVOKE, List.of(FAMILY_KEY + familyId),
                "REVOKED:" + idempotencyKey));
    }

    public boolean isFamilyRevokedBy(String familyId, String idempotencyKey) {
        String value = redis.opsForValue().get(FAMILY_KEY + familyId);
        return value != null && value.equals("REVOKED:" + idempotencyKey);
    }

    /**
     * 标记 Refresh Token 已被使用；返回 SAME_KEY_RETRY 表示同一逻辑动作重试，
     * REPLAY 表示不同幂等键复用旧令牌。
     */
    public RefreshUse markRefreshUsed(String tokenId, String idempotencyKey, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(USED_KEY + tokenId, idempotencyKey, ttl);
        if (Boolean.TRUE.equals(ok)) {
            return RefreshUse.FIRST_USE;
        }
        String owner = redis.opsForValue().get(USED_KEY + tokenId);
        return owner != null && owner.equals(idempotencyKey)
                ? RefreshUse.SAME_KEY_RETRY : RefreshUse.REPLAY;
    }

    public enum RefreshUse {
        FIRST_USE,
        SAME_KEY_RETRY,
        REPLAY
    }

    public void clearRefreshUsed(String tokenId) {
        redis.delete(USED_KEY + tokenId);
    }
    public void createStaffFamily(String familyId,Long staffId,Integer credentialVersion,Duration ttl) {
        redis.opsForValue().setIfAbsent(FAMILY_KEY+familyId,"ACTIVE:STAFF:"+staffId+":"+credentialVersion,ttl);
    }
    public boolean isStaffFamilyActive(String familyId,Long staffId,Integer credentialVersion) {
        return ("ACTIVE:STAFF:"+staffId+":"+credentialVersion).equals(redis.opsForValue().get(FAMILY_KEY+familyId));
    }

}
