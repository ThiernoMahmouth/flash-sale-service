package com.thierno.flashsaleservice.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void save(String topic, String aggregateId, Object event) {
        try {
            OutboxEvent outbox = new OutboxEvent();
            outbox.setTopic(topic);
            outbox.setAggregateId(aggregateId);
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outboxEventRepository.save(outbox);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }

    public List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status) {
        return outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(status);
    }

    public OutboxEvent saveOrUpdate(OutboxEvent outboxEvent) {
        return outboxEventRepository.save(outboxEvent);
    }
}
