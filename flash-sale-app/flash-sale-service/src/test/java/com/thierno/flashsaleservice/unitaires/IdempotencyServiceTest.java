package com.thierno.flashsaleservice.unitaires;

import com.thierno.flashsaleservice.idempotency.IdempotencyService;
import com.thierno.flashsaleservice.idempotency.ProcessedEvent;
import com.thierno.flashsaleservice.idempotency.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    void isDuplicate_whenEventIdExists_returnsTrue() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        assertThat(idempotencyService.isDuplicate(eventId)).isTrue();
    }

    @Test
    void isDuplicate_whenEventIdUnknown_returnsFalse() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        assertThat(idempotencyService.isDuplicate(eventId)).isFalse();
    }

    @Test
    void markProcessed_savesEventIdAndType() {
        UUID eventId = UUID.randomUUID();

        idempotencyService.markProcessed(eventId, "PURCHASE_CONFIRMED");

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getEventType()).isEqualTo("PURCHASE_CONFIRMED");
    }
}
