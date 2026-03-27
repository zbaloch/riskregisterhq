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
import com.riskregister.riskregisterapp.entities.RiskCategory;
import com.riskregister.riskregisterapp.entities.RiskDimension;
import com.riskregister.riskregisterapp.entities.RiskSubcategory;
import com.riskregister.riskregisterapp.entities.RiskNote;
import com.riskregister.riskregisterapp.entities.Task;
import com.riskregister.riskregisterapp.entities.Asset;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.repositories.AuditTrailRepository;
import com.riskregister.riskregisterapp.enums.RiskReviewFrequency;
import com.riskregister.riskregisterapp.lookups.RiskTreatment;
import com.riskregister.riskregisterapp.entities.RiskStatus;
import com.riskregister.riskregisterapp.repositories.RiskCategoryRepository;
import com.riskregister.riskregisterapp.repositories.RiskDimensionRepository;
import com.riskregister.riskregisterapp.repositories.RiskStatusRepository;
import com.riskregister.riskregisterapp.repositories.RiskSubcategoryRepository;
import com.riskregister.riskregisterapp.repositories.UserRepository;
import com.riskregister.riskregisterapp.services.AuditTrailService;
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
    private RiskCategoryRepository riskCategoryRepository;

    @Autowired
    private RiskSubcategoryRepository riskSubcategoryRepository;

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

    @GetMapping("/risks")
    public String index(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        model.addAttribute("risks", riskService.findAll(orgId));
        model.addAttribute("categoryMap", categoryNameMap());
        model.addAttribute("subcategoryMap", subcategoryNameMap());
        model.addAttribute("dimensionMap", dimensionNameMap());
        model.addAttribute("statusMap", statusNameMap());
        return "risks/index";
    }

    @GetMapping("/risks/new")
    public String newForm(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Risk newRisk = new Risk();
        newRisk.setRiskTreatment(RiskTreatment.AWAITING_ASSESSMENT);
        model.addAttribute("risk", newRisk);
        model.addAttribute("reviewFrequencies", RiskReviewFrequency.values());
        model.addAttribute("treatments", RiskTreatment.values());
        model.addAttribute("statuses", riskStatusRepository.findAll());
        model.addAttribute("users", userRepository.findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(orgId));
        model.addAttribute("riskCategories", riskCategoryRepository.findAll());
        model.addAttribute("riskSubcategories", riskSubcategoryRepository.findAll());
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
        model.addAttribute("categoryMap", categoryNameMap());
        model.addAttribute("subcategoryMap", subcategoryNameMap());
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

        model.addAttribute("linkedAssets", linkedAssets);
        model.addAttribute("allAssets", allAssets);
        model.addAttribute("linkedAssetIds", linkedAssetIds);

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
        model.addAttribute("riskCategories", riskCategoryRepository.findAll());
        model.addAttribute("riskSubcategories", riskSubcategoryRepository.findAll());
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

        // Apply all form fields
        risk.setRiskId(form.getRiskId());
        risk.setTitle(form.getTitle());
        risk.setDescription(form.getDescription());
        risk.setRiskOwnerName(form.getRiskOwnerName());
        risk.setRiskCategoryId(form.getRiskCategoryId());
        risk.setRiskSubcategoryId(form.getRiskSubcategoryId());
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
            categoryNameMap(), subcategoryNameMap(), dimensionNameMap(),
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

    @PostMapping("/risks/{riskId}/notes")
    public ResponseEntity<?> addNote(@PathVariable Long riskId,
                                     @RequestParam(value = "content", required = true) String content,
                                     Principal principal) {
        String authorId = principal != null ? principal.getName() : "system";
        String authorName = getActorName(authorId);
        RiskNote newNote = riskNoteService.addNote(riskId, content, authorId, authorName);
        return ResponseEntity.ok(newNote);
    }

    @GetMapping("/api/risks/{riskId}/notes")
    public ResponseEntity<List<RiskNote>> getNotes(@PathVariable Long riskId) {
        List<RiskNote> notes = riskNoteService.findNotesByRisk(riskId);
        return ResponseEntity.ok(notes);
    }

    @DeleteMapping("/risks/{riskId}/notes/{noteId}")
    public ResponseEntity<Map<String, String>> deleteNote(
            @PathVariable Long riskId,
            @PathVariable Long noteId) {
        riskNoteService.deleteNote(noteId);
        return ResponseEntity.ok(Map.of("success", "true"));
    }

    private Map<Long, String> categoryNameMap() {
        return riskCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(RiskCategory::getId, RiskCategory::getName));
    }

    private Map<Long, String> subcategoryNameMap() {
        return riskSubcategoryRepository.findAll().stream()
                .collect(Collectors.toMap(RiskSubcategory::getId, RiskSubcategory::getName));
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
        snap.setRiskCategoryId(r.getRiskCategoryId());
        snap.setRiskSubcategoryId(r.getRiskSubcategoryId());
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
