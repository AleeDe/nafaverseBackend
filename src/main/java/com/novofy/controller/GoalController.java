package com.novofy.controller;

import com.novofy.dto.CreateGoalRequest;
import com.novofy.model.Goal;
import com.novofy.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    // POST - create a new Goal
    @PostMapping(path = "/create", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> createGoal(@RequestBody CreateGoalRequest request) {
        try {
            Goal savedGoal = goalService.createGoal(
                request.getGoalName(),
                request.getCity(),
                request.getTargetYear(),
                request.getPrompt()
            );
            System.out.println(savedGoal);
            return ResponseEntity.ok(savedGoal);
        } catch (ResponseStatusException ex) {
            // Preserve status from service (e.g., 429 for AI quota)
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    // GET - fetch all goals for current user
//    @GetMapping("/my")
//    public ResponseEntity<?> getMyGoals() {
//        try {
//            return ResponseEntity.ok(goalService.getUserGoals());
//        } catch (Exception e) {
//            return ResponseEntity.status(401).body("Unauthorized or error fetching goals");
//        }
//    }

    // GET - fetch single goal by ID
//    @GetMapping("/{goalId}")
//    public ResponseEntity<?> getGoalById(@PathVariable String goalId) {
//        try {
//            return ResponseEntity.ok(goalService.getGoalById(goalId));
//        } catch (Exception e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    // DELETE - delete goal
//    @DeleteMapping("/{goalId}")
//    public ResponseEntity<?> deleteGoal(@PathVariable String goalId) {
//        try {
//            goalService.deleteGoal(goalId);
//            return ResponseEntity.ok("Goal deleted successfully");
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("Error deleting goal: " + e.getMessage());
//        }
//    }
}
