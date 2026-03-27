package com.riskregister.riskregisterapp.controllers;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.riskregister.riskregisterapp.entities.CustomUserDetails;
import com.riskregister.riskregisterapp.entities.Task;
import com.riskregister.riskregisterapp.entities.User;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.riskregister.riskregisterapp.services.RiskService;
import com.riskregister.riskregisterapp.services.TaskService;
import com.riskregister.riskregisterapp.services.UserService;
import com.riskregister.riskregisterapp.services.EffectivenessScoreService;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private RiskService riskService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private EffectivenessScoreService effectivenessScoreService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String index(Model model, @ModelAttribute("currentUser") User currentUser, Principal principalUser) {
        // Handle unauthenticated users
        if (currentUser == null || currentUser.getOrganizationId() == null) {
            model.addAttribute("totalRisks", 0);
            model.addAttribute("highRisks", 0);
            model.addAttribute("risksByScoreLevel", java.util.Map.of());
            model.addAttribute("highestRisks", java.util.List.of());
            model.addAttribute("statusCounts", java.util.Map.of());
            model.addAttribute("categoryCounts", java.util.Map.of());
            model.addAttribute("riskScoreDistribution", java.util.List.of());
            model.addAttribute("risksByStatusAndCategory", java.util.Map.of());
            model.addAttribute("riskScoresByCategory", java.util.Map.of());
            model.addAttribute("riskHeatmaps", java.util.Map.of());
            model.addAttribute("totalTasks", 0);
            model.addAttribute("tasksInProgress", 0);
            model.addAttribute("overdueTasks", 0);
            model.addAttribute("taskCompletionRate", 0);
            model.addAttribute("tasksByStatus", java.util.Map.of());
            model.addAttribute("latestEffectivenessScore", null);
            model.addAttribute("effectivenessScores90Days", java.util.List.of());
            model.addAttribute("effectivenessScores1Year", java.util.List.of());
            model.addAttribute("effectivenessScores2Years", java.util.List.of());
            model.addAttribute("myTasks", java.util.List.of());
            return "index";
        }

        Long orgId = currentUser.getOrganizationId();

        // Risk metrics
        model.addAttribute("totalRisks", riskService.countActive(orgId));
        model.addAttribute("highRisks", riskService.countHighRisks(orgId));
        model.addAttribute("risksByScoreLevel", riskService.getRisksByScoreLevel(orgId));
        model.addAttribute("highestRisks", riskService.getHighestRisks(5, orgId));

        // Risk status and category
        model.addAttribute("statusCounts", riskService.countRisksByStatus(orgId));
        model.addAttribute("categoryCounts", riskService.countRisksByCategory(orgId));

        // Risk charts
        model.addAttribute("riskScoreDistribution", riskService.getRiskScoreDistribution(orgId));
        model.addAttribute("risksByStatusAndCategory", riskService.getRisksByStatusAndCategory(orgId));
        model.addAttribute("riskScoresByCategory", riskService.getRiskScoresByCategory(orgId));

        // Risk heatmaps (inherent and residual)
        model.addAttribute("riskHeatmaps", riskService.getRiskHeatmaps(orgId));

        // Task metrics
        model.addAttribute("totalTasks", taskService.countTotalTasks(orgId));
        model.addAttribute("tasksInProgress", taskService.countTasksInProgress(orgId));
        model.addAttribute("overdueTasks", taskService.countOverdueTasks(orgId));
        model.addAttribute("taskCompletionRate", taskService.getCompletionRate(orgId));
        model.addAttribute("tasksByStatus", taskService.getTasksByStatus(orgId));

        // Effectiveness score metrics
        model.addAttribute("latestEffectivenessScore", effectivenessScoreService.getLatestScore(orgId));
        model.addAttribute("effectivenessScores90Days", effectivenessScoreService.getScoresForLastDays(90, orgId));
        model.addAttribute("effectivenessScores1Year", effectivenessScoreService.getScoresForLastDays(365, orgId));
        model.addAttribute("effectivenessScores2Years", effectivenessScoreService.getScoresForLastDays(730, orgId));

        // My Tasks (assigned to current user)
        log.info("Fetching tasks for current user {}", principalUser != null ? principalUser.getName() : "anonymous");
        String currentUserId = userService.getUserId(principalUser.getName());
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // if (authentication != null && authentication.isAuthenticated()) {
        //     Object principal = authentication.getPrincipal();
        //     if (principal instanceof CustomUserDetails) {
        //         currentUserId = ((CustomUserDetails) principal).getUser().getId();
        //     }
        // }
        List<Task> tasks = taskService.getTasksByAssignee(currentUserId, orgId);
        log.info("Current user ID: {}, My Tasks count: {}", currentUserId, tasks.size());
        model.addAttribute("myTasks", currentUserId != null ? tasks : java.util.List.of());

        return "index";
    }
}
