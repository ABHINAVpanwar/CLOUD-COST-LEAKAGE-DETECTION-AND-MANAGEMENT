package com.cloudcost.cloud_cost_optimizer.controller;

import com.cloudcost.cloud_cost_optimizer.model.User;
import com.cloudcost.cloud_cost_optimizer.service.AuthService;
import com.cloudcost.cloud_cost_optimizer.service.EngineerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/engineer")
public class EngineerController {

    @Autowired
    private EngineerService engineerService;

    @Autowired
    private AuthService authService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return authService.getUserByUsername(username);
    }

    // US014 — Get assigned clients
    @GetMapping("/clients")
    public ResponseEntity<?> getClients() {
        try {
            return ResponseEntity.ok(engineerService.getAssignedClients(getCurrentUser().getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // US014 — Analyze specific client data
    @GetMapping("/clients/{clientId}/analysis")
    public ResponseEntity<?> getClientAnalysis(@PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(engineerService.getClientAnalysis(getCurrentUser().getId(), clientId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // US017 — Bulk analysis
    @GetMapping("/bulk-analysis")
    public ResponseEntity<?> bulkAnalysis() {
        try {
            return ResponseEntity.ok(engineerService.bulkAnalysis(getCurrentUser().getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // US015 + US016 — Get config (thresholds + cost coefficient)
    @GetMapping("/clients/{clientId}/config")
    public ResponseEntity<?> getConfig(@PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(engineerService.getConfig(getCurrentUser().getId(), clientId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // US015 + US016 — Save config
    @PutMapping("/clients/{clientId}/config")
    public ResponseEntity<?> saveConfig(@PathVariable Long clientId, @RequestBody Map<String, Object> body) {
        try {
            User engineer = getCurrentUser();
            return ResponseEntity.ok(engineerService.saveConfig(engineer.getId(), clientId, body, engineer.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // US018 — Savings tracking
    @GetMapping("/clients/{clientId}/savings")
    public ResponseEntity<?> getSavings(@PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(engineerService.getSavingsTracking(getCurrentUser().getId(), clientId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
