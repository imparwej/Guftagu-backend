package com.guftagu.controller;

import com.guftagu.model.Message;
import com.guftagu.model.MessageType;
import com.guftagu.service.MessageService;
import com.guftagu.service.PushNotificationService;
import com.guftagu.service.TypingService;
import com.guftagu.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final PushNotificationService pushNotificationService;
    private final TypingService typingService;
    private final LocationService locationService;

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
            try {
                // Attempt to parse if it was sent as string somehow
                message.setType(MessageType.TEXT);
            } catch (Exception e) {
                message.setType(MessageType.TEXT);
            }
        }

        // Block check: if receiver has blocked the sender, silently drop the message
        if (messageService.isBlocked(message.getReceiverId(), message.getSenderId())) {
            log.info("Message from {} to {} dropped — sender is blocked", message.getSenderId(), message.getReceiverId());
            return;
        }

        // Also check reverse: if sender has blocked receiver, don't allow sending
        if (messageService.isBlocked(message.getSenderId(), message.getReceiverId())) {
            log.info("Message from {} to {} dropped — receiver is blocked by sender", message.getSenderId(), message.getReceiverId());
            return;
        }

        Message savedMessage = messageService.sendMessage(message);
        if (savedMessage == null) {
            System.err.println("ChatController: sendMessage returned null, not sending");
            return;
        }
        // Send to receiver using their user ID (Principal is now user ID)
        messagingTemplate.convertAndSendToUser(savedMessage.getReceiverId(), "/queue/messages", savedMessage);
        // Send confirmation back to sender
        messagingTemplate.convertAndSendToUser(savedMessage.getSenderId(), "/queue/messages", savedMessage);

        // Send push notification to receiver
        try {
            pushNotificationService.sendMessageNotification(savedMessage);
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
            messagingTemplate.convertAndSendToUser(receiverId, "/queue/typing", payload);
        }
    }

    @MessageMapping("/chat.liveLocation")
    public void liveLocation(@Payload Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        
        String conversationId = (String) payload.get("conversationId");
        String userId = (String) payload.get("userId");
        if (userId == null) userId = (String) payload.get("senderId");
        
        if (conversationId != null && userId != null) {
            locationService.updateLiveLocation(conversationId, userId, payload);
            // Broadcast to the new topic requested
            messagingTemplate.convertAndSend("/topic/location/" + conversationId, payload);
        }
    }
}
