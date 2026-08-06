package com.thierno.flashsaleservice.unitaires;

import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.entity.FlashSaleStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlashSaleStatusTest {

    private FlashSale saleWindow(Instant start, Instant end, int totalStock, int soldStock) {
        FlashSale sale = new FlashSale();
        sale.setId(UUID.randomUUID());
        sale.setProductId(UUID.randomUUID());
        sale.setStartTime(start);
        sale.setEndTime(end);
        sale.setTotalStock(totalStock);
        sale.setSoldStock(soldStock);
        return sale;
    }

    @Test
    void beforeStartTime_isScheduled() {
        Instant now = Instant.now();
        FlashSale sale = saleWindow(now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS), 10, 0);

        assertThat(sale.statusAt(now)).isEqualTo(FlashSaleStatus.SCHEDULED);
    }

    @Test
    void exactlyAtStartTime_isActive() {
        Instant now = Instant.now();
        FlashSale sale = saleWindow(now, now.plus(1, ChronoUnit.HOURS), 10, 0);

        assertThat(sale.statusAt(now)).isEqualTo(FlashSaleStatus.ACTIVE);
    }

    @Test
    void betweenStartAndEnd_isActive() {
        Instant now = Instant.now();
        FlashSale sale = saleWindow(now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), 10, 0);

        assertThat(sale.statusAt(now)).isEqualTo(FlashSaleStatus.ACTIVE);
    }

    @Test
    void exactlyAtEndTime_isEnded() {
        Instant now = Instant.now();
        FlashSale sale = saleWindow(now.minus(1, ChronoUnit.HOURS), now, 10, 0);

        assertThat(sale.statusAt(now)).isEqualTo(FlashSaleStatus.ENDED);
    }

    @Test
    void afterEndTime_isEnded() {
        Instant now = Instant.now();
        FlashSale sale = saleWindow(now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), 10, 0);

        assertThat(sale.statusAt(now)).isEqualTo(FlashSaleStatus.ENDED);
    }

    @Test
    void soldStockEqualsTotalStock_isSoldOut_evenDuringActiveWindow() {
        Instant now = Instant.now();
        FlashSale sale = saleWindow(now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), 5, 5);

        assertThat(sale.remainingStock()).isZero();
        assertThat(sale.statusAt(now)).isEqualTo(FlashSaleStatus.SOLD_OUT);
    }

    @Test
    void soldOutTakesPrecedenceOverScheduled() {
        Instant now = Instant.now();
        FlashSale sale = saleWindow(now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS), 0, 0);

        assertThat(sale.statusAt(now)).isEqualTo(FlashSaleStatus.SOLD_OUT);
    }
}
