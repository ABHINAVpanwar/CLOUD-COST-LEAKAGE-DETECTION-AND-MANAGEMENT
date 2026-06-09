package com.cloudcost.cloud_cost_optimizer.repository;
import com.cloudcost.cloud_cost_optimizer.model.ResourceUsage;
import com.cloudcost.cloud_cost_optimizer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResourceRepository extends JpaRepository<ResourceUsage, Long> {
    List<ResourceUsage> findByUser(User user);
    void deleteByUser(User user);
}