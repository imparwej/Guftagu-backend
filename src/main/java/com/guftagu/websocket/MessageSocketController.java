package com.guftagu.websocket;

import com.guftagu.model.ChatRoom;
import com.guftagu.model.Message;
import com.guftagu.repository.ChatRoomRepository;
import com.guftagu.service.ChatService;
import com.guftagu.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserService userService;
    private final ChatRoomRepository chatRoomRepository;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Message message) {
        log.info("Received message via WebSocket: {}", message);
        
        // Save message in MongoDB
        Message savedMessage = chatService.saveMessage(message);
        
        // Broadcast to all participants in the chat room
        Optional<ChatRoom> chatRoom = chatRoomRepository.findById(message.getRoomId());
        
        if (chatRoom.isPresent()) {
            chatRoom.get().getParticipants().forEach(participantId -> {
                log.info("Broadcasting message to participant: {}", participantId);
                messagingTemplate.convertAndSendToUser(
                        participantId,
                        "/queue/messages",
                        savedMessage
                );
            });
        } else {
            log.warn("ChatRoom not found: {}", message.getRoomId());
            // Fallback: send to receiverId if roomId lookup fails (backward compatibility/safety)
            if (message.getReceiverId() != null) {
                messagingTemplate.convertAndSendToUser(
                        message.getReceiverId(),
                        "/queue/messages",
                        savedMessage
                );
            }
        }
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            String phoneNumber = principal.getName();
            log.info("User connected: {}", phoneNumber);
            userService.updateOnlineStatus(phoneNumber, true);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            String phoneNumber = principal.getName();
            log.info("User disconnected: {}", phoneNumber);
            userService.updateOnlineStatus(phoneNumber, false);
        }
    }
}
