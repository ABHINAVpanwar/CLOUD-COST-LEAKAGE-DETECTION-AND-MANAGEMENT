package com.cloudcost.cloud_cost_optimizer.model;

import java.util.Map;

public class DashboardData {
    private Double totalSpend;
    private Double estimatedSavings;
    private Integer totalLeakages;
    private Map<String, Double> costByService;
    private Map<String, Integer> leakageSummary;
    private String welcomeMessage;
    
    // Getters and Setters
    public Double getTotalSpend() { return totalSpend; }
    public void setTotalSpend(Double totalSpend) { this.totalSpend = totalSpend; }
    public Double getEstimatedSavings() { return estimatedSavings; }
    public void setEstimatedSavings(Double estimatedSavings) { this.estimatedSavings = estimatedSavings; }
    public Integer getTotalLeakages() { return totalLeakages; }
    public void setTotalLeakages(Integer totalLeakages) { this.totalLeakages = totalLeakages; }
    public Map<String, Double> getCostByService() { return costByService; }
    public void setCostByService(Map<String, Double> costByService) { this.costByService = costByService; }
    public Map<String, Integer> getLeakageSummary() { return leakageSummary; }
    public void setLeakageSummary(Map<String, Integer> leakageSummary) { this.leakageSummary = leakageSummary; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
}