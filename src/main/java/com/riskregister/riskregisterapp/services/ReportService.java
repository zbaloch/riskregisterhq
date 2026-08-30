package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.dto.reports.ChangeEntry;
import com.riskregister.riskregisterapp.dto.reports.ChangeReport;
import com.riskregister.riskregisterapp.dto.reports.GapRow;
import com.riskregister.riskregisterapp.dto.reports.HeatmapCell;
import com.riskregister.riskregisterapp.dto.reports.HeatmapReport;
import com.riskregister.riskregisterapp.dto.reports.IssuesByRiskRow;
import com.riskregister.riskregisterapp.dto.reports.OwnerRow;
import com.riskregister.riskregisterapp.dto.reports.ReductionRow;
import com.riskregister.riskregisterapp.dto.reports.ReviewDueRow;
import com.riskregister.riskregisterapp.dto.reports.TreatmentRow;
import com.riskregister.riskregisterapp.dto.reports.TrendPoint;
import com.riskregister.riskregisterapp.entities.AuditTrail;
import com.riskregister.riskregisterapp.entities.EffectivenessScore;
import com.riskregister.riskregisterapp.entities.Issue;
import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.entities.RiskCategory;
import com.riskregister.riskregisterapp.entities.RiskSubcategory;
import com.riskregister.riskregisterapp.entities.Task;
import com.riskregister.riskregisterapp.enums.TaskStatus;
import com.riskregister.riskregisterapp.lookups.RiskTreatment;
import com.riskregister.riskregisterapp.repositories.AuditTrailRepository;
import com.riskregister.riskregisterapp.repositories.RiskCategoryRepository;
import com.riskregister.riskregisterapp.repositories.RiskSubcategoryRepository;

/**
 * Builds the risk register reports. Everything here reads from the live register —
 * no report state is stored — so each page reflects the register at the moment it loads.
 */
@Service
public class ReportService {

    @Autowired
    private RiskService riskService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private EffectivenessScoreService effectivenessScoreService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private RiskCategoryRepository riskCategoryRepository;

    @Autowired
    private RiskSubcategoryRepository riskSubcategoryRepository;

    @Autowired
    private AuditTrailRepository auditTrailRepository;

    @Autowired
    private AuditTrailService auditTrailService;

    // =======================================================================
    // 1. Heatmaps (inherent + residual, with per-cell drill-down)
    // =======================================================================

    public HeatmapReport buildHeatmap(Long orgId, boolean residual) {
        return buildHeatmap(riskService.findAll(orgId), residual);
    }

    private HeatmapReport buildHeatmap(List<Risk> risks, boolean residual) {
        // cells[impact][likelihood] — 1-based indices, index 0 unused
        Map<String, List<Risk>> buckets = new LinkedHashMap<>();
        int plotted = 0;
        int unscored = 0;

        for (Risk r : risks) {
            Integer l = residual ? r.getResidualLikelihood() : r.getInherentLikelihood();
            Integer i = residual ? r.getResidualImpact() : r.getInherentImpact();
            if (l == null || i == null || l < 1 || l > 5 || i < 1 || i > 5) {
                unscored++;
                continue;
            }
            buckets.computeIfAbsent(l + ":" + i, k -> new ArrayList<>()).add(r);
            plotted++;
        }

        // Rows top-to-bottom are impact 5 → 1, columns left-to-right likelihood 1 → 5
        List<List<HeatmapCell>> rows = new ArrayList<>();
        for (int impact = 5; impact >= 1; impact--) {
            List<HeatmapCell> row = new ArrayList<>();
            for (int likelihood = 1; likelihood <= 5; likelihood++) {
                int score = likelihood * impact;
                List<Risk> cellRisks = buckets.getOrDefault(likelihood + ":" + impact, List.of());
                row.add(new HeatmapCell(likelihood, impact, score,
                    scoreLevel(score), heatmapCellClass(score), cellRisks));
            }
            rows.add(row);
        }

        return new HeatmapReport(residual ? "Residual Risk" : "Inherent Risk", rows, plotted, unscored);
    }

    // =======================================================================
    // 2. Risk reduction by category / subcategory
    // =======================================================================

    public List<ReductionRow> buildReductionReport(Long orgId) {
        List<Risk> risks = riskService.findAll(orgId);
        Map<Long, String> catNames = categoryNames();
        Map<Long, String> subNames = subcategoryNames();

        Map<Long, List<Risk>> byCategory = risks.stream()
            .collect(Collectors.groupingBy(r -> r.getRiskCategoryId() != null ? r.getRiskCategoryId() : -1L,
                                           LinkedHashMap::new, Collectors.toList()));

        List<ReductionRow> out = new ArrayList<>();
        for (Map.Entry<Long, List<Risk>> e : byCategory.entrySet()) {
            Long catId = e.getKey();
            List<Risk> catRisks = e.getValue();
            String catName = catId == -1L ? "Uncategorized" : catNames.getOrDefault(catId, "Category " + catId);

            // Subcategory rows nested under the category
            Map<Long, List<Risk>> bySub = catRisks.stream()
                .collect(Collectors.groupingBy(r -> r.getRiskSubcategoryId() != null ? r.getRiskSubcategoryId() : -1L,
                                               LinkedHashMap::new, Collectors.toList()));

            List<ReductionRow> children = bySub.entrySet().stream()
                .map(se -> new ReductionRow(
                    se.getKey() == -1L ? "No subcategory" : subNames.getOrDefault(se.getKey(), "Subcategory " + se.getKey()),
                    false,
                    se.getValue().size(),
                    avg(se.getValue(), true),
                    avg(se.getValue(), false),
                    List.of()))
                .sorted(Comparator.comparing(ReductionRow::name))
                .toList();

            out.add(new ReductionRow(catName, true, catRisks.size(),
                avg(catRisks, true), avg(catRisks, false), children));
        }

        // Biggest exposure first — that is where a weak reduction matters most
        out.sort(Comparator.comparing((ReductionRow r) -> r.avgInherent() == null ? 0.0 : r.avgInherent()).reversed());
        return out;
    }

    private static Double avg(List<Risk> risks, boolean inherent) {
        List<Integer> scores = risks.stream()
            .map(r -> inherent ? r.getInherentScore() : r.getResidualScore())
            .filter(java.util.Objects::nonNull)
            .toList();
        if (scores.isEmpty()) return null;
        return scores.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    // =======================================================================
    // 3. Treatment decision summary
    // =======================================================================

    public List<TreatmentRow> buildTreatmentReport(Long orgId) {
        List<Risk> risks = riskService.findAll(orgId);
        List<TreatmentRow> rows = new ArrayList<>();

        for (RiskTreatment t : RiskTreatment.values()) {
            List<Risk> matching = risks.stream()
                .filter(r -> r.getRiskTreatment() == t)
                .sorted(Comparator.comparing(ReportService::effectiveScore, Comparator.reverseOrder()))
                .toList();
            rows.add(new TreatmentRow(t.name(), treatmentLabel(t), treatmentBadgeClass(t), matching));
        }

        // Risks with no treatment recorded at all
        List<Risk> none = risks.stream().filter(r -> r.getRiskTreatment() == null).toList();
        if (!none.isEmpty()) {
            rows.add(new TreatmentRow("NONE", "Not Set", "bg-gray-100 text-gray-700", none));
        }

        // Backlog first, then largest groups
        rows.sort(Comparator.comparing((TreatmentRow r) -> !r.isBacklog())
                            .thenComparing(r -> -r.count()));
        return rows;
    }

    /** How long a risk has been sitting awaiting assessment. */
    public long daysSinceCreated(Risk risk) {
        if (risk.getCreatedAt() == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(
            risk.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now());
    }

    // =======================================================================
    // 4. Mitigation gap report (exception report)
    // =======================================================================

    public List<GapRow> buildMitigationGapReport(Long orgId) {
        List<Risk> risks = riskService.findAll(orgId);
        List<Task> tasks = taskService.findAll(orgId);
        Map<Long, String> catNames = categoryNames();
        LocalDate today = LocalDate.now();

        Map<Long, List<Task>> tasksByRisk = tasks.stream()
            .filter(t -> t.getRiskId() != null)
            .collect(Collectors.groupingBy(Task::getRiskId));

        List<GapRow> gaps = new ArrayList<>();
        for (Risk risk : risks) {
            // Only risks the organisation has committed to actively treating
            if (risk.getRiskTreatment() != RiskTreatment.MITIGATE) continue;
            // A closed risk needs no open mitigation
            if (risk.getStatusId() != null && risk.getStatusId() == 5L) continue;

            List<Task> riskTasks = tasksByRisk.getOrDefault(risk.getId(), List.of());
            long open = riskTasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).count();
            long overdue = riskTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED
                          && t.getDueDate() != null && t.getDueDate().isBefore(today))
                .count();

            String gap = null;
            String severity = null;
            if (riskTasks.isEmpty()) {
                gap = "No mitigation tasks";
                severity = "critical";
            } else if (open == 0) {
                gap = "All tasks closed, risk still open";
                severity = "warning";
            } else if (overdue == open) {
                gap = "Every open task overdue";
                severity = "critical";
            } else if (overdue > 0) {
                gap = overdue + " of " + open + " open tasks overdue";
                severity = "warning";
            }

            if (gap != null) {
                gaps.add(new GapRow(risk,
                    risk.getRiskCategoryId() != null ? catNames.getOrDefault(risk.getRiskCategoryId(), "—") : "—",
                    riskTasks.size(), open, overdue, gap, severity));
            }
        }

        // Critical first, then by exposure
        gaps.sort(Comparator.comparing((GapRow g) -> !g.isCritical())
                            .thenComparing(g -> -effectiveScore(g.risk())));
        return gaps;
    }

    // =======================================================================
    // 5. Owner accountability
    // =======================================================================

    public List<OwnerRow> buildOwnerReport(Long orgId, int appetiteThreshold) {
        List<Risk> risks = riskService.findAll(orgId);
        List<Task> tasks = taskService.findAll(orgId);
        LocalDate today = LocalDate.now();

        Map<Long, List<Task>> tasksByRisk = tasks.stream()
            .filter(t -> t.getRiskId() != null)
            .collect(Collectors.groupingBy(Task::getRiskId));

        Map<String, List<Risk>> byOwner = risks.stream()
            .collect(Collectors.groupingBy(
                r -> r.getRiskOwnerName() == null || r.getRiskOwnerName().isBlank() ? "" : r.getRiskOwnerName().trim(),
                LinkedHashMap::new, Collectors.toList()));

        List<OwnerRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<Risk>> e : byOwner.entrySet()) {
            List<Risk> owned = e.getValue().stream()
                .sorted(Comparator.comparing(ReportService::effectiveScore, Comparator.reverseOrder()))
                .toList();

            Integer maxResidual = owned.stream()
                .map(Risk::getResidualScore)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);

            long aboveAppetite = owned.stream()
                .filter(r -> r.getResidualScore() != null && r.getResidualScore() >= appetiteThreshold)
                .count();

            long reviewsOverdue = owned.stream().filter(Risk::isReviewOverdue).count();

            long openTasks = 0;
            long overdueTasks = 0;
            for (Risk r : owned) {
                List<Task> rt = tasksByRisk.getOrDefault(r.getId(), List.of());
                openTasks += rt.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).count();
                overdueTasks += rt.stream()
                    .filter(t -> t.getStatus() != TaskStatus.COMPLETED
                              && t.getDueDate() != null && t.getDueDate().isBefore(today))
                    .count();
            }

            rows.add(new OwnerRow(e.getKey(), owned, maxResidual, aboveAppetite,
                                  reviewsOverdue, openTasks, overdueTasks));
        }

        // Heaviest load first; unassigned pinned to the end where it reads as a gap
        rows.sort(Comparator.comparing(OwnerRow::isUnassigned)
                            .thenComparing(r -> -(r.maxResidual() == null ? 0 : r.maxResidual()))
                            .thenComparing(r -> -r.riskCount()));
        return rows;
    }

    // =======================================================================
    // 6. Change report for a period
    // =======================================================================

    public ChangeReport buildChangeReport(Long orgId, LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<AuditTrail> entries = auditTrailRepository
            .findByOrganizationIdAndCreatedAtBetweenOrderByCreatedAtDesc(orgId, fromInstant, toInstant);

        Map<Long, Risk> risksById = riskService.findAll(orgId).stream()
            .collect(Collectors.toMap(Risk::getId, r -> r, (a, b) -> a));

        List<ChangeEntry> mapped = new ArrayList<>();
        long created = 0, deleted = 0, updated = 0, scoreChanges = 0, tasksTouched = 0, taxonomy = 0;

        for (AuditTrail e : entries) {
            List<com.riskregister.riskregisterapp.dto.FieldChange> changes = auditTrailService.parseChanges(e);
            Risk risk = "Risk".equals(e.getEntityType()) ? risksById.get(e.getEntityId()) : null;

            ChangeEntry ce = new ChangeEntry(e, changes, entityLabel(e.getEntityType()),
                risk != null ? risk.getRiskId() : null,
                risk != null ? risk.getTitle() : null);
            mapped.add(ce);

            if ("Risk".equals(e.getEntityType())) {
                switch (e.getAction()) {
                    case "CREATED" -> created++;
                    case "DELETED" -> deleted++;
                    case "UPDATED" -> updated++;
                    default -> { }
                }
                if (!ce.scoreChanges().isEmpty()) scoreChanges++;
            } else if ("Task".equals(e.getEntityType())) {
                tasksTouched++;
            } else if ("RiskCategory".equals(e.getEntityType()) || "RiskSubcategory".equals(e.getEntityType())) {
                taxonomy++;
            }
        }

        return new ChangeReport(from, to, created, deleted, updated, scoreChanges, tasksTouched, taxonomy, mapped);
    }

    // =======================================================================
    // 7. Effectiveness trend
    // =======================================================================

    private static final DateTimeFormatter MONTH_FMT =
        DateTimeFormatter.ofPattern("MMM yyyy").withZone(ZoneId.systemDefault());

    public List<TrendPoint> buildEffectivenessTrend(Long orgId, int days) {
        List<EffectivenessScore> scores = effectivenessScoreService.getScoresForLastDays(days, orgId);
        return scores.stream()
            .map(s -> new TrendPoint(
                s.getCalculatedAt() != null ? MONTH_FMT.format(s.getCalculatedAt()) : "—",
                s.getScore(), s.getTotalInherentScore(), s.getTotalResidualScore(), s.getRiskCount()))
            .toList();
    }

    // =======================================================================
    // 8. Reviews due
    // =======================================================================

    public List<ReviewDueRow> buildReviewsDueReport(Long orgId, boolean overdueOnly) {
        List<Risk> risks = riskService.findAll(orgId);
        Map<Long, String> catNames = categoryNames();

        List<ReviewDueRow> rows = new ArrayList<>();
        for (Risk r : risks) {
            // Closed risks fall out of the review cycle
            if (r.getStatusId() != null && r.getStatusId() == 5L) continue;
            if (r.getReviewFrequency() == null) continue;

            boolean never = r.isNeverReviewed();
            Long overdue = r.getDaysOverdueForReview();
            if (overdueOnly && !r.isReviewOverdue()) continue;

            rows.add(new ReviewDueRow(r,
                r.getRiskCategoryId() != null ? catNames.getOrDefault(r.getRiskCategoryId(), "—") : "—",
                r.getReviewFrequency().getDisplayName(), overdue, never));
        }

        // Never-reviewed first, then most overdue
        rows.sort(Comparator.comparing((ReviewDueRow row) -> !row.neverReviewed())
                            .thenComparing(row -> -(row.daysOverdue() == null ? 0 : row.daysOverdue())));
        return rows;
    }

    // =======================================================================
    // 9. Risks above appetite
    // =======================================================================

    public List<Risk> buildAboveAppetiteReport(Long orgId, int threshold) {
        return riskService.findAll(orgId).stream()
            .filter(r -> r.getResidualScore() != null && r.getResidualScore() >= threshold)
            .sorted(Comparator.comparing(Risk::getResidualScore, Comparator.reverseOrder()))
            .toList();
    }

    // =======================================================================
    // 10. Issue aging
    // =======================================================================

    private static final List<String> AGE_BANDS =
        List.of("0–30 days", "31–90 days", "91–180 days", "181–365 days", "Over a year");

    /** Open issues bucketed by how long they have been open, oldest bands last. */
    public Map<String, List<Issue>> buildIssueAgingReport(Long orgId) {
        List<Issue> open = issueService.findOpen(orgId);
        Map<String, List<Issue>> banded = new LinkedHashMap<>();
        for (String band : AGE_BANDS) {
            banded.put(band, new ArrayList<>());
        }
        for (Issue issue : open) {
            banded.computeIfAbsent(issue.getAgeBand(), k -> new ArrayList<>()).add(issue);
        }
        // Oldest first within each band — those are the ones drifting
        banded.values().forEach(list -> list.sort(
            Comparator.comparing((Issue i) -> i.getAgeDays() == null ? 0L : i.getAgeDays()).reversed()));
        return banded;
    }

    // =======================================================================
    // 11. Issues by risk — the feedback loop between the two registers
    // =======================================================================

    /**
     * Risks that carry open findings against their controls, worst first. A risk with open
     * High/Critical issues has a residual score that assumes controls the evidence says
     * are not working.
     */
    public List<IssuesByRiskRow> buildIssuesByRiskReport(Long orgId) {
        List<Risk> risks = riskService.findAll(orgId);
        List<Issue> issues = issueService.findAll(orgId);
        Map<Long, String> catNames = categoryNames();

        List<IssuesByRiskRow> rows = new ArrayList<>();
        for (Risk risk : risks) {
            List<Issue> linked = issues.stream()
                .filter(i -> i.getLinkedRiskIdList().contains(risk.getId()))
                .sorted(Comparator.comparing(Issue::isClosed)
                                  .thenComparing(i -> -(i.getSeverityScore() == null ? 0 : i.getSeverityScore())))
                .toList();
            if (linked.isEmpty()) continue;

            long open = linked.stream().filter(i -> !i.isClosed()).count();
            long severe = linked.stream()
                .filter(i -> !i.isClosed())
                .filter(i -> i.getSeverityScore() != null && i.getSeverityScore() >= 10)
                .count();

            rows.add(new IssuesByRiskRow(risk,
                risk.getRiskCategoryId() != null ? catNames.getOrDefault(risk.getRiskCategoryId(), "—") : "—",
                linked, open, severe));
        }

        // Risks whose scores are most in doubt come first
        rows.sort(Comparator.comparing((IssuesByRiskRow r) -> -r.openSevere())
                            .thenComparing(r -> -r.openIssues()));
        return rows;
    }

    /** Unlinked open issues — findings not yet mapped to any risk. */
    public List<Issue> findUnlinkedOpenIssues(Long orgId) {
        return issueService.findOpen(orgId).stream()
            .filter(i -> i.getLinkedRiskIdList().isEmpty())
            .toList();
    }

    // =======================================================================
    // Shared helpers
    // =======================================================================

    /** Residual where set, otherwise inherent — the register's working exposure figure. */
    public static int effectiveScore(Risk r) {
        Integer s = r.getResidualScore() != null ? r.getResidualScore() : r.getInherentScore();
        return s != null ? s : 0;
    }

    public Map<Long, String> categoryNames() {
        return riskCategoryRepository.findAll().stream()
            .collect(Collectors.toMap(RiskCategory::getId, RiskCategory::getName, (a, b) -> a));
    }

    public Map<Long, String> subcategoryNames() {
        return riskSubcategoryRepository.findAll().stream()
            .collect(Collectors.toMap(RiskSubcategory::getId, RiskSubcategory::getName, (a, b) -> a));
    }

    /** Matches Risk.scoreToLevel banding so reports and the register agree. */
    public static String scoreLevel(int score) {
        if (score <= 2)  return "Very Low";
        if (score <= 4)  return "Low";
        if (score <= 9)  return "Medium";
        if (score <= 16) return "High";
        return "Very High";
    }

    public static String heatmapCellClass(int score) {
        if (score <= 2)  return "bg-green-100 text-green-800";
        if (score <= 4)  return "bg-yellow-100 text-yellow-800";
        if (score <= 9)  return "bg-orange-100 text-orange-800";
        if (score <= 16) return "bg-rose-100 text-rose-800";
        return "bg-red-200 text-red-900";
    }

    public static String scoreBadgeClass(Integer score) {
        if (score == null) return "bg-gray-100 text-gray-500";
        if (score <= 2)  return "bg-green-50 text-green-700 ring-green-600/20";
        if (score <= 4)  return "bg-yellow-50 text-yellow-700 ring-yellow-600/20";
        if (score <= 9)  return "bg-orange-50 text-orange-700 ring-orange-600/20";
        if (score <= 16) return "bg-rose-50 text-rose-700 ring-rose-600/20";
        return "bg-red-50 text-red-700 ring-red-600/20";
    }

    private static String treatmentLabel(RiskTreatment t) {
        return switch (t) {
            case AWAITING_ASSESSMENT -> "Awaiting Assessment";
            case ACCEPT   -> "Accept";
            case MITIGATE -> "Mitigate";
            case TRANSFER -> "Transfer";
            case AVOID    -> "Avoid";
        };
    }

    private static String treatmentBadgeClass(RiskTreatment t) {
        return switch (t) {
            case AWAITING_ASSESSMENT -> "bg-amber-100 text-amber-800";
            case ACCEPT   -> "bg-sky-100 text-sky-800";
            case MITIGATE -> "bg-green-100 text-green-800";
            case TRANSFER -> "bg-purple-100 text-purple-800";
            case AVOID    -> "bg-gray-200 text-gray-800";
        };
    }

    private static String entityLabel(String entityType) {
        if (entityType == null) return "—";
        return switch (entityType) {
            case "Risk"            -> "Risk";
            case "Task"            -> "Task";
            case "Asset"           -> "Asset";
            case "RiskCategory"    -> "Risk Category";
            case "RiskSubcategory" -> "Subcategory";
            default -> entityType;
        };
    }
}
