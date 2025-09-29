package com.novofy.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class GoalAiResponse {
    private BigDecimal estimatedCost;
    private BigDecimal monthlySavingRequired;
    private int roiRate;
    private int inflationRate;
    private List<GraphPoint> graphData;
    private BigDecimal finalAmount; // value at targetYear (should equal last graphData.projectedValue)

    @Data
    public static class GraphPoint {
        private int year;
        private BigDecimal projectedValue;
    }
}
