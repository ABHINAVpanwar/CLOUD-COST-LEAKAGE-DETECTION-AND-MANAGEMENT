package com.cloudcost.cloud_cost_optimizer.service;

import com.cloudcost.cloud_cost_optimizer.model.*;
import com.cloudcost.cloud_cost_optimizer.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EngineerService {

    @Autowired private UserRepository userRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private EngineerConfigRepository engineerConfigRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private AnalysisService analysisService;

    // Get all clients assigned to this engineer (for now all ROLE_CLIENT users)
    public List<Map<String, Object>> getAssignedClients(Long engineerId) {
        return userRepository.findByRole("ROLE_CLIENT").stream().map(client -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", client.getId());
            m.put("username", client.getUsername());
            m.put("fullName", client.getFullName());
            m.put("email", client.getEmail());
            m.put("status", client.getStatus());
            List<ResourceUsage> resources = resourceRepository.findByUser(client);
            m.put("resourceCount", resources.size());
            double totalCost = resources.stream().mapToDouble(r -> r.getCost() != null ? r.getCost() : 0).sum();
            m.put("totalCost", totalCost);
            return m;
        }).collect(Collectors.toList());
    }

    // Get client data for analysis
    public Map<String, Object> getClientAnalysis(Long engineerId, Long clientId) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        EngineerConfig config = engineerConfigRepository
                .findByEngineerIdAndClientId(engineerId, clientId)
                .orElse(defaultConfig(engineerId, clientId));

        List<ResourceUsage> resources = resourceRepository.findByUser(client);

        double coeff = config.getCostCoefficient();
        double totalCost = resources.stream().mapToDouble(r -> (r.getCost() != null ? r.getCost() : 0) * coeff).sum();

        // Delegate to shared leakage logic — same result as client/admin view
        List<Map<String, Object>> leakages = analysisService.computeLeakages(resources, config);
        double savings = leakages.stream()
                .mapToDouble(l -> ((Number) l.get("estimatedWastedCost")).doubleValue()).sum();

        // Build recommendations using same thresholds
        List<Map<String, Object>> recommendations = buildRecommendations(resources, config);

        Map<String, Object> result = new HashMap<>();
        result.put("clientId",        clientId);
        result.put("clientName",      client.getFullName());
        result.put("totalResources",  resources.size());
        result.put("totalCost",       Math.round(totalCost * 100.0) / 100.0);
        result.put("estimatedSavings",Math.round(savings * 100.0) / 100.0);
        result.put("leakageCount",    leakages.size());
        result.put("leakages",        leakages);
        result.put("recommendations", recommendations);
        result.put("config",          configToMap(config));
        return result;
    }

    // Bulk analysis across all clients
    public List<Map<String, Object>> bulkAnalysis(Long engineerId) {
        List<User> clients = userRepository.findByRole("ROLE_CLIENT");
        List<Map<String, Object>> results = new ArrayList<>();
        for (User client : clients) {
            try {
                Map<String, Object> analysis = getClientAnalysis(engineerId, client.getId());
                results.add(analysis);
            } catch (Exception ignored) {}
        }
        return results;
    }

    // Get/save threshold config for a client
    public Map<String, Object> getConfig(Long engineerId, Long clientId) {
        EngineerConfig config = engineerConfigRepository
                .findByEngineerIdAndClientId(engineerId, clientId)
                .orElse(defaultConfig(engineerId, clientId));
        return configToMap(config);
    }

    @Transactional
    public Map<String, Object> saveConfig(Long engineerId, Long clientId, Map<String, Object> body, String engineerUsername) {
        double cpuIdle  = toDouble(body.get("cpuIdleThreshold"), 10.0);
        double cpuUnder = toDouble(body.get("cpuUnderutilizedThreshold"), 60.0);
        double coeff    = toDouble(body.get("costCoefficient"), 1.0);
        if (cpuIdle >= cpuUnder)
            throw new RuntimeException("CPU Idle Threshold must be less than CPU Underutilized Threshold.");
        if (coeff <= 0)
            throw new RuntimeException("Cost Coefficient must be greater than 0.");

        EngineerConfig config = engineerConfigRepository
                .findByEngineerIdAndClientId(engineerId, clientId)
                .orElse(new EngineerConfig());

        config.setEngineerId(engineerId);
        config.setClientId(clientId);
        if (body.containsKey("cpuIdleThreshold"))
            config.setCpuIdleThreshold(toDouble(body.get("cpuIdleThreshold"), AnalysisService.DEFAULT_CPU_IDLE));
        if (body.containsKey("cpuUnderutilizedThreshold"))
            config.setCpuUnderutilizedThreshold(toDouble(body.get("cpuUnderutilizedThreshold"), AnalysisService.DEFAULT_CPU_UNDER));
        if (body.containsKey("uptimeIdleThreshold"))
            config.setUptimeIdleThreshold(toDouble(body.get("uptimeIdleThreshold"), AnalysisService.DEFAULT_UPTIME_IDLE));
        if (body.containsKey("costCoefficient"))
            config.setCostCoefficient(toDouble(body.get("costCoefficient"), AnalysisService.DEFAULT_COST_COEFF));
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(engineerUsername);

        engineerConfigRepository.save(config);

        // Audit log
        AuditLog log = new AuditLog(engineerId, engineerUsername, "CONFIG_CHANGE", "EngineerConfig",
                clientId.toString(), "Updated thresholds for client " + clientId, "system");
        auditLogRepository.save(log);

        return configToMap(config);
    }

    // Savings history: before/after config changes
    public Map<String, Object> getSavingsTracking(Long engineerId, Long clientId) {
        List<AuditLog> configLogs = auditLogRepository.findByUsername(
                userRepository.findById(engineerId).map(User::getUsername).orElse(""));

        Map<String, Object> result = new HashMap<>();
        result.put("clientId", clientId);

        // Current savings
        Map<String, Object> current = getClientAnalysis(engineerId, clientId);
        result.put("currentSavings", current.get("estimatedSavings"));
        result.put("currentCost", current.get("totalCost"));
        result.put("configChanges", configLogs.stream()
                .filter(l -> "CONFIG_CHANGE".equals(l.getAction()) && clientId.toString().equals(l.getEntityId()))
                .map(l -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("timestamp", l.getCreatedAt());
                    m.put("details", l.getDetails());
                    return m;
                }).collect(Collectors.toList()));
        return result;
    }

    private EngineerConfig defaultConfig(Long engineerId, Long clientId) {
        EngineerConfig c = new EngineerConfig();
        c.setEngineerId(engineerId);
        c.setClientId(clientId);
        c.setCpuIdleThreshold(10.0);
        c.setCpuUnderutilizedThreshold(60.0);
        c.setUptimeIdleThreshold(80.0);
        c.setCostCoefficient(1.0);
        return c;
    }

    private Map<String, Object> configToMap(EngineerConfig c) {
        Map<String, Object> m = new HashMap<>();
        m.put("cpuIdleThreshold", c.getCpuIdleThreshold());
        m.put("cpuUnderutilizedThreshold", c.getCpuUnderutilizedThreshold());
        m.put("uptimeIdleThreshold", c.getUptimeIdleThreshold());
        m.put("costCoefficient", c.getCostCoefficient());
        m.put("updatedAt", c.getUpdatedAt());
        m.put("updatedBy", c.getUpdatedBy());
        return m;
    }

    private List<Map<String, Object>> buildRecommendations(List<ResourceUsage> resources, EngineerConfig cfg) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        double cpuIdle    = cfg.getCpuIdleThreshold();
        double cpuUnder   = cfg.getCpuUnderutilizedThreshold();
        double uptimeIdle = cfg.getUptimeIdleThreshold();
        double coeff      = cfg.getCostCoefficient();
        for (ResourceUsage r : resources) {
            double cost = (r.getCost() != null ? r.getCost() : 0) * coeff;
            double cpu  = r.getCpuUsage() != null ? r.getCpuUsage() : 0;
            double ram  = r.getRamUsage() != null ? r.getRamUsage() : 0;
            double up   = r.getUptime()   != null ? r.getUptime()   : 0;
            String type = r.getResourceType() != null ? r.getResourceType() : "";
            if (cpu < cpuIdle && up < uptimeIdle)
                recommendations.add(buildRec(r, "Completely idle (CPU: " + fmt(cpu) + "%, Uptime: " + fmt(up) + "%)", "Terminate this resource", cost * 0.95, "high"));
            else if (up >= uptimeIdle && cpu >= cpuUnder)
                recommendations.add(buildRec(r, "Always-on on-demand pricing (Uptime: " + fmt(up) + "%)", "Switch to Reserved Instance or Savings Plan", cost * 0.45, "high"));
            else if (cpu < cpuUnder && up < uptimeIdle)
                recommendations.add(buildRec(r, "Low utilization (CPU: " + fmt(cpu) + "%, Uptime: " + fmt(up) + "%)", "Implement auto-scheduling", cost * 0.4, "medium"));
            else if (ram < 20 && cpu >= cpuUnder)
                recommendations.add(buildRec(r, "Over-allocated memory (RAM: " + fmt(ram) + "%)", "Switch to general-purpose instance", cost * 0.3, "medium"));
            else if (cpu >= cpuIdle && cpu < cpuUnder)
                recommendations.add(buildRec(r, "Over-provisioned CPU (CPU: " + fmt(cpu) + "%)", "Downsize instance type", cost * 0.35, "medium"));
            if ("Storage".equalsIgnoreCase(type) && up < 40)
                recommendations.add(buildRec(r, "Infrequently accessed storage", "Move to cold/archive tier", cost * 0.65, "medium"));
            if ("Database".equalsIgnoreCase(type) && up < 50)
                recommendations.add(buildRec(r, "Database idle >50% of time", "Migrate to serverless database", cost * 0.55, "high"));
            if ("Network".equalsIgnoreCase(type) && cpu < cpuUnder && cost > 500)
                recommendations.add(buildRec(r, "High-cost network with low utilization", "Review routes or introduce CDN", cost * 0.3, "medium"));
        }
        return recommendations;
    }

    private Map<String, Object> buildRec(ResourceUsage r, String issue, String action, double savings, String severity) {
        Map<String, Object> m = new HashMap<>();
        m.put("resource", r.getResourceName());
        m.put("resourceType", r.getResourceType());
        m.put("issue", issue);
        m.put("action", action);
        m.put("savings", Math.round(savings * 100.0) / 100.0);
        m.put("severity", severity);
        return m;
    }

    private Double toDouble(Object val, Double def) {
        try { if (val != null) return Double.valueOf(val.toString()); } catch (Exception ignored) {}
        return def;
    }

    private String fmt(double val) { return String.format("%.1f", val); }
}
