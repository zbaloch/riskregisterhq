package com.riskregister.riskregisterapp.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.riskregister.riskregisterapp.dto.FieldChange;
import com.riskregister.riskregisterapp.entities.AuditTrail;
import com.riskregister.riskregisterapp.entities.Issue;
import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.enums.IssueStatus;
import com.riskregister.riskregisterapp.enums.LookupType;
import com.riskregister.riskregisterapp.repositories.UserRepository;
import com.riskregister.riskregisterapp.services.AssetService;
import com.riskregister.riskregisterapp.services.AuditTrailService;
import com.riskregister.riskregisterapp.services.IssueService;
import com.riskregister.riskregisterapp.services.RiskService;
import com.riskregister.riskregisterapp.services.TaskService;

/**
 * The issue register: control deficiencies and audit findings, tracked to validated closure.
 * Kept separate from risks because an issue is certain and terminal, whereas a risk is
 * uncertain and perpetual — they carry different fields and answer to different forums.
 */
@Controller
public class IssuesController {

    @Autowired
    private IssueService issueService;

    @Autowired
    private RiskService riskService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.riskregister.riskregisterapp.services.LookupService lookupService;

    @Autowired
    private com.riskregister.riskregisterapp.services.IssueNoteService issueNoteService;

    // -----------------------------------------------------------------------
    // List
    // -----------------------------------------------------------------------

    @GetMapping("/issues")
    public String index(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        List<Issue> issues = issueService.findAll(orgId);

        model.addAttribute("issues", issues);
        model.addAttribute("sourceMap", lookupService.map(LookupType.ISSUE_SOURCE, orgId));
        model.addAttribute("categoryMap", lookupService.map(LookupType.ISSUE_CATEGORY, orgId));
        model.addAttribute("dimensionMap", lookupService.map(LookupType.ISSUE_DIMENSION, orgId));
        model.addAttribute("openCount", issues.stream().filter(i -> !i.isClosed()).count());
        model.addAttribute("overdueCount", issues.stream().filter(Issue::isOverdue).count());
        model.addAttribute("awaitingValidationCount", issues.stream().filter(Issue::isAwaitingValidation).count());
        model.addAttribute("extendedCount", issues.stream().filter(i -> !i.isClosed() && i.isExtended()).count());

        // Where open findings concentrate. Ordered by the admin's own ordering, empties omitted.
        Map<String, Long> openByDimension = new java.util.LinkedHashMap<>();
        for (com.riskregister.riskregisterapp.entities.LookupValue d
                : lookupService.findAll(LookupType.ISSUE_DIMENSION, orgId)) {
            long n = issues.stream()
                .filter(i -> !i.isClosed())
                .filter(i -> d.getCode().equals(i.getDimension()))
                .count();
            if (n > 0) openByDimension.put(d.getName(), n);
        }
        model.addAttribute("openByDimension", openByDimension);
        return "issues/index";
    }

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    @GetMapping("/issues/new")
    public String newForm(Model model, @ModelAttribute("currentUser") User currentUser) {
        Issue issue = new Issue();
        // Offer the next reference in sequence; the user can overwrite it
        issue.setIssueRef(issueService.suggestNextRef(currentUser.getOrganizationId()));
        issue.setStatus(IssueStatus.OPEN);
        issue.setDateRaised(java.time.LocalDate.now());
        issue.setImpact(3);
        issue.setPervasiveness(3);
        model.addAttribute("issue", issue);
        addFormOptions(model, currentUser.getOrganizationId());
        return "issues/create";
    }

    @PostMapping("/issues")
    public String create(@ModelAttribute Issue issue,
                         @RequestParam(required = false) List<Long> linkedRiskIds,
                         @RequestParam(required = false) List<Long> linkedAssetIds,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        String actorEmail = actorEmail(principal);

        issue.setLinkedRiskIds(joinIds(linkedRiskIds));
        issue.setLinkedAssetIds(joinIds(linkedAssetIds));

        Issue saved;
        try {
            saved = issueService.create(issue, orgId, actorEmail);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Keep the user's typing rather than bouncing them back to an empty form
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/issues/new";
        }

        auditTrailService.logIssueCreated(saved, actorEmail, actorName(actorEmail), orgId);

        redirectAttrs.addFlashAttribute("success", "Issue " + saved.getIssueRef() + " raised.");
        return "redirect:/issues/" + saved.getId();
    }

    // -----------------------------------------------------------------------
    // View
    // -----------------------------------------------------------------------

    @GetMapping("/issues/{id}")
    public String view(@PathVariable Long id, Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Issue issue = issueService.findById(orgId, id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        model.addAttribute("issue", issue);
        model.addAttribute("statuses", IssueStatus.values());
        model.addAttribute("sourceMap", lookupService.map(LookupType.ISSUE_SOURCE, orgId));
        model.addAttribute("categoryMap", lookupService.map(LookupType.ISSUE_CATEGORY, orgId));
        model.addAttribute("dimensionMap", lookupService.map(LookupType.ISSUE_DIMENSION, orgId));
        // Needed by the validation modal's validator picker
        model.addAttribute("users", userRepository.findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(orgId));

        // Risks this finding bears on
        List<Long> riskIds = issue.getLinkedRiskIdList();
        model.addAttribute("linkedRisks", riskService.findAll(orgId).stream()
            .filter(r -> riskIds.contains(r.getId()))
            .toList());

        // Remediation actions
        model.addAttribute("tasks", taskService.findAllByIssue(orgId, id));

        // Notes thread, plus who is reading it so only their own comments offer Delete
        model.addAttribute("notes", issueNoteService.findByIssue(orgId, id));
        model.addAttribute("currentUserEmail", currentUser.getEmail());

        // Audit trail, with field changes already parsed for the timeline
        List<AuditTrail> entries = auditTrailService.findByIssue(orgId, id);
        Map<Long, List<FieldChange>> changes = entries.stream()
            .collect(Collectors.toMap(AuditTrail::getId, auditTrailService::parseChanges, (a, b) -> a));
        model.addAttribute("auditEntries", entries);
        model.addAttribute("auditChanges", changes);

        return "issues/view";
    }

    // -----------------------------------------------------------------------
    // Notes
    // -----------------------------------------------------------------------

    @PostMapping("/issues/{id}/notes")
    public String addNote(@PathVariable Long id,
                          @RequestParam String content,
                          @ModelAttribute("currentUser") User currentUser,
                          RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        // Confirm the issue is ours before hanging a comment off it
        issueService.findById(orgId, id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        String actorEmail = actorEmail(principal);
        try {
            issueNoteService.add(orgId, id, content, actorEmail, actorName(actorEmail));
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/issues/" + id + "?tab=notes";
    }

    @PostMapping("/issues/{id}/notes/{noteId}/delete")
    public String deleteNote(@PathVariable Long id,
                             @PathVariable Long noteId,
                             @ModelAttribute("currentUser") User currentUser,
                             RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        try {
            issueNoteService.delete(orgId, noteId, actorEmail(principal));
            redirectAttrs.addFlashAttribute("success", "Comment removed.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/issues/" + id + "?tab=notes";
    }

    // -----------------------------------------------------------------------
    // Edit
    // -----------------------------------------------------------------------

    @GetMapping("/issues/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Issue issue = issueService.findById(orgId, id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
        model.addAttribute("issue", issue);
        addFormOptions(model, orgId, issue.getSource(), issue.getCategory(), issue.getDimension());
        return "issues/edit";
    }

    @PostMapping("/issues/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute Issue form,
                         @RequestParam(required = false) List<Long> linkedRiskIds,
                         @RequestParam(required = false) List<Long> linkedAssetIds,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        Issue existing = issueService.findById(orgId, id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        Issue before = snapshot(existing);
        form.setLinkedRiskIds(joinIds(linkedRiskIds));
        form.setLinkedAssetIds(joinIds(linkedAssetIds));

        String actorEmail = actorEmail(principal);
        Issue saved;
        try {
            saved = issueService.update(existing, form, actorEmail);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/issues/" + id + "/edit";
        }

        auditTrailService.logIssueUpdated(before, saved, actorEmail, actorName(actorEmail), orgId);

        String note = saved.isExtended() && before.getTargetDate() != null
                      && saved.getTargetDate() != null && saved.getTargetDate().isAfter(before.getTargetDate())
            ? " Target date extended — now " + saved.getExtensionCount() + " extension(s) on record."
            : "";
        redirectAttrs.addFlashAttribute("success", "Issue " + saved.getIssueRef() + " updated." + note);
        return "redirect:/issues/" + id;
    }

    // -----------------------------------------------------------------------
    // Status transitions and validation
    // -----------------------------------------------------------------------

    @PostMapping("/issues/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam IssueStatus status,
                               @ModelAttribute("currentUser") User currentUser,
                               RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        Issue issue = issueService.findById(orgId, id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        String oldLabel = issue.getStatus() != null ? issue.getStatus().getDisplayName() : "—";
        String actorEmail = actorEmail(principal);
        Issue saved = issueService.changeStatus(issue, status, actorEmail);

        auditTrailService.logIssueStatusChanged(saved, oldLabel, status.getDisplayName(),
            actorEmail, actorName(actorEmail), orgId);

        redirectAttrs.addFlashAttribute("success",
            "Issue " + saved.getIssueRef() + " moved to " + status.getDisplayName() + ".");
        return "redirect:/issues/" + id;
    }

    /**
     * Record independent confirmation the fix worked. Blocked when the validator is the
     * remediation owner — self-validation is exactly what this control exists to prevent.
     */
    @PostMapping("/issues/{id}/validate")
    public String validate(@PathVariable Long id,
                           @RequestParam String validatedByName,
                           @ModelAttribute("currentUser") User currentUser,
                           RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        Issue issue = issueService.findById(orgId, id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (validatedByName == null || validatedByName.isBlank()) {
            redirectAttrs.addFlashAttribute("error", "A validator name is required to close the issue.");
            return "redirect:/issues/" + id;
        }
        if (issue.getOwnerName() != null && validatedByName.trim().equalsIgnoreCase(issue.getOwnerName().trim())) {
            redirectAttrs.addFlashAttribute("error",
                "The remediation owner cannot validate their own fix. Validation must be independent.");
            return "redirect:/issues/" + id;
        }

        String actorEmail = actorEmail(principal);
        Issue saved = issueService.validate(issue, validatedByName.trim(), actorEmail);

        auditTrailService.logIssueValidated(saved, actorEmail, actorName(actorEmail), orgId);

        redirectAttrs.addFlashAttribute("success",
            "Issue " + saved.getIssueRef() + " validated by " + saved.getValidatedByName() + " and closed.");
        return "redirect:/issues/" + id;
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------

    @PostMapping("/issues/{id}/delete")
    public String delete(@PathVariable Long id,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        Issue issue = issueService.findById(orgId, id).orElse(null);
        issueService.softDelete(orgId, id);

        if (issue != null) {
            String actorEmail = actorEmail(principal);
            auditTrailService.logIssueDeleted(issue, actorEmail, actorName(actorEmail), orgId);
        }

        redirectAttrs.addFlashAttribute("success", "Issue has been removed.");
        return "redirect:/issues";
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void addFormOptions(Model model, Long orgId) {
        addFormOptions(model, orgId, null);
    }

    /**
     * @param currentSource keeps a deactivated option selectable while editing the record that
     *                      already uses it, so an unrelated edit cannot blank the field
     */
    private void addFormOptions(Model model, Long orgId, String currentSource) {
        addFormOptions(model, orgId, currentSource, null);
    }

    private void addFormOptions(Model model, Long orgId, String currentSource, String currentCategory) {
        addFormOptions(model, orgId, currentSource, currentCategory, null);
    }

    private void addFormOptions(Model model, Long orgId,
                                String currentSource, String currentCategory, String currentDimension) {
        model.addAttribute("sources", lookupService.findActiveIncluding(LookupType.ISSUE_SOURCE, orgId, currentSource));
        model.addAttribute("categories", lookupService.findActiveIncluding(LookupType.ISSUE_CATEGORY, orgId, currentCategory));
        model.addAttribute("dimensions", lookupService.findActiveIncluding(LookupType.ISSUE_DIMENSION, orgId, currentDimension));
        model.addAttribute("statuses", IssueStatus.values());
        model.addAttribute("users", userRepository.findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(orgId));
        model.addAttribute("risks", riskService.findAll(orgId));
        model.addAttribute("assets", assetService.findAll(orgId));
    }

    private static String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String actorEmail(Principal principal) {
        return principal != null ? principal.getName() : "system";
    }

    private String actorName(String email) {
        if (email == null || email.equals("system")) return "System";
        User user = userRepository.findByEmail(email);
        return user != null ? user.getDisplayName() : email;
    }

    /** Pre-edit copy so the audit diff has something to compare against. */
    private static Issue snapshot(Issue i) {
        Issue s = new Issue();
        s.setId(i.getId());
        s.setIssueRef(i.getIssueRef());
        s.setTitle(i.getTitle());
        s.setDescription(i.getDescription());
        s.setSource(i.getSource());
        s.setExternalReference(i.getExternalReference());
        s.setImpact(i.getImpact());
        s.setPervasiveness(i.getPervasiveness());
        s.setRootCause(i.getRootCause());
        s.setRemediationPlan(i.getRemediationPlan());
        s.setOwnerName(i.getOwnerName());
        s.setStatus(i.getStatus());
        s.setDateRaised(i.getDateRaised());
        s.setTargetDate(i.getTargetDate());
        s.setOriginalTargetDate(i.getOriginalTargetDate());
        s.setExtensionCount(i.getExtensionCount());
        s.setLinkedRiskIds(i.getLinkedRiskIds());
        s.setLinkedAssetIds(i.getLinkedAssetIds());
        return s;
    }
}
