package com.riskregister.riskregisterapp.controllers;

import java.security.Principal;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.riskregister.riskregisterapp.entities.AuditTrail;
import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.entities.RiskDimension;
import com.riskregister.riskregisterapp.entities.RiskNote;
import com.riskregister.riskregisterapp.entities.Task;
import com.riskregister.riskregisterapp.entities.Asset;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.repositories.AuditTrailRepository;
import com.riskregister.riskregisterapp.enums.RiskReviewFrequency;
import com.riskregister.riskregisterapp.lookups.RiskTreatment;
import com.riskregister.riskregisterapp.entities.RiskStatus;
import com.riskregister.riskregisterapp.repositories.RiskDimensionRepository;
import com.riskregister.riskregisterapp.repositories.RiskStatusRepository;
import com.riskregister.riskregisterapp.repositories.UserRepository;
import com.riskregister.riskregisterapp.services.AuditTrailService;
import com.riskregister.riskregisterapp.services.IssueService;
import com.riskregister.riskregisterapp.services.RiskService;
import com.riskregister.riskregisterapp.services.RiskNoteService;
import com.riskregister.riskregisterapp.services.TaskService;
import com.riskregister.riskregisterapp.services.AssetService;

@Controller
public class RisksController {

    @Autowired
    private RiskService riskService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RiskDimensionRepository riskDimensionRepository;

    @Autowired
    private RiskStatusRepository riskStatusRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RiskNoteService riskNoteService;

    @Autowired
    private AuditTrailRepository auditTrailRepository;

    @Autowired
    private AssetService assetService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private com.riskregister.riskregisterapp.services.LookupService lookupService;

    @GetMapping("/risks")
    public String index(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        model.addAttribute("risks", riskService.findAll(orgId));
        model.addAttribute("categoryMap", categoryNameMap(orgId));
        model.addAttribute("dimensionMap", dimensionNameMap());
        model.addAttribute("statusMap", statusNameMap());
        return "risks/index";
    }

    @GetMapping("/risks/new")
    public String newForm(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Risk newRisk = new Risk();
        newRisk.setRiskTreatment(RiskTreatment.AWAITING_ASSESSMENT);
        // Suggest the next reference; the user can overwrite it with their own numbering
        newRisk.setRiskId(riskService.suggestNextRiskId(orgId));
        model.addAttribute("risk", newRisk);
        model.addAttribute("reviewFrequencies", RiskReviewFrequency.values());
        model.addAttribute("treatments", RiskTreatment.values());
        model.addAttribute("statuses", riskStatusRepository.findAll());
        model.addAttribute("users", userRepository.findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(orgId));
        model.addAttribute("riskCategories",
            lookupService.findActive(com.riskregister.riskregisterapp.enums.LookupType.RISK_CATEGORY, orgId));
        model.addAttribute("riskDimensions", riskDimensionRepository.findAll());
        return "risks/create";
    }

    @PostMapping("/risks")
    public String create(@ModelAttribute Risk risk, @ModelAttribute("currentUser") User currentUser, RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        risk.setOrganizationId(orgId);
        if (principal != null) {
            risk.setCreatedByEmail(principal.getName());
        }

        try {
            risk.setRiskId(riskService.validateRiskId(risk.getRiskId(), orgId, null));
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/risks/new";
        }

        // status is already set by form binding
        riskService.save(risk);

        // Log creation after save so risk.getId() is populated
        String actorEmail = principal != null ? principal.getName() : "system";
        String actorName = getActorName(actorEmail);
        auditTrailService.logRiskCreated(risk, actorEmail, actorName, orgId);

        redirectAttrs.addFlashAttribute("success", "Risk created successfully.");
        return "redirect:/risks";
    }

    @GetMapping("/risks/{id}")
    public String view(@PathVariable Long id, Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Risk risk = riskService.findById(orgId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));
        model.addAttribute("risk", risk);
        model.addAttribute("categoryMap", categoryNameMap(orgId));
        model.addAttribute("dimensionMap", dimensionNameMap());
        model.addAttribute("statusMap", statusNameMap());

        // Tasks for this risk
        List<Task> tasks = taskService.findAllByRisk(orgId, id);
        model.addAttribute("tasks", tasks);
        model.addAttribute("users", userRepository.findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(orgId));

        // Assets - linked and available
        List<Asset> allAssets = assetService.findAll(orgId);
        List<Asset> linkedAssets = new ArrayList<>();
        List<Long> linkedAssetIds = new ArrayList<>();

        if (risk.getLinkedAssetIds() != null && !risk.getLinkedAssetIds().isEmpty()) {
            String[] ids = risk.getLinkedAssetIds().split(",");
            for (String idStr : ids) {
                try {
                    Long assetId = Long.parseLong(idStr.trim());
                    linkedAssetIds.add(assetId);
                    allAssets.stream()
                        .filter(a -> a.getId().equals(assetId))
                        .findFirst()
                        .ifPresent(linkedAssets::add);
                } catch (NumberFormatException e) {
                    // Skip invalid IDs
                }
            }
        }

        // Asset types for the picker's filter, plus code→name for its JSON-rendered rows
        model.addAttribute("assetTypes", lookupService.findActive(com.riskregister.riskregisterapp.enums.LookupType.ASSET_TYPE, orgId));
        java.util.Map<String, String> assetTypeNames = new java.util.LinkedHashMap<>();
        for (var t : lookupService.findAll(com.riskregister.riskregisterapp.enums.LookupType.ASSET_TYPE, orgId)) {
            assetTypeNames.put(t.getCode(), t.getName());
        }
        model.addAttribute("assetTypeNames", assetTypeNames);

        model.addAttribute("linkedAssets", linkedAssets);
        model.addAttribute("allAssets", allAssets);
        model.addAttribute("linkedAssetIds", linkedAssetIds);

        // Findings raised against this risk's controls. Open severe ones mean the residual
        // score assumes controls that are demonstrably not working.
        // Notes thread, plus who is reading it so only their own notes offer Delete
        model.addAttribute("riskNotes", riskNoteService.findNotesByRisk(id));
        model.addAttribute("currentUserEmail", currentUser.getEmail());

        model.addAttribute("linkedIssues", issueService.findByRisk(orgId, id));
        model.addAttribute("openSevereIssues", issueService.findOpenSevereByRisk(orgId, id));
        model.addAttribute("sourceMap", lookupService.map(
            com.riskregister.riskregisterapp.enums.LookupType.ISSUE_SOURCE, orgId));

        // Audit entries for this risk (both Risk and Task entries, newest first)
        List<AuditTrail> allAuditEntries = new ArrayList<>();
        // Add Risk entries
        allAuditEntries.addAll(auditTrailService.findByRisk(orgId, id));
        // Add Task entries for tasks under this risk
        for (Task task : tasks) {
            allAuditEntries.addAll(auditTrailRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("Task", task.getId()));
        }
        // Sort by newest first
        allAuditEntries.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        model.addAttribute("auditEntries", allAuditEntries);

        return "risks/view";
    }

    @GetMapping("/risks/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Risk risk = riskService.findById(orgId, id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));
        model.addAttribute("risk", risk);
        model.addAttribute("reviewFrequencies", RiskReviewFrequency.values());
        model.addAttribute("treatments", RiskTreatment.values());
        model.addAttribute("statuses", riskStatusRepository.findAll());
        model.addAttribute("users", userRepository.findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(orgId));
        // Keep a deactivated category selectable while editing the risk that already uses it
        model.addAttribute("riskCategories", lookupService.findActiveIncluding(
            com.riskregister.riskregisterapp.enums.LookupType.RISK_CATEGORY, orgId, risk.getRiskCategory()));
        model.addAttribute("riskDimensions", riskDimensionRepository.findAll());
        return "risks/edit";
    }

    @PostMapping("/risks/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Risk form,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        Risk risk = riskService.findById(orgId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));

        // === SNAPSHOT old state before any mutations ===
        Risk oldSnapshot = snapshotRisk(risk);

        // Only vet the reference when it actually changes. Registers created before this check
        // may already hold duplicates, and blocking every edit to those risks would be a
        // regression — but a deliberate change to a taken reference is still refused.
        String submittedId = form.getRiskId() == null ? "" : form.getRiskId().trim();
        if (!submittedId.equalsIgnoreCase(risk.getRiskId() == null ? "" : risk.getRiskId().trim())) {
            try {
                submittedId = riskService.validateRiskId(submittedId, orgId, id);
            } catch (IllegalArgumentException e) {
                redirectAttrs.addFlashAttribute("error", e.getMessage());
                return "redirect:/risks/" + id + "/edit";
            }
        }

        // Apply all form fields
        risk.setRiskId(submittedId);
        risk.setTitle(form.getTitle());
        risk.setDescription(form.getDescription());
        risk.setRiskOwnerName(form.getRiskOwnerName());
        risk.setRiskCategory(form.getRiskCategory());
        risk.setRiskDimensionId(form.getRiskDimensionId());
        risk.setCategories(form.getCategories());
        risk.setReviewFrequency(form.getReviewFrequency());
        risk.setInherentLikelihood(form.getInherentLikelihood());
        risk.setInherentImpact(form.getInherentImpact());
        risk.setInherentRationale(form.getInherentRationale());
        risk.setResidualLikelihood(form.getResidualLikelihood());
        risk.setResidualImpact(form.getResidualImpact());
        risk.setResidualRationale(form.getResidualRationale());
        risk.setRiskTreatment(form.getRiskTreatment());
        risk.setStatusId(form.getStatusId());
        if (principal != null) {
            risk.setUpdatedByEmail(principal.getName());
        }
        riskService.save(risk);

        // === Log changes using pre-built maps ===
        String actorEmail = principal != null ? principal.getName() : "system";
        String actorName = getActorName(actorEmail);
        auditTrailService.logRiskUpdated(
            oldSnapshot, risk,
            categoryNameMap(orgId), dimensionNameMap(),
            statusNameMap(),
            actorEmail, actorName, orgId
        );

        redirectAttrs.addFlashAttribute("success", "Risk updated successfully.");
        return "redirect:/risks/" + id;
    }

    @PostMapping("/risks/{id}/delete")
    public String delete(@PathVariable Long id, @ModelAttribute("currentUser") User currentUser, RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        // Load before deleting so we have the title/riskId for the log entry
        Risk risk = riskService.findById(orgId, id).orElse(null);
        riskService.softDelete(orgId, id);

        if (risk != null) {
            String actorEmail = principal != null ? principal.getName() : "system";
            String actorName = getActorName(actorEmail);
            auditTrailService.logRiskDeleted(risk, actorEmail, actorName, orgId);
        }

        redirectAttrs.addFlashAttribute("success", "Risk has been removed.");
        return "redirect:/risks";
    }

    /**
     * Record that a periodic review happened, resetting the risk's review clock.
     * Kept separate from a normal edit so "we looked at this and it still stands"
     * is captured even when nothing about the risk changed.
     */
    @PostMapping("/risks/{id}/mark-reviewed")
    public String markReviewed(@PathVariable Long id,
                               @RequestParam(required = false) String returnTo,
                               @ModelAttribute("currentUser") User currentUser,
                               RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        String actorEmail = principal != null ? principal.getName() : "system";
        String actorName = getActorName(actorEmail);

        Risk risk = riskService.markReviewed(orgId, id, actorName).orElse(null);
        if (risk == null) {
            redirectAttrs.addFlashAttribute("error", "Risk not found.");
            return "redirect:/risks";
        }

        auditTrailService.logRiskReviewed(risk, actorEmail, actorName, orgId);

        redirectAttrs.addFlashAttribute("success",
            "Review recorded for " + risk.getRiskId() + ". Next review due " + risk.getNextReviewDateFormatted() + ".");
        return "redirect:" + (returnTo != null && returnTo.startsWith("/") ? returnTo : "/risks/" + id);
    }

    @PostMapping("/risks/{riskId}/notes")
    public String addNote(@PathVariable Long riskId,
                          @RequestParam String content,
                          @ModelAttribute("currentUser") User currentUser,
                          RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        // Confirm the risk is ours before hanging a note off it
        riskService.findById(orgId, riskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));

        String authorId = principal != null ? principal.getName() : "system";
        try {
            riskNoteService.addNote(riskId, content, authorId, getActorName(authorId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/risks/" + riskId + "?tab=notes";
    }

    @PostMapping("/risks/{riskId}/notes/{noteId}/delete")
    public String deleteNote(@PathVariable Long riskId,
                             @PathVariable Long noteId,
                             @ModelAttribute("currentUser") User currentUser,
                             RedirectAttributes redirectAttrs, Principal principal) {
        Long orgId = currentUser.getOrganizationId();
        // Resolving the risk in this organisation is what scopes the note deletion
        riskService.findById(orgId, riskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));

        String requesterId = principal != null ? principal.getName() : "system";
        try {
            riskNoteService.deleteNote(noteId, riskId, requesterId);
            redirectAttrs.addFlashAttribute("success", "Note removed.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/risks/" + riskId + "?tab=notes";
    }

    private Map<String, String> categoryNameMap(Long orgId) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (var v : lookupService.findAll(com.riskregister.riskregisterapp.enums.LookupType.RISK_CATEGORY, orgId)) {
            map.put(v.getCode(), v.getName());
        }
        return map;
    }


    private Map<Long, String> dimensionNameMap() {
        return riskDimensionRepository.findAll().stream()
                .collect(Collectors.toMap(RiskDimension::getId, RiskDimension::getName));
    }

    private Map<Long, String> statusNameMap() {
        return riskStatusRepository.findAll().stream()
                .collect(Collectors.toMap(RiskStatus::getId, RiskStatus::getName));
    }

    @PostMapping("/api/risks/{id}/assets")
    public ResponseEntity<?> updateLinkedAssets(@PathVariable Long id, @RequestBody Map<String, List<Long>> payload, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Risk risk = riskService.findById(orgId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));

        List<Long> assetIds = payload.getOrDefault("assetIds", new ArrayList<>());
        String linkedAssetIds = assetIds.isEmpty() ? null : assetIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        risk.setLinkedAssetIds(linkedAssetIds);
        riskService.save(risk);

        return ResponseEntity.ok(Map.of("success", true, "message", "Assets linked successfully"));
    }

    private String getActorName(String email) {
        if (email == null || email.equals("system")) {
            return "System";
        }
        var user = userRepository.findByEmail(email);
        return user != null ? user.getDisplayName() : email;
    }

    private Risk snapshotRisk(Risk r) {
        Risk snap = new Risk();
        snap.setId(r.getId());
        snap.setRiskId(r.getRiskId());
        snap.setTitle(r.getTitle());
        snap.setDescription(r.getDescription());
        snap.setRiskOwnerName(r.getRiskOwnerName());
        snap.setRiskCategory(r.getRiskCategory());
        snap.setRiskDimensionId(r.getRiskDimensionId());
        snap.setCategories(r.getCategories());
        snap.setReviewFrequency(r.getReviewFrequency());
        snap.setInherentLikelihood(r.getInherentLikelihood());
        snap.setInherentImpact(r.getInherentImpact());
        snap.setInherentRationale(r.getInherentRationale());
        snap.setResidualLikelihood(r.getResidualLikelihood());
        snap.setResidualImpact(r.getResidualImpact());
        snap.setResidualRationale(r.getResidualRationale());
        snap.setRiskTreatment(r.getRiskTreatment());
        snap.setStatusId(r.getStatusId());
        return snap;
    }
}
