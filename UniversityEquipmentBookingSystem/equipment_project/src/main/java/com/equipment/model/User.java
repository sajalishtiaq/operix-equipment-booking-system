package com.equipment.model;

import java.time.LocalDateTime;

/**
 * User - Domain Model Entity
 * Represents Teacher, Lab Manager, or Technician
 */
public class User {

    public enum Role { TEACHER, LAB_MANAGER, TECHNICIAN }

    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private Role role;
    private LocalDateTime createdAt;

    public User() {}

    public User(String username, String password, String fullName, String email, Role role) {
        this.username  = username;
        this.password  = password;
        this.fullName  = fullName;
        this.email     = email;
        this.role      = role;
    }

    // Getters & Setters
    public int getUserId()                  { return userId; }
    public void setUserId(int userId)       { this.userId = userId; }

    public String getUsername()             { return username; }
    public void setUsername(String u)       { this.username = u; }

    public String getPassword()             { return password; }
    public void setPassword(String p)       { this.password = p; }

    public String getFullName()             { return fullName; }
    public void setFullName(String n)       { this.fullName = n; }

    public String getEmail()                { return email; }
    public void setEmail(String e)          { this.email = e; }

    public Role getRole()                   { return role; }
    public void setRole(Role r)             { this.role = r; }

    public LocalDateTime getCreatedAt()     { return createdAt; }
    public void setCreatedAt(LocalDateTime t){ this.createdAt = t; }

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', name='%s', role=%s}",
                userId, username, fullName, role);
    }
}
