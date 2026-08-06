package com.thierno.flashsaleservice.exception;

public class CustomerNotFoundException extends BusinessException {
    public CustomerNotFoundException(String customerId) {
        super(ErrorType.CUSTOMER_NOT_FOUND, "No customer found for id: " + customerId);
    }
}
