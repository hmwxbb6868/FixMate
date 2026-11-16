package com.fixmate.exception;

import com.fixmate.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BusinessException validation(String message) {
        return new BusinessException(ErrorCode.Codes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.Codes.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.Codes.FORBIDDEN, HttpStatus.FORBIDDEN, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.Codes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    public static BusinessException stateConflict(String message) {
        return new BusinessException(ErrorCode.Codes.STATE_CONFLICT, HttpStatus.CONFLICT, message);
    }

    public static BusinessException versionConflict(String message) {
        return new BusinessException(ErrorCode.Codes.VERSION_CONFLICT, HttpStatus.CONFLICT, message);
    }

    public static BusinessException duplicateRequest(String message) {
        return new BusinessException(ErrorCode.Codes.DUPLICATE_REQUEST, HttpStatus.CONFLICT, message);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
