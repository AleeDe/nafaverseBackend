package com.novofy.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    @Id
    private ObjectId goalId;

    @DBRef(lazy = true)
    private ObjectId userId;

    private String originalPrompt;

    private String goalType;

    private String city;

    private BigDecimal estimatedCost;

    private List<Integer> targetMonth;

    private List<BigDecimal> monthlySavingRequired;

    private int roiRate;

    private int inflationRate;

    @DBRef
    private SuggestedPlan suggestedPlan;
    
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<String> graphData;
    
}
