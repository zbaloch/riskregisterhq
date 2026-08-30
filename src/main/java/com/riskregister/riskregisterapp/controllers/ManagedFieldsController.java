package com.riskregister.riskregisterapp.controllers;

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

import com.riskregister.riskregisterapp.entities.LookupValue;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.enums.LookupType;
import com.riskregister.riskregisterapp.services.LookupService;

/**
 * Administration of the options behind managed dropdown fields
 * (Settings → Managed Fields). Every endpoint is field-type agnostic: the type travels in
 * the request, so adding a new managed field needs no new endpoint.
 */
@Controller
@RequestMapping("/settings/fields")
@PreAuthorize("hasRole('ADMIN')")
public class ManagedFieldsController {

    private static final Logger log = LoggerFactory.getLogger(ManagedFieldsController.class);

    @Autowired
    private LookupService lookupService;

    @PostMapping("/create")
    public String create(@RequestParam String fieldType,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "false") boolean flagValue,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs) {
        LookupType type = LookupType.fromCode(fieldType);
        if (type == null) return redirect(fieldType);

        try {
            LookupValue created = lookupService.create(type, currentUser.getOrganizationId(),
                name, description, flagValue);
            redirectAttrs.addFlashAttribute("successMessage",
                "\"" + created.getName() + "\" added to " + type.getSingularName() + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error creating lookup value", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error adding option: " + e.getMessage());
        }
        return redirect(fieldType);
    }

    @PostMapping("/update")
    public String update(@RequestParam String fieldType,
                         @RequestParam Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "false") boolean flagValue,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs) {
        try {
            LookupValue updated = lookupService.update(currentUser.getOrganizationId(), id,
                name, description, flagValue);
            redirectAttrs.addFlashAttribute("successMessage", "\"" + updated.getName() + "\" updated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error updating lookup value", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error updating option: " + e.getMessage());
        }
        return redirect(fieldType);
    }

    /** Deactivating hides an option from pickers without touching records that already use it. */
    @PostMapping("/toggle")
    public String toggle(@RequestParam String fieldType,
                         @RequestParam Long id,
                         @RequestParam boolean active,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs) {
        try {
            LookupValue value = lookupService.setActive(currentUser.getOrganizationId(), id, active);
            redirectAttrs.addFlashAttribute("successMessage",
                "\"" + value.getName() + "\" " + (active ? "activated." : "deactivated — it stays on existing records but is no longer offered."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error toggling lookup value", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error updating option: " + e.getMessage());
        }
        return redirect(fieldType);
    }

    @PostMapping("/delete")
    public String delete(@RequestParam String fieldType,
                         @RequestParam Long id,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs) {
        try {
            LookupValue removed = lookupService.delete(currentUser.getOrganizationId(), id);
            redirectAttrs.addFlashAttribute("successMessage", "\"" + removed.getName() + "\" deleted.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting lookup value", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error deleting option: " + e.getMessage());
        }
        return redirect(fieldType);
    }

    @PostMapping("/move")
    public String move(@RequestParam String fieldType,
                       @RequestParam Long id,
                       @RequestParam int direction,
                       @ModelAttribute("currentUser") User currentUser,
                       RedirectAttributes redirectAttrs) {
        try {
            lookupService.move(currentUser.getOrganizationId(), id, direction);
        } catch (Exception e) {
            log.error("Error reordering lookup value", e);
            redirectAttrs.addFlashAttribute("errorMessage", "Error reordering options: " + e.getMessage());
        }
        return redirect(fieldType);
    }

    private String redirect(String fieldType) {
        return "redirect:/settings?tab=fields&fieldType=" + fieldType;
    }
}
