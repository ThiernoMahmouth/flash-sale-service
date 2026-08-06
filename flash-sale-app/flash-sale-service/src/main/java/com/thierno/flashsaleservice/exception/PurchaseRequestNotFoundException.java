package com.thierno.flashsaleservice.exception;

import java.util.UUID;

public class PurchaseRequestNotFoundException extends BusinessException {
    public PurchaseRequestNotFoundException(UUID requestId) {
        super(ErrorType.PURCHASE_REQUEST_NOT_FOUND, "No purchase request found for id: " + requestId);
    }
}
