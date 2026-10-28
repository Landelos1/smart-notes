package com.ldd.initialization.exception;

/**
 * 配额超限异常
 */
public class QuotaExceededException extends RuntimeException {
    
    private final int errorCode;
    private final Object data;

    public QuotaExceededException(String message, int errorCode, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}
