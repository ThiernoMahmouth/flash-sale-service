package com.thierno.flashsaleservice.exception;

import java.time.Instant;
import java.util.UUID;

public class SaleNotStartedException extends BusinessException {
    public SaleNotStartedException(UUID flashSaleId, Instant startTime) {
        super(ErrorType.SALE_NOT_STARTED,
                "Flash sale " + flashSaleId + " starts at " + startTime);
    }
}
