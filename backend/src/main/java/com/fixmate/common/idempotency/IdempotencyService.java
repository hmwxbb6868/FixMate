package com.fixmate.common.idempotency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.config.JwtProperties;
import com.fixmate.entity.IdempotencyRecordEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.IdempotencyRecordMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 幂等记录存储。核心是原子抢占：
 * 先 INSERT PENDING（唯一约束保证只有一个请求能成功），执行完成后固化结果。
 */
@Service
public class IdempotencyService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final IdempotencyRecordMapper mapper;
    private final SecretKeySpec encryptionKey;

    public IdempotencyService(IdempotencyRecordMapper mapper, JwtProperties jwtProperties) {
        this.mapper = mapper;
        this.encryptionKey = new SecretKeySpec(deriveKey(jwtProperties.getSecret()), "AES");
    }

    public String hashRequest(byte[] body) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(body == null ? new byte[0] : body));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 原子抢占幂等键。owned=true 表示本请求持有执行权。
     *
     * 过期记录的重新认领必须走条件更新（CAS），不能用"先删后插"：
     * 并发下两个请求都删掉旧行后，只有一个能插回唯一键，另一个会拿到裸的 DuplicateKeyException。
     */
    public Claim claim(IdempotencyContext ctx) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
        entity.setScopeKey(ctx.scope());
        entity.setFamilyId(ctx.familyId());
        entity.setIdempotencyKey(ctx.idempotencyKey());
        entity.setStatus(STATUS_PENDING);
        entity.setOperation(ctx.operation());
        entity.setRequestHash(ctx.requestHash());
        entity.setSensitiveFlag(ctx.sensitive());
        entity.setExpiresAt(now.plusSeconds(ctx.ttlSeconds()));
        entity.setCreatedAt(now);

        try {
            mapper.insert(entity);
            return new Claim(entity, true);
        } catch (DuplicateKeyException ex) {
            int reclaimed = mapper.reclaimExpired(ctx.scope(), ctx.idempotencyKey(), ctx.familyId(),
                    ctx.operation(), ctx.requestHash(), ctx.sensitive(), entity.getExpiresAt(),
                    entity.getCreatedAt(), now);
            if (reclaimed == 1) {
                IdempotencyRecordEntity mine = find(ctx.scope(), ctx.idempotencyKey());
                return new Claim(mine == null ? entity : mine, true);
            }
            IdempotencyRecordEntity existing = find(ctx.scope(), ctx.idempotencyKey());
            if (existing == null) {
                // 旧记录恰在本次查询前被其它请求接管或清理，重试一次插入
                try {
                    mapper.insert(entity);
                    return new Claim(entity, true);
                } catch (DuplicateKeyException retryEx) {
                    existing = find(ctx.scope(), ctx.idempotencyKey());
                }
            }
            if (existing == null) {
                throw BusinessException.duplicateRequest("相同请求正在处理中，请稍后重试");
            }
            if (!existing.getRequestHash().equals(ctx.requestHash())) {
                throw BusinessException.duplicateRequest("相同幂等键对应不同请求");
            }
            return new Claim(existing, false);
        }
    }

    /**
     * 同键同体的并发请求必须重放首个结果，不能返回临时冲突。
     *
     * @return 已固化的记录；记录被放弃（abandon）后消失时返回 null；超时仍为 PENDING 时返回原记录
     */
    public IdempotencyRecordEntity awaitCompletion(IdempotencyRecordEntity record, long waitMillis) {
        long deadline = System.nanoTime() + waitMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            IdempotencyRecordEntity fresh = mapper.selectById(record.getId());
            if (fresh == null) {
                return null;
            }
            if (!STATUS_PENDING.equals(fresh.getStatus())) {
                return fresh;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return record;
            }
        }
        IdempotencyRecordEntity fresh = mapper.selectById(record.getId());
        return fresh == null ? null : fresh;
    }

    public record Claim(IdempotencyRecordEntity record, boolean owned) {
    }

    public void complete(Long id, int responseStatus, String responseBody, boolean sensitive) {
        mapper.update(null, new UpdateWrapper<IdempotencyRecordEntity>()
                .eq("id", id)
                .set("status", STATUS_COMPLETED)
                .set("response_status", responseStatus)
                .set("response_body", sensitive ? encrypt(responseBody) : responseBody));
    }

    public void abandon(Long id) {
        // 提交可能已成功、只是确认响应丢失；不能删除已固化的业务结果。
        mapper.delete(new LambdaQueryWrapper<IdempotencyRecordEntity>()
                .eq(IdempotencyRecordEntity::getId, id)
                .eq(IdempotencyRecordEntity::getStatus, STATUS_PENDING));
    }

    public String readBody(IdempotencyRecordEntity record) {
        return Boolean.TRUE.equals(record.getSensitiveFlag())
                ? decrypt(record.getResponseBody())
                : record.getResponseBody();
    }

    /** 用户启动新版本时分批加密旧员工PII回执，保留原状态/哈希/响应语义，不重做业务。 */
    @org.springframework.transaction.annotation.Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int upgradeLegacyStaffResponses(LocalDateTime now,long accessTtl) {
        var rows=mapper.selectList(new LambdaQueryWrapper<IdempotencyRecordEntity>()
                .likeRight(IdempotencyRecordEntity::getScopeKey,"STAFF:")
                .in(IdempotencyRecordEntity::getOperation,java.util.List.of("createStaff","setStaffStatus","confirmStaffBinding","rejectStaffBinding","revokeStaffBinding","revokeStaffInvitation"))
                .eq(IdempotencyRecordEntity::getStatus,STATUS_COMPLETED).eq(IdempotencyRecordEntity::getSensitiveFlag,false)
                .isNotNull(IdempotencyRecordEntity::getResponseBody).orderByAsc(IdempotencyRecordEntity::getId).last("LIMIT 100 FOR UPDATE SKIP LOCKED"));
        for(var row:rows){var limit=now.plusSeconds(Math.min(900,accessTtl));var expiry=row.getExpiresAt().isBefore(limit)?row.getExpiresAt():limit;
            if(mapper.update(null,new UpdateWrapper<IdempotencyRecordEntity>().eq("id",row.getId()).eq("status",STATUS_COMPLETED).eq("sensitive_flag",false)
                    .set("sensitive_flag",true).set("response_body",encrypt(row.getResponseBody())).set("expires_at",expiry))!=1)
                throw BusinessException.versionConflict("历史敏感回执已变化，请重试升级");
        }
        return rows.size();
    }

    private IdempotencyRecordEntity find(String scope, String key) {
        return mapper.selectOne(new LambdaQueryWrapper<IdempotencyRecordEntity>()
                .eq(IdempotencyRecordEntity::getScopeKey, scope)
                .eq(IdempotencyRecordEntity::getIdempotencyKey, key));
    }

    private byte[] deriveKey(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest((secret + ":idempotency").getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("idempotency encrypt failed", ex);
        }
    }

    private String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("idempotency decrypt failed", ex);
        }
    }

    public static String scopeForUser(Long userId, String operation) {
        return "USER:" + userId + ":" + operation;
    }

    public static String scopeForFamily(String familyId, String operation) {
        return "FAMILY:" + familyId + ":" + operation;
    }

    public static String scopeForAnonymous(String operation) {
        return "ANON:" + operation;
    }
}
