package com.cloudcost.cloud_cost_optimizer.service;

import com.cloudcost.cloud_cost_optimizer.model.*;
import com.cloudcost.cloud_cost_optimizer.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private BudgetThresholdRepository budgetThresholdRepository;

    @Autowired
    private EngineerConfigRepository engineerConfigRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AnalysisService analysisService;
    
    // Dashboard Data
    public AdminDashboardData getDashboardData() {
        AdminDashboardData data = new AdminDashboardData();
        
        data.setTotalUsers(userRepository.count());
        data.setTotalClients(userRepository.countByRole("ROLE_CLIENT"));
        data.setTotalEngineers(userRepository.countByRole("ROLE_ENGINEER"));
        data.setActiveUsers(userRepository.findByStatus("ACTIVE").size());
        data.setInactiveUsers(userRepository.findByStatus("INACTIVE").size());
        data.setLockedUsers(userRepository.findByStatus("LOCKED").size());
        
        // Get recent users (last 10)
        List<User> recentUsers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null)
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList());
        data.setRecentUsers(recentUsers.stream().map(UserManagementDTO::new).collect(Collectors.toList()));
        
        // Recent audit logs
        data.setRecentAuditLogs(auditLogRepository.findRecentLogs().stream().limit(20).collect(Collectors.toList()));
        
        // Activity stats
        List<Map<String, Object>> activityStats = new ArrayList<>();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long loginsCount = auditLogRepository.countByActionSince("LOGIN", weekAgo);
        long uploadsCount = auditLogRepository.countByActionSince("UPLOAD", weekAgo);
        long configChanges = auditLogRepository.countByActionSince("CONFIG_CHANGE", weekAgo);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("logins", loginsCount);
        stats.put("uploads", uploadsCount);
        stats.put("configChanges", configChanges);
        activityStats.add(stats);
        data.setUserActivityStats(activityStats);
        
        data.setTotalEstimatedSavings(analysisService.getTotalEstimatedSavings());
        
        return data;
    }
    
    // Get all users
    public List<UserManagementDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserManagementDTO::new)
                .collect(Collectors.toList());
    }
    
    // Get user by ID
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    // Search users
    public List<UserManagementDTO> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm).stream()
                .map(UserManagementDTO::new)
                .collect(Collectors.toList());
    }
    
    // Create new user
    @Transactional
    public User createUser(CreateUserRequest request, Long adminId, String adminUsername, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        String generatedPassword = generateRandomPassword();
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setMobileNo(request.getMobileNo());
        user.setAddress(request.getAddress());
        String role = request.getRole() != null ? request.getRole() : "ROLE_CLIENT";
        if (!role.startsWith("ROLE_")) role = "ROLE_" + role;
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        
        User savedUser = userRepository.save(user);
        
        // Audit log
        AuditLog log = new AuditLog(adminId, adminUsername, "CREATE_USER", "User", savedUser.getId().toString(), 
            "Created user: " + request.getUsername() + " with role: " + user.getRole(), getClientIp(httpRequest));
        auditLogRepository.save(log);
        
        // In real implementation, send email with password
        // sendEmailWithPassword(request.getEmail(), request.getUsername(), generatedPassword);
        
        return savedUser;
    }
    
    // Update user
    @Transactional
    public User updateUser(Long userId, UpdateUserRequest request, Long adminId, String adminUsername, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found...."));
        
        String oldRole = user.getRole();
        String oldStatus = user.getStatus();
        
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getMobileNo() != null) user.setMobileNo(request.getMobileNo());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
            if ("LOCKED".equals(request.getStatus())) {
                user.setAccountLocked(true);
            } else if ("ACTIVE".equals(request.getStatus())) {
                user.setAccountLocked(false);
                user.setFailedAttempts(0);
            }
        }
        
        User updatedUser = userRepository.save(user);
        
        // Audit log
        String details = String.format("Updated user ID %d: role changed from %s to %s, status changed from %s to %s",
            userId, oldRole, user.getRole(), oldStatus, user.getStatus());
        AuditLog log = new AuditLog(adminId, adminUsername, "UPDATE_USER", "User", userId.toString(), 
            details, getClientIp(httpRequest));
        auditLogRepository.save(log);
        
        return updatedUser;
    }
    
    // Deactivate user (set INACTIVE — does NOT delete)
    @Transactional
    public void deactivateUser(Long userId, Long adminId, String adminUsername, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus("INACTIVE");
        userRepository.save(user);
        AuditLog log = new AuditLog(adminId, adminUsername, "DEACTIVATE_USER", "User", userId.toString(),
            "Deactivated user: " + user.getUsername(), getClientIp(httpRequest));
        auditLogRepository.save(log);
    }

    // Hard delete user and all their data
    @Transactional
    public void deleteUser(Long userId, Long adminId, String adminUsername, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Delete child records first to avoid FK violation
        resourceRepository.deleteAll(resourceRepository.findByUser(user));
        budgetThresholdRepository.findByUserId(userId).ifPresent(budgetThresholdRepository::delete);
        engineerConfigRepository.findByEngineerId(userId).forEach(engineerConfigRepository::delete);
        engineerConfigRepository.findByClientId(userId).forEach(engineerConfigRepository::delete);
        auditLogRepository.findByUserId(userId).forEach(auditLogRepository::delete);
        userRepository.delete(user);
        AuditLog log = new AuditLog(adminId, adminUsername, "DELETE_USER", "User", userId.toString(),
            "Permanently deleted user: " + user.getUsername(), getClientIp(httpRequest));
        auditLogRepository.save(log);
    }
    
    // Unlock locked account
    @Transactional
    public void unlockUser(Long userId, Long adminId, String adminUsername, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAccountLocked(false);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setStatus("ACTIVE");
        userRepository.save(user);
        AuditLog log = new AuditLog(adminId, adminUsername, "UNLOCK_USER", "User", userId.toString(),
            "Unlocked account for user: " + user.getUsername(), getClientIp(httpRequest));
        auditLogRepository.save(log);
    }

    // Activate user
    @Transactional
    public void activateUser(Long userId, Long adminId, String adminUsername, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setStatus("ACTIVE");
        user.setAccountLocked(false);
        user.setFailedAttempts(0);
        userRepository.save(user);
        
        AuditLog log = new AuditLog(adminId, adminUsername, "ACTIVATE_USER", "User", userId.toString(), 
            "Activated user: " + user.getUsername(), getClientIp(httpRequest));
        auditLogRepository.save(log);
    }
    
    // Reset user password
    @Transactional
    public String resetPassword(Long userId, Long adminId, String adminUsername, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        AuditLog log = new AuditLog(adminId, adminUsername, "RESET_PASSWORD", "User", userId.toString(), 
            "Reset password for user: " + user.getUsername(), getClientIp(httpRequest));
        auditLogRepository.save(log);
        
        return newPassword;
    }
    
    // Get audit logs
    public List<AuditLog> getAuditLogs(String username, Integer days) {
        if (username != null && !username.isEmpty()) {
            return auditLogRepository.findByUsername(username);
        } else if (days != null) {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            return auditLogRepository.findByDateRange(since, LocalDateTime.now());
        }
        return auditLogRepository.findRecentLogs();
    }
    
    // Get system settings
    public Map<String, String> getSystemSettings() {
        Map<String, String> settings = new HashMap<>();
        List<SystemSettings> allSettings = systemSettingsRepository.findAll();
        for (SystemSettings setting : allSettings) {
            settings.put(setting.getKey(), setting.getValue());
        }
        return settings;
    }
    
    // Update system settings
    @Transactional
    public void updateSystemSettings(Map<String, String> settings, Long adminId, String adminUsername, HttpServletRequest httpRequest) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            SystemSettings setting = systemSettingsRepository.findByKey(entry.getKey()).orElse(null);
            if (setting == null) {
                setting = new SystemSettings(entry.getKey(), entry.getValue(), "");
            } else {
                setting.setValue(entry.getValue());
            }
            setting.setUpdatedBy(adminUsername);
            setting.setUpdatedAt(LocalDateTime.now().toString());
            systemSettingsRepository.save(setting);
        }
        
        AuditLog log = new AuditLog(adminId, adminUsername, "CONFIG_CHANGE", "Settings", "global", 
            "Updated system settings", getClientIp(httpRequest));
        auditLogRepository.save(log);
    }
    
    // Get platform stats summary
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.findByStatus("ACTIVE").size());
        stats.put("totalLoginsLast30Days", auditLogRepository.countByActionSince("LOGIN", thirtyDaysAgo));
        stats.put("totalUploadsLast30Days", auditLogRepository.countByActionSince("UPLOAD", thirtyDaysAgo));
        
        return stats;
    }
    
    // Helper methods
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            return request.getRemoteAddr();
        }
        return ipAddress.split(",")[0].trim();
    }
}