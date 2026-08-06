package com.thierno.flashsaleservice.dto;

import com.thierno.flashsaleservice.entity.MembershipLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCustomerRequest(
        @NotBlank String customerId,
        String name,
        @NotNull MembershipLevel membershipLevel) {
}
