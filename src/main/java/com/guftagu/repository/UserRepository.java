package com.guftagu.repository;

import com.guftagu.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // Find user by phone number (for login)
    Optional<User> findByPhoneNumber(String phoneNumber);

    // Find multiple users by phone numbers (for contacts sync)
    List<User> findByPhoneNumberIn(List<String> phoneNumbers);

    // Search users by name or phone number
    List<User> findByNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(
            String name,
            String phoneNumber
    );
}