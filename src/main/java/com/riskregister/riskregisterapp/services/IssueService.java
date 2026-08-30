package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.Issue;
import com.riskregister.riskregisterapp.enums.IssueStatus;
import com.riskregister.riskregisterapp.repositories.IssueRepository;

@Service
public class IssueService {

    @Autowired
    private IssueRepository issueRepository;

    // -----------------------------------------------------------------------
    // Basic CRUD
    // -----------------------------------------------------------------------

    public List<Issue> findAll(Long organizationId) {
        return issueRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId);
    }

    public Optional<Issue> findById(Long organizationId, Long id) {
        return issueRepository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId, id);
    }

    /**
     * Persist a new issue. Allocates the ISS-nnn reference, stamps the original target date
     * so later extensions stay visible, and defaults the dates that governance depends on.
     */
    public Issue create(Issue issue, Long organizationId, String actorEmail) {
        Instant now = Instant.now();
        issue.setOrganizationId(organizationId);
        // The form pre-fills a suggestion, but the reference is the user's to set
        issue.setIssueRef(validateRef(issue.getIssueRef(), organizationId, null));
        issue.setCategory(requireChoice(issue.getCategory(), "category"));
        issue.setDimension(requireChoice(issue.getDimension(), "impact area"));
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);
        issue.setCreatedByEmail(actorEmail);

        if (issue.getDateRaised() == null) {
            issue.setDateRaised(LocalDate.now());
        }
        if (issue.getStatus() == null) {
            issue.setStatus(IssueStatus.OPEN);
        }
        // The first committed date is the baseline every later extension is measured against
        issue.setOriginalTargetDate(issue.getTargetDate());
        issue.setExtensionCount(0);

        return issueRepository.save(issue);
    }

    /**
     * Apply an edit to an existing issue. Moving the target date later counts as an
     * extension and is recorded — repeated re-forecasting is the metric regulators look at.
     * Returns the saved issue.
     */
    public Issue update(Issue existing, Issue form, String actorEmail) {
        existing.setIssueRef(validateRef(form.getIssueRef(), existing.getOrganizationId(), existing.getId()));
        existing.setCategory(requireChoice(form.getCategory(), "category"));
        existing.setDimension(requireChoice(form.getDimension(), "impact area"));
        existing.setTitle(form.getTitle());
        existing.setDescription(form.getDescription());
        existing.setSource(form.getSource());
        existing.setExternalReference(form.getExternalReference());
        existing.setImpact(form.getImpact());
        existing.setPervasiveness(form.getPervasiveness());
        existing.setRootCause(form.getRootCause());
        existing.setRemediationPlan(form.getRemediationPlan());
        existing.setOwnerName(form.getOwnerName());
        existing.setDateRaised(form.getDateRaised());
        existing.setLinkedRiskIds(form.getLinkedRiskIds());
        existing.setLinkedAssetIds(form.getLinkedAssetIds());

        // Count a genuine push-out of the deadline, not the initial commitment or a pull-in
        LocalDate oldTarget = existing.getTargetDate();
        LocalDate newTarget = form.getTargetDate();
        if (oldTarget != null && newTarget != null && newTarget.isAfter(oldTarget)) {
            existing.setExtensionCount(existing.getExtensionCount() == null ? 1 : existing.getExtensionCount() + 1);
        }
        existing.setTargetDate(newTarget);
        // Backfill the baseline if the issue predates having one
        if (existing.getOriginalTargetDate() == null) {
            existing.setOriginalTargetDate(newTarget);
        }

        applyStatus(existing, form.getStatus());

        existing.setUpdatedAt(Instant.now());
        existing.setUpdatedByEmail(actorEmail);
        return issueRepository.save(existing);
    }

    /**
     * Move an issue to a new status, keeping the closure date consistent. Validation is
     * recorded separately via {@link #validate} — reaching CLOSED does not by itself mean
     * anyone independent confirmed the fix.
     */
    public Issue changeStatus(Issue issue, IssueStatus newStatus, String actorEmail) {
        applyStatus(issue, newStatus);
        issue.setUpdatedAt(Instant.now());
        issue.setUpdatedByEmail(actorEmail);
        return issueRepository.save(issue);
    }

    private void applyStatus(Issue issue, IssueStatus newStatus) {
        if (newStatus == null) return;
        IssueStatus old = issue.getStatus();
        issue.setStatus(newStatus);

        if (newStatus.isTerminal() && issue.getClosedDate() == null) {
            issue.setClosedDate(LocalDate.now());
        }
        // Reopening clears the closure date so age is measured to today again
        if (!newStatus.isTerminal() && old != null && old.isTerminal()) {
            issue.setClosedDate(null);
        }
    }

    /** Record independent confirmation that the remediation actually worked. */
    public Issue validate(Issue issue, String validatorName, String actorEmail) {
        issue.setValidatedByName(validatorName);
        issue.setValidatedAt(Instant.now());
        applyStatus(issue, IssueStatus.CLOSED);
        issue.setUpdatedAt(Instant.now());
        issue.setUpdatedByEmail(actorEmail);
        return issueRepository.save(issue);
    }

    public void softDelete(Long organizationId, Long id) {
        issueRepository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId, id).ifPresent(issue -> {
            issue.setDeletedAt(Instant.now());
            issueRepository.save(issue);
        });
    }

    // -----------------------------------------------------------------------
    // Queries used by the dashboard, reports and risk view
    // -----------------------------------------------------------------------

    /** Open (non-terminal) issues, worst severity first. */
    public List<Issue> findOpen(Long organizationId) {
        return findAll(organizationId).stream()
            .filter(i -> !i.isClosed())
            .sorted(Comparator.comparing((Issue i) -> i.getSeverityScore() == null ? 0 : i.getSeverityScore())
                              .reversed())
            .toList();
    }

    public List<Issue> findOverdue(Long organizationId) {
        return findAll(organizationId).stream()
            .filter(Issue::isOverdue)
            .sorted(Comparator.comparing((Issue i) -> i.getDaysOverdue() == null ? 0L : i.getDaysOverdue())
                              .reversed())
            .toList();
    }

    public long countOpen(Long organizationId) {
        return findAll(organizationId).stream().filter(i -> !i.isClosed()).count();
    }

    public long countOverdue(Long organizationId) {
        return findAll(organizationId).stream().filter(Issue::isOverdue).count();
    }

    /** Issues awaiting independent validation — remediated but not yet signed off. */
    public List<Issue> findAwaitingValidation(Long organizationId) {
        return findAll(organizationId).stream()
            .filter(Issue::isAwaitingValidation)
            .toList();
    }

    /** Open issues linked to a given risk, worst first. Drives the risk view's Issues tab. */
    public List<Issue> findByRisk(Long organizationId, Long riskId) {
        return findAll(organizationId).stream()
            .filter(i -> i.getLinkedRiskIdList().contains(riskId))
            .sorted(Comparator.comparing((Issue i) -> i.isClosed())
                              .thenComparing(i -> -(i.getSeverityScore() == null ? 0 : i.getSeverityScore())))
            .toList();
    }

    /**
     * Open High or Critical findings against a risk's controls. When this is non-empty the
     * risk's residual score is probably optimistic — the controls it assumes are demonstrably
     * not working.
     */
    public List<Issue> findOpenSevereByRisk(Long organizationId, Long riskId) {
        return findByRisk(organizationId, riskId).stream()
            .filter(i -> !i.isClosed())
            .filter(i -> i.getSeverityScore() != null && i.getSeverityScore() >= 10)
            .toList();
    }

    /** Count of open severe issues per risk id, for bulk use on list pages and reports. */
    public Map<Long, Long> openSevereCountByRisk(Long organizationId) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Issue issue : findAll(organizationId)) {
            if (issue.isClosed()) continue;
            Integer score = issue.getSeverityScore();
            if (score == null || score < 10) continue;
            for (Long riskId : issue.getLinkedRiskIdList()) {
                counts.merge(riskId, 1L, Long::sum);
            }
        }
        return counts;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Classification fields are mandatory: they are what group findings for committee
     * reporting, and a partly-classified backlog cannot be summarised at all.
     */
    private static String requireChoice(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "Select a" + (fieldLabel.startsWith("i") ? "n " : " ") + fieldLabel
                + ". Every issue must have one.");
        }
        return value.trim();
    }

    /**
     * Check a user-supplied reference and fall back to the next in sequence when blank.
     * References must be unique among live issues; a soft-deleted one frees its reference.
     *
     * @param excludeId the issue being edited, so it does not clash with itself
     */
    private String validateRef(String issueRef, Long organizationId, Long excludeId) {
        String clean = issueRef == null ? "" : issueRef.trim();
        if (clean.isEmpty()) {
            return suggestNextRef(organizationId);
        }
        issueRepository.findByOrganizationIdAndIssueRefIgnoreCaseAndDeletedAtIsNull(organizationId, clean)
            .filter(other -> !other.getId().equals(excludeId))
            .ifPresent(other -> {
                throw new IllegalArgumentException("Issue ID \"" + clean
                    + "\" is already used by another issue. Choose a different reference.");
            });
        return clean;
    }

    /** Next reference in the ISS-nnn sequence — offered as the default on the create form. */
    public String suggestNextRef(Long organizationId) {
        int next = issueRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId).stream()
            .map(Issue::getIssueRef)
            .filter(ref -> ref != null && ref.startsWith("ISS-"))
            .map(ref -> {
                try {
                    return Integer.parseInt(ref.substring(4));
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .max(Integer::compareTo)
            .orElse(0) + 1;
        return "ISS-" + String.format("%03d", next);
    }
}
