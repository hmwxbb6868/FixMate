package com.fixmate.common.api;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;
    private String timestamp;

    public ApiResponse() {
    }

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = ErrorCode.OK;
        r.message = "success";
        r.data = data;
        r.traceId = newTraceId();
        r.timestamp = now();
        return r;
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return of(code, message, null);
    }

    public static <T> ApiResponse<T> of(String code, String message, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        r.data = data;
        r.traceId = newTraceId();
        r.timestamp = now();
        return r;
    }

    private static String newTraceId() {
        return com.fixmate.common.trace.TraceContext.currentOrNew();
    }

    private static String now() {
        return OffsetDateTime.now(ZoneOffset.ofHours(8)).toString();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
