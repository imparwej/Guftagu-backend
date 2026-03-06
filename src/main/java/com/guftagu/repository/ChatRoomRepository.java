package com.guftagu.repository;

import com.guftagu.model.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    // Get all chat rooms where a user is a participant
    List<ChatRoom> findByParticipantsContaining(String userId);

    // Find chat room between two users
    Optional<ChatRoom> findByParticipantsContainingAndParticipantsContaining(
            String user1,
            String user2
    );
}