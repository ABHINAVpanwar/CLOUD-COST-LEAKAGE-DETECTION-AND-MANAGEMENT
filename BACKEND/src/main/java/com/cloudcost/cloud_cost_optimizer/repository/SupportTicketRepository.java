package com.cloudcost.cloud_cost_optimizer.repository;

import com.cloudcost.cloud_cost_optimizer.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<SupportTicket> findByAssignedToOrderByCreatedAtDesc(String assignedTo);
}
