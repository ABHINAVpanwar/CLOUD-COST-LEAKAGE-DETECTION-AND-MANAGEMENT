package com.cloudcost.cloud_cost_optimizer.controller;

import com.cloudcost.cloud_cost_optimizer.model.User;
import com.cloudcost.cloud_cost_optimizer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "email", user.getEmail(),
            "mobileNo", user.getMobileNo() != null ? user.getMobileNo() : "",
            "address", user.getAddress() != null ? user.getAddress() : "",
            "role", user.getRole(),
            "status", user.getStatus(),
            "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
            "lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : ""
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        if (body.containsKey("fullName")) user.setFullName(body.get("fullName"));
        if (body.containsKey("mobileNo")) user.setMobileNo(body.get("mobileNo"));
        if (body.containsKey("address")) user.setAddress(body.get("address"));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }
}
