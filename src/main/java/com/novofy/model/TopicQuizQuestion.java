package com.novofy.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "topic_quiz_questions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicQuizQuestion {

    @Id
    private ObjectId id;

    private ObjectId topicId; // Reference to the topic
    private String question;
    private List<String> options;
    private String correctAnswer; // Now storing the correct answer text

    private LocalDateTime createdAt = LocalDateTime.now();

    
}
