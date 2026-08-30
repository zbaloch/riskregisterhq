package com.riskregister.riskregisterapp.entities;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.riskregister.riskregisterapp.enums.IssueStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A control deficiency, audit finding or regulatory finding — something that is already
 * true and needs remediating, as opposed to a Risk, which is an uncertain future event.
 *
 * Deliberately has NO likelihood field. An issue's probability is 1.0, so it is rated on
 * severity (impact x pervasiveness) instead. Scoring likelihood on a certainty understates
 * it and makes issue and risk scores non-comparable.
 */
@Entity
@Table(name = "issues")
@Getter
@Setter
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String issueRef; // User-friendly identifier, e.g. "ISS-001"

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Code of the admin-managed Issue Source option (see LookupType.ISSUE_SOURCE).
     * Stored as the code rather than an id so the option can be renamed without
     * rewriting history, and so the original enum values still resolve unchanged.
     */
    private String source;

    /**
     * Code of the admin-managed Issue Category option (see LookupType.ISSUE_CATEGORY).
     * Required on every issue — it is what groups findings for committee reporting.
     */
    private String category;

    /**
     * Code of the admin-managed Issue Impact Area option (see LookupType.ISSUE_DIMENSION).
     * Deliberately held on the issue rather than inherited from a linked risk: an issue may
     * have no linked risk, may link to several with differing dimensions, and the harm from a
     * deficiency is not always the harm from the risk it undermines.
     */
    private String dimension;

    /** Reference in the system the finding came from, e.g. a ServiceNow INC or audit report ref. */
    private String externalReference;

    // --- Severity: impact x pervasiveness, both 1-5. No likelihood, by design. ---

    /** How bad the consequence is if left unremediated (1 = Very Low ... 5 = Very High). */
    private Integer impact;

    /** How widespread the deficiency is (1 = isolated ... 5 = pervasive across the estate). */
    private Integer pervasiveness;

    @Column(columnDefinition = "TEXT")
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String remediationPlan;

    private String ownerName; // Remediation owner

    @Enumerated(EnumType.STRING)
    private IssueStatus status;

    private LocalDate dateRaised;
    private LocalDate targetDate;

    /**
     * The first target date ever committed to. Never overwritten, so date extensions stay
     * visible — repeated re-forecasting is the metric regulators scrutinise most.
     */
    private LocalDate originalTargetDate;

    private Integer extensionCount;

    private LocalDate closedDate;

    // Independent confirmation that the fix actually worked
    private String validatedByName;
    private Instant validatedAt;

    private String linkedRiskIds;  // Comma-separated Risk ids
    private String linkedAssetIds; // Comma-separated Asset ids

    private Long organizationId;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt; // null = active; non-null = soft-deleted

    private String createdByEmail;
    private String updatedByEmail;

    // --- Computed helpers (not persisted) ---

    /** Severity score 1-25. Impact x pervasiveness — never involves probability. */
    public Integer getSeverityScore() {
        return (impact != null && pervasiveness != null) ? impact * pervasiveness : null;
    }

    public String getSeverityLevel() {
        return severityToLevel(getSeverityScore());
    }

    /**
     * Four bands, matching how internal audit conventionally rates findings.
     * Deliberately different labels from the risk scale so the two are not read as equivalent.
     */
    public static String severityToLevel(Integer score) {
        if (score == null) return "Unrated";
        if (score <= 4)  return "Low";
        if (score <= 9)  return "Medium";
        if (score <= 16) return "High";
        return "Critical";
    }

    public static String valueToLabel(Integer v) {
        if (v == null) return "";
        return switch (v) {
            case 1 -> "Very Low";
            case 2 -> "Low";
            case 3 -> "Medium";
            case 4 -> "High";
            case 5 -> "Very High";
            default -> String.valueOf(v);
        };
    }

    /** Pervasiveness reads differently from impact, so it gets its own wording. */
    public static String pervasivenessToLabel(Integer v) {
        if (v == null) return "";
        return switch (v) {
            case 1 -> "Isolated";
            case 2 -> "Limited";
            case 3 -> "Moderate";
            case 4 -> "Widespread";
            case 5 -> "Pervasive";
            default -> String.valueOf(v);
        };
    }

    public String getImpactLabel()        { return valueToLabel(impact); }
    public String getPervasivenessLabel() { return pervasivenessToLabel(pervasiveness); }

    public boolean isClosed() {
        return status != null && status.isTerminal();
    }

    /** Past its target date and not yet in a terminal state. */
    public boolean isOverdue() {
        return !isClosed() && targetDate != null && targetDate.isBefore(LocalDate.now());
    }

    public Long getDaysOverdue() {
        if (targetDate == null || isClosed()) return null;
        long days = ChronoUnit.DAYS.between(targetDate, LocalDate.now());
        return days > 0 ? days : null;
    }

    /** How long the finding has been open, in days. */
    public Long getAgeDays() {
        if (dateRaised == null) return null;
        LocalDate end = (isClosed() && closedDate != null) ? closedDate : LocalDate.now();
        return ChronoUnit.DAYS.between(dateRaised, end);
    }

    /** Aging band used by the issue aging report. */
    public String getAgeBand() {
        Long age = getAgeDays();
        if (age == null) return "Unknown";
        if (age <= 30)  return "0–30 days";
        if (age <= 90)  return "31–90 days";
        if (age <= 180) return "91–180 days";
        if (age <= 365) return "181–365 days";
        return "Over a year";
    }

    public boolean isExtended() {
        return extensionCount != null && extensionCount > 0;
    }

    /** Remediation is done but nobody independent has signed it off yet. */
    public boolean isAwaitingValidation() {
        return status == IssueStatus.REMEDIATED;
    }

    public boolean isValidated() {
        return validatedAt != null;
    }

    // --- Link helpers ---

    public java.util.List<Long> getLinkedRiskIdList() {
        return parseIds(linkedRiskIds);
    }

    public java.util.List<Long> getLinkedAssetIdList() {
        return parseIds(linkedAssetIds);
    }

    private static java.util.List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return java.util.List.of();
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                ids.add(Long.valueOf(trimmed));
            } catch (NumberFormatException ignored) {
                // Skip malformed entries rather than failing the whole page render
            }
        }
        return ids;
    }

    // --- Formatting ---

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter STAMP_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    public String getDateRaisedFormatted()         { return dateRaised         != null ? dateRaised.format(DATE_FMT)         : null; }
    public String getTargetDateFormatted()         { return targetDate         != null ? targetDate.format(DATE_FMT)         : null; }
    public String getOriginalTargetDateFormatted() { return originalTargetDate != null ? originalTargetDate.format(DATE_FMT) : null; }
    public String getClosedDateFormatted()         { return closedDate         != null ? closedDate.format(DATE_FMT)         : null; }
    public String getCreatedAtFormatted()          { return createdAt          != null ? STAMP_FMT.format(createdAt)         : null; }
    public String getValidatedAtFormatted()        { return validatedAt        != null ? STAMP_FMT.format(validatedAt)       : null; }
}
