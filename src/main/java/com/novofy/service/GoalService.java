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
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

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
            Always respond in strictly valid JSON ONLY.

            IMPORTANT — before producing projections:
            1) Determine the CURRENT MARKET PRICE of the target item/service as of today and use it as the base (currentPrice).
               - If you have access to a reliable public data source, use it and list the source in dataSources[].
               - If live data is unavailable, estimate currentPrice and explain the estimation method in the "notes" field.
            2) Consider global macroeconomic conditions when projecting (inflation, short-term interest rates, exchange rates, major commodity trends, recent economic indicators, and major geopolitical risks). Use these to adjust projections and state which indicators were used in "notes" or "dataSources".

            Special-case for pilgrimage goals:
            - If the goal mentions "haj", "Perform Hajj", "umrah" or "Perform Umrah" (case-insensitive), assume default costs unless the user provides a different amount:
              * Haj (hajj): 1,300,000 (local currency) — treat this as the approximate cost for the year 2025.
              * Umrah: 400,000 (local currency) — treat this as the approximate cost for the year 2025.
            - Use these defaults as currentPrice when applicable and explicitly state that these values represent 2025 costs in "dataSources" or "notes".
            - If the user supplies a different amount or specifies a different year, use the user's input instead and note the override.

            Rules:
            - graphData MUST start at year %d (the current year) and include one entry for every year up to %d (inclusive).
            - The projectedValue for the first year MUST equal currentPrice.
            - finalAmount MUST equal the projectedValue for year %d (the last year).
            - estimatedCost MUST equal finalAmount.
            - Use the inputs exactly as provided.
            - Output must be strictly valid JSON only and numeric values should be rounded to two decimal places.
            - %s is a city according to input parameters.

            Input:
            Goal: %s
            City: %s
            Target Year: %d
            Assume nominal ROI = 12%% and baseline inflation = 6%% (you may adjust these with justification based on current data).

            Required Output JSON schema (only these keys, extra diagnostic keys allowed but keep JSON valid):
            {
              "currentPrice": number,
              "estimatedCost": number,
              "monthlySavingRequired": number,
              "totalInvestment": number,   // total amount invested over the period (based on monthlySavingRequired * months target year from now)
              "roiRate": number,
              "inflationRate": number,
              "graphData": [
             { "year": number, "projectedValue": number }
              ],
              "finalAmount": number,
              "dataSources": [ string ],    // list data sources or 'estimated' when necesary
              "notes": string               // brief explanation of assumptions / method
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

        GoalAiResponse aiResponse;
        try {
            aiResponse = chatClient
                .prompt()
                .user(escapedPrompt)
                .call()
                .entity(GoalAiResponse.class);
        } catch (NonTransientAiException e) {
            // OpenAI quota or similar non-retryable error
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI quota exceeded. Please try later.");
        } catch (RestClientResponseException e) {
            HttpStatus status = HttpStatus.resolve(e.getRawStatusCode());
            throw new ResponseStatusException(status != null ? status : HttpStatus.BAD_GATEWAY,
                    "AI call failed: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI call failed: " + e.getMessage());
        }

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

        // compute total months until target (whole years). If you want partial-year use LocalDate differences.
        int totalMonths = Math.max(0, (targetYear - currentYear) * 12);
        BigDecimal totalInvestment = monthly.multiply(BigDecimal.valueOf(totalMonths))
                                            .setScale(2, RoundingMode.HALF_UP);
        goal.setTotalInvestment(totalInvestment);

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
