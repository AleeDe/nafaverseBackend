package com.novofy.service;

import com.novofy.config.securityConfig;
import com.novofy.dto.SimulationAiResponse;
import com.novofy.dto.SimulationRequest;
import com.novofy.model.Simulation;
import com.novofy.model.User;
import com.novofy.repository.SimulationRepository;
import com.novofy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final ChatClient chatClient;
    private final SimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    private static String nz(String s) { return s == null ? "" : s; }
    private static BigDecimal n0(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String esc(String s) { return s == null ? null : s.replace("{", "\\{").replace("}", "\\}"); }

    private static List<Simulation.GoalGraphData> toCalendarSeries(List<SimulationAiResponse.GraphPoint> idx, int startYear) {
        List<Simulation.GoalGraphData> out = new ArrayList<>();
        if (idx == null) return out;
        for (int i = 0; i < idx.size(); i++) {
            int calendarYear = startYear + i;
            BigDecimal pv = idx.get(i).getProjectedValue() == null ? BigDecimal.ZERO : idx.get(i).getProjectedValue();
            out.add(new Simulation.GoalGraphData(calendarYear, pv));
        }
        return out;
    }

    private static List<SimulationAiResponse.GraphPoint> normalizeIndexSeries(List<SimulationAiResponse.GraphPoint> input, int periods) {
        Map<Integer, SimulationAiResponse.GraphPoint> byIdx = new HashMap<>();
        if (input != null) {
            for (SimulationAiResponse.GraphPoint p : input) {
                if (p != null && p.getYear() >= 1 && p.getYear() <= periods) byIdx.put(p.getYear(), p);
            }
        }
        List<SimulationAiResponse.GraphPoint> out = new ArrayList<>(periods);
        BigDecimal last = BigDecimal.ZERO;
        for (int i = 1; i <= periods; i++) {
            SimulationAiResponse.GraphPoint src = byIdx.get(i);
            SimulationAiResponse.GraphPoint gp = new SimulationAiResponse.GraphPoint();
            gp.setYear(i);
            if (src != null && src.getProjectedValue() != null) {
                gp.setProjectedValue(src.getProjectedValue());
                last = src.getProjectedValue();
            } else {
                gp.setProjectedValue(last);
            }
            out.add(gp);
        }
        return out;
    }

    public Simulation createSimulation(SimulationRequest req) {
        String email = securityConfig.getCurrentUserEmail();
        if (email == null) throw new RuntimeException("Unauthorized");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) throw new RuntimeException("User not found: " + email);

        int currentYear = LocalDate.now().getYear();

        if (req.getDurationYears() == null || req.getDurationYears() <= 0)
            throw new IllegalArgumentException("durationYears must be > 0");

        int periods = req.getDurationYears();
        BigDecimal oneTime = n0(req.getOneTimeInvestment());
        BigDecimal monthly = req.getMonthlyInvestment(); // may be null if targetAmount is given

        // Build a strict JSON-only prompt (no % formatting)
        StringBuilder sb = new StringBuilder();
        sb.append("You are a financial planner AI.\n")
          .append("Always respond in JSON format ONLY.\n\n")
          .append("Scenario:\n")
          .append("- City: ").append(nz(req.getCity())).append("\n")
          .append("- Duration (years): ").append(periods).append("\n")
          .append("- Start year index at 1 (represents calendar year ").append(currentYear).append(") and go to ").append(periods).append(".\n")
          .append("- One-time investment at start (year 1): ").append(oneTime).append("\n");

        if (monthly != null) {
            sb.append("- Monthly investment (fixed): ").append(monthly).append("\n");
        } else if (req.getTargetAmount() != null) {
            sb.append("- Target amount at end of year ").append(periods).append(": ").append(req.getTargetAmount()).append("\n")
              .append("- Compute monthlySavingRequired needed to reach the target.\n");
        } else {
            throw new IllegalArgumentException("Provide monthlyInvestment or targetAmount");
        }

        if (req.getRoiRate() != null) sb.append("- ROI rate (annual, %): ").append(req.getRoiRate()).append("\n");
        if (req.getInflationRate() != null) sb.append("- Inflation rate (annual, %): ").append(req.getInflationRate()).append("\n");

        sb.append("\nRules:\n")
          .append("- Use annual ROI; assume monthly compounding when applying ROI to monthly contributions.\n")
          .append("- graphData must be an array of length ").append(periods).append(", each item: { \"year\": number, \"projectedValue\": number }.\n")
          .append("- \"year\" is an index (1..").append(periods).append("), not a calendar year.\n")
          .append("- projectedValue is WITH ROI applied (grows over time).\n")
          .append("- finalAmount MUST equal graphData[").append(periods).append("].projectedValue.\n")
          .append("- totalInvestment = oneTime + (monthlySavingRequired or monthlyInvestment) * 12 * ").append(periods).append(".\n")
          .append("- totalAmount MUST equal finalAmount.\n")
          .append("- If monthlyInvestment was provided, set monthlySavingRequired = monthlyInvestment.\n")
          .append("- Output must be strictly valid JSON; no extra text.\n\n")
          .append("Output JSON schema:\n")
          .append("{\n")
          .append("  \"roiRate\": number,\n")
          .append("  \"inflationRate\": number,\n")
          .append("  \"monthlySavingRequired\": number,\n")
          .append("  \"totalInvestment\": number,\n")
          .append("  \"totalAmount\": number,\n")
          .append("  \"graphData\": [ { \"year\": number, \"projectedValue\": number } ],\n")
          .append("  \"finalAmount\": number\n")
          .append("}\n");

        if (req.getPrompt() != null && !req.getPrompt().isBlank()) {
            sb.append("\nAdditional user instructions (take precedence; keep JSON-only):\n<<<\n")
              .append(req.getPrompt().trim()).append("\n>>>\n");
        }

        String finalPrompt = esc(sb.toString());

        SimulationAiResponse ai;
        try {
            ai = chatClient
                    .prompt()
                    .user(finalPrompt)
                    .call()
                    .entity(SimulationAiResponse.class);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new RuntimeException("AI quota exceeded (429). Configure billing or retry later.");
            }
            throw new RuntimeException("AI call failed: " + e.getStatusCode().value() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("AI call failed: " + e.getMessage());
        }

        // Normalize and enforce invariants
        List<SimulationAiResponse.GraphPoint> norm = normalizeIndexSeries(ai.getGraphData(), periods);
        BigDecimal finalAmount = norm.isEmpty() ? BigDecimal.ZERO : (norm.get(norm.size() - 1).getProjectedValue() == null ? BigDecimal.ZERO : norm.get(norm.size() - 1).getProjectedValue());
        if (!norm.isEmpty()) norm.get(norm.size() - 1).setProjectedValue(finalAmount);

        BigDecimal monthlyRequired = monthly != null ? monthly : n0(ai.getMonthlySavingRequired());
        BigDecimal totalInvestment = n0(ai.getTotalInvestment());
        if (totalInvestment.signum() == 0) {
            totalInvestment = oneTime.add(monthlyRequired.multiply(BigDecimal.valueOf(12L * periods)));
        }

        BigDecimal totalAmount = n0(ai.getTotalAmount());
        if (totalAmount.signum() == 0) totalAmount = finalAmount;

        // Persist
        Simulation sim = new Simulation();
        sim.setCity(req.getCity());
        sim.setOriginalPrompt(finalPrompt);
        sim.setOneTimeInvestment(oneTime);
        sim.setMonthlyInvestment(monthlyRequired);
        sim.setDuration(java.util.List.of(periods));
        sim.setInflationRate(ai.getInflationRate() == null
                ? (req.getInflationRate() == null ? java.util.List.of() : java.util.List.of(req.getInflationRate()))
                : java.util.List.of(ai.getInflationRate()));
        sim.setRoiRate(ai.getRoiRate() == null ? (req.getRoiRate() == null ? 0 : req.getRoiRate()) : ai.getRoiRate());
        sim.setTotalInvestment(totalInvestment);
        sim.setTotalAmount(totalAmount);
        sim.setGraphData(toCalendarSeries(norm, currentYear));
        sim.setCreatedAt(java.time.LocalDateTime.now());
        sim.setUpdatedAt(java.time.LocalDateTime.now());

        Simulation saved = simulationRepository.save(sim);

        return saved;
    }
}