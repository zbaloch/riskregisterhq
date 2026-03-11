package com.riskregister.riskregisterapp.entities;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    private String id;

    private String email;

    private String firstName;

    private String lastName;
    
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role;
    
    private boolean approved; // User must be approved by admin before they can login
    
    private String token;
    private Calendar tokenExpirationDate;
    private Calendar tokenUsedDate;

    public User() {
        this.id = UUID.randomUUID().toString();
        this.role = Role.MEMBER; // Set default role to MEMBER
        this.approved = false; // Default to not approved - admin must approve
    }

    public String getDisplayName() {
        String name = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        return name.isEmpty() ? email : name;
    }

    // TODO: First user an admin always
    // TODO: Add audit logs which captures the changes made by users
    // TODO: Add commentable to everything risks, tasks reviews etc
    // TODO: Add notifications for users when they are assigned a task, when a risk they own is updated, when they are mentioned in a comment, etc.
    // TODO: Reports
    // Documents to a risk
    // Controls section where controls based on standards that are applicable are managed (for future), users can add custom or import controls from standards, and link them to risks, and track the implementation of controls.
    // Add helpful tips everywhere so even a noob can do risk management


}
