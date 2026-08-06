package com.thierno.flashsaleservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public RecordMessageConverter recordMessageConverter() {
        return new StringJacksonJsonMessageConverter();
    }
}
