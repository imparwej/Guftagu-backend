package com.guftagu.repository;

import com.guftagu.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    @Query("{$or: [{user1Id: ?0, user2Id: ?1}, {user1Id: ?1, user2Id: ?0}]}")
    Optional<Conversation> findConversationBetweenUsers(String user1Id, String user2Id);

    List<Conversation> findByUser1IdOrUser2IdOrderByLastMessageTimeDesc(String user1Id, String user2Id);
}
