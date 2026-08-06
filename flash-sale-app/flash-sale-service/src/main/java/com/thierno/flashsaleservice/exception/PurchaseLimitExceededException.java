package com.thierno.flashsaleservice.exception;

import java.util.UUID;

public class PurchaseLimitExceededException extends BusinessException {
    public PurchaseLimitExceededException(UUID flashSaleId, String customerId, int maxUnitsPerCustomer) {
        super(ErrorType.PURCHASE_LIMIT_EXCEEDED,
                "Customer " + customerId + " is limited to " + maxUnitsPerCustomer
                        + " units for flash sale " + flashSaleId);
    }
}
