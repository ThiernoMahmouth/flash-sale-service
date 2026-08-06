package com.thierno.flashsaleservice.exception;

import org.springframework.http.HttpStatus;

public enum ErrorType {

    FLASH_SALE_NOT_FOUND(
            "SALE_404",
            "Flash Sale Not Found",
            HttpStatus.NOT_FOUND,
            "No flash sale found for the given id",
            "flash-sale-not-found"),

    PURCHASE_REQUEST_NOT_FOUND(
            "REQ_404",
            "Purchase Request Not Found",
            HttpStatus.NOT_FOUND,
            "No purchase request found for the given id",
            "purchase-request-not-found"),

    SALE_NOT_STARTED(
            "SALE_001",
            "Flash Sale Not Started",
            HttpStatus.CONFLICT,
            "This flash sale has not started yet",
            "sale-not-started"),

    SALE_ENDED(
            "SALE_002",
            "Flash Sale Ended",
            HttpStatus.CONFLICT,
            "This flash sale has already ended",
            "sale-ended"),

    SALE_SOLD_OUT(
            "SALE_004",
            "Flash Sale Sold Out",
            HttpStatus.CONFLICT,
            "No stock remains for this flash sale",
            "sale-sold-out"),

    INVALID_SALE_WINDOW(
            "SALE_003",
            "Invalid Flash Sale Window",
            HttpStatus.BAD_REQUEST,
            "earlyAccessStart must be <= startTime, and startTime must be before endTime",
            "invalid-sale-window"),

    EARLY_ACCESS_DENIED(
            "SALE_005",
            "Early Access Denied",
            HttpStatus.FORBIDDEN,
            "This flash sale is in its early-access window, reserved for higher-tier members",
            "early-access-denied"),

    CUSTOMER_NOT_FOUND(
            "CUST_404",
            "Customer Not Found",
            HttpStatus.NOT_FOUND,
            "No customer found for the given id",
            "customer-not-found");

    private final String     code;
    private final String     title;
    private final String     message;
    private final String     uriSlug;
    private final HttpStatus status;

    ErrorType(String code, String title, HttpStatus status, String message,
              String uriSlug) {
        this.code    = code;
        this.title   = title;
        this.message = message;
        this.uriSlug = uriSlug;
        this.status  = status;
    }

    public String     getCode()    { return code; }
    public String     getTitle()   { return title; }
    public String     getMessage() { return message; }
    public HttpStatus getStatus()  { return status; }
    public String     getTypeUri() { return "https://flash-sale-app.com/errors/" + uriSlug; }
}
