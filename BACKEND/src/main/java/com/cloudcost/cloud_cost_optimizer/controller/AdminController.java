package com.cloudcost.cloud_cost_optimizer.controller;

import com.cloudcost.cloud_cost_optimizer.model.*;
import com.cloudcost.cloud_cost_optimizer.service.AdminService;
import com.cloudcost.cloud_cost_optimizer.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AuthService authService;

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = authService.getUserByUsername(username);
        return user != null ? user.getId() : null;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        try {
            return ResponseEntity.ok(adminService.getDashboardData());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            return ResponseEntity.ok(adminService.getAllUsers());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam String q) {
        try {
            return ResponseEntity.ok(adminService.searchUsers(q));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = adminService.getUserById(id);
            if (user == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(new UserManagementDTO(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        try {
            // Normalize role prefix
            if (request.getRole() != null && !request.getRole().startsWith("ROLE_")) {
                request.setRole("ROLE_" + request.getRole());
            }
            User user = adminService.createUser(request, getCurrentUserId(), getCurrentUsername(), httpRequest);
            return ResponseEntity.ok(Map.of(
                "message", "User created successfully",
                "userId", user.getId(),
                "username", user.getUsername()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request, HttpServletRequest httpRequest) {
        try {
            // Normalize role prefix
            if (request.getRole() != null && !request.getRole().startsWith("ROLE_")) {
                request.setRole("ROLE_" + request.getRole());
            }
            // Block role changes for default seeded accounts
            User target = adminService.getUserById(id);
            if (target != null && List.of("admin", "engineer", "client").contains(target.getUsername())) {
                request.setRole(target.getRole()); // preserve original role
            }
            User user = adminService.updateUser(id, request, getCurrentUserId(), getCurrentUsername(), httpRequest);
            return ResponseEntity.ok(Map.of("message", "User updated successfully", "user", new UserManagementDTO(user)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            adminService.deactivateUser(id, getCurrentUserId(), getCurrentUsername(), httpRequest);
            return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}/hard")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            adminService.deleteUser(id, getCurrentUserId(), getCurrentUsername(), httpRequest);
            return ResponseEntity.ok(Map.of("message", "User permanently deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            adminService.activateUser(id, getCurrentUserId(), getCurrentUsername(), httpRequest);
            return ResponseEntity.ok(Map.of("message", "User activated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            adminService.unlockUser(id, getCurrentUserId(), getCurrentUsername(), httpRequest);
            return ResponseEntity.ok(Map.of("message", "User unlocked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String newPassword = adminService.resetPassword(id, getCurrentUserId(), getCurrentUsername(), httpRequest);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully", "newPassword", newPassword));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(@RequestParam(required = false) String username, @RequestParam(required = false) Integer days) {
        try {
            return ResponseEntity.ok(adminService.getAuditLogs(username, days));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSystemSettings() {
        try {
            return ResponseEntity.ok(adminService.getSystemSettings());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSystemSettings(@RequestBody Map<String, String> settings, HttpServletRequest httpRequest) {
        try {
            adminService.updateSystemSettings(settings, getCurrentUserId(), getCurrentUsername(), httpRequest);
            return ResponseEntity.ok(Map.of("message", "Settings updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getPlatformStats() {
        try {
            return ResponseEntity.ok(adminService.getPlatformStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
