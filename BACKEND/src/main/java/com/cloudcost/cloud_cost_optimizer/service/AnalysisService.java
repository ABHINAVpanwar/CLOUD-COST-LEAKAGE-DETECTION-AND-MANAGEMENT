package com.cloudcost.cloud_cost_optimizer.service;

import com.cloudcost.cloud_cost_optimizer.model.*;
import com.cloudcost.cloud_cost_optimizer.repository.EngineerConfigRepository;
import com.cloudcost.cloud_cost_optimizer.repository.ResourceRepository;
import com.cloudcost.cloud_cost_optimizer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnalysisService {

    @Autowired private ResourceRepository resourceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EngineerConfigRepository engineerConfigRepository;

    // ── Single source of truth for default thresholds ──────────────────────
    public static final double DEFAULT_CPU_IDLE       = 10.0;
    public static final double DEFAULT_CPU_UNDER      = 60.0;
    public static final double DEFAULT_UPTIME_IDLE    = 80.0;
    public static final double DEFAULT_COST_COEFF     = 1.0;

    public EngineerConfig getEffectiveConfig(Long clientId) {
        return engineerConfigRepository.findByClientId(clientId)
                .stream().findFirst()
                .orElseGet(() -> {
                    EngineerConfig def = new EngineerConfig();
                    def.setCpuIdleThreshold(DEFAULT_CPU_IDLE);
                    def.setCpuUnderutilizedThreshold(DEFAULT_CPU_UNDER);
                    def.setUptimeIdleThreshold(DEFAULT_UPTIME_IDLE);
                    def.setCostCoefficient(DEFAULT_COST_COEFF);
                    return def;
                });
    }

    public void saveResources(User user, List<ResourceUsage> resources) {
        for (ResourceUsage resource : resources) {
            resource.setUser(user);
            resource.setCreatedAt(LocalDateTime.now());
        }
        resourceRepository.saveAll(resources);
    }

    // ── Core leakage computation — used by BOTH client and engineer paths ──
    public List<Map<String, Object>> computeLeakages(List<ResourceUsage> resources, EngineerConfig cfg) {
        List<Map<String, Object>> leakages = new ArrayList<>();
        double cpuIdle   = cfg.getCpuIdleThreshold();
        double cpuUnder  = cfg.getCpuUnderutilizedThreshold();
        double uptimeIdle = cfg.getUptimeIdleThreshold();
        double coeff     = cfg.getCostCoefficient();

        for (ResourceUsage r : resources) {
            double cost   = (r.getCost() != null ? r.getCost() : 0) * coeff;
            double cpu    = r.getCpuUsage() != null ? r.getCpuUsage() : -1;
            double uptime = r.getUptime()   != null ? r.getUptime()   : -1;

            List<String> issues = new ArrayList<>();
            double wastedCost = 0;

            // CPU check (independent of uptime)
            if (cpu >= 0 && cpu < cpuIdle) {
                issues.add("Unused (CPU " + fmt(cpu) + "% < " + cpuIdle + "%)");
                wastedCost = Math.max(wastedCost, cost * 0.9);
            } else if (cpu >= 0 && cpu < cpuUnder) {
                issues.add("Over-provisioned (CPU " + fmt(cpu) + "% < " + cpuUnder + "%)");
                wastedCost = Math.max(wastedCost, cost * 0.5);
            }

            // Uptime check (independent of CPU)
            if (uptime >= 0 && uptime < uptimeIdle) {
                issues.add("Idle (Uptime " + fmt(uptime) + "% < " + uptimeIdle + "%)");
                wastedCost = Math.max(wastedCost, cost * 0.7);
            }

            if (!issues.isEmpty()) {
                Map<String, Object> item = new HashMap<>();
                item.put("resourceName",        r.getResourceName());
                item.put("resourceType",        r.getResourceType());
                item.put("provider",            r.getProvider());
                item.put("cpuUsage",            r.getCpuUsage());
                item.put("uptime",              r.getUptime());
                item.put("cost",                r.getCost());
                item.put("issue",               String.join(" + ", issues));
                item.put("estimatedWastedCost", Math.round(wastedCost * 100.0) / 100.0);
                // alias so engineer template (uses estimatedWaste) also works
                item.put("estimatedWaste",      Math.round(wastedCost * 100.0) / 100.0);
                item.put("action",              "Consider downsizing or terminating this resource");
                leakages.add(item);
            }
        }
        return leakages;
    }

    public DashboardData getDashboardData(String username) {
        DashboardData data = new DashboardData();
        User user = userRepository.findByUsername(username).orElse(null);
        List<ResourceUsage> resources = user != null ? resourceRepository.findByUser(user) : List.of();

        if (user == null || resources.isEmpty()) {
            data.setWelcomeMessage(user != null ? "Welcome, " + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + "!" : "Welcome!");
            data.setTotalSpend(0.0);
            data.setEstimatedSavings(0.0);
            data.setTotalLeakages(0);
            data.setCostByService(new HashMap<>());
            data.setLeakageSummary(new HashMap<>());
            return data;
        }

        EngineerConfig cfg = getEffectiveConfig(user.getId());
        double cpuIdle    = cfg.getCpuIdleThreshold();
        double cpuUnder   = cfg.getCpuUnderutilizedThreshold();
        double uptimeIdle = cfg.getUptimeIdleThreshold();
        double coeff      = cfg.getCostCoefficient();

        double totalSpend = resources.stream().mapToDouble(r -> (r.getCost() != null ? r.getCost() : 0) * coeff).sum();
        data.setTotalSpend(totalSpend);

        // Use actual computed waste as estimated savings (consistent with leakage report)
        List<Map<String, Object>> leakageList = computeLeakages(resources, cfg);
        double estimatedSavings = leakageList.stream()
                .mapToDouble(l -> ((Number) l.get("estimatedWastedCost")).doubleValue()).sum();
        data.setEstimatedSavings(Math.round(estimatedSavings * 100.0) / 100.0);
        data.setTotalLeakages(leakageList.size());

        Map<String, Double> costByService = new HashMap<>();
        for (ResourceUsage r : resources) {
            String type = r.getResourceType() != null ? r.getResourceType() : "Others";
            costByService.merge(type, (r.getCost() != null ? r.getCost() : 0) * coeff, Double::sum);
        }
        data.setCostByService(costByService);

        Map<String, Integer> leakageSummary = new HashMap<>();
        leakageSummary.put("Unused VMs", (int) resources.stream()
                .filter(r -> "VM".equals(r.getResourceType()) && r.getCpuUsage() != null && r.getCpuUsage() < cpuIdle).count());
        leakageSummary.put("Orphaned Volumes", (int) resources.stream()
                .filter(r -> "Storage".equals(r.getResourceType()) && r.getStorageUsage() != null && r.getStorageUsage() < 10).count());
        leakageSummary.put("Idle Databases", (int) resources.stream()
                .filter(r -> "Database".equals(r.getResourceType()) && r.getUptime() != null && r.getUptime() < uptimeIdle).count());
        leakageSummary.put("Over-provisioned", (int) resources.stream()
                .filter(r -> r.getCpuUsage() != null && r.getCpuUsage() < cpuUnder).count());
        data.setLeakageSummary(leakageSummary);

        data.setWelcomeMessage("Welcome, " + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + "!");
        return data;
    }

    public List<Map<String, Object>> getResourceUtilization(String username) {
        List<Map<String, Object>> utilization = new ArrayList<>();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return utilization;
        for (ResourceUsage r : resourceRepository.findByUser(user)) {
            Map<String, Object> item = new HashMap<>();
            item.put("name",         r.getResourceName()  != null ? r.getResourceName()  : "Unknown");
            item.put("resourceType", r.getResourceType()  != null ? r.getResourceType()  : "Unknown");
            item.put("provider",     r.getProvider()      != null ? r.getProvider()      : "Unknown");
            item.put("region",       r.getRegion()        != null ? r.getRegion()        : "Unknown");
            item.put("cpuUsage",     r.getCpuUsage()      != null ? r.getCpuUsage()      : 0);
            item.put("ramUsage",     r.getRamUsage()      != null ? r.getRamUsage()      : 0);
            item.put("storageUsage", r.getStorageUsage()  != null ? r.getStorageUsage()  : 0);
            item.put("cost",         r.getCost()          != null ? r.getCost()          : 0);
            item.put("uptime",       r.getUptime()        != null ? r.getUptime()        : 0);
            item.put("uploadedAt",   r.getCreatedAt()     != null ? r.getCreatedAt().toString() : "");
            utilization.add(item);
        }
        return utilization;
    }

    public List<Map<String, Object>> getLeakageReport(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return new ArrayList<>();
        return computeLeakages(resourceRepository.findByUser(user), getEffectiveConfig(user.getId()));
    }

    public List<Map<String, Object>> getLeakageReportByUserId(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return new ArrayList<>();
        return computeLeakages(resourceRepository.findByUser(user), getEffectiveConfig(user.getId()));
    }

    public List<Map<String, String>> getRecommendations(String username) {
        List<Map<String, String>> recommendations = new ArrayList<>();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return recommendations;

        List<ResourceUsage> resources = resourceRepository.findByUser(user);
        if (resources == null || resources.isEmpty()) {
            recommendations.add(rec("System", "", "No data available", "Please upload cloud usage data", 0, "info"));
            return recommendations;
        }

        EngineerConfig cfg = getEffectiveConfig(user.getId());
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

            if (cpu < cpuIdle && up < uptimeIdle) {
                recommendations.add(rec(r.getResourceName(), type,
                    "Completely idle (CPU: " + fmt(cpu) + "%, Uptime: " + fmt(up) + "%)",
                    "Terminate this resource — it is consuming cost with zero utilization", cost * 0.95, "high"));
            } else if (up >= uptimeIdle && cpu >= cpuUnder) {
                recommendations.add(rec(r.getResourceName(), type,
                    "Always-on resource likely on on-demand pricing (Uptime: " + fmt(up) + "%)",
                    "Switch to Reserved Instance or Savings Plan for 40-60% cost reduction", cost * 0.45, "high"));
            } else if (cpu < cpuUnder && up < uptimeIdle) {
                recommendations.add(rec(r.getResourceName(), type,
                    "Low utilization (CPU: " + fmt(cpu) + "%, Uptime: " + fmt(up) + "%)",
                    "Implement auto-scheduling: shut down during nights and weekends", cost * 0.4, "medium"));
            } else if (ram < 20 && cpu >= cpuUnder) {
                recommendations.add(rec(r.getResourceName(), type,
                    "Over-allocated memory with low RAM usage (RAM: " + fmt(ram) + "%)",
                    "Switch from memory-optimized to general-purpose instance", cost * 0.3, "medium"));
            } else if (cpu >= cpuIdle && cpu < cpuUnder) {
                recommendations.add(rec(r.getResourceName(), type,
                    "Over-provisioned CPU (CPU: " + fmt(cpu) + "%)",
                    "Downsize to a smaller instance type with fewer vCPUs", cost * 0.35, "medium"));
            }
            if ("Storage".equalsIgnoreCase(type) && up < 40) {
                recommendations.add(rec(r.getResourceName(), type,
                    "Infrequently accessed storage (Uptime: " + fmt(up) + "%)",
                    "Move to cold/archive storage tier (e.g. S3 Glacier) for up to 70% cost reduction", cost * 0.65, "medium"));
            }
            if ("Database".equalsIgnoreCase(type) && up < 50) {
                recommendations.add(rec(r.getResourceName(), type,
                    "Database idle >50% of time (Uptime: " + fmt(up) + "%)",
                    "Migrate to serverless database engine (e.g. Aurora Serverless)", cost * 0.55, "high"));
            }
            if ("Network".equalsIgnoreCase(type) && cpu < cpuUnder && cost > 500) {
                recommendations.add(rec(r.getResourceName(), type,
                    "High-cost network resource with low utilization (CPU: " + fmt(cpu) + "%)",
                    "Review data transfer routes or introduce a CDN to reduce egress costs", cost * 0.3, "medium"));
            }
        }

        Map<String, List<ResourceUsage>> byProvider = new HashMap<>();
        for (ResourceUsage r : resources)
            byProvider.computeIfAbsent(r.getProvider() != null ? r.getProvider() : "Unknown", k -> new ArrayList<>()).add(r);
        for (Map.Entry<String, List<ResourceUsage>> entry : byProvider.entrySet()) {
            List<ResourceUsage> provRes = entry.getValue();
            long distinctRegions = provRes.stream().map(r -> r.getRegion() != null ? r.getRegion() : "").distinct().count();
            long lowUtil = provRes.stream().filter(r -> r.getCpuUsage() != null && r.getCpuUsage() < cpuUnder).count();
            if (distinctRegions >= 2 && lowUtil >= 2) {
                double waste = provRes.stream().filter(r -> r.getCpuUsage() != null && r.getCpuUsage() < cpuUnder)
                        .mapToDouble(r -> (r.getCost() != null ? r.getCost() : 0) * coeff * 0.25).sum();
                recommendations.add(rec(entry.getKey() + " (" + distinctRegions + " regions)", "Multi-Resource",
                    "Underutilized resources spread across " + distinctRegions + " regions",
                    "Consolidate workloads into fewer regions to reduce cross-region transfer costs", waste, "low"));
            }
        }

        if (recommendations.isEmpty())
            recommendations.add(rec("All Resources", "", "No optimization opportunities found", "Your cloud resources are well optimized!", 0, "low"));
        return recommendations;
    }

    private Map<String, String> rec(String resource, String type, String issue, String action, double savings, String severity) {
        Map<String, String> m = new HashMap<>();
        m.put("resource",     resource != null ? resource : "Unknown");
        m.put("resourceType", type);
        m.put("issue",        issue);
        m.put("action",       action);
        m.put("savings",      "\u20b9" + String.format("%.2f", savings));
        m.put("severity",     severity);
        return m;
    }

    // Used by admin dashboard total savings — now uses same threshold logic
    public double getTotalEstimatedSavings() {
        double savings = 0;
        for (User user : userRepository.findAll()) {
            List<ResourceUsage> resources = resourceRepository.findByUser(user);
            if (resources.isEmpty()) continue;
            EngineerConfig cfg = getEffectiveConfig(user.getId());
            savings += computeLeakages(resources, cfg).stream()
                    .mapToDouble(l -> ((Number) l.get("estimatedWastedCost")).doubleValue()).sum();
        }
        return Math.round(savings * 100.0) / 100.0;
    }

    public List<Map<String, Object>> getUploadHistory(String username) {
        List<Map<String, Object>> history = new ArrayList<>();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return history;
        List<ResourceUsage> resources = resourceRepository.findByUser(user);
        Map<String, List<ResourceUsage>> byDate = new LinkedHashMap<>();
        for (ResourceUsage r : resources) {
            String date = r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : "Unknown";
            byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<String, List<ResourceUsage>> entry : byDate.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", entry.getKey());
            item.put("resourceCount", entry.getValue().size());
            double totalCost = entry.getValue().stream().mapToDouble(r -> r.getCost() != null ? r.getCost() : 0).sum();
            item.put("totalCost", totalCost);
            EngineerConfig cfg = getEffectiveConfig(user.getId());
            double waste = computeLeakages(entry.getValue(), cfg).stream()
                    .mapToDouble(l -> ((Number) l.get("estimatedWastedCost")).doubleValue()).sum();
            item.put("estimatedSavings", Math.round(waste * 100.0) / 100.0);
            history.add(item);
        }
        return history;
    }

    private String fmt(double val) { return String.format("%.1f", val); }
}
