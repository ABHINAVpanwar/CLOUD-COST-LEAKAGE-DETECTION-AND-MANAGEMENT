package com.cloudcost.cloud_cost_optimizer.controller;

import com.cloudcost.cloud_cost_optimizer.model.*;
import com.cloudcost.cloud_cost_optimizer.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String token = authService.authenticate(request.getUsername(), request.getPassword(), httpRequest);
            User user = authService.getUserByUsername(request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

//    @PostMapping("/register")
//    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
//        try {
//            // Server-side field validation (US001)
//            if (request.getName() == null || !request.getName().matches("[A-Za-z ]{3,}"))
//                return ResponseEntity.badRequest().body(new ErrorResponse("Name must be at least 3 characters long and contain only letters."));
//            if (request.getEmail() == null || !request.getEmail().matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"))
//                return ResponseEntity.badRequest().body(new ErrorResponse("Enter a valid email address."));
//            if (request.getMobileNo() == null || !request.getMobileNo().replaceAll("^\\+\\d{1,4}", "").trim().matches("\\d{8,10}"))
//                return ResponseEntity.badRequest().body(new ErrorResponse("Enter a valid mobile number (8-10 digits)."));
//            if (request.getAddress() == null || request.getAddress().trim().length() < 10)
//                return ResponseEntity.badRequest().body(new ErrorResponse("Address must be at least 10 characters long."));
//            if (request.getUsername() == null || request.getUsername().contains(" ") || request.getUsername().length() < 5)
//                return ResponseEntity.badRequest().body(new ErrorResponse("Username must be at least 5 characters and contain no spaces."));
//            if (request.getPassword() == null || !request.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$"))
//                return ResponseEntity.badRequest().body(new ErrorResponse("Password must be at least 8 characters and include uppercase, lowercase, number, and special character."));
//
//            User user = new User(
//                request.getUsername(),
//                request.getPassword(),
//                request.getEmail(),
//                request.getName(),
//                request.getMobileNo(),
//                request.getAddress()
//            );
//            String token = authService.register(user);
//            User saved = authService.getUserByUsername(request.getUsername());
//            return ResponseEntity.ok(Map.of(
//                "token", token,
//                "username", saved.getUsername(),
//                "role", saved.getRole(),
//                "userId", saved.getId(),
//                "fullName", saved.getFullName() != null ? saved.getFullName() : "",
//                "email", saved.getEmail()
//            ));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
//        }
//    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        try {
            // Server-side field validation (US001)
            if (request.getName() == null || !request.getName().matches("[A-Za-z ]{3,}"))
                return ResponseEntity.badRequest().body(new ErrorResponse("Name must be at least 3 characters long and contain only letters."));
            
            // Updated: Email must be @gmail.com only
            if (request.getEmail() == null || !request.getEmail().matches("^[\\w.+\\-]+@gmail\\.com$"))
                return ResponseEntity.badRequest().body(new ErrorResponse("Only Gmail addresses are allowed (must end with @gmail.com)."));
            
            String cleanedMobile = request.getMobileNo() != null
            	    ? request.getMobileNo().replaceAll("[^0-9]", "")  // remove everything except digits
            	    : null;

            	// Remove country code (like 91)
            	if (cleanedMobile != null && cleanedMobile.length() > 10) {
            	    cleanedMobile = cleanedMobile.substring(cleanedMobile.length() - 10);
            	}

            	if (cleanedMobile == null || !cleanedMobile.matches("[6789]\\d{9}")) {
            	    return ResponseEntity.badRequest()
            	        .body(new ErrorResponse("Enter a valid mobile number (10 digits starting with 6,7,8, or 9)."));
            	}
            if (request.getAddress() == null || request.getAddress().trim().length() < 10)
                return ResponseEntity.badRequest().body(new ErrorResponse("Address must be at least 10 characters long."));
            if (request.getUsername() == null || request.getUsername().contains(" ") || request.getUsername().length() < 5)
                return ResponseEntity.badRequest().body(new ErrorResponse("Username must be at least 5 characters and contain no spaces."));
            if (request.getPassword() == null || !request.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$"))
                return ResponseEntity.badRequest().body(new ErrorResponse("Password must be at least 8 characters and include uppercase, lowercase, number, and special character."));

            User user = new User(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getName(),
                request.getMobileNo(),
                request.getAddress()
            );
            String token = authService.register(user);
            User saved = authService.getUserByUsername(request.getUsername());
            return ResponseEntity.ok(Map.of(
                "token", token,
                "username", saved.getUsername(),
                "role", saved.getRole(),
                "userId", saved.getId(),
                "fullName", saved.getFullName() != null ? saved.getFullName() : "",
                "email", saved.getEmail()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String token = authService.initiatePasswordReset(email);
            if (token != null) {
                return ResponseEntity.ok(Map.of("token", token));
            }
            return ResponseEntity.ok(Map.of("message", "No account found with that email."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "No account found with that email."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String newPassword = body.get("newPassword");
            authService.resetPasswordWithToken(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
