package com.novofy.controller;

import com.novofy.dto.SimulationRequest;
import com.novofy.model.Simulation;
import com.novofy.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping(path = "/create", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> create(@RequestBody SimulationRequest request) {
        try {
            Simulation saved = simulationService.createSimulation(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating simulation: " + e.getMessage());
        }
    }
}