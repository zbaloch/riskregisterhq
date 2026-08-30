package com.riskregister.riskregisterapp.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.riskregister.riskregisterapp.entities.Asset;
import com.riskregister.riskregisterapp.entities.AssetNote;
import com.riskregister.riskregisterapp.entities.AuditTrail;
import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.repositories.AssetNoteRepository;
import com.riskregister.riskregisterapp.repositories.RiskStatusRepository;
import com.riskregister.riskregisterapp.repositories.UserRepository;
import com.riskregister.riskregisterapp.services.AssetService;
import com.riskregister.riskregisterapp.services.AuditTrailService;
import com.riskregister.riskregisterapp.services.RiskService;

@Controller
@RequestMapping("/assets")
@PreAuthorize("isAuthenticated()")
public class AssetsController {

    @Autowired
    private com.riskregister.riskregisterapp.services.LookupService lookupService;

    private static final Logger log = LoggerFactory.getLogger(AssetsController.class);

    @Autowired
    private AssetService assetService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RiskService riskService;

    @Autowired
    private RiskStatusRepository riskStatusRepository;

    @Autowired
    private AssetNoteRepository assetNoteRepository;

    // -----------------------------------------------------------------------
    // Index
    // -----------------------------------------------------------------------

    @GetMapping
    public String index(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        List<Asset> assets = assetService.findAll(orgId);
        Map<String, String> userMap = userRepository.findByOrganizationId(orgId).stream()
            .collect(Collectors.toMap(u -> u.getEmail() != null ? u.getEmail() : "", u -> u.getDisplayName()));

        model.addAttribute("assets", assets);
        model.addAttribute("typeMap", lookupService.map(com.riskregister.riskregisterapp.enums.LookupType.ASSET_TYPE, orgId));
        model.addAttribute("userMap", userMap);
        return "assets/index";
    }

    // -----------------------------------------------------------------------
    // Create (GET)
    // -----------------------------------------------------------------------

    @GetMapping("/new")
    public String newAsset(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Asset asset = new Asset();
        asset.setConfidentiality(3);
        asset.setIntegrity(3);
        asset.setAvailability(3);
        asset.setStatus("Active");

        model.addAttribute("asset", asset);
        model.addAttribute("assetTypes", lookupService.findActiveIncluding(com.riskregister.riskregisterapp.enums.LookupType.ASSET_TYPE, orgId, asset.getType()));
        model.addAttribute("users", userRepository.findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(orgId));
        return "assets/create";
    }

    // -----------------------------------------------------------------------
    // Create (POST)
    // -----------------------------------------------------------------------

    @PostMapping
    public String create(@ModelAttribute Asset asset, @ModelAttribute("currentUser") User currentUser, RedirectAttributes redirectAttrs, Principal principal) {
        try {
            Long orgId = currentUser.getOrganizationId();
            asset.setOrganizationId(orgId);
            if (principal != null) {
                asset.setCreatedByEmail(principal.getName());
            }
            Asset saved = assetService.save(asset);

            String actorEmail = principal != null ? principal.getName() : "system";
            String actorName = getActorName(actorEmail);
            auditTrailService.logAssetCreated(saved, actorEmail, actorName, orgId);

            redirectAttrs.addFlashAttribute("success", "Asset created successfully.");
            return "redirect:/assets/" + saved.getId();
        } catch (Exception e) {
            log.error("Error creating asset", e);
            redirectAttrs.addFlashAttribute("error", "Error creating asset: " + e.getMessage());
            return "redirect:/assets/new";
        }
    }

    // -----------------------------------------------------------------------
    // View
    // -----------------------------------------------------------------------

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Asset asset = assetService.findById(orgId, id)
            .orElseThrow(() -> new RuntimeException("Asset not found"));

        List<AuditTrail> auditEntries = auditTrailService.findByAsset(orgId, id);
        List<AssetNote> notes = assetNoteRepository.findByAssetIdOrderByCreatedAtDesc(id);

        // Fetch linked risks
        List<Risk> linkedRisks = riskService.findAll(orgId).stream()
            .filter(risk -> risk.getLinkedAssetIds() != null
                        && risk.getLinkedAssetIds().contains(String.valueOf(id)))
            .collect(Collectors.toList());

        // Build maps for display
        Map<Long, String> statusMap = riskStatusRepository.findAll().stream()
            .collect(Collectors.toMap(rs -> rs.getId(), rs -> rs.getName()));

        model.addAttribute("asset", asset);
        model.addAttribute("typeMap", lookupService.map(com.riskregister.riskregisterapp.enums.LookupType.ASSET_TYPE, orgId));
        model.addAttribute("auditEntries", auditEntries);
        model.addAttribute("notes", notes);
        model.addAttribute("users", userRepository.findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(orgId));
        model.addAttribute("linkedRisks", linkedRisks);
        model.addAttribute("statusMap", statusMap);

        return "assets/view";
    }

    // -----------------------------------------------------------------------
    // Add Note
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/notes")
    public String addNote(@PathVariable Long id, @RequestParam String content,
                         RedirectAttributes redirectAttrs, Principal principal, @ModelAttribute("currentUser") User currentUser) {
        try {
            Long orgId = currentUser.getOrganizationId();
            Asset asset = assetService.findById(orgId, id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

            String authorEmail = principal != null ? principal.getName() : "system";
            AssetNote note = new AssetNote();
            note.setAsset(asset);
            note.setContent(content);
            note.setAuthorEmail(authorEmail);
            note.setAuthorName(getActorName(authorEmail));
            note.setCreatedAt(java.time.Instant.now());

            assetNoteRepository.save(note);
            redirectAttrs.addFlashAttribute("success", "Note added successfully.");
        } catch (Exception e) {
            log.error("Error adding note", e);
            redirectAttrs.addFlashAttribute("error", "Error adding note: " + e.getMessage());
        }

        return "redirect:/assets/" + id + "?tab=notes";
    }

    // -----------------------------------------------------------------------
    // Edit (GET)
    // -----------------------------------------------------------------------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        Asset asset = assetService.findById(orgId, id)
            .orElseThrow(() -> new RuntimeException("Asset not found"));

        model.addAttribute("asset", asset);
        model.addAttribute("assetTypes", lookupService.findActiveIncluding(com.riskregister.riskregisterapp.enums.LookupType.ASSET_TYPE, orgId, asset.getType()));
        model.addAttribute("users", userRepository.findByOrganizationIdAndApprovedTrueOrderByFirstNameAscLastNameAsc(orgId));
        return "assets/edit";
    }

    // -----------------------------------------------------------------------
    // Update (POST)
    // -----------------------------------------------------------------------

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute Asset asset,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs,
                         Principal principal) {
        try {
            Long orgId = currentUser.getOrganizationId();
            Asset existing = assetService.findById(orgId, id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

            // Snapshot old asset for audit diff
            Asset oldSnapshot = snapshotAsset(existing);

            // Update fields
            existing.setName(asset.getName());
            existing.setDescription(asset.getDescription());
            existing.setType(asset.getType());
            existing.setStatus(asset.getStatus());
            existing.setLocation(asset.getLocation());
            existing.setNotes(asset.getNotes());
            existing.setOwnerEmail(asset.getOwnerEmail());
            existing.setOwnerName(asset.getOwnerName());
            existing.setConfidentiality(asset.getConfidentiality());
            existing.setIntegrity(asset.getIntegrity());
            existing.setAvailability(asset.getAvailability());

            if (principal != null) {
                existing.setUpdatedByEmail(principal.getName());
            }

            Asset saved = assetService.save(existing);

            String actorEmail = principal != null ? principal.getName() : "system";
            String actorName = getActorName(actorEmail);
            auditTrailService.logAssetUpdated(oldSnapshot, saved, actorEmail, actorName, orgId);

            redirectAttrs.addFlashAttribute("success", "Asset updated successfully.");
            return "redirect:/assets/" + id;
        } catch (Exception e) {
            log.error("Error updating asset", e);
            redirectAttrs.addFlashAttribute("error", "Error updating asset: " + e.getMessage());
            return "redirect:/assets/" + id + "/edit";
        }
    }

    // -----------------------------------------------------------------------
    // Delete (POST)
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @ModelAttribute("currentUser") User currentUser,
                         RedirectAttributes redirectAttrs,
                         Principal principal) {
        try {
            Long orgId = currentUser.getOrganizationId();
            Asset asset = assetService.findById(orgId, id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

            assetService.softDelete(orgId, id);

            String actorEmail = principal != null ? principal.getName() : "system";
            String actorName = getActorName(actorEmail);
            auditTrailService.logAssetDeleted(asset, actorEmail, actorName, orgId);

            redirectAttrs.addFlashAttribute("success", "Asset deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting asset", e);
            redirectAttrs.addFlashAttribute("error", "Error deleting asset: " + e.getMessage());
        }

        return "redirect:/assets";
    }

    // -----------------------------------------------------------------------
    // API Endpoints
    // -----------------------------------------------------------------------

    @GetMapping("/api/assets")
    public ResponseEntity<List<Asset>> getAssetsApi(@ModelAttribute("currentUser") User currentUser) {
        Long orgId = currentUser.getOrganizationId();
        List<Asset> assets = assetService.findAll(orgId);
        return ResponseEntity.ok(assets);
    }

    // -----------------------------------------------------------------------
    // Private Helpers
    // -----------------------------------------------------------------------

    private String getActorName(String email) {
        if (email == null || email.equals("system")) return "System";
        var user = userRepository.findByEmail(email);
        return user != null ? user.getDisplayName() : email;
    }

    private Asset snapshotAsset(Asset asset) {
        Asset snapshot = new Asset();
        snapshot.setId(asset.getId());
        snapshot.setName(asset.getName());
        snapshot.setDescription(asset.getDescription());
        snapshot.setType(asset.getType());
        snapshot.setStatus(asset.getStatus());
        snapshot.setLocation(asset.getLocation());
        snapshot.setNotes(asset.getNotes());
        snapshot.setOwnerEmail(asset.getOwnerEmail());
        snapshot.setOwnerName(asset.getOwnerName());
        snapshot.setConfidentiality(asset.getConfidentiality());
        snapshot.setIntegrity(asset.getIntegrity());
        snapshot.setAvailability(asset.getAvailability());
        snapshot.setCreatedAt(asset.getCreatedAt());
        snapshot.setUpdatedAt(asset.getUpdatedAt());
        snapshot.setDeletedAt(asset.getDeletedAt());
        snapshot.setCreatedByEmail(asset.getCreatedByEmail());
        snapshot.setUpdatedByEmail(asset.getUpdatedByEmail());
        return snapshot;
    }
}
