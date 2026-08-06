package com.thierno.flashsaleservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePurchaseRequestDto(
        @NotBlank String customerId,
        @NotNull @Min(1) Integer quantity) {
}
