package com.guftagu.service;

import com.guftagu.dto.ChatListDTO;
import com.guftagu.model.Chat;
import com.guftagu.model.Message;
import com.guftagu.model.User;
import com.guftagu.repository.ChatRepository;
import com.guftagu.repository.MessageRepository;
import com.guftagu.repository.UserRepository;
import com.guftagu.service.PresenceService;
import com.guftagu.service.TypingService;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final PresenceService presenceService;
    private final TypingService typingService;

    @Data
    private static class LastMessageResult {
        private String id;
        private Message lastMessage;
    }



    @Data
    private static class UnreadCountResult {
        private String id;
        private int unreadCount;
    }

    public List<ChatListDTO> getChats(String userId) {
        List<Chat> chats = chatRepository.findByParticipantsContaining(userId)
                .stream()
                .filter(chat -> chat.getDeletedByUsers() == null || !chat.getDeletedByUsers().contains(userId))
                .collect(Collectors.toList());

        if (chats.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chatIds = chats.stream().map(Chat::getId).collect(Collectors.toList());
        List<String> otherUserIds = chats.stream().map(chat -> chat.getParticipants().stream()
                .filter(id -> !id.equals(userId))
                .findFirst().orElse(userId)).distinct().collect(Collectors.toList());

        Map<String, User> userMap = new HashMap<>();
        userRepository.findAllById(otherUserIds).forEach(u -> userMap.put(u.getId(), u));

        // Aggregate Last Messages
        Aggregation lastMsgAgg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("conversationId").in(chatIds)),
                Aggregation.sort(Sort.Direction.DESC, "timestamp"),
                Aggregation.group("conversationId").first("$$ROOT").as("lastMessage")
        );
        Map<String, Message> lastMessageMap = new HashMap<>();
        mongoTemplate.aggregate(lastMsgAgg, "messages", LastMessageResult.class)
                .getMappedResults()
                .forEach(res -> lastMessageMap.put(res.getId(), res.getLastMessage()));

        // Aggregate Unread Counts
        Aggregation unreadAgg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("conversationId").in(chatIds)
                        .and("receiverId").is(userId)
                        .and("seen").is(false)),
                Aggregation.group("conversationId").count().as("unreadCount")
        );
        Map<String, Integer> unreadCountMap = new HashMap<>();
        mongoTemplate.aggregate(unreadAgg, "messages", UnreadCountResult.class)
                .getMappedResults()
                .forEach(res -> unreadCountMap.put(res.getId(), res.getUnreadCount()));

        return chats.stream()
                .map(chat -> {
                    String otherUserId = chat.getParticipants().stream()
                            .filter(id -> !id.equals(userId))
                            .findFirst().orElse(userId);

                    User otherUser = userMap.get(otherUserId);
                    String name = otherUser != null ? otherUser.getName() : "Unknown";
                    String avatar = otherUser != null ? otherUser.getProfilePicture() : null;

                    Message lastMessage = lastMessageMap.get(chat.getId());
                    String lastMessageText = lastMessage != null ? lastMessage.getContent() : null;
                    java.time.LocalDateTime lastMessageTime = lastMessage != null ? 
                            java.time.Instant.ofEpochMilli(lastMessage.getTimestamp()).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;

                    int unreadCount = unreadCountMap.getOrDefault(chat.getId(), 0);

                    boolean isMuted = chat.getMutedByUsers() != null && chat.getMutedByUsers().contains(userId);
                    boolean isPinned = chat.getPinnedByUsers() != null && chat.getPinnedByUsers().contains(userId);
                    boolean isTyping = typingService.isOtherUserTyping(chat.getId(), userId);
                    boolean isOnline = presenceService.isUserOnline(otherUserId);
                    java.time.LocalDateTime lastSeen = otherUser != null ? otherUser.getLastSeen() : null;

                    return ChatListDTO.builder()
                            .chatId(chat.getId())
                            .userId(otherUserId)
                            .name(name)
                            .profilePicture(avatar)
                            .lastMessage(lastMessageText)
                            .lastMessageTime(lastMessageTime)
                            .unreadCount(unreadCount)
                            .isMuted(isMuted)
                            .isPinned(isPinned)
                            .isTyping(isTyping)
                            .isOnline(isOnline)
                            .lastSeen(lastSeen)
                            .build();
                })
                .sorted((c1, c2) -> {
                    if (c1.isPinned() != c2.isPinned()) {
                        return c1.isPinned() ? -1 : 1;
                    }
                    if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
                    if (c1.getLastMessageTime() == null) return 1;
                    if (c2.getLastMessageTime() == null) return -1;
                    return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
                })
                .collect(Collectors.toList());
    }

    public void pinChat(String chatId, String userId) {
        chatRepository.findById(chatId).ifPresent(chat -> {
            if (chat.getPinnedByUsers() == null) chat.setPinnedByUsers(new ArrayList<>());
            if (!chat.getPinnedByUsers().contains(userId)) {
                chat.getPinnedByUsers().add(userId);
                chatRepository.save(chat);
            }
        });
    }

    public void unpinChat(String chatId, String userId) {
        chatRepository.findById(chatId).ifPresent(chat -> {
            if (chat.getPinnedByUsers() != null) {
                chat.getPinnedByUsers().remove(userId);
                chatRepository.save(chat);
            }
        });
    }

    public void muteChat(String chatId, String userId) {
        chatRepository.findById(chatId).ifPresent(chat -> {
            if (chat.getMutedByUsers() == null) chat.setMutedByUsers(new ArrayList<>());
            if (!chat.getMutedByUsers().contains(userId)) {
                chat.getMutedByUsers().add(userId);
                chatRepository.save(chat);
            }
        });
    }

    public void unmuteChat(String chatId, String userId) {
        chatRepository.findById(chatId).ifPresent(chat -> {
            if (chat.getMutedByUsers() != null) {
                chat.getMutedByUsers().remove(userId);
                chatRepository.save(chat);
            }
        });
    }

    public void deleteChat(String chatId, String userId) {
        chatRepository.findById(chatId).ifPresent(chat -> {
            if (chat.getDeletedByUsers() == null) chat.setDeletedByUsers(new ArrayList<>());
            if (!chat.getDeletedByUsers().contains(userId)) {
                chat.getDeletedByUsers().add(userId);
            }
            
            // If both users deleted it, remove chat completely
            boolean allDeleted = chat.getParticipants().stream().allMatch(p -> chat.getDeletedByUsers().contains(p));
            if (allDeleted) {
                chatRepository.delete(chat);
                messageRepository.deleteByConversationId(chatId);
            } else {
                chatRepository.save(chat);
            }
        });
    }

    public void clearChat(String chatId, String userId) {
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(chatId);
        messages.forEach(m -> {
            if (m.getDeletedFor() == null) {
                m.setDeletedFor(new ArrayList<>());
            }
            if (!m.getDeletedFor().contains(userId)) {
                m.getDeletedFor().add(userId);
                messageRepository.save(m);
            }
        });
    }

    public void markMessagesAsRead(String chatId, String userId) {
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(chatId);
        messages.stream()
                .filter(m -> userId.equals(m.getReceiverId()) && !m.isSeen())
                .forEach(m -> {
                    m.setSeen(true);
                    messageRepository.save(m);
                });
    }
}
