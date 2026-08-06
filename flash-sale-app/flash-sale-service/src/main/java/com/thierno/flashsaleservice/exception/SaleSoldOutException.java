package com.thierno.flashsaleservice.exception;

import java.util.UUID;

public class SaleSoldOutException extends BusinessException {
    public SaleSoldOutException(UUID flashSaleId) {
        super(ErrorType.SALE_SOLD_OUT, "Flash sale " + flashSaleId + " is sold out");
    }
}
