package com.guftagu.config;

import com.guftagu.model.User;
import com.guftagu.repository.UserRepository;
import com.guftagu.security.UserPrincipal;
import com.guftagu.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Track active sessions: SessionId -> UserId
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        if (event.getUser() instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken) event.getUser();
            if (userToken != null && userToken.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) userToken.getPrincipal();
                String userId = principal.getUser().getId();
                
                if (userId != null) {
                    activeSessions.put(sessionId, userId);
                    
                    userRepository.findById(userId).ifPresent(user -> {
                        user.setOnlineStatus(true);
                        userRepository.save(user);

                        // Broadcast presence
                        Map<String, Object> presenceUpdate = Map.of(
                                "userId", userId,
                                "isOnline", true,
                                "lastSeen", user.getLastSeen() != null ? user.getLastSeen() : LocalDateTime.now()
                        );
                        messagingTemplate.convertAndSend("/topic/presence", presenceUpdate);
                        log.info("User {} connected via WebSocket {}", userId, sessionId);
                    });

                    messageService.markMessagesAsDelivered(userId);
                }
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = activeSessions.remove(sessionId);

        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                user.setOnlineStatus(false);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);

                // Broadcast presence
                Map<String, Object> presenceUpdate = Map.of(
                        "userId", userId,
                        "isOnline", false,
                        "lastSeen", user.getLastSeen()
                );
                messagingTemplate.convertAndSend("/topic/presence", presenceUpdate);
                log.info("User {} disconnected from WebSocket {}", userId, sessionId);
            });
        }
    }
}
