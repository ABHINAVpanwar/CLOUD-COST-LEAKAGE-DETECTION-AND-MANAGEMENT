package com.cloudcost.cloud_cost_optimizer.repository;

import com.cloudcost.cloud_cost_optimizer.model.BudgetThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BudgetThresholdRepository extends JpaRepository<BudgetThreshold, Long> {
    Optional<BudgetThreshold> findByUserId(Long userId);
}
