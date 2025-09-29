package com.novofy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SimulationAiResponse {
    private Integer roiRate;
    private Integer inflationRate;
    private BigDecimal monthlySavingRequired; // if targetAmount given and monthlyInvestment was null
    private BigDecimal totalInvestment;       // oneTime + monthly*12*years
    private BigDecimal totalAmount;           // with ROI (should equal finalAmount)
    private BigDecimal finalAmount;           // must equal last graphData.projectedValue

    private List<GraphPoint> graphData;       // year index 1..N with ROI

    @Data
    public static class GraphPoint {
        private int year;                     // index: 1..N
        private BigDecimal projectedValue;    // with ROI
    }
}