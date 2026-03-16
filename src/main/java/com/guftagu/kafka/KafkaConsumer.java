package com.guftagu.kafka;

import com.guftagu.config.KafkaConfig;
import com.guftagu.model.Message;
import com.guftagu.repository.MessageRepository;
import com.guftagu.service.MessageService;
import com.guftagu.websocket.WebSocketMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final WebSocketMessagePublisher webSocketPublisher;

    @KafkaListener(topics = KafkaConfig.MESSAGES_TOPIC, groupId = "guftagu-group")
    public void consume(Message message) {
        log.info("Received message from Kafka topic: {}", message.getId());

        try {
            // 1. Save message to MongoDB as per specification
            Message savedMessage = messageRepository.save(message);
            log.info("Message saved to MongoDB: {}", savedMessage.getId());

            // 2. Update conversation metadata (last message, unread count)
            // We use MessageService for these cross-cutting concerns
            messageService.updateConversationLastMessage(savedMessage);
            messageService.incrementUnreadCount(savedMessage);

            // 3. Call WebSocket publisher to broadcast the message
            webSocketPublisher.publishMessage(savedMessage);

        } catch (Exception e) {
            log.error("Error processing consumed Kafka message", e);
        }
    }
}
