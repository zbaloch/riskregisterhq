package com.riskregister.riskregisterapp.entities;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {
    private User user;
     
    public CustomUserDetails(User user) {
        this.user = user;
    }
 
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }
 
    @Override
    public String getPassword() {
        // Return empty string if null (magic link users don't have passwords)
        return user.getPassword() != null ? user.getPassword() : "";
    }
 
    @Override
    public String getUsername() {
        return user.getEmail();
    }
 
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
 
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
 
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
 
    @Override
    public boolean isEnabled() {
        return user.isApproved(); // User must be approved by admin to login
    }
     
    public String getFullName() {
        return user.getFirstName() + " " + user.getLastName();
    }
    
    public Role getRole() {
        return user.getRole();
    }
    
    public User getUser() {
        return user;
    }
}
