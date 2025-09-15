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


@Document(collection = "simulations")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Simulation {
    @Id
    private ObjectId id;

    @DBRef(lazy = true)
    private ObjectId userId;

    private String originalPrompt;

    private String city;

    private BigDecimal oneTimeInvestment;

    private BigDecimal monthlyInvestment;

    private List<Integer> duration;

    private List<Integer> inflationRate;

    private List<Integer> roiRate;

    private BigDecimal totalInvestment;

    private BigDecimal totalAmount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<String> graphData;
}
