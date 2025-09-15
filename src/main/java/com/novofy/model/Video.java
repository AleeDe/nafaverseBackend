package com.novofy.model;

import java.time.LocalDateTime;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "videos")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Video {
    @Id
    private ObjectId id;

    @DBRef(lazy = true)
    private ObjectId topicId; 
    private String title;
    private String url;
    private int duration;
    private List<String> language;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and setters
}