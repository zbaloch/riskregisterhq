package com.riskregister.riskregisterapp.controllers;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.riskregister.riskregisterapp.entities.Organization;
import com.riskregister.riskregisterapp.entities.Role;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.repositories.OrganizationRepository;
import com.riskregister.riskregisterapp.repositories.UserRepository;
import com.riskregister.riskregisterapp.enums.LookupType;
import com.riskregister.riskregisterapp.services.EmailService;
import com.riskregister.riskregisterapp.services.LookupService;
import com.riskregister.riskregisterapp.services.RiskTaxonomyService;
import com.riskregister.riskregisterapp.services.UserService;

@Controller
@RequestMapping("/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);
    private final SecureRandom random = new SecureRandom();
    private static final int TOKEN_BYTE_SIZE = 32;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RiskTaxonomyService taxonomyService;

    @Autowired
    private LookupService lookupService;

    @GetMapping
    public String settingsPage(Model model, Principal principal,
                               @RequestParam(required = false) String fieldType,
                               @ModelAttribute("currentUser") User currentUser) {
        List<User> users = userRepository.findByOrganizationIdOrderByFirstNameAscLastNameAsc(currentUser.getOrganizationId());
        Organization org = organizationRepository.findById(currentUser.getOrganizationId()).orElse(null);
        model.addAttribute("users", users);
        model.addAttribute("organization", org);
        model.addAttribute("roles", Role.values());
        model.addAttribute("taxonomy", taxonomyService.getTaxonomy(currentUser.getOrganizationId()));
        model.addAttribute("uncategorizedSubs", taxonomyService.getUncategorizedSubcategories(currentUser.getOrganizationId()));

        // Managed fields: the type list drives the left pane, the selected type the right
        LookupType selected = LookupType.fromCode(fieldType);
        if (selected == null) selected = LookupType.values()[0];
        model.addAttribute("fieldTypes", LookupType.values());
        model.addAttribute("selectedFieldType", selected);
        model.addAttribute("fieldValues", lookupService.findAll(selected, currentUser.getOrganizationId()));
        model.addAttribute("fieldUsage", lookupService.usageCounts(selected, currentUser.getOrganizationId()));
        return "settings/index";
    }

    @PostMapping("/users/create")
    public String createUser(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String role,
            @ModelAttribute("currentUser") User currentUser,
            RedirectAttributes redirectAttrs) {

        // Check if user already exists
        User existingUser = userRepository.findByEmail(email);
        if (existingUser != null) {
            redirectAttrs.addFlashAttribute("errorMessage", "User with this email already exists.");
            return "redirect:/settings?tab=users";
        }

        try {
            // Create new user
            User newUser = new User();
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setEmail(email);
            newUser.setRole(Role.valueOf(role));
            newUser.setApproved(false); // Admin must send magic link, approval happens on first login
            newUser.setOrganizationId(currentUser.getOrganizationId());

            // Generate magic link token
            String token = generateToken();
            newUser.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 24);
            newUser.setTokenExpirationDate(expirationDate);

            // Save user
            userRepository.save(newUser);

            // Send magic link email
            emailService.sendEmail(newUser);

            redirectAttrs.addFlashAttribute("successMessage",
                "User created successfully. Magic link sent to " + email);
        } catch (Exception e) {
            log.error("Error creating user", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error creating user: " + e.getMessage());
        }

        return "redirect:/settings?tab=users";
    }

    @PostMapping("/users/suspend")
    public String suspendUser(
            @RequestParam String userId,
            @ModelAttribute("currentUser") User currentUser,
            RedirectAttributes redirectAttrs) {

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                // Verify user belongs to current organization
                if (!user.getOrganizationId().equals(currentUser.getOrganizationId())) {
                    redirectAttrs.addFlashAttribute("errorMessage", "Unauthorized to modify this user.");
                    return "redirect:/settings?tab=users";
                }
                user.setApproved(false);
                userRepository.save(user);
                redirectAttrs.addFlashAttribute("successMessage",
                    "User " + user.getDisplayName() + " has been suspended.");
            } else {
                redirectAttrs.addFlashAttribute("errorMessage", "User not found.");
            }
        } catch (Exception e) {
            log.error("Error suspending user", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error suspending user: " + e.getMessage());
        }

        return "redirect:/settings?tab=users";
    }

    @PostMapping("/users/activate")
    public String activateUser(
            @RequestParam String userId,
            @ModelAttribute("currentUser") User currentUser,
            RedirectAttributes redirectAttrs) {

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                // Verify user belongs to current organization
                if (!user.getOrganizationId().equals(currentUser.getOrganizationId())) {
                    redirectAttrs.addFlashAttribute("errorMessage", "Unauthorized to modify this user.");
                    return "redirect:/settings?tab=users";
                }
                user.setApproved(true);
                userRepository.save(user);
                redirectAttrs.addFlashAttribute("successMessage",
                    "User " + user.getDisplayName() + " has been activated.");
            } else {
                redirectAttrs.addFlashAttribute("errorMessage", "User not found.");
            }
        } catch (Exception e) {
            log.error("Error activating user", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error activating user: " + e.getMessage());
        }

        return "redirect:/settings?tab=users";
    }

    @PostMapping("/users/update")
    public String updateUser(
            @RequestParam String userId,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String role,
            @RequestParam(defaultValue = "false") String approved,
            @ModelAttribute("currentUser") User currentUser,
            RedirectAttributes redirectAttrs) {

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                // Verify user belongs to current organization
                if (!user.getOrganizationId().equals(currentUser.getOrganizationId())) {
                    redirectAttrs.addFlashAttribute("errorMessage", "Unauthorized to modify this user.");
                    return "redirect:/settings?tab=users";
                }
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setRole(Role.valueOf(role));
                user.setApproved("true".equals(approved));
                userRepository.save(user);
                redirectAttrs.addFlashAttribute("successMessage",
                    "User " + user.getDisplayName() + " has been updated.");
            } else {
                redirectAttrs.addFlashAttribute("errorMessage", "User not found.");
            }
        } catch (Exception e) {
            log.error("Error updating user", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error updating user: " + e.getMessage());
        }

        return "redirect:/settings?tab=users";
    }

    @PostMapping("/organization")
    public String updateOrganization(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer riskAppetiteThreshold,
            @ModelAttribute("currentUser") User currentUser,
            RedirectAttributes redirectAttrs) {
        Organization org = organizationRepository.findById(currentUser.getOrganizationId()).orElse(null);
        if (org != null) {
            if (riskAppetiteThreshold != null && (riskAppetiteThreshold < 1 || riskAppetiteThreshold > 25)) {
                redirectAttrs.addFlashAttribute("errorMessage",
                    "Risk appetite threshold must be between 1 and 25 (the range of possible residual scores).");
                return "redirect:/settings?tab=organization";
            }
            org.setName(name);
            org.setDescription(description);
            if (riskAppetiteThreshold != null) {
                org.setRiskAppetiteThreshold(riskAppetiteThreshold);
            }
            org.setUpdatedAt(java.time.Instant.now());
            organizationRepository.save(org);
            redirectAttrs.addFlashAttribute("successMessage", "Organization updated.");
        }
        return "redirect:/settings?tab=organization";
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_SIZE];
        random.nextBytes(bytes);
        return String.valueOf(Hex.encode(bytes));
    }
}
