package com.guftagu.service;

import com.guftagu.model.Message;
import com.guftagu.model.User;
import com.guftagu.repository.ConversationRepository;
import com.guftagu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Push notification service using Firebase Cloud Messaging.
 * NOTE: Requires Firebase Admin SDK setup with credentials.
 * Until Firebase is configured, notifications will be logged but not sent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    /**
     * Send push notification to a user when they receive a new message.
     */
    public void sendMessageNotification(Message message) {
        if (message == null || message.getReceiverId() == null) return;

        userRepository.findById(message.getReceiverId()).ifPresent(receiver -> {
            String deviceToken = receiver.getDeviceToken();
            if (deviceToken == null || deviceToken.isEmpty()) {
                log.debug("No device token for user {}, skipping push notification", message.getReceiverId());
                return;
            }

            // Check if receiver has muted this conversation
            boolean isMuted = conversationRepository.findById(message.getConversationId())
                    .map(conv -> conv.getMutedByUsers() != null && conv.getMutedByUsers().contains(message.getReceiverId()))
                    .orElse(false);

            if (isMuted) {
                log.debug("User {} has muted conversation {}, skipping push notification", message.getReceiverId(), message.getConversationId());
                return;
            }

            // Get sender name
            String senderName = userRepository.findById(message.getSenderId())
                    .map(User::getName)
                    .orElse("Someone");

            // Message preview (truncate to 100 chars)
            String body = message.getContent();
            if (body == null || body.isEmpty()) {
                body = "[" + (message.getType() != null ? message.getType().name() : "Message") + "]";
            }
            if (body.length() > 100) {
                body = body.substring(0, 100) + "...";
            }

            // TODO: Integrate Firebase Admin SDK to actually send the notification
            // For now, log the notification details
            log.info("PUSH NOTIFICATION → to: {} (token: {}), title: {}, body: {}",
                    message.getReceiverId(), deviceToken.substring(0, Math.min(10, deviceToken.length())) + "...",
                    senderName, body);

            try {
                com.google.firebase.messaging.Message fcmMessage = com.google.firebase.messaging.Message.builder()
                    .setToken(deviceToken)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(senderName)
                        .setBody(body)
                        .build())
                    .putData("conversationId", message.getConversationId())
                    .putData("senderId", message.getSenderId())
                    .build();

                com.google.firebase.messaging.FirebaseMessaging.getInstance().sendAsync(fcmMessage);
            } catch (Exception e) {
                log.error("Failed to send Firebase notification: ", e);
            }
        });
    }
}
