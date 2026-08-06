package com.thierno.flashsaleservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchase")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flash_sale_id", nullable = false)
    private UUID flashSaleId;

    @Column(name = "purchase_request_id", nullable = false)
    private UUID purchaseRequestId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;

    @PrePersist
    void prePersist() {
        if (purchasedAt == null) purchasedAt = Instant.now();
    }
}
