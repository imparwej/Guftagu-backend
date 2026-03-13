package com.guftagu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chats")
public class Chat {

    @Id
    private String id;

    @Indexed
    private List<String> participants = new ArrayList<>();

    @Builder.Default
    private List<String> pinnedByUsers = new ArrayList<>();

    @Builder.Default
    private List<String> mutedByUsers = new ArrayList<>();

    @Builder.Default
    private List<String> deletedByUsers = new ArrayList<>();
}
