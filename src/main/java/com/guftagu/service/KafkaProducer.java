package com.guftagu.service;

import com.guftagu.config.KafkaConfig;
import com.guftagu.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(Message message) {
        log.info("Publishing message to Kafka topic {}: {}", KafkaConfig.MESSAGES_TOPIC, message.getId());
        kafkaTemplate.send(KafkaConfig.MESSAGES_TOPIC, message.getConversationId(), message);
    }
}
