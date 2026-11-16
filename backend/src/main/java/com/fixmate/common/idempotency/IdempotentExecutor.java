package com.fixmate.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.api.ErrorCode;
import com.fixmate.entity.IdempotencyRecordEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.security.TokenFamilyStore;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 幂等执行器：原子抢占 → 唯一执行 → 固化结果。
 * 并发冲突由唯一约束保证只有一个请求真正执行。
 */
@Service
public class IdempotentExecutor {

    private final IdempotencyService idempotencyService;
    private final TokenFamilyStore tokenFamilyStore;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate currentReadTransactions;

    public IdempotentExecutor(IdempotencyService idempotencyService,
                              TokenFamilyStore tokenFamilyStore,
                              ObjectMapper objectMapper,
                              PlatformTransactionManager transactionManager) {
        this.idempotencyService = idempotencyService;
        this.tokenFamilyStore = tokenFamilyStore;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.currentReadTransactions = new TransactionTemplate(transactionManager);
        this.currentReadTransactions.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public String hash(Object request) {
        try {
            return idempotencyService.hashRequest(objectMapper.writeValueAsBytes(request));
        } catch (Exception ex) {
            throw new IllegalStateException("request hash failed", ex);
        }
    }

    /**
     * 路径参数参与请求一致性比较，但不进入幂等作用域。
     * 契约作用域固定为「用户ID + operationId + Idempotency-Key」；同一幂等键指向不同资源时，
     * 必须因请求哈希不同而判定为 DUPLICATE_REQUEST，而不是各走各的作用域。
     */
    public String hashWithPath(Object request, Object pathId) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("pathId", pathId);
        payload.put("body", request);
        return hash(payload);
    }
    public String hashCommand(String method,String canonicalPath,Object body) {
        return hash(commandPayload(method,canonicalPath,body));
    }
    public static Object commandPayload(String method,String canonicalPath,Object body) {
        java.util.Map<String,Object> payload=new java.util.LinkedHashMap<>();
        payload.put("method",method);payload.put("path",canonicalPath);payload.put("body",body);return payload;
    }

    /** 同键同体的并发请求等待首个结果的最长时间。 */
    private static final long CONCURRENT_WAIT_MILLIS = 3000L;

    public ResponseEntity<Object> execute(IdempotencyContext ctx, Supplier<ResponseEntity<Object>> action) {
        return execute(ctx,action,()->{});
    }

    /** 敏感员工命令：首次执行及缓存重放前均重新校验当前账号/资源权限。 */
    public ResponseEntity<Object> execute(IdempotencyContext ctx, Supplier<ResponseEntity<Object>> action,Runnable guard) {
        return executeWith(ctx,action,guard,transactionTemplate);
    }

    /** B2-C聚合命令：根行锁与子资源读取使用当前已提交版本；旧命令保持原隔离级别。 */
    public ResponseEntity<Object> executeCurrentRead(IdempotencyContext ctx,Supplier<ResponseEntity<Object>> action,Runnable guard) {
        return executeWith(ctx,action,guard,currentReadTransactions);
    }

    private ResponseEntity<Object> executeWith(IdempotencyContext ctx,Supplier<ResponseEntity<Object>> action,Runnable guard,TransactionTemplate transactions) {
        guard.run();
        IdempotencyService.Claim claim = idempotencyService.claim(ctx);

        if (!claim.owned()) {
            IdempotencyRecordEntity settled =
                    idempotencyService.awaitCompletion(claim.record(), CONCURRENT_WAIT_MILLIS);
            if (settled == null) {
                // 首个请求已放弃执行（异常路径），本请求重新抢占后正常执行
                claim = idempotencyService.claim(ctx);
                if (!claim.owned()) {
                    return conflict(ErrorCode.Codes.DUPLICATE_REQUEST, "相同请求正在处理中，请稍后重试");
                }
            } else if (IdempotencyService.STATUS_COMPLETED.equals(settled.getStatus())) {
                if (ctx.requireActiveFamily() && ctx.familyId() != null
                        && !tokenFamilyStore.isFamilyActive(ctx.familyId())) {
                    return unauthorized("登录已失效");
                }
                guard.run();
                return ResponseEntity.status(settled.getResponseStatus())
                        .body(parse(idempotencyService.readBody(settled)));
            } else {
                return conflict(ErrorCode.Codes.DUPLICATE_REQUEST, "相同请求正在处理中，请稍后重试");
            }
        }

        IdempotencyRecordEntity record = claim.record();
        try {
            // 业务写入与成功结果必须在同一个数据库事务里提交：
            // 若结果固化失败，业务写入也一起回滚，重试不会重复执行副作用
            ResponseEntity<Object> response = transactions.execute(status -> {
                guard.run();
                ResponseEntity<Object> result = action.get();
                idempotencyService.complete(record.getId(), result.getStatusCode().value(),
                        toJson(result.getBody()), ctx.sensitive());
                return result;
            });
            return response;
        } catch (BusinessException ex) {
            // 业务异常已经让上面的业务事务回滚；失败结果单独固化，保证同键重试能重放同一结果
            ApiResponse<Void> body = ApiResponse.error(ex.getCode(), ex.getMessage());
            try {
                transactions.executeWithoutResult(status ->
                        idempotencyService.complete(record.getId(), ex.getStatus().value(),
                                toJson(body), ctx.sensitive()));
            } catch (RuntimeException completeFailure) {
                idempotencyService.abandon(record.getId());
                throw completeFailure;
            }
            return ResponseEntity.status(ex.getStatus()).body(body);
        } catch (RuntimeException ex) {
            idempotencyService.abandon(record.getId());
            throw ex;
        }
    }

    private ResponseEntity<Object> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.Codes.UNAUTHORIZED, message));
    }

    private ResponseEntity<Object> conflict(String code, String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(code, message));
    }

    private String toJson(Object body) {
        if (body == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            throw new IllegalStateException("response serialize failed", ex);
        }
    }

    private Object parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
