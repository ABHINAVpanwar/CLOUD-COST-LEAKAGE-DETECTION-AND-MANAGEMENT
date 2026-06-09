package com.cloudcost.cloud_cost_optimizer.model;

import java.util.List;
import java.util.Map;

public class AdminDashboardData {
    private long totalUsers;
    private long totalClients;
    private long totalEngineers;
    private long activeUsers;
    private long inactiveUsers;
    private long lockedUsers;
    private long totalAnalyses;
    private long totalUploads;
    private double totalEstimatedSavings;
    private List<UserManagementDTO> recentUsers;
    private List<Map<String, Object>> userActivityStats;
    private List<AuditLog> recentAuditLogs;
    
    // Getters and Setters
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    
    public long getTotalClients() { return totalClients; }
    public void setTotalClients(long totalClients) { this.totalClients = totalClients; }
    
    public long getTotalEngineers() { return totalEngineers; }
    public void setTotalEngineers(long totalEngineers) { this.totalEngineers = totalEngineers; }
    
    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
    
    public long getInactiveUsers() { return inactiveUsers; }
    public void setInactiveUsers(long inactiveUsers) { this.inactiveUsers = inactiveUsers; }
    
    public long getLockedUsers() { return lockedUsers; }
    public void setLockedUsers(long lockedUsers) { this.lockedUsers = lockedUsers; }
    
    public long getTotalAnalyses() { return totalAnalyses; }
    public void setTotalAnalyses(long totalAnalyses) { this.totalAnalyses = totalAnalyses; }
    
    public long getTotalUploads() { return totalUploads; }
    public void setTotalUploads(long totalUploads) { this.totalUploads = totalUploads; }
    
    public double getTotalEstimatedSavings() { return totalEstimatedSavings; }
    public void setTotalEstimatedSavings(double totalEstimatedSavings) { this.totalEstimatedSavings = totalEstimatedSavings; }
    
    public List<UserManagementDTO> getRecentUsers() { return recentUsers; }
    public void setRecentUsers(List<UserManagementDTO> recentUsers) { this.recentUsers = recentUsers; }
    
    public List<Map<String, Object>> getUserActivityStats() { return userActivityStats; }
    public void setUserActivityStats(List<Map<String, Object>> userActivityStats) { this.userActivityStats = userActivityStats; }
    
    public List<AuditLog> getRecentAuditLogs() { return recentAuditLogs; }
    public void setRecentAuditLogs(List<AuditLog> recentAuditLogs) { this.recentAuditLogs = recentAuditLogs; }
}