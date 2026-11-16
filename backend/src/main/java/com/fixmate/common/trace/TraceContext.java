package com.fixmate.common.trace;

import java.util.UUID;

/**
 * 请求级 trace 上下文：响应 traceId、审计记录与状态日志必须使用同一个值，
 * 否则一次请求在日志里无法串起来。由 RequestHeaderValidationFilter 设置与清理。
 */
public final class TraceContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TraceContext() {
    }

    public static void set(String traceId) {
        CURRENT.set(traceId);
    }

    public static String current() {
        return CURRENT.get();
    }

    public static String currentOrNew() {
        String traceId = CURRENT.get();
        return traceId != null ? traceId : UUID.randomUUID().toString().replace("-", "");
    }

    public static void clear() {
        CURRENT.remove();
    }
}
