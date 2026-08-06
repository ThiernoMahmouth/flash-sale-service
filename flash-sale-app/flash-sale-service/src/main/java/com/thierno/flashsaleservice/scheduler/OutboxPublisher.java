package com.thierno.flashsaleservice.scheduler;

import com.thierno.flashsaleservice.outbox.OutboxEvent;
import com.thierno.flashsaleservice.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> rawKafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending =
                outboxService.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        if (pending.isEmpty()) return;

        log.debug("Publishing {} pending outbox events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                rawKafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish outbox event id={} topic={}",
                                        event.getId(), event.getTopic(), ex);
                            }
                        });

                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
                outboxService.saveOrUpdate(event);

            } catch (Exception e) {
                log.error("Error processing outbox event id={}", event.getId(), e);
            }
        }
    }
}
