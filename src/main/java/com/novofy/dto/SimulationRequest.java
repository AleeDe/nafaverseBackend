package com.novofy.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SimulationRequest {
    private String city;
    private Integer durationYears;          // required
    private BigDecimal oneTimeInvestment;   // optional (default 0)
    private BigDecimal monthlyInvestment;   // optional; if null and targetAmount present, AI should compute it
    private BigDecimal targetAmount;        // optional; if present and monthlyInvestment is null, solve for monthly
    private Integer roiRate;                // optional; if null, AI can pick a reasonable default
    private Integer inflationRate;          // optional; if null, AI can pick a reasonable default
    private String prompt;                  // optional user instructions
}