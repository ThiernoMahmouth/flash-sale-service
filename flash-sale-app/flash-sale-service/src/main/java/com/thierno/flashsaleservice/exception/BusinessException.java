package com.thierno.flashsaleservice.exception;

public abstract class BusinessException extends RuntimeException {

    private final ErrorType errorType;
    private final String    detail;

    protected BusinessException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.detail    = errorType.getMessage();
    }

    protected BusinessException(ErrorType errorType, String detail) {
        super(detail);
        this.errorType = errorType;
        this.detail    = detail;
    }

    public ErrorType getErrorType() { return errorType; }
    public String    getDetail()    { return detail; }
}
