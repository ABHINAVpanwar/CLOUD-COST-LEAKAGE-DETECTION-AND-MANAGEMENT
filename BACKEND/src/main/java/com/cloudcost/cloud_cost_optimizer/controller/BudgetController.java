package com.cloudcost.cloud_cost_optimizer.controller;

import com.cloudcost.cloud_cost_optimizer.model.BudgetThreshold;
import com.cloudcost.cloud_cost_optimizer.model.User;
import com.cloudcost.cloud_cost_optimizer.repository.BudgetThresholdRepository;
import com.cloudcost.cloud_cost_optimizer.repository.ResourceRepository;
import com.cloudcost.cloud_cost_optimizer.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    @Autowired
    private BudgetThresholdRepository budgetRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private AuthService authService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return authService.getUserByUsername(username);
    }

    @GetMapping
    public ResponseEntity<?> getBudget() {
        User user = getCurrentUser();
        BudgetThreshold budget = budgetRepository.findByUserId(user.getId()).orElse(null);
        if (budget == null) return ResponseEntity.ok(Map.of("monthlyLimit", 0, "alertPercentage", 80, "configured", false));

        double currentSpend = resourceRepository.findByUser(user).stream()
                .mapToDouble(r -> r.getCost() != null ? r.getCost() : 0).sum();
        double usagePercent = budget.getMonthlyLimit() > 0 ? (currentSpend / budget.getMonthlyLimit()) * 100 : 0;
        boolean alertTriggered = usagePercent >= budget.getAlertPercentage();

        Map<String, Object> result = new HashMap<>();
        result.put("monthlyLimit", budget.getMonthlyLimit());
        result.put("alertPercentage", budget.getAlertPercentage());
        result.put("currentSpend", Math.round(currentSpend * 100.0) / 100.0);
        result.put("usagePercent", Math.round(usagePercent * 10.0) / 10.0);
        result.put("alertTriggered", alertTriggered);
        result.put("configured", true);
        if (alertTriggered) {
            result.put("alertMessage", String.format(
                "Alert: Your cloud spend (Rs.%.0f) has exceeded %d%% of your monthly budget (Rs.%.0f).",
                currentSpend, budget.getAlertPercentage(), budget.getMonthlyLimit()));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> setBudget(@RequestBody Map<String, Object> body) {
        User user = getCurrentUser();
        BudgetThreshold budget = budgetRepository.findByUserId(user.getId()).orElse(new BudgetThreshold());
        budget.setUserId(user.getId());
        if (body.containsKey("monthlyLimit"))
            budget.setMonthlyLimit(Double.valueOf(body.get("monthlyLimit").toString()));
        if (body.containsKey("alertPercentage"))
            budget.setAlertPercentage(Integer.valueOf(body.get("alertPercentage").toString()));
        budget.setUpdatedAt(LocalDateTime.now());
        budgetRepository.save(budget);
        return ResponseEntity.ok(Map.of("message", "Budget threshold saved successfully."));
    }
}
