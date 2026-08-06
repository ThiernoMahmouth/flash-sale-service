package com.thierno.flashsaleservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flash_sale")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FlashSale {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "total_stock", nullable = false)
    private Integer totalStock;

    @Column(name = "sold_stock", nullable = false)
    private Integer soldStock = 0;

    @Column(name = "early_access_start", nullable = false)
    private Instant earlyAccessStart;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "max_units_per_customer")
    private Integer maxUnitsPerCustomer;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public int remainingStock() {
        return totalStock - soldStock;
    }

    public FlashSaleStatus statusAt(Instant now) {
        if (remainingStock() <= 0) {
            return FlashSaleStatus.SOLD_OUT;
        }
        if (now.isBefore(startTime)) {
            return FlashSaleStatus.SCHEDULED;
        }
        if (!now.isBefore(endTime)) {
            return FlashSaleStatus.ENDED;
        }
        return FlashSaleStatus.ACTIVE;
    }
}
