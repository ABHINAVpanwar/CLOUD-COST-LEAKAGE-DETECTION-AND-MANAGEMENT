package com.cloudcost.cloud_cost_optimizer.controller;

import com.cloudcost.cloud_cost_optimizer.model.AuditLog;
import com.cloudcost.cloud_cost_optimizer.model.ResourceUsage;
import com.cloudcost.cloud_cost_optimizer.model.User;
import com.cloudcost.cloud_cost_optimizer.repository.AuditLogRepository;
import com.cloudcost.cloud_cost_optimizer.repository.ResourceRepository;
import com.cloudcost.cloud_cost_optimizer.repository.UserRepository;
import com.cloudcost.cloud_cost_optimizer.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip == null || ip.isEmpty()) ? request.getRemoteAddr() : ip.split(",")[0];
    }

    @PostMapping("/csv")
    public ResponseEntity<Map<String, Object>> uploadCSV(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("error", "Invalid file format or empty file.");
            return ResponseEntity.badRequest().body(response);
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null ||
                (!fileName.toLowerCase().endsWith(".csv") &&
                 !fileName.toLowerCase().endsWith(".txt"))) {
            response.put("success", false);
            response.put("error", "Invalid file format. Only CSV files are allowed.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<ResourceUsage> resources = parseCSV(file);
            if (resources.isEmpty()) {
                response.put("success", false);
                response.put("error", "No valid data found in file. Check format: resource_type,resource_name,provider,region,cpu_usage,ram_usage,storage_usage,cost,uptime");
                return ResponseEntity.badRequest().body(response);
            }

            for (ResourceUsage r : resources) {
                r.setUser(user);
                r.setCreatedAt(LocalDateTime.now());
                resourceRepository.save(r);
            }

            AuditLog log = new AuditLog(user.getId(), user.getUsername(), "UPLOAD",
                    "ResourceUsage", user.getId().toString(),
                    "Uploaded " + resources.size() + " resources from file: " + fileName,
                    getClientIp(httpRequest));
            auditLogRepository.save(log);

            response.put("success", true);
            response.put("message", "File uploaded successfully. Processed " + resources.size() + " resources.");
            response.put("count", resources.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error processing file. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private List<ResourceUsage> parseCSV(MultipartFile file) throws IOException {
        List<ResourceUsage> resources = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (firstLine && line.toLowerCase().contains("resource")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;
                String[] parts = line.contains(",") ? line.split(",") :
                                 line.contains("\t") ? line.split("\t") : line.split("\\s+");
                for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
                if (parts.length >= 9) {
                    ResourceUsage r = new ResourceUsage();
                    r.setResourceType(parts[0].isEmpty() ? "VM" : parts[0]);
                    r.setResourceName(parts[1].isEmpty() ? "Unknown" : parts[1]);
                    r.setProvider(parts[2].isEmpty() ? "AWS" : parts[2]);
                    r.setRegion(parts[3].isEmpty() ? "us-east-1" : parts[3]);
                    r.setCpuUsage(parseDouble(parts[4], 50.0));
                    r.setRamUsage(parseDouble(parts[5], 50.0));
                    r.setStorageUsage(parseDouble(parts[6], 100.0));
                    r.setCost(parseDouble(parts[7], 1000.0));
                    r.setUptime(parseDouble(parts[8], 90.0));
                    resources.add(r);
                }
            }
        }
        return resources;
    }

    private Double parseDouble(String value, Double defaultValue) {
        try {
            if (value != null && !value.isEmpty()) return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}
        return defaultValue;
    }

    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> addManualEntry(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        User user = getCurrentUser();
        if (user == null) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Validate required fields
        if (requestData.get("resourceName") == null || requestData.get("resourceName").toString().isEmpty()) {
            response.put("success", false);
            response.put("error", "Resource name is required.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            ResourceUsage resource = new ResourceUsage();
            resource.setUser(user);
            resource.setResourceType((String) requestData.getOrDefault("resourceType", "VM"));
            resource.setResourceName((String) requestData.getOrDefault("resourceName", "Manual Entry"));
            resource.setProvider((String) requestData.getOrDefault("provider", "AWS"));
            resource.setRegion((String) requestData.getOrDefault("region", "us-east-1"));
            resource.setCpuUsage(toDouble(requestData.get("cpuUsage"), 0.0));
            resource.setRamUsage(toDouble(requestData.get("ramUsage"), 0.0));
            resource.setStorageUsage(toDouble(requestData.get("storageUsage"), 0.0));
            resource.setCost(toDouble(requestData.get("cost"), 0.0));
            resource.setUptime(toDouble(requestData.get("uptime"), 100.0));
            resource.setCreatedAt(LocalDateTime.now());

            ResourceUsage saved = resourceRepository.save(resource);

            AuditLog log = new AuditLog(user.getId(), user.getUsername(), "MANUAL_ENTRY",
                    "ResourceUsage", saved.getId().toString(),
                    "Manual entry: " + resource.getResourceName(),
                    getClientIp(httpRequest));
            auditLogRepository.save(log);

            response.put("success", true);
            response.put("message", "Data submitted successfully.");
            response.put("id", saved.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error processing data. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private Double toDouble(Object val, Double def) {
        try {
            if (val != null) return Double.valueOf(val.toString());
        } catch (NumberFormatException ignored) {}
        return def;
    }
}
