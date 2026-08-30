package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskregister.riskregisterapp.dto.FieldChange;
import com.riskregister.riskregisterapp.entities.AuditTrail;
import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.repositories.AuditTrailRepository;

@Service
public class AuditTrailService {

    @Autowired
    private AuditTrailRepository auditTrailRepository;

    @Autowired
    private ObjectMapper objectMapper;  // Auto-configured by spring-boot-starter-web

    @Autowired
    private LookupService lookupService;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Log a Risk creation event. */
    public void logRiskCreated(Risk risk, String actorEmail, String actorName, Long organizationId) {
        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Risk");
        entry.setEntityId(risk.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("CREATED");
        entry.setSummary("Risk created (" + risk.getRiskId() + "): " + risk.getTitle());
        entry.setChangesJson(null);
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(Instant.now());
        auditTrailRepository.save(entry);
    }

    /**
     * Compare old and new Risk, build a FieldChange list, and persist an UPDATED entry.
     *
     * @param oldRisk    the entity state BEFORE the update (loaded from DB)
     * @param newRisk    the entity state AFTER the update (already saved)
     * @param categoryMap    code→name for risk categories
     * @param dimensionMap   id→name for dimensions
     * @param statusMap      id→name for statuses
     * @param actorEmail     the logged-in user's email
     * @param actorName      the logged-in user's display name
     */
    public void logRiskUpdated(Risk oldRisk, Risk newRisk,
                               Map<String, String> categoryMap,
                               Map<Long, String> dimensionMap,
                               Map<Long, String> statusMap,
                               String actorEmail, String actorName, Long organizationId) {
        List<FieldChange> changes = diffRisk(oldRisk, newRisk,
                                             categoryMap, dimensionMap, statusMap);
        if (changes.isEmpty()) return;  // Nothing actually changed — skip

        String changedFieldLabels = changes.stream()
            .map(FieldChange::label)
            .collect(Collectors.joining(", "));

        String summary = "Risk updated (" + newRisk.getRiskId() + "): " + changedFieldLabels;

        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Risk");
        entry.setEntityId(newRisk.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("UPDATED");
        entry.setSummary(summary);
        entry.setChangesJson(toJson(changes));
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(Instant.now());
        auditTrailRepository.save(entry);
    }

    /** Log a Risk soft-delete event. */
    public void logRiskDeleted(Risk risk, String actorEmail, String actorName, Long organizationId) {
        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Risk");
        entry.setEntityId(risk.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("DELETED");
        entry.setSummary("Risk deleted (" + risk.getRiskId() + "): " + risk.getTitle());
        entry.setChangesJson(null);
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(Instant.now());
        auditTrailRepository.save(entry);
    }

    /**
     * Log that a periodic review was carried out. Recorded as its own action so the
     * trail distinguishes "reviewed, no change needed" from an ordinary edit.
     */
    public void logRiskReviewed(Risk risk, String actorEmail, String actorName, Long organizationId) {
        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Risk");
        entry.setEntityId(risk.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("REVIEWED");
        String next = risk.getNextReviewDateFormatted();
        entry.setSummary("Risk reviewed (" + risk.getRiskId() + "): " + risk.getTitle()
            + (next != null ? " — next review due " + next : ""));
        entry.setChangesJson(null);
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(Instant.now());
        auditTrailRepository.save(entry);
    }

    /** Retrieve all audit trail entries for a Risk, newest first. */
    public List<AuditTrail> findByRisk(Long organizationId, Long riskId) {
        return auditTrailRepository.findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(organizationId, "Risk", riskId);
    }

    /**
     * Deserialize the changesJson field back to a List<FieldChange>.
     * Returns an empty list if changesJson is null or blank.
     * Used in the controller to pass structured data to the view.
     */
    public List<FieldChange> parseChanges(AuditTrail entry) {
        if (entry.getChangesJson() == null || entry.getChangesJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(entry.getChangesJson(),
                new TypeReference<List<FieldChange>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // Private: field diff logic
    // -----------------------------------------------------------------------

    private List<FieldChange> diffRisk(Risk before, Risk after,
                                       Map<String, String> catMap,
                                       Map<Long, String> dimMap,
                                       Map<Long, String> statusMap) {
        List<FieldChange> changes = new ArrayList<>();

        diff(changes, "riskId",                "Risk ID",
             before.getRiskId(),               after.getRiskId());
        diff(changes, "title",                 "Title",
             before.getTitle(),                after.getTitle());
        diff(changes, "description",           "Description",
             before.getDescription(),          after.getDescription());
        diff(changes, "riskOwnerName",         "Owner",
             before.getRiskOwnerName(),        after.getRiskOwnerName());
        diff(changes, "statusId",              "Status",
             nameOrId(before.getStatusId(), statusMap),
             nameOrId(after.getStatusId(),  statusMap));
        diff(changes, "riskTreatment",         "Risk Treatment",
             enumLabel(before.getRiskTreatment()), enumLabel(after.getRiskTreatment()));
        diff(changes, "reviewFrequency",       "Review Frequency",
             enumLabel(before.getReviewFrequency()), enumLabel(after.getReviewFrequency()));

        // Category/subcategory/dimension — resolve names from maps
        diff(changes, "riskCategory",          "Risk Category",
             codeToName(before.getRiskCategory(), catMap),
             codeToName(after.getRiskCategory(),  catMap));
        diff(changes, "riskDimensionId",       "Dimension",
             nameOrId(before.getRiskDimensionId(), dimMap),
             nameOrId(after.getRiskDimensionId(),  dimMap));

        diff(changes, "categories",            "Categories",
             before.getCategories(),           after.getCategories());

        // Integer scores — convert to "3 – Medium" style labels
        diff(changes, "inherentLikelihood",    "Inherent Likelihood",
             sliderLabel(before.getInherentLikelihood()),
             sliderLabel(after.getInherentLikelihood()));
        diff(changes, "inherentImpact",        "Inherent Impact",
             sliderLabel(before.getInherentImpact()),
             sliderLabel(after.getInherentImpact()));
        diff(changes, "inherentRationale",     "Inherent Rationale",
             before.getInherentRationale(),    after.getInherentRationale());
        diff(changes, "residualLikelihood",    "Residual Likelihood",
             sliderLabel(before.getResidualLikelihood()),
             sliderLabel(after.getResidualLikelihood()));
        diff(changes, "residualImpact",        "Residual Impact",
             sliderLabel(before.getResidualImpact()),
             sliderLabel(after.getResidualImpact()));
        diff(changes, "residualRationale",     "Residual Rationale",
             before.getResidualRationale(),    after.getResidualRationale());

        return changes;
    }

    /** Add a FieldChange only when old and new values actually differ. */
    private static void diff(List<FieldChange> out,
                              String field, String label,
                              String oldVal, String newVal) {
        String o = normalize(oldVal);
        String n = normalize(newVal);
        if (!Objects.equals(o, n)) {
            out.add(new FieldChange(field, label, o, n));
        }
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? "" : s.trim();
    }

    /** Resolve a managed-field code to its current label, falling back to the raw code. */
    private static String codeToName(String code, Map<String, String> map) {
        if (code == null || code.isBlank()) return "";
        String name = map.get(code);
        return name != null ? name : code;
    }

    private static String nameOrId(Long id, Map<Long, String> map) {
        if (id == null) return "";
        String name = map.get(id);
        return name != null ? name : String.valueOf(id);
    }

    private static String enumLabel(Enum<?> e) {
        if (e == null) return "";
        // Convert "VERY_HIGH" → "Very High"
        String raw = e.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static String sliderLabel(Integer v) {
        if (v == null) return "";
        return v + " – " + Risk.valueToLabel(v);
    }

    // -----------------------------------------------------------------------
    // Task logging (reuses generic AuditTrail with entityType="Task")
    // -----------------------------------------------------------------------

    /** Log a Task creation event. */
    public void logTaskCreated(com.riskregister.riskregisterapp.entities.Task task, String actorEmail, String actorName, Long organizationId) {
        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Task");
        entry.setEntityId(task.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("CREATED");
        entry.setSummary("Task created: " + task.getTitle());
        entry.setChangesJson(null);
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(java.time.Instant.now());
        auditTrailRepository.save(entry);
    }

    /** Log a Task update event with field-level changes. */
    public void logTaskUpdated(com.riskregister.riskregisterapp.entities.Task oldTask,
                               com.riskregister.riskregisterapp.entities.Task newTask,
                               String actorEmail, String actorName, Long organizationId) {
        List<FieldChange> changes = diffTask(oldTask, newTask);
        if (changes.isEmpty()) return;  // Nothing changed — skip

        String changedFieldLabels = changes.stream()
            .map(FieldChange::label)
            .collect(Collectors.joining(", "));

        String summary = "Task updated: " + changedFieldLabels;

        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Task");
        entry.setEntityId(newTask.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("UPDATED");
        entry.setSummary(summary);
        entry.setChangesJson(toJson(changes));
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(java.time.Instant.now());
        auditTrailRepository.save(entry);
    }

    /** Log a Task deletion event. */
    public void logTaskDeleted(com.riskregister.riskregisterapp.entities.Task task, String actorEmail, String actorName, Long organizationId) {
        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Task");
        entry.setEntityId(task.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("DELETED");
        entry.setSummary("Task deleted: " + task.getTitle());
        entry.setChangesJson(null);
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(java.time.Instant.now());
        auditTrailRepository.save(entry);
    }

    // -----------------------------------------------------------------------
    // Private: Task field diff logic
    // -----------------------------------------------------------------------

    private List<FieldChange> diffTask(com.riskregister.riskregisterapp.entities.Task before,
                                        com.riskregister.riskregisterapp.entities.Task after) {
        List<FieldChange> changes = new ArrayList<>();

        diff(changes, "title", "Title", before.getTitle(), after.getTitle());
        diff(changes, "description", "Description", before.getDescription(), after.getDescription());
        diff(changes, "status", "Status", enumLabel(before.getStatus()), enumLabel(after.getStatus()));
        diff(changes, "priority", "Priority", enumLabel(before.getPriority()), enumLabel(after.getPriority()));
        diff(changes, "assigneeName", "Assigned To", before.getAssigneeName(), after.getAssigneeName());
        diff(changes, "dueDate", "Due Date",
             before.getDueDate() != null ? before.getDueDate().toString() : "",
             after.getDueDate() != null ? after.getDueDate().toString() : "");

        return changes;
    }

    // -----------------------------------------------------------------------
    // Asset logging
    // -----------------------------------------------------------------------

    /** Log an Asset creation event. */
    public void logAssetCreated(com.riskregister.riskregisterapp.entities.Asset asset, String actorEmail, String actorName, Long organizationId) {
        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Asset");
        entry.setEntityId(asset.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("CREATED");
        entry.setSummary("Asset created: " + asset.getName());
        entry.setChangesJson(null);
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(Instant.now());
        auditTrailRepository.save(entry);
    }

    /** Log an Asset update event with field-level changes. */
    public void logAssetUpdated(com.riskregister.riskregisterapp.entities.Asset oldAsset,
                                com.riskregister.riskregisterapp.entities.Asset newAsset,
                                String actorEmail, String actorName, Long organizationId) {
        List<FieldChange> changes = diffAsset(oldAsset, newAsset);
        if (changes.isEmpty()) return;  // Nothing changed — skip

        String changedFieldLabels = changes.stream()
            .map(FieldChange::label)
            .collect(Collectors.joining(", "));

        String summary = "Asset updated: " + changedFieldLabels;

        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Asset");
        entry.setEntityId(newAsset.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("UPDATED");
        entry.setSummary(summary);
        entry.setChangesJson(toJson(changes));
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(Instant.now());
        auditTrailRepository.save(entry);
    }

    /** Log an Asset deletion event. */
    public void logAssetDeleted(com.riskregister.riskregisterapp.entities.Asset asset, String actorEmail, String actorName, Long organizationId) {
        AuditTrail entry = new AuditTrail();
        entry.setEntityType("Asset");
        entry.setEntityId(asset.getId());
        entry.setOrganizationId(organizationId);
        entry.setAction("DELETED");
        entry.setSummary("Asset deleted: " + asset.getName());
        entry.setChangesJson(null);
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(Instant.now());
        auditTrailRepository.save(entry);
    }

    /** Retrieve all audit trail entries for an Asset, newest first. */
    public List<AuditTrail> findByAsset(Long organizationId, Long assetId) {
        return auditTrailRepository.findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(organizationId, "Asset", assetId);
    }

    // -----------------------------------------------------------------------
    // Private: Asset field diff logic
    // -----------------------------------------------------------------------

    private List<FieldChange> diffAsset(com.riskregister.riskregisterapp.entities.Asset before,
                                         com.riskregister.riskregisterapp.entities.Asset after) {
        List<FieldChange> changes = new ArrayList<>();

        diff(changes, "name", "Name", before.getName(), after.getName());
        diff(changes, "description", "Description", before.getDescription(), after.getDescription());
        diff(changes, "type", "Type",
             lookupLabel(com.riskregister.riskregisterapp.enums.LookupType.ASSET_TYPE,
                         before.getType(), before.getOrganizationId()),
             lookupLabel(com.riskregister.riskregisterapp.enums.LookupType.ASSET_TYPE,
                         after.getType(), after.getOrganizationId()));
        diff(changes, "status", "Status", before.getStatus(), after.getStatus());
        diff(changes, "location", "Location", before.getLocation(), after.getLocation());
        diff(changes, "notes", "Notes", before.getNotes(), after.getNotes());
        diff(changes, "ownerName", "Owner", before.getOwnerName(), after.getOwnerName());
        diff(changes, "confidentiality", "Confidentiality",
             before.getConfidentiality() != null ? before.getConfidentiality().toString() : "",
             after.getConfidentiality() != null ? after.getConfidentiality().toString() : "");
        diff(changes, "integrity", "Integrity",
             before.getIntegrity() != null ? before.getIntegrity().toString() : "",
             after.getIntegrity() != null ? after.getIntegrity().toString() : "");
        diff(changes, "availability", "Availability",
             before.getAvailability() != null ? before.getAvailability().toString() : "",
             after.getAvailability() != null ? after.getAvailability().toString() : "");

        return changes;
    }

    // -----------------------------------------------------------------------
    // Issue logging (control deficiencies / audit findings)
    // -----------------------------------------------------------------------

    public void logIssueCreated(com.riskregister.riskregisterapp.entities.Issue issue,
                                String actorEmail, String actorName, Long organizationId) {
        saveEntry("Issue", issue.getId(), organizationId, "CREATED",
            "Issue raised (" + issue.getIssueRef() + "): " + issue.getTitle(),
            null, actorEmail, actorName);
    }

    public void logIssueUpdated(com.riskregister.riskregisterapp.entities.Issue before,
                                com.riskregister.riskregisterapp.entities.Issue after,
                                String actorEmail, String actorName, Long organizationId) {
        List<FieldChange> changes = diffIssue(before, after);
        if (changes.isEmpty()) return;  // Nothing actually changed — skip

        String changedFieldLabels = changes.stream().map(FieldChange::label).collect(Collectors.joining(", "));
        saveEntry("Issue", after.getId(), organizationId, "UPDATED",
            "Issue updated (" + after.getIssueRef() + "): " + changedFieldLabels,
            toJson(changes), actorEmail, actorName);
    }

    public void logIssueDeleted(com.riskregister.riskregisterapp.entities.Issue issue,
                                String actorEmail, String actorName, Long organizationId) {
        saveEntry("Issue", issue.getId(), organizationId, "DELETED",
            "Issue removed (" + issue.getIssueRef() + "): " + issue.getTitle(),
            null, actorEmail, actorName);
    }

    /** Status transitions get their own action so the lifecycle is legible in the change report. */
    public void logIssueStatusChanged(com.riskregister.riskregisterapp.entities.Issue issue,
                                      String oldStatus, String newStatus,
                                      String actorEmail, String actorName, Long organizationId) {
        List<FieldChange> changes = List.of(new FieldChange("status", "Status", oldStatus, newStatus));
        saveEntry("Issue", issue.getId(), organizationId, "STATUS_CHANGED",
            "Issue " + issue.getIssueRef() + " moved to " + newStatus,
            toJson(changes), actorEmail, actorName);
    }

    /** Independent sign-off that a remediation actually worked. */
    public void logIssueValidated(com.riskregister.riskregisterapp.entities.Issue issue,
                                  String actorEmail, String actorName, Long organizationId) {
        saveEntry("Issue", issue.getId(), organizationId, "VALIDATED",
            "Issue " + issue.getIssueRef() + " validated and closed by " + issue.getValidatedByName(),
            null, actorEmail, actorName);
    }

    public List<AuditTrail> findByIssue(Long organizationId, Long issueId) {
        return auditTrailRepository.findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            organizationId, "Issue", issueId);
    }

    private List<FieldChange> diffIssue(com.riskregister.riskregisterapp.entities.Issue before,
                                         com.riskregister.riskregisterapp.entities.Issue after) {
        List<FieldChange> changes = new ArrayList<>();

        diff(changes, "issueRef", "Issue ID", before.getIssueRef(), after.getIssueRef());
        diff(changes, "title", "Title", before.getTitle(), after.getTitle());
        diff(changes, "description", "Description", before.getDescription(), after.getDescription());
        // Source and category are managed lookup codes; resolve to their current labels
        // so the trail reads plainly rather than showing raw codes
        diff(changes, "category", "Category",
             lookupLabel(com.riskregister.riskregisterapp.enums.LookupType.ISSUE_CATEGORY,
                         before.getCategory(), before.getOrganizationId()),
             lookupLabel(com.riskregister.riskregisterapp.enums.LookupType.ISSUE_CATEGORY,
                         after.getCategory(), after.getOrganizationId()));
        diff(changes, "dimension", "Impact Area",
             lookupLabel(com.riskregister.riskregisterapp.enums.LookupType.ISSUE_DIMENSION,
                         before.getDimension(), before.getOrganizationId()),
             lookupLabel(com.riskregister.riskregisterapp.enums.LookupType.ISSUE_DIMENSION,
                         after.getDimension(), after.getOrganizationId()));
        diff(changes, "source", "Source",
             lookupLabel(com.riskregister.riskregisterapp.enums.LookupType.ISSUE_SOURCE,
                         before.getSource(), before.getOrganizationId()),
             lookupLabel(com.riskregister.riskregisterapp.enums.LookupType.ISSUE_SOURCE,
                         after.getSource(), after.getOrganizationId()));
        diff(changes, "externalReference", "External Reference",
             before.getExternalReference(), after.getExternalReference());
        diff(changes, "impact", "Impact",
             severityPart(before.getImpact(), before.getImpactLabel()),
             severityPart(after.getImpact(), after.getImpactLabel()));
        diff(changes, "pervasiveness", "Pervasiveness",
             severityPart(before.getPervasiveness(), before.getPervasivenessLabel()),
             severityPart(after.getPervasiveness(), after.getPervasivenessLabel()));
        diff(changes, "rootCause", "Root Cause", before.getRootCause(), after.getRootCause());
        diff(changes, "remediationPlan", "Remediation Plan", before.getRemediationPlan(), after.getRemediationPlan());
        diff(changes, "ownerName", "Remediation Owner", before.getOwnerName(), after.getOwnerName());
        diff(changes, "status", "Status",
             before.getStatus() != null ? before.getStatus().getDisplayName() : "",
             after.getStatus() != null ? after.getStatus().getDisplayName() : "");
        diff(changes, "dateRaised", "Date Raised",
             before.getDateRaisedFormatted(), after.getDateRaisedFormatted());
        diff(changes, "targetDate", "Target Date",
             before.getTargetDateFormatted(), after.getTargetDateFormatted());

        return changes;
    }

    private String lookupLabel(com.riskregister.riskregisterapp.enums.LookupType type,
                               String code, Long organizationId) {
        if (code == null || code.isBlank()) return "";
        if (organizationId == null) return code;
        return lookupService.find(type, organizationId, code)
            .map(com.riskregister.riskregisterapp.entities.LookupValue::getName)
            .orElse(code);
    }

    private static String severityPart(Integer value, String label) {
        if (value == null) return "";
        return value + " – " + label;
    }

    // -----------------------------------------------------------------------
    // Risk taxonomy logging (categories + subcategories)
    // -----------------------------------------------------------------------







    /** Shared writer for the simpler entity types that don't need bespoke summaries. */
    private void saveEntry(String entityType, Long entityId, Long organizationId,
                           String action, String summary, String changesJson,
                           String actorEmail, String actorName) {
        AuditTrail entry = new AuditTrail();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setOrganizationId(organizationId);
        entry.setAction(action);
        entry.setSummary(summary);
        entry.setChangesJson(changesJson);
        entry.setActorEmail(actorEmail);
        entry.setActorName(actorName);
        entry.setCreatedAt(Instant.now());
        auditTrailRepository.save(entry);
    }

    private String toJson(List<FieldChange> changes) {
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (Exception e) {
            return "[]";
        }
    }
}
