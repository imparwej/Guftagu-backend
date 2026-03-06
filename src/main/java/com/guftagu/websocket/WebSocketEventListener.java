package com.guftagu.websocket;

import com.guftagu.model.User;
import com.guftagu.repository.UserRepository;
import com.guftagu.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

@Component
@Slf4j
public class WebSocketEventListener {

    @Autowired
    private UserRepository userRepository;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
        
        if (userToken != null) {
            UserPrincipal principal = (UserPrincipal) userToken.getPrincipal();
            String userId = principal.getUser().getId();
            log.info("User connected: {}", userId);
            
            userRepository.findById(userId).ifPresent(user -> {
                user.setOnlineStatus(true);
                userRepository.save(user);
            });
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();

        if (userToken != null) {
            UserPrincipal principal = (UserPrincipal) userToken.getPrincipal();
            String userId = principal.getUser().getId();
            log.info("User disconnected: {}", userId);

            userRepository.findById(userId).ifPresent(user -> {
                user.setOnlineStatus(false);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);
            });
        }
    }
}
