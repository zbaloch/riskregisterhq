package com.riskregister.riskregisterapp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.riskregister.riskregisterapp.entities.CustomUserDetails;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.repositories.UserRepository;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute("currentUser")
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUser();
            } else if (principal instanceof String) {
                // If principal is just a username string, try to fetch the user
                String username = (String) principal;
                if (!"anonymousUser".equals(username)) {
                    return userRepository.findByEmail(username);
                }
            }
        }
        return null;
    }
}
