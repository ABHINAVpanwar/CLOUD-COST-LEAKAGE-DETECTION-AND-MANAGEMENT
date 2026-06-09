package com.cloudcost.cloud_cost_optimizer.controller;

import com.cloudcost.cloud_cost_optimizer.model.DashboardData;
import com.cloudcost.cloud_cost_optimizer.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private AnalysisService analysisService;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @GetMapping("/data")
    public ResponseEntity<DashboardData> getDashboardData() {
        String username = getCurrentUsername();
        DashboardData data = analysisService.getDashboardData(username);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/utilization")
    public ResponseEntity<?> getUtilization() {
        return ResponseEntity.ok(analysisService.getResourceUtilization(getCurrentUsername()));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations() {
        return ResponseEntity.ok(analysisService.getRecommendations(getCurrentUsername()));
    }

    @GetMapping("/leakages")
    public ResponseEntity<?> getLeakages() {
        return ResponseEntity.ok(analysisService.getLeakageReport(getCurrentUsername()));
    }

    @GetMapping("/leakages/{userId}")
    public ResponseEntity<?> getLeakagesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(analysisService.getLeakageReportByUserId(userId));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(analysisService.getUploadHistory(getCurrentUsername()));
    }
}
