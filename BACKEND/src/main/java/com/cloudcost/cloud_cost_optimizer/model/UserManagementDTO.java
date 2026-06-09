package com.cloudcost.cloud_cost_optimizer.model;

public class UserManagementDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String mobileNo;
    private String role;
    private String status;
    private boolean accountLocked;
    private String createdAt;
    private String lastLogin;
    
    public UserManagementDTO() {}
    
    public UserManagementDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.mobileNo = user.getMobileNo();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.accountLocked = Boolean.TRUE.equals(user.getAccountLocked());
        this.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;
        this.lastLogin = user.getLastLogin() != null ? user.getLastLogin().toString() : null;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isAccountLocked() { return accountLocked; }
    public void setAccountLocked(boolean accountLocked) { this.accountLocked = accountLocked; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
}