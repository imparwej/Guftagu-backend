package com.guftagu.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String MESSAGES_TOPIC = "guftagu-messages";

    @Bean
    public NewTopic messagesTopic() {
        return TopicBuilder.name(MESSAGES_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
