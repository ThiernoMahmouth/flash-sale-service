package com.thierno.flashsaleservice.entity;

public enum PurchaseRequestStatus {
    PENDING,
    CONFIRMED,
    REJECTED_TOO_EARLY,
    REJECTED_SALE_ENDED,
    REJECTED_SOLD_OUT
}
