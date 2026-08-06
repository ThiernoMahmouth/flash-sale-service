package com.thierno.flashsaleservice.exception;

import java.util.UUID;

public class FlashSaleNotFoundException extends BusinessException {
    public FlashSaleNotFoundException(UUID flashSaleId) {
        super(ErrorType.FLASH_SALE_NOT_FOUND, "No flash sale found for id: " + flashSaleId);
    }
}
