package com.thierno.flashsaleservice.exception;

public class InvalidSaleWindowException extends BusinessException {
    public InvalidSaleWindowException() {
        super(ErrorType.INVALID_SALE_WINDOW);
    }
}
