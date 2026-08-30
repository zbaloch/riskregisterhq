package com.riskregister.riskregisterapp.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.riskregister.riskregisterapp.entities.Organization;
import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.repositories.OrganizationRepository;
import com.riskregister.riskregisterapp.services.ReportService;
import com.riskregister.riskregisterapp.services.RiskService;
import com.riskregister.riskregisterapp.services.TaskService;

/**
 * Risk register reports. Each report is its own page under /reports so it can be
 * printed or linked to on its own; /reports itself is the index of what is available.
 */
@Controller
@RequestMapping("/reports")
public class ReportsController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private RiskService riskService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private com.riskregister.riskregisterapp.services.IssueService issueService;

    @Autowired
    private com.riskregister.riskregisterapp.services.LookupService lookupService;

    // -----------------------------------------------------------------------
    // Index
    // -----------------------------------------------------------------------

    @GetMapping
    public String index(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        int threshold = threshold(orgId);
        List<Risk> risks = riskService.findAll(orgId);

        // Headline counts so the index tells the user which reports need attention today
        model.addAttribute("totalRisks", risks.size());
        model.addAttribute("awaitingAssessment", risks.stream()
            .filter(r -> r.getRiskTreatment() == com.riskregister.riskregisterapp.lookups.RiskTreatment.AWAITING_ASSESSMENT)
            .count());
        model.addAttribute("mitigationGaps", reportService.buildMitigationGapReport(orgId).size());
        model.addAttribute("reviewsOverdue", reportService.buildReviewsDueReport(orgId, true).size());
        model.addAttribute("aboveAppetite", reportService.buildAboveAppetiteReport(orgId, threshold).size());
        model.addAttribute("appetiteThreshold", threshold);
        model.addAttribute("ownerCount", reportService.buildOwnerReport(orgId, threshold).size());
        model.addAttribute("overdueTasks", taskService.countOverdueTasks(orgId));
        model.addAttribute("overdueIssues", issueService.countOverdue(orgId));
        model.addAttribute("scoresInDoubt", reportService.buildIssuesByRiskReport(orgId).stream()
            .filter(r -> r.scoreInDoubt())
            .count());
        return "reports/index";
    }

    // -----------------------------------------------------------------------
    // 1. Heatmap
    // -----------------------------------------------------------------------

    @GetMapping("/heatmap")
    public String heatmap(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("inherent", reportService.buildHeatmap(orgId, false));
        model.addAttribute("residual", reportService.buildHeatmap(orgId, true));
        model.addAttribute("reportTitle", "Risk Heatmap");
        model.addAttribute("reportSubtitle",
            "Inherent versus residual exposure across the 5x5 likelihood and impact matrix. Select a cell to list the risks in it.");
        return "reports/heatmap";
    }

    // -----------------------------------------------------------------------
    // 2. Risk reduction by category
    // -----------------------------------------------------------------------

    @GetMapping("/risk-reduction")
    public String riskReduction(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("rows", reportService.buildReductionReport(orgId));
        model.addAttribute("reportTitle", "Risk Reduction by Category");
        model.addAttribute("reportSubtitle",
            "Average inherent versus residual score per risk category — where controls are working, and where they are not.");
        return "reports/risk-reduction";
    }

    // -----------------------------------------------------------------------
    // 3. Treatment summary
    // -----------------------------------------------------------------------

    @GetMapping("/treatment")
    public String treatment(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("rows", reportService.buildTreatmentReport(orgId));
        model.addAttribute("reportService", reportService);
        model.addAttribute("reportTitle", "Treatment Decisions");
        model.addAttribute("reportSubtitle",
            "How every risk in the register has been dispositioned, with the assessment backlog broken out by age.");
        return "reports/treatment";
    }

    // -----------------------------------------------------------------------
    // 4. Mitigation gap
    // -----------------------------------------------------------------------

    @GetMapping("/mitigation-gap")
    public String mitigationGap(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("rows", reportService.buildMitigationGapReport(orgId));
        model.addAttribute("reportTitle", "Mitigation Gaps");
        model.addAttribute("reportSubtitle",
            "Risks marked for mitigation where nothing is actually happening — no tasks, stalled tasks, or every task overdue.");
        return "reports/mitigation-gap";
    }

    // -----------------------------------------------------------------------
    // 5. Owner accountability
    // -----------------------------------------------------------------------

    @GetMapping("/owners")
    public String owners(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        int threshold = threshold(orgId);
        model.addAttribute("rows", reportService.buildOwnerReport(orgId, threshold));
        model.addAttribute("appetiteThreshold", threshold);
        model.addAttribute("reportTitle", "Owner Accountability");
        model.addAttribute("reportSubtitle",
            "What each risk owner is carrying: exposure, risks above appetite, overdue reviews and open mitigation work.");
        return "reports/owners";
    }

    // -----------------------------------------------------------------------
    // 6. Change report
    // -----------------------------------------------------------------------

    @GetMapping("/changes")
    public String changes(Model model,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        // Default to the last full quarter of activity
        LocalDate toDate = to != null ? to : LocalDate.now();
        LocalDate fromDate = from != null ? from : toDate.minusMonths(3);
        if (fromDate.isAfter(toDate)) {
            LocalDate swap = fromDate;
            fromDate = toDate;
            toDate = swap;
        }

        model.addAttribute("report", reportService.buildChangeReport(orgId, fromDate, toDate));
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
        model.addAttribute("reportTitle", "Change Report");
        model.addAttribute("reportSubtitle",
            "Every recorded change to the register in the selected period, with the field-level before and after values.");
        return "reports/changes";
    }

    // -----------------------------------------------------------------------
    // 7. Effectiveness trend
    // -----------------------------------------------------------------------

    @GetMapping("/effectiveness")
    public String effectiveness(Model model,
                                @RequestParam(defaultValue = "730") int days,
                                @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        List<Risk> risks = riskService.findAll(orgId);
        int threshold = threshold(orgId);

        model.addAttribute("points", reportService.buildEffectivenessTrend(orgId, days));
        model.addAttribute("days", days);
        model.addAttribute("latest", reportService.buildEffectivenessTrend(orgId, days).stream().reduce((a, b) -> b).orElse(null));
        model.addAttribute("currentHighCount", risks.stream()
            .filter(r -> r.getResidualScore() != null && r.getResidualScore() >= threshold)
            .count());
        model.addAttribute("appetiteThreshold", threshold);
        model.addAttribute("reportTitle", "Control Effectiveness Trend");
        model.addAttribute("reportSubtitle",
            "Monthly effectiveness score alongside the exposure behind it, so the percentage is read in context.");
        return "reports/effectiveness";
    }

    // -----------------------------------------------------------------------
    // 8. Reviews due
    // -----------------------------------------------------------------------

    @GetMapping("/reviews-due")
    public String reviewsDue(Model model,
                             @RequestParam(defaultValue = "true") boolean overdueOnly,
                             @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("rows", reportService.buildReviewsDueReport(orgId, overdueOnly));
        model.addAttribute("overdueOnly", overdueOnly);
        model.addAttribute("reportTitle", "Reviews Due");
        model.addAttribute("reportSubtitle",
            "Risks past their review cycle, based on when each was last reviewed and how often it is meant to be.");
        return "reports/reviews-due";
    }

    // -----------------------------------------------------------------------
    // 9. Above appetite
    // -----------------------------------------------------------------------

    @GetMapping("/above-appetite")
    public String aboveAppetite(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        int threshold = threshold(orgId);
        model.addAttribute("risks", reportService.buildAboveAppetiteReport(orgId, threshold));
        model.addAttribute("categoryMap", reportService.categoryNames(orgId));
        model.addAttribute("appetiteThreshold", threshold);
        model.addAttribute("reportTitle", "Risks Above Appetite");
        model.addAttribute("reportSubtitle",
            "Risks whose residual score sits at or above the organisation's risk appetite threshold and need explicit sign-off.");
        return "reports/above-appetite";
    }

    // -----------------------------------------------------------------------
    // 10. Issue aging
    // -----------------------------------------------------------------------

    @GetMapping("/issue-aging")
    public String issueAging(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("bands", reportService.buildIssueAgingReport(orgId));
        model.addAttribute("sourceMap", lookupService.map(
            com.riskregister.riskregisterapp.enums.LookupType.ISSUE_SOURCE, orgId));
        model.addAttribute("openCount", issueService.countOpen(orgId));
        model.addAttribute("overdueCount", issueService.countOverdue(orgId));
        model.addAttribute("awaitingValidation", issueService.findAwaitingValidation(orgId).size());
        model.addAttribute("reportTitle", "Issue Aging");
        model.addAttribute("reportSubtitle",
            "How long open findings have been outstanding. Age and repeated date extensions are what regulators scrutinise first.");
        return "reports/issue-aging";
    }

    // -----------------------------------------------------------------------
    // 11. Issues by risk
    // -----------------------------------------------------------------------

    @GetMapping("/issues-by-risk")
    public String issuesByRisk(Model model, @ModelAttribute("currentUser") User currentUser) {
        Long orgId = orgId(currentUser);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("rows", reportService.buildIssuesByRiskReport(orgId));
        model.addAttribute("unlinked", reportService.findUnlinkedOpenIssues(orgId));
        model.addAttribute("reportTitle", "Issues by Risk");
        model.addAttribute("reportSubtitle",
            "Which risk assessments the current findings call into question. Open severe issues mean a residual score credits controls that are not working.");
        return "reports/issues-by-risk";
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Long orgId(User currentUser) {
        return currentUser != null ? currentUser.getOrganizationId() : null;
    }

    private int threshold(Long orgId) {
        Organization org = organizationRepository.findById(orgId).orElse(null);
        return org != null ? org.getEffectiveRiskAppetiteThreshold() : 15;
    }
}
