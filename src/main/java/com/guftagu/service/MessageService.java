package com.guftagu.service;

import com.guftagu.model.Conversation;
import com.guftagu.model.Message;
import com.guftagu.model.MessageType;
import com.guftagu.dto.ConversationResponse;
import com.guftagu.model.User;
import com.guftagu.repository.ConversationRepository;
import com.guftagu.repository.MessageRepository;
import com.guftagu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public Conversation createConversationIfNotExists(String user1Id, String user2Id) {
        return conversationRepository.findConversationBetweenUsers(user1Id, user2Id)
                .orElseGet(() -> {
                    Conversation conversation = Conversation.builder()
                            .user1Id(user1Id)
                            .user2Id(user2Id)
                            .unreadCountUser1(0)
                            .unreadCountUser2(0)
                            .lastMessageTime(LocalDateTime.now())
                            .build();
                    return conversationRepository.save(conversation);
                });
    }

    public Message sendMessage(Message message) {
        // Resolve conversationId if it's missing or a placeholder 'new'
        if (message.getConversationId() == null || message.getConversationId().isEmpty() || "new".equals(message.getConversationId())) {
            Conversation conversation = createConversationIfNotExists(message.getSenderId(), message.getReceiverId());
            message.setConversationId(conversation.getId());
        }

        message.setTimestamp(System.currentTimeMillis());
        message.setDelivered(false);
        message.setSeen(false);

        // Initialize lists if null (safety)
        if (message.getDeletedFor() == null) {
            message.setDeletedFor(new ArrayList<>());
        }

        Message savedMessage = messageRepository.save(message);
        updateConversationLastMessage(savedMessage);
        incrementUnreadCount(savedMessage);
        return savedMessage;
    }

    public void updateConversationLastMessage(Message message) {
        conversationRepository.findById(message.getConversationId()).ifPresent(conversation -> {
            String lastMsg = message.getContent();
            if (message.getType() != null && message.getType() != MessageType.TEXT && (lastMsg == null || lastMsg.isEmpty())) {
                lastMsg = "[" + message.getType().name() + "]";
            }
            conversation.setLastMessage(lastMsg);
            conversation.setLastMessageTime(LocalDateTime.now());
            conversationRepository.save(conversation);
        });
    }

    public void incrementUnreadCount(Message message) {
        conversationRepository.findById(message.getConversationId()).ifPresent(conversation -> {
            if (message.getReceiverId().equals(conversation.getUser1Id())) {
                conversation.setUnreadCountUser1(conversation.getUnreadCountUser1() + 1);
            } else if (message.getReceiverId().equals(conversation.getUser2Id())) {
                conversation.setUnreadCountUser2(conversation.getUnreadCountUser2() + 1);
            }
            conversationRepository.save(conversation);
        });
    }

    public void markMessagesAsRead(String conversationId, String userId) {
        if (conversationId == null || userId == null) return;
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        messages.stream()
                .filter(m -> userId.equals(m.getReceiverId()) && !m.isSeen())
                .forEach(m -> {
                    m.setSeen(true);
                    m.setDelivered(true);
                    messageRepository.save(m);
                });

        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            if (userId.equals(conversation.getUser1Id())) {
                conversation.setUnreadCountUser1(0);
            } else if (userId.equals(conversation.getUser2Id())) {
                conversation.setUnreadCountUser2(0);
            }
            conversationRepository.save(conversation);
        });
    }

    public void markMessagesAsDelivered(String userId) {
        List<Message> undeliveredMessages = messageRepository.findByReceiverIdAndSeen(userId, false);
        undeliveredMessages.forEach(m -> {
            if (!m.isDelivered()) {
                m.setDelivered(true);
                messageRepository.save(m);
            }
        });
    }

    public List<ConversationResponse> getChatList(String userId) {
        List<Conversation> conversations = conversationRepository.findByUser1IdOrUser2IdOrderByLastMessageTimeDesc(userId, userId);
        return conversations.stream()
                .map(conversation -> {
                    String otherUserId = conversation.getUser1Id().equals(userId) ? conversation.getUser2Id() : conversation.getUser1Id();
                    Optional<User> otherUserOpt = userRepository.findById(otherUserId);
                    
                    String name = otherUserOpt.map(User::getName).orElse("Unknown");
                    String avatar = otherUserOpt.map(User::getProfilePicture).orElse(null);
                    int unreadCount = conversation.getUser1Id().equals(userId) ? conversation.getUnreadCountUser1() : conversation.getUnreadCountUser2();

                    return ConversationResponse.builder()
                            .conversationId(conversation.getId())
                            .otherUserId(otherUserId)
                            .otherUserName(name)
                            .otherUserAvatar(avatar)
                            .lastMessage(conversation.getLastMessage())
                            .lastMessageTime(conversation.getLastMessageTime())
                            .unreadCount(unreadCount)
                            .isPinned(conversation.getPinnedBy() != null && conversation.getPinnedBy().contains(userId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get message history, filtering out messages deleted for the requesting user.
     */
    public List<Message> getMessageHistory(String conversationId, String userId) {
        List<Message> allMessages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        if (userId == null || userId.isEmpty()) {
            return allMessages;
        }
        return allMessages.stream()
                .filter(m -> m.getDeletedFor() == null || !m.getDeletedFor().contains(userId))
                .collect(Collectors.toList());
    }

    /**
     * Legacy overload for backward compatibility.
     */
    public List<Message> getMessageHistory(String conversationId) {
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

    public void updateMessageStatus(String messageId, boolean delivered, boolean seen) {
        messageRepository.findById(messageId).ifPresent(message -> {
            message.setDelivered(delivered);
            message.setSeen(seen);
            messageRepository.save(message);
        });
    }

    /**
     * Delete a single message (for current user OR for everyone).
     */
    public Message deleteMessage(String messageId, String userId, boolean forEveryone) {
        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isPresent()) {
            Message message = msgOpt.get();
            if (forEveryone) {
                // Check if message is less than 30 minutes old
                if (System.currentTimeMillis() - message.getTimestamp() < 30 * 60 * 1000) {
                    message.setDeletedForEveryone(true);
                    message.setContent("This message was deleted");
                    message.setMediaUrl(null);
                    message.setType(MessageType.TEXT); // Fallback to text so the deleted string renders
                }
            } else {
                if (message.getDeletedFor() == null) {
                    message.setDeletedFor(new ArrayList<>());
                }
                if (!message.getDeletedFor().contains(userId)) {
                    message.getDeletedFor().add(userId);
                }
            }
            return messageRepository.save(message);
        }
        return null;
    }


    /**
     * Toggle starred status of a message.
     */
    public Message toggleMessageStar(String messageId) {
        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isPresent()) {
            Message message = msgOpt.get();
            message.setStarred(!message.isStarred());
            return messageRepository.save(message);
        }
        return null;
    }

    /**
     * Get all starred messages for a specific user across all conversations.
     */
    public List<Message> getStarredMessages(String userId) {
        return messageRepository.findByStarredTrueAndSenderIdOrReceiverIdOrderByTimestampDesc(userId, userId);
    }

    /**
     * Clear chat for a specific user — adds userId to deletedFor for all messages in the conversation.
     */
    public void clearChatForUser(String conversationId, String userId) {
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        messages.forEach(m -> {
            if (m.getDeletedFor() == null) {
                m.setDeletedFor(new ArrayList<>());
            }
            if (!m.getDeletedFor().contains(userId)) {
                m.getDeletedFor().add(userId);
                messageRepository.save(m);
            }
        });

        // Reset unread count
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            if (userId.equals(conversation.getUser1Id())) {
                conversation.setUnreadCountUser1(0);
            } else if (userId.equals(conversation.getUser2Id())) {
                conversation.setUnreadCountUser2(0);
            }
            conversation.setLastMessage(null);
            conversationRepository.save(conversation);
        });
    }

    /**
     * Clear all messages in a chat (deletes from DB for both users).
     */
    public void clearChat(String conversationId) {
        messageRepository.deleteByConversationId(conversationId);
        
        // Reset last message and unread counts
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setLastMessage(null);
            conversation.setUnreadCountUser1(0);
            conversation.setUnreadCountUser2(0);
            conversationRepository.save(conversation);
        });
    }

    /**
     * Mute a conversation for a specific user.
     */
    public void muteChat(String conversationId, String userId) {
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            if (conversation.getMutedByUsers() == null) {
                conversation.setMutedByUsers(new ArrayList<>());
            }
            if (!conversation.getMutedByUsers().contains(userId)) {
                conversation.getMutedByUsers().add(userId);
                conversationRepository.save(conversation);
            }
        });
    }

    /**
     * Unmute a conversation for a specific user.
     */
    public void unmuteChat(String conversationId, String userId) {
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            if (conversation.getMutedByUsers() != null) {
                conversation.getMutedByUsers().remove(userId);
                conversationRepository.save(conversation);
            }
        });
    }


    /**
     * Get media messages for the Media/Links/Docs screen.
     */
    public List<Message> getMediaMessages(String conversationId, String category) {
        List<MessageType> types;
        switch (category) {
            case "media":
                types = List.of(MessageType.IMAGE, MessageType.GIF, MessageType.VIDEO);
                break;
            case "docs":
                types = List.of(MessageType.DOCUMENT);
                break;
            case "links":
                types = List.of(MessageType.LINK);
                break;
            default:
                types = List.of(MessageType.IMAGE, MessageType.GIF, MessageType.VIDEO, MessageType.DOCUMENT, MessageType.LINK);
                break;
        }
        return messageRepository.findByConversationIdAndTypeInOrderByTimestampDesc(conversationId, types);
    }

    /**
     * Explicit APIs requested by the user for fetching media types specifically.
     */
    public List<Message> getMedia(String chatId) {
        return messageRepository.findByConversationIdAndTypeIn(chatId, List.of(MessageType.IMAGE, MessageType.VIDEO));
    }

    public List<Message> getLinks(String chatId) {
        return messageRepository.findByConversationIdAndType(chatId, MessageType.LINK);
    }

    public List<Message> getDocs(String chatId) {
        return messageRepository.findByConversationIdAndType(chatId, MessageType.DOCUMENT);
    }


    /**
     * Check if a user is blocked by another user.
     */
    public boolean isBlocked(String blockerId, String blockedId) {
        return userRepository.findById(blockerId)
                .map(user -> user.getBlockedUsers() != null && user.getBlockedUsers().contains(blockedId))
                .orElse(false);
    }

    /**
     * React to a message — only one reaction per user.
     */
    public Message reactToMessage(String messageId, String userId, String reaction) {
        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isPresent()) {
            Message message = msgOpt.get();
            if (message.getReactions() == null) {
                message.setReactions(new HashMap<>());
            }
            // If same reaction already set, remove it (toggle); otherwise set/update
            String existing = message.getReactions().get(userId);
            if (reaction.equals(existing)) {
                message.getReactions().remove(userId);
            } else {
                message.getReactions().put(userId, reaction);
            }
            return messageRepository.save(message);
        }
        return null;
    }

    /**
     * Edit a message — only within 15 minutes of sending.
     */
    public Message editMessage(String messageId, String newContent) {
        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isPresent()) {
            Message message = msgOpt.get();
            long fifteenMinutes = 15 * 60 * 1000;
            if (System.currentTimeMillis() - message.getTimestamp() < fifteenMinutes) {
                message.setContent(newContent);
                message.setEdited(true);
                message.setEditedAt(System.currentTimeMillis());
                return messageRepository.save(message);
            }
        }
        return null;
    }

    /**
     * Toggle pin status for a conversation.
     */
    public boolean togglePin(String conversationId, String userId) {
        Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
        if (convOpt.isPresent()) {
            Conversation conv = convOpt.get();
            if (conv.getPinnedBy() == null) {
                conv.setPinnedBy(new ArrayList<>());
            }
            boolean isPinned;
            if (conv.getPinnedBy().contains(userId)) {
                conv.getPinnedBy().remove(userId);
                isPinned = false;
            } else {
                conv.getPinnedBy().add(userId);
                isPinned = true;
            }
            conversationRepository.save(conv);
            return isPinned;
        }
        return false;
    }
}
