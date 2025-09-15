package com.novofy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import org.bson.types.ObjectId;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "educationProgress")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EducationProgress {
    @Id
    private ObjectId id;

    @DBRef(lazy = true)
    private ObjectId userId;

    @DBRef(lazy = true)
    private List<Topic> completedTopics;

    private List<String> badges;

    private int earnCoins;

    private int earnXP;

    private int tier;

    private LocalDate createdAt;

    private LocalDate updatedAt;
}