package com.cloudcost.cloud_cost_optimizer.repository;

import com.cloudcost.cloud_cost_optimizer.model.EngineerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EngineerConfigRepository extends JpaRepository<EngineerConfig, Long> {
    List<EngineerConfig> findByEngineerId(Long engineerId);
    List<EngineerConfig> findByClientId(Long clientId);
    Optional<EngineerConfig> findByEngineerIdAndClientId(Long engineerId, Long clientId);
}
