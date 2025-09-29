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

    private String originalPrompt;

    private String goalName;

    private String city;

    private BigDecimal estimatedCost;

    private Integer targetYear;

    private BigDecimal monthlySavingRequired;

    private int roiRate;

    private int inflationRate;

    private BigDecimal finalAmount; // value at targetYear (should equal last graphData.projectedValue)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<GoalGraphData> graphData;
    

    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GoalGraphData {
        private int year;
        private BigDecimal projectedValue;
    }
}
