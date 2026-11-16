package com.fixmate.common.api;

public final class ErrorCode {

    public static final String OK = "OK";

    private ErrorCode() {
    }

    public static final class Codes {
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
        public static final String STATE_CONFLICT = "STATE_CONFLICT";
        public static final String VERSION_CONFLICT = "VERSION_CONFLICT";
        public static final String QUOTE_NOT_CURRENT = "QUOTE_NOT_CURRENT";
        public static final String DUPLICATE_REQUEST = "DUPLICATE_REQUEST";
        public static final String HARD_RULE_BLOCKED = "HARD_RULE_BLOCKED";
        public static final String PHONE_BIND_UNAVAILABLE = "PHONE_BIND_UNAVAILABLE";
        public static final String RATE_LIMITED = "RATE_LIMITED";
        public static final String STORAGE_UNAVAILABLE = "STORAGE_UNAVAILABLE";
        public static final String WECHAT_UNAVAILABLE = "WECHAT_UNAVAILABLE";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

        private Codes() {
        }
    }
}
