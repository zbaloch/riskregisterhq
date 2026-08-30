package com.riskregister.riskregisterapp.controllers;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.repositories.UserRepository;
import com.riskregister.riskregisterapp.services.RiskTaxonomyService;

/**
 * Admin endpoints for the Risk Taxonomy tab in Settings.
 * All mutations redirect back to /settings?tab=taxonomy with a flash message.
 */
@Controller
@RequestMapping("/settings/taxonomy")
@PreAuthorize("hasRole('ADMIN')")
public class TaxonomyController {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyController.class);
    private static final String REDIRECT = "redirect:/settings?tab=taxonomy";

    @Autowired
    private RiskTaxonomyService taxonomyService;

    @Autowired
    private UserRepository userRepository;

    // -----------------------------------------------------------------------
    // Categories
    // -----------------------------------------------------------------------

    @PostMapping("/categories/create")
    public String createCategory(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 Principal principal,
                                 @ModelAttribute("currentUser") User currentUser,
                                 RedirectAttributes redirectAttrs) {
        try {
            var category = taxonomyService.createCategory(name, description,
                actorEmail(principal), actorName(principal), currentUser.getOrganizationId());
            redirectAttrs.addFlashAttribute("successMessage",
                "Category \"" + category.getName() + "\" created.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error creating risk category", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error creating category: " + e.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/categories/update")
    public String updateCategory(@RequestParam Long id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 Principal principal,
                                 @ModelAttribute("currentUser") User currentUser,
                                 RedirectAttributes redirectAttrs) {
        try {
            var category = taxonomyService.updateCategory(id, name, description,
                actorEmail(principal), actorName(principal), currentUser.getOrganizationId());
            redirectAttrs.addFlashAttribute("successMessage",
                "Category \"" + category.getName() + "\" updated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error updating risk category", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error updating category: " + e.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/categories/delete")
    public String deleteCategory(@RequestParam Long id,
                                 Principal principal,
                                 @ModelAttribute("currentUser") User currentUser,
                                 RedirectAttributes redirectAttrs) {
        try {
            taxonomyService.deleteCategory(id,
                actorEmail(principal), actorName(principal), currentUser.getOrganizationId());
            redirectAttrs.addFlashAttribute("successMessage", "Category deleted.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting risk category", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error deleting category: " + e.getMessage());
        }
        return REDIRECT;
    }

    // -----------------------------------------------------------------------
    // Subcategories
    // -----------------------------------------------------------------------

    @PostMapping("/subcategories/create")
    public String createSubcategory(@RequestParam String name,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(required = false) Long categoryId,
                                    Principal principal,
                                    @ModelAttribute("currentUser") User currentUser,
                                    RedirectAttributes redirectAttrs) {
        try {
            var sub = taxonomyService.createSubcategory(name, description, categoryId,
                actorEmail(principal), actorName(principal), currentUser.getOrganizationId());
            redirectAttrs.addFlashAttribute("successMessage",
                "Subcategory \"" + sub.getName() + "\" created.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error creating risk subcategory", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error creating subcategory: " + e.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/subcategories/update")
    public String updateSubcategory(@RequestParam Long id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(required = false) Long categoryId,
                                    Principal principal,
                                    @ModelAttribute("currentUser") User currentUser,
                                    RedirectAttributes redirectAttrs) {
        try {
            var sub = taxonomyService.updateSubcategory(id, name, description, categoryId,
                actorEmail(principal), actorName(principal), currentUser.getOrganizationId());
            redirectAttrs.addFlashAttribute("successMessage",
                "Subcategory \"" + sub.getName() + "\" updated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error updating risk subcategory", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error updating subcategory: " + e.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/subcategories/delete")
    public String deleteSubcategory(@RequestParam Long id,
                                    Principal principal,
                                    @ModelAttribute("currentUser") User currentUser,
                                    RedirectAttributes redirectAttrs) {
        try {
            taxonomyService.deleteSubcategory(id,
                actorEmail(principal), actorName(principal), currentUser.getOrganizationId());
            redirectAttrs.addFlashAttribute("successMessage", "Subcategory deleted.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting risk subcategory", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error deleting subcategory: " + e.getMessage());
        }
        return REDIRECT;
    }

    // -----------------------------------------------------------------------
    // Actor resolution (same convention as RisksController)
    // -----------------------------------------------------------------------

    private String actorEmail(Principal principal) {
        return principal != null ? principal.getName() : "system";
    }

    private String actorName(Principal principal) {
        if (principal == null) return "System";
        User user = userRepository.findByEmail(principal.getName());
        return user != null ? user.getDisplayName() : principal.getName();
    }
}
