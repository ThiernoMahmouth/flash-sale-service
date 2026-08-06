package com.thierno.flashsaleservice.exception;

import java.util.UUID;

public class SaleEndedException extends BusinessException {
    public SaleEndedException(UUID flashSaleId) {
        super(ErrorType.SALE_ENDED, "Flash sale " + flashSaleId + " has already ended");
    }
}
