package com.guftagu.service;

import com.guftagu.model.ChatRoom;
import com.guftagu.model.Message;
import com.guftagu.repository.ChatRoomRepository;
import com.guftagu.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;

    public ChatRoom startChat(String userId, String targetUserId) {

        Optional<ChatRoom> existingRoom =
                chatRoomRepository.findByParticipantsContainingAndParticipantsContaining(
                        userId,
                        targetUserId
                );

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        ChatRoom newRoom = ChatRoom.builder()
                .participants(List.of(userId, targetUserId))
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();

        return chatRoomRepository.save(newRoom);
    }

    public List<ChatRoom> getChatRooms(String userId) {
        return chatRoomRepository.findByParticipantsContaining(userId);
    }

    public List<Message> getChatHistory(String roomId) {
        return messageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    public Message saveMessage(Message message) {

        message.setTimestamp(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        chatRoomRepository.findById(message.getRoomId()).ifPresent(room -> {
            room.setLastMessage(message.getMessage());
            room.setLastUpdated(LocalDateTime.now());
            chatRoomRepository.save(room);
        });

        return savedMessage;
    }
}