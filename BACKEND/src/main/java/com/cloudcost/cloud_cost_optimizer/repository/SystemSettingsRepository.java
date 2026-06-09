package com.cloudcost.cloud_cost_optimizer.repository;

import com.cloudcost.cloud_cost_optimizer.model.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
    Optional<SystemSettings> findByKey(String key);
    boolean existsByKey(String key);
}