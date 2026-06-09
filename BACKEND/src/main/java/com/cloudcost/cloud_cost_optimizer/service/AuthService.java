package com.cloudcost.cloud_cost_optimizer.service;

import com.cloudcost.cloud_cost_optimizer.model.AuditLog;
import com.cloudcost.cloud_cost_optimizer.model.User;
import com.cloudcost.cloud_cost_optimizer.repository.AuditLogRepository;
import com.cloudcost.cloud_cost_optimizer.repository.SystemSettingsRepository;
import com.cloudcost.cloud_cost_optimizer.repository.UserRepository;
import com.cloudcost.cloud_cost_optimizer.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public String register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_CLIENT");
        user.setStatus("ACTIVE");
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return jwtUtil.generateToken(savedUser.getUsername(), savedUser.getRole());
    }

    public String authenticate(String username, String password, HttpServletRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Account is locked until " + user.getLockedUntil().toString());
            } else {
                user.setAccountLocked(false);
                user.setFailedAttempts(0);
                user.setStatus("ACTIVE");
                userRepository.save(user);
            }
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            int maxAttempts = systemSettingsRepository.findByKey("login.max.attempts")
                .map(s -> { try { return Integer.parseInt(s.getValue()); } catch (Exception e) { return 5; } })
                .orElse(5);
            if (attempts >= maxAttempts) {
                user.setAccountLocked(true);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                user.setStatus("LOCKED");
            }
            userRepository.save(user);
            throw new RuntimeException("Invalid credentials");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Account is not active");
        }

        user.setFailedAttempts(0);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        try {
            AuditLog log = new AuditLog();
            log.setUserId(user.getId());
            log.setUsername(username);
            log.setAction("LOGIN");
            log.setEntityType("User");
            log.setEntityId(user.getId().toString());
            log.setDetails("User logged in successfully");
            log.setIpAddress(getClientIp(request));
            log.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception ignored) {}

        return jwtUtil.generateToken(username, user.getRole());
    }

    public String initiatePasswordReset(String email) {
        return userRepository.findByEmail(email).map(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            System.out.println("[PASSWORD RESET] User: " + user.getUsername() + " | Token: " + token);
            return token;
        }).orElse(null);
    }

    public void resetPasswordWithToken(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token."));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Reset token has expired. Please request a new one.");
        if (newPassword == null || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$"))
            throw new RuntimeException("Password must be at least 8 characters and include uppercase, lowercase, number, and special character.");
        if (passwordEncoder.matches(newPassword, user.getPassword()))
            throw new RuntimeException("New password must be different from your current password.");
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void resetPassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
