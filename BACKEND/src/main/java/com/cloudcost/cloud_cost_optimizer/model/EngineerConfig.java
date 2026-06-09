package com.cloudcost.cloud_cost_optimizer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "engineer_configs")
public class EngineerConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "engineer_id", nullable = false)
    private Long engineerId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "cpu_idle_threshold")
    private Double cpuIdleThreshold = 10.0;

    @Column(name = "cpu_underutilized_threshold")
    private Double cpuUnderutilizedThreshold = 60.0;

    @Column(name = "uptime_idle_threshold")
    private Double uptimeIdleThreshold = 80.0;

    @Column(name = "cost_coefficient")
    private Double costCoefficient = 1.0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private String updatedBy;

    public EngineerConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEngineerId() { return engineerId; }
    public void setEngineerId(Long engineerId) { this.engineerId = engineerId; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public Double getCpuIdleThreshold() { return cpuIdleThreshold; }
    public void setCpuIdleThreshold(Double cpuIdleThreshold) { this.cpuIdleThreshold = cpuIdleThreshold; }
    public Double getCpuUnderutilizedThreshold() { return cpuUnderutilizedThreshold; }
    public void setCpuUnderutilizedThreshold(Double cpuUnderutilizedThreshold) { this.cpuUnderutilizedThreshold = cpuUnderutilizedThreshold; }
    public Double getUptimeIdleThreshold() { return uptimeIdleThreshold; }
    public void setUptimeIdleThreshold(Double uptimeIdleThreshold) { this.uptimeIdleThreshold = uptimeIdleThreshold; }
    public Double getCostCoefficient() { return costCoefficient; }
    public void setCostCoefficient(Double costCoefficient) { this.costCoefficient = costCoefficient; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
