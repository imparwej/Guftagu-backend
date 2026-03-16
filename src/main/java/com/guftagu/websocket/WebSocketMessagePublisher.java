package com.guftagu.websocket;

import com.guftagu.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMessage(Message message) {
        String destination = "/topic/messages/" + message.getConversationId();
        log.info("Broadcasting message to WebSocket topic: {}", destination);
        messagingTemplate.convertAndSend(destination, message);
        
        // Also notify user directly for private messaging support
        messagingTemplate.convertAndSendToUser(message.getReceiverId(), "/queue/messages", message);
    }

    public void publishTypingStatus(String receiverId, Object payload) {
        log.info("Sending typing status to user: {}", receiverId);
        messagingTemplate.convertAndSendToUser(receiverId, "/queue/typing", payload);
    }

    public void publishLiveLocation(String conversationId, Object payload) {
        String destination = "/topic/location/" + conversationId;
        log.info("Broadcasting live location to topic: {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }
}
