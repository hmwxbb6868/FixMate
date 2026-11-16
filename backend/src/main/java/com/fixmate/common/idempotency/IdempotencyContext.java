package com.fixmate.common.idempotency;

/**
 * 一次写请求的幂等上下文。
 *
 * familyId 非空时，重放前必须校验 token family 仍然有效（用于 refresh/logout 等敏感接口）。
 */
public record IdempotencyContext(
        String scope,
        String idempotencyKey,
        String operation,
        String requestHash,
        String familyId,
        boolean sensitive,
        long ttlSeconds,
        boolean requireActiveFamily) {

    public static IdempotencyContext forUser(Long userId, String operation, String key, String hash,
                                             long ttlSeconds) {
        return new IdempotencyContext(IdempotencyService.scopeForUser(userId, operation), key, operation,
                hash, null, false, ttlSeconds, false);
    }

    public static IdempotencyContext forFamily(String familyId, String operation, String key, String hash,
                                               boolean sensitive, boolean requireActiveFamily,
                                               long ttlSeconds) {
        return new IdempotencyContext(IdempotencyService.scopeForFamily(familyId, operation), key, operation,
                hash, familyId, sensitive, ttlSeconds, requireActiveFamily);
    }

    public static IdempotencyContext forAnonymous(String operation, String key, String hash,
                                                  boolean sensitive, long ttlSeconds) {
        return new IdempotencyContext(IdempotencyService.scopeForAnonymous(operation), key, operation,
                hash, null, sensitive, ttlSeconds, false);
    }
    public static IdempotencyContext forStaff(Long staffId,String operation,String key,String hash,long ttlSeconds) {
        return new IdempotencyContext("STAFF:"+staffId+":"+operation,key,operation,hash,null,false,ttlSeconds,false);
    }
    public static IdempotencyContext forSensitiveStaff(Long staffId,String operation,String key,String hash,long accessTtlSeconds) {
        return new IdempotencyContext("STAFF:"+staffId+":"+operation,key,operation,hash,null,true,Math.min(900,accessTtlSeconds),false);
    }
    public static IdempotencyContext forStaffFamily(String family,String operation,String key,String hash,
            boolean sensitive,boolean requireActive,long ttlSeconds) {
        return new IdempotencyContext("STAFF:FAMILY:"+family+":"+operation,key,operation,hash,family,sensitive,ttlSeconds,requireActive);
    }

}
