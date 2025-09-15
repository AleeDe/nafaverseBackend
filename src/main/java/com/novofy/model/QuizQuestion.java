package com.novofy.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quiz_questions")
public class QuizQuestion {
    @Id
    private String id;

    private String topicId; // FK
    private String question;
    private List<String> options;
    private int correctAnswerIndex;
    private String explanation;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and setters
}
