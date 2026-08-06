package com.thierno.flashsaleservice.exception;

import com.thierno.flashsaleservice.entity.MembershipLevel;

import java.util.UUID;

public class EarlyAccessDeniedException extends BusinessException {
    public EarlyAccessDeniedException(UUID flashSaleId, String customerId, MembershipLevel requiredLevel) {
        super(ErrorType.EARLY_ACCESS_DENIED,
                "Customer " + customerId + " needs membership level " + requiredLevel
                        + " or higher for early access to flash sale " + flashSaleId);
    }
}
