package com.thierno.flashsaleservice.unitaires;

import com.thierno.flashsaleservice.dto.CreateFlashSaleRequest;
import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.exception.FlashSaleNotFoundException;
import com.thierno.flashsaleservice.exception.InvalidSaleWindowException;
import com.thierno.flashsaleservice.repository.FlashSaleRepository;
import com.thierno.flashsaleservice.service.FlashSaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlashSaleServiceTest {

    @Mock
    private FlashSaleRepository flashSaleRepository;

    @InjectMocks
    private FlashSaleService flashSaleService;

    private UUID productId;
    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        start = Instant.now().plus(1, ChronoUnit.HOURS);
        end = Instant.now().plus(2, ChronoUnit.HOURS);
    }

    @Test
    void create_withValidWindow_savesAndReturnsSale() {
        when(flashSaleRepository.save(any(FlashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlashSale saved = flashSaleService.create(new CreateFlashSaleRequest(productId, 10, start, end));

        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getTotalStock()).isEqualTo(10);
        assertThat(saved.getSoldStock()).isZero();
        verify(flashSaleRepository).save(any(FlashSale.class));
    }

    @Test
    void create_whenStartTimeNotBeforeEndTime_throwsInvalidSaleWindow() {
        assertThatThrownBy(() -> flashSaleService.create(new CreateFlashSaleRequest(productId, 10, end, start)))
                .isInstanceOf(InvalidSaleWindowException.class);
    }

    @Test
    void create_whenStartTimeEqualsEndTime_throwsInvalidSaleWindow() {
        assertThatThrownBy(() -> flashSaleService.create(new CreateFlashSaleRequest(productId, 10, start, start)))
                .isInstanceOf(InvalidSaleWindowException.class);
    }

    @Test
    void findById_whenMissing_throwsFlashSaleNotFound() {
        UUID id = UUID.randomUUID();
        when(flashSaleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flashSaleService.findById(id))
                .isInstanceOf(FlashSaleNotFoundException.class);
    }
}
