package com.cloudcost.cloud_cost_optimizer.controller;

import com.cloudcost.cloud_cost_optimizer.model.SupportTicket;
import com.cloudcost.cloud_cost_optimizer.model.User;
import com.cloudcost.cloud_cost_optimizer.repository.SupportTicketRepository;
import com.cloudcost.cloud_cost_optimizer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    // Client submits a ticket
    @PostMapping("/ticket")
    public ResponseEntity<?> submitTicket(@RequestBody Map<String, String> body) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        String subject = body.get("subject");
        String message = body.get("message");
        String assignedTo = body.get("assignedTo"); // "ADMIN" or "ENGINEER"
        String priority = body.getOrDefault("priority", "MEDIUM");

        if (subject == null || subject.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Subject is required."));
        if (message == null || message.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Message is required."));
        if (!"ADMIN".equals(assignedTo) && !"ENGINEER".equals(assignedTo))
            return ResponseEntity.badRequest().body(Map.of("error", "assignedTo must be ADMIN or ENGINEER."));

        SupportTicket ticket = new SupportTicket();
        ticket.setClientId(user.getId());
        ticket.setClientUsername(user.getUsername());
        ticket.setClientName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        ticket.setSubject(subject);
        ticket.setMessage(message);
        ticket.setAssignedTo(assignedTo);
        ticket.setPriority(priority.toUpperCase());

        SupportTicket saved = ticketRepository.save(ticket);
        return ResponseEntity.ok(Map.of("message", "Ticket submitted successfully.", "ticketId", saved.getId()));
    }

    // Client views their own tickets
    @GetMapping("/my-tickets")
    public ResponseEntity<?> getMyTickets() {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        return ResponseEntity.ok(ticketRepository.findByClientIdOrderByCreatedAtDesc(user.getId()));
    }

    // Admin views tickets assigned to ADMIN
    @GetMapping("/admin-tickets")
    public ResponseEntity<?> getAdminTickets() {
        return ResponseEntity.ok(ticketRepository.findByAssignedToOrderByCreatedAtDesc("ADMIN"));
    }

    // Engineer views tickets assigned to ENGINEER
    @GetMapping("/engineer-tickets")
    public ResponseEntity<?> getEngineerTickets() {
        return ResponseEntity.ok(ticketRepository.findByAssignedToOrderByCreatedAtDesc("ENGINEER"));
    }

    // Admin or Engineer updates ticket status
    @PutMapping("/ticket/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ticketRepository.findById(id).map(ticket -> {
            ticket.setStatus(body.getOrDefault("status", ticket.getStatus()).toUpperCase());
            ticketRepository.save(ticket);
            return ResponseEntity.ok(Map.of("message", "Status updated."));
        }).orElse(ResponseEntity.notFound().build());
    }
}
