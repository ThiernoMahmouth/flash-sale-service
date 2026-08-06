package com.thierno.flashsaleservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseConfirmedEvent(
        UUID eventId,
        UUID purchaseId,
        UUID flashSaleId,
        UUID purchaseRequestId,
        String customerId,
        int quantity,
        BigDecimal unitPrice,
        Instant occurredAt) {
}
