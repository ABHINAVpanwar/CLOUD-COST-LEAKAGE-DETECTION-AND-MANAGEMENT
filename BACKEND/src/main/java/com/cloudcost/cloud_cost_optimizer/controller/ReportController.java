package com.cloudcost.cloud_cost_optimizer.controller;

import com.cloudcost.cloud_cost_optimizer.model.ResourceUsage;
import com.cloudcost.cloud_cost_optimizer.model.User;
import com.cloudcost.cloud_cost_optimizer.repository.ResourceRepository;
import com.cloudcost.cloud_cost_optimizer.service.AnalysisService;
import com.cloudcost.cloud_cost_optimizer.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private AnalysisService analysisService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return authService.getUserByUsername(username);
    }

    // US011 — Download CSV report (PDF requires iText/Apache PDFBox dependency; CSV works without extra deps)
    @GetMapping("/download/csv")
    public ResponseEntity<byte[]> downloadCsvReport() {
        try {
            User user = getCurrentUser();
            List<ResourceUsage> resources = resourceRepository.findByUser(user);
            List<Map<String, Object>> leakages = analysisService.getLeakageReport(user.getUsername());
            List<Map<String, String>> recommendations = analysisService.getRecommendations(user.getUsername());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos);

            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            writer.println("Cloud Cost Optimization Report");
            writer.println("Generated: " + now);
            writer.println("User: " + user.getUsername());
            writer.println();

            // Section 1: Cost Summary
            writer.println("=== COST SUMMARY ===");
            writer.println("Resource Name,Type,Provider,Region,CPU%,RAM%,Storage GB,Cost (INR),Uptime%");
            double totalCost = 0;
            for (ResourceUsage r : resources) {
                totalCost += r.getCost() != null ? r.getCost() : 0;
                writer.printf("%s,%s,%s,%s,%.1f,%.1f,%.1f,%.2f,%.1f%n",
                    safe(r.getResourceName()), safe(r.getResourceType()), safe(r.getProvider()),
                    safe(r.getRegion()), orZero(r.getCpuUsage()), orZero(r.getRamUsage()),
                    orZero(r.getStorageUsage()), orZero(r.getCost()), orZero(r.getUptime()));
            }
            writer.println();
            writer.println("Total Cost (INR)," + String.format("%.2f", totalCost));
            writer.println("Estimated Savings (INR)," + String.format("%.2f", totalCost * 0.2));
            writer.println();

            // Section 2: Leakages
            writer.println("=== COST LEAKAGES ===");
            writer.println("Resource,Type,Issue,Estimated Waste (INR),Action");
            for (Map<String, Object> l : leakages) {
                writer.printf("%s,%s,%s,%.2f,%s%n",
                    safe(l.get("resourceName")), safe(l.get("resourceType")),
                    safe(l.get("issue")), toDouble(l.get("estimatedWastedCost")),
                    safe(l.get("action")));
            }
            writer.println();

            // Section 3: Recommendations
            writer.println("=== OPTIMIZATION RECOMMENDATIONS ===");
            writer.println("Resource,Type,Issue,Action,Estimated Savings,Severity");
            for (Map<String, String> r : recommendations) {
                writer.printf("%s,%s,%s,%s,%s,%s%n",
                    safe(r.get("resource")), safe(r.get("resourceType")),
                    safe(r.get("issue")), safe(r.get("action")),
                    safe(r.get("savings")), safe(r.get("severity")));
            }

            writer.flush();
            byte[] bytes = baos.toByteArray();

            String filename = "cloud-cost-report-" + user.getUsername() + "-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(bytes.length)
                    .body(bytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String safe(Object val) {
        if (val == null) return "";
        return "\"" + val.toString().replace("\"", "'") + "\"";
    }

    private double orZero(Double val) { return val != null ? val : 0.0; }
    private double toDouble(Object val) {
        try { if (val != null) return Double.parseDouble(val.toString()); } catch (Exception ignored) {}
        return 0.0;
    }
}
