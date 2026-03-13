package com.guftagu.repository;

import com.guftagu.model.Message;
import com.guftagu.model.MessageType;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByConversationIdOrderByTimestampAsc(String conversationId);
    List<Message> findByReceiverIdAndSeen(String receiverId, boolean seen);

    Message findTopByConversationIdOrderByTimestampDesc(String conversationId);
    int countByConversationIdAndReceiverIdAndSeenFalse(String conversationId, String receiverId);

    @org.springframework.data.mongodb.repository.Query(value = "{'conversationId': ?0}", delete = true)
    void deleteByChatId(String chatId);

    // For disappearing messages — delete expired
    long deleteByExpiresAtNotNullAndExpiresAtLessThan(long timestamp);

    // For clearing entire chat history
    void deleteByConversationId(String conversationId);

    // For media/links/docs page (legacy)
    List<Message> findByConversationIdAndTypeInOrderByTimestampDesc(String conversationId, List<MessageType> types);

    // Explicit APIs for Media (IMAGE, VIDEO)
    List<Message> findByConversationIdAndTypeIn(String conversationId, List<MessageType> types);

    // Explicit APIs for Docs / Links
    List<Message> findByConversationIdAndType(String conversationId, MessageType type);

    // Find starred messages for a specific user
    List<Message> findByStarredTrueAndSenderIdOrReceiverIdOrderByTimestampDesc(String senderId, String receiverId);
}
