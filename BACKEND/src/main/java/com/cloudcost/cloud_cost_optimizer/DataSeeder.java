package com.cloudcost.cloud_cost_optimizer;

import com.cloudcost.cloud_cost_optimizer.model.User;
import com.cloudcost.cloud_cost_optimizer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("admin",    "Admin@1234",    "admin@cloudopt.com",    "System Admin",    "ROLE_ADMIN");
        seedUser("engineer", "Engineer@1234", "engineer@cloudopt.com", "Cloud Engineer",  "ROLE_ENGINEER");
        seedUser("client",   "Client@1234",   "client@cloudopt.com",   "Demo Client",     "ROLE_CLIENT");
    }

    private void seedUser(String username, String password, String email, String fullName, String role) {
        if (userRepository.existsByUsername(username)) return;
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setMobileNo("+911234567890");
        user.setAddress("Default Address");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        userRepository.save(user);
        System.out.println("[Seeder] Created " + role + " user: " + username + " / " + password);
    }
}
