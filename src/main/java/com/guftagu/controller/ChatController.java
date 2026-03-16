package com.guftagu.controller;

import com.guftagu.model.Message;
import com.guftagu.model.MessageType;
import com.guftagu.service.MessageService;
import com.guftagu.service.PushNotificationService;
import com.guftagu.service.TypingService;
import com.guftagu.service.LocationService;
import com.guftagu.dto.LocationUpdate;
import com.guftagu.service.KafkaProducer;
import com.guftagu.websocket.WebSocketMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final PushNotificationService pushNotificationService;
    private final TypingService typingService;
    private final LocationService locationService;
    private final KafkaProducer kafkaProducer;
    private final WebSocketMessagePublisher webSocketPublisher;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Message message) {
        if (message == null) {
            return;
        }
        if (message.getReceiverId() == null || message.getReceiverId().isEmpty()) {
            System.err.println("ChatController: receiverId is null/empty, rejecting message");
            return;
        }
        if (message.getSenderId() == null || message.getSenderId().isEmpty()) {
            System.err.println("ChatController: senderId is null/empty, rejecting message");
            return;
        }

        // Validate message type — default to TEXT if unknown
        if (message.getType() == null) {
            message.setType(MessageType.TEXT);
        }

        // Block check
        if (messageService.isBlocked(message.getReceiverId(), message.getSenderId())) {
            log.info("Message from {} to {} dropped — sender is blocked", message.getSenderId(), message.getReceiverId());
            return;
        }

        if (messageService.isBlocked(message.getSenderId(), message.getReceiverId())) {
            log.info("Message from {} to {} dropped — receiver is blocked by sender", message.getSenderId(), message.getReceiverId());
            return;
        }

        // Prepare message metadata (timestamp, etc.) via service but DO NOT save to DB here.
        // The KafkaConsumer will handle persistence to ensure decoupling.
        Message preparedMessage = messageService.prepareMessage(message);

        // Push to Kafka for persistence and asynchronous delivery
        kafkaProducer.sendMessage(preparedMessage);

        // Send push notification to receiver (external to the primary messaging flow)
        try {
            pushNotificationService.sendMessageNotification(preparedMessage);
        } catch (Exception e) {
            log.warn("Failed to send push notification: {}", e.getMessage());
        }
    }


    @MessageMapping("/chat.typing")
    public void typingStatus(@Payload Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        String receiverId = (String) payload.get("receiverId");
        String senderId = (String) payload.get("senderId");
        String chatId = (String) payload.get("chatId");

        if (chatId != null && senderId != null) {
            typingService.setTypingStatus(chatId, senderId);
        }

        if (receiverId != null && !receiverId.isEmpty()) {
            webSocketPublisher.publishTypingStatus(receiverId, payload);
        }
    }

    @MessageMapping("/chat.liveLocation")
    public void liveLocation(@Payload LocationUpdate payload) {
        if (payload == null) {
            return;
        }
        
        String conversationId = payload.getConversationId();
        String userId = payload.getUserId();
        if (userId == null) userId = payload.getSenderId();
        
        log.debug("Received live location update for conversation {}: {}", conversationId, payload);
        
        if (conversationId != null && userId != null) {
            locationService.updateLiveLocation(conversationId, userId, payload);
            webSocketPublisher.publishLiveLocation(conversationId, payload);
            log.debug("Broadcasted location update to /topic/location/{}", conversationId);
        } else {
            log.warn("Payload missing conversationId or userId: {}", payload);
        }
    }
}
