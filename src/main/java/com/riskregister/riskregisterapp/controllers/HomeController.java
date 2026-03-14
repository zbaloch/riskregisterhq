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
    public String index(Model model, Principal principalUser) {
        // Risk metrics
        model.addAttribute("totalRisks", riskService.countActive());
        model.addAttribute("highRisks", riskService.countHighRisks());
        model.addAttribute("risksByScoreLevel", riskService.getRisksByScoreLevel());
        model.addAttribute("highestRisks", riskService.getHighestRisks(5));

        // Risk status and category
        model.addAttribute("statusCounts", riskService.countRisksByStatus());
        model.addAttribute("categoryCounts", riskService.countRisksByCategory());

        // Risk charts
        model.addAttribute("riskScoreDistribution", riskService.getRiskScoreDistribution());
        model.addAttribute("risksByStatusAndCategory", riskService.getRisksByStatusAndCategory());
        model.addAttribute("riskScoresByCategory", riskService.getRiskScoresByCategory());

        // Risk heatmaps (inherent and residual)
        model.addAttribute("riskHeatmaps", riskService.getRiskHeatmaps());

        // Task metrics
        model.addAttribute("totalTasks", taskService.countTotalTasks());
        model.addAttribute("tasksInProgress", taskService.countTasksInProgress());
        model.addAttribute("overdueTasks", taskService.countOverdueTasks());
        model.addAttribute("taskCompletionRate", taskService.getCompletionRate());
        model.addAttribute("tasksByStatus", taskService.getTasksByStatus());

        // Effectiveness score metrics
        model.addAttribute("latestEffectivenessScore", effectivenessScoreService.getLatestScore());
        model.addAttribute("effectivenessScores90Days", effectivenessScoreService.getScoresForLastDays(90));
        model.addAttribute("effectivenessScores1Year", effectivenessScoreService.getScoresForLastDays(365));
        model.addAttribute("effectivenessScores2Years", effectivenessScoreService.getScoresForLastDays(730));

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
        List<Task> tasks = taskService.getTasksByAssignee(currentUserId);
        log.info("Current user ID: {}, My Tasks count: {}", currentUserId, tasks.size());
        model.addAttribute("myTasks", currentUserId != null ? tasks : java.util.List.of());

        return "index";
    }
}
