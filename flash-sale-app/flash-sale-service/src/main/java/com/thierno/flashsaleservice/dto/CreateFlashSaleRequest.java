package com.thierno.flashsaleservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateFlashSaleRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer totalStock,
        @Min(1) Integer maxUnitsPerCustomer,
        @NotNull Instant earlyAccessStart,
        @NotNull Instant startTime,
        @NotNull Instant endTime) {

    public CreateFlashSaleRequest(UUID productId, Integer totalStock, Instant earlyAccessStart,
                                   Instant startTime, Instant endTime) {
        this(productId, totalStock, null, earlyAccessStart, startTime, endTime);
    }
}
