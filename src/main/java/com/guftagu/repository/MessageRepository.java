package com.guftagu.repository;

import com.guftagu.model.Message;
import com.guftagu.model.MessageType;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByConversationIdOrderByTimestampAsc(String conversationId);
    List<Message> findByReceiverIdAndSeen(String receiverId, boolean seen);

    // For disappearing messages — delete expired
    long deleteByExpiresAtNotNullAndExpiresAtLessThan(long timestamp);

    // For media/links/docs page
    List<Message> findByConversationIdAndTypeInOrderByTimestampDesc(String conversationId, List<MessageType> types);

    // Find starred messages for a specific user
    List<Message> findByStarredTrueAndSenderIdOrReceiverIdOrderByTimestampDesc(String senderId, String receiverId);
}
