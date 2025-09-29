package com.novofy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novofy.config.securityConfig;
import com.novofy.dto.GoalAiResponse;
import com.novofy.model.Goal;
import com.novofy.model.User;
import com.novofy.repository.GoalRepository;
import com.novofy.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    @Autowired
    private UserRepository userRepository;
    private final ChatClient chatClient;
    private final GoalRepository goalRepository;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    private static String escapeBraces(String s) {
        return s == null ? null : s.replace("{", "\\{").replace("}", "\\}");
    }

    // Ensure data covers every year [startYear, endYear], filling gaps with last known value (or 0)
    private static List<GoalAiResponse.GraphPoint> normalizeGraph(List<GoalAiResponse.GraphPoint> input, int startYear, int endYear) {
        Map<Integer, BigDecimal> byYear = new HashMap<>();
        if (input != null) {
            for (GoalAiResponse.GraphPoint p : input) {
                if (p != null) byYear.put(p.getYear(), p.getProjectedValue());
            }
        }
        BigDecimal last = null;
        ArrayList<GoalAiResponse.GraphPoint> out = new ArrayList<>();
        for (int y = startYear; y <= endYear; y++) {
            BigDecimal v = (y == startYear)
                    ? BigDecimal.ZERO // first year must start at 0
                    : byYear.getOrDefault(y, last != null ? last : BigDecimal.ZERO);
            GoalAiResponse.GraphPoint gp = new GoalAiResponse.GraphPoint();
            gp.setYear(y);
            gp.setProjectedValue(v);
            out.add(gp);
            last = v;
        }
        return out;
    }

    // Deterministic projection from monthlySaving and ROI (monthly compounding), first year = 0
    private static List<GoalAiResponse.GraphPoint> computeGraphFromMonthly(BigDecimal monthlySaving, int startYear, int endYear, int roiRatePercent) {
        ArrayList<GoalAiResponse.GraphPoint> out = new ArrayList<>();
        MathContext mc = MathContext.DECIMAL64;
        BigDecimal zero = BigDecimal.ZERO;
        if (monthlySaving == null || monthlySaving.signum() <= 0) {
            // produce all zeros
            for (int y = startYear; y <= endYear; y++) {
                GoalAiResponse.GraphPoint gp = new GoalAiResponse.GraphPoint();
                gp.setYear(y);
                gp.setProjectedValue(zero);
                out.add(gp);
            }
            return out;
        }
        BigDecimal rAnnual = BigDecimal.valueOf(roiRatePercent).divide(BigDecimal.valueOf(100), mc);
        BigDecimal rMonthly = rAnnual.divide(BigDecimal.valueOf(12), mc);
        BigDecimal onePlusRm = BigDecimal.ONE.add(rMonthly, mc);

        for (int y = startYear; y <= endYear; y++) {
            GoalAiResponse.GraphPoint gp = new GoalAiResponse.GraphPoint();
            gp.setYear(y);
            if (y == startYear) {
                gp.setProjectedValue(zero); // first year = 0
            } else {
                int months = (y - startYear) * 12;
                BigDecimal pow = onePlusRm.pow(months, mc);
                // FV of monthly contributions (ordinary annuity): m * ((pow - 1) / rMonthly)
                BigDecimal fv = monthlySaving.multiply(pow.subtract(BigDecimal.ONE, mc)
                        .divide(rMonthly, mc), mc);
                gp.setProjectedValue(fv);
            }
            out.add(gp);
        }
        return out;
    }

    // If monthly is missing but final is known, invert the annuity formula to estimate monthly
    private static BigDecimal solveMonthlyFromFinal(BigDecimal targetFinal, int years, int roiRatePercent) {
        if (targetFinal == null || targetFinal.signum() <= 0 || years <= 0 || roiRatePercent <= 0) return BigDecimal.ZERO;
        MathContext mc = MathContext.DECIMAL64;
        BigDecimal rAnnual = BigDecimal.valueOf(roiRatePercent).divide(BigDecimal.valueOf(100), mc);
        BigDecimal rMonthly = rAnnual.divide(BigDecimal.valueOf(12), mc);
        BigDecimal onePlusRm = BigDecimal.ONE.add(rMonthly, mc);
        int months = years * 12;
        BigDecimal pow = onePlusRm.pow(months, mc);
        BigDecimal denom = pow.subtract(BigDecimal.ONE, mc).divide(rMonthly, mc);
        if (denom.signum() == 0) return BigDecimal.ZERO;
        return targetFinal.divide(denom, mc).setScale(2, RoundingMode.HALF_UP);
    }

    public Goal createGoal(String goalName, String city, int targetYear, String userPrompt) throws Exception {

        String userEmail = securityConfig.getCurrentUserEmail();
        if (userEmail == null) {
            throw new RuntimeException("Unauthorized: No user found in JWT");
        }

        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + userEmail);
        }

        int currentYear = LocalDate.now().getYear();
        if (targetYear < currentYear) {
            throw new IllegalArgumentException("Target year must be >= " + currentYear);
        }

        String basePrompt = """
            You are a financial planner AI.
            Always respond in JSON format ONLY.

            Rules:
            - graphData MUST start at year %d (the current year) and include one entry for every year up to %d (inclusive).
            - The first year's projectedValue MUST be 0.
            - finalAmount MUST equal the projectedValue for year %d (the last year).
            - estimatedCost MUST be the same as finalAmount (i.e., equal to the last projectedValue).
            - Use the inputs exactly as provided.
            - Output must be strictly valid JSON only.
            - %s is a city according to input parameters.

            Input:
            Goal: %s
            City: %s
            Target Year: %d
            Assume ROI = 12%% and Inflation = 6%%.

            Output JSON schema:
            {
              "estimatedCost": number,
              "monthlySavingRequired": number,
              "roiRate": 12,
              "inflationRate": 6,
              "graphData": [
                 { "year": number, "projectedValue": number }
              ],
              "finalAmount": number
            }
            """.formatted(
                currentYear,
                targetYear,
                targetYear,
                city,
                goalName,
                city,
                targetYear
            );

        String finalPrompt = (userPrompt != null && !userPrompt.isBlank())
                ? basePrompt + "\n\nAdditional user instructions (take precedence; keep JSON-only):\n<<<\n"
                  + userPrompt.trim() + "\n>>>"
                : basePrompt;

        String escapedPrompt = escapeBraces(finalPrompt);

        GoalAiResponse aiResponse = chatClient
                .prompt()
                .user(escapedPrompt)
                .call()
                .entity(GoalAiResponse.class);

        // Server-side correction for "correct data"
        int roiRate = aiResponse.getRoiRate() > 0 ? aiResponse.getRoiRate() : 12; // default 12%
        int periodsYears = targetYear - currentYear;

        // Prefer AI's monthly; if missing, try to infer from AI estimatedCost
        BigDecimal monthly = aiResponse.getMonthlySavingRequired();
        if (monthly == null || monthly.signum() <= 0) {
            BigDecimal aiFinal = aiResponse.getEstimatedCost();
            if (aiFinal == null || aiFinal.signum() <= 0) aiFinal = aiResponse.getFinalAmount();
            monthly = solveMonthlyFromFinal(aiFinal, periodsYears, roiRate);
        }
        if (monthly == null) monthly = BigDecimal.ZERO;

        // Deterministic recompute of the graph (first year 0, monthly compounding)
        List<GoalAiResponse.GraphPoint> computed =
                computeGraphFromMonthly(monthly, currentYear, targetYear, roiRate);

        // Authoritative final amount from computed graph
        BigDecimal finalAmount = computed.isEmpty()
                ? BigDecimal.ZERO
                : computed.get(computed.size() - 1).getProjectedValue();

        // Force estimatedCost to equal finalAmount
        BigDecimal estimatedCost = finalAmount;

        // Map to entity
        Goal goal = new Goal();
        goal.setGoalName(goalName);
        goal.setCity(city);
        goal.setTargetYear(targetYear);
        goal.setOriginalPrompt(finalPrompt);
        goal.setEstimatedCost(estimatedCost);
        goal.setMonthlySavingRequired(monthly);
        goal.setRoiRate(roiRate);
        goal.setInflationRate(aiResponse.getInflationRate() > 0 ? aiResponse.getInflationRate() : 6);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        List<Goal.GoalGraphData> graphData = computed.stream()
                .map(g -> new Goal.GoalGraphData(g.getYear(), g.getProjectedValue()))
                .collect(Collectors.toList());
        goal.setGraphData(graphData);
        goal.setFinalAmount(finalAmount);

        Goal savedGoal = goalRepository.save(goal);
        userService.addGoalToUser(userOpt.get().getId(), savedGoal);
        return savedGoal;
    }
}
