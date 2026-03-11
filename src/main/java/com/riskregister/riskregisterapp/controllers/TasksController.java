package com.riskregister.riskregisterapp.controllers;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.entities.Task;
import com.riskregister.riskregisterapp.entities.TaskUpdate;
import com.riskregister.riskregisterapp.enums.TaskStatus;
import com.riskregister.riskregisterapp.repositories.RiskRepository;
import com.riskregister.riskregisterapp.repositories.UserRepository;
import com.riskregister.riskregisterapp.services.AuditTrailService;
import com.riskregister.riskregisterapp.services.TaskService;

@Controller
public class TasksController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RiskRepository riskRepository;

    @GetMapping("/tasks")
    public String index(Model model) {
        List<Task> tasks = taskService.findAll();
        model.addAttribute("tasks", tasks);
        model.addAttribute("users", userRepository.findByApprovedTrueOrderByFirstNameAscLastNameAsc());
        Map<Long, Risk> riskMap = riskRepository.findAll().stream()
            .filter(r -> r.getDeletedAt() == null)
            .collect(Collectors.toMap(Risk::getId, r -> r));
        model.addAttribute("riskMap", riskMap);
        return "tasks/index";
    }

    @PostMapping("/risks/{riskId}/tasks")
    public String create(@PathVariable Long riskId,
                        @ModelAttribute Task task,
                        @RequestParam(value = "redirectTo", required = false) String redirectTo,
                        RedirectAttributes redirectAttrs,
                        Principal principal) {
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.BACKLOG);
        }
        task.setRiskId(riskId);
        task.setCreatedAt(Instant.now());
        if (principal != null) {
            task.setCreatedByEmail(principal.getName());
        }
        taskService.save(task);

        // Log creation
        String actorEmail = principal != null ? principal.getName() : "system";
        String actorName = getActorName(actorEmail);
        auditTrailService.logTaskCreated(task, actorEmail, actorName);

        redirectAttrs.addFlashAttribute("success", "Task created successfully.");
        return "redirect:" + (redirectTo != null ? redirectTo : "/risks/" + riskId + "?tab=tasks");
    }

    @PostMapping("/risks/{riskId}/tasks/{taskId}")
    public Object update(@PathVariable Long riskId,
                        @PathVariable Long taskId,
                        @ModelAttribute Task form,
                        @RequestParam(value = "redirectTo", required = false) String redirectTo,
                        @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                        RedirectAttributes redirectAttrs,
                        Principal principal) {
        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        // Snapshot old state
        Task oldSnapshot = snapshotTask(task);

        // Apply form fields
        task.setTitle(form.getTitle());
        task.setDescription(form.getDescription());
        task.setStatus(form.getStatus());
        task.setPriority(form.getPriority());
        task.setAssigneeId(form.getAssigneeId());
        task.setAssigneeName(form.getAssigneeName());
        task.setDueDate(form.getDueDate());
        task.setUpdatedAt(Instant.now());
        if (principal != null) {
            task.setUpdatedByEmail(principal.getName());
        }
        taskService.save(task);

        // Log changes
        String actorEmail = principal != null ? principal.getName() : "system";
        String actorName = getActorName(actorEmail);
        auditTrailService.logTaskUpdated(oldSnapshot, task, actorEmail, actorName);

        // Return JSON for AJAX requests, redirect for traditional form submissions
        if ("XMLHttpRequest".equals(requestedWith)) {
            return ResponseEntity.ok(task);
        }

        redirectAttrs.addFlashAttribute("success", "Task updated successfully.");
        return "redirect:" + (redirectTo != null ? redirectTo : "/risks/" + riskId + "?tab=tasks");
    }

    @PostMapping("/risks/{riskId}/tasks/{taskId}/delete")
    public String delete(@PathVariable Long riskId,
                        @PathVariable Long taskId,
                        @RequestParam(value = "redirectTo", required = false) String redirectTo,
                        RedirectAttributes redirectAttrs,
                        Principal principal) {
        Task task = taskService.findById(taskId).orElse(null);
        taskService.softDelete(taskId);

        if (task != null) {
            String actorEmail = principal != null ? principal.getName() : "system";
            String actorName = getActorName(actorEmail);
            auditTrailService.logTaskDeleted(task, actorEmail, actorName);
        }

        redirectAttrs.addFlashAttribute("success", "Task deleted successfully.");
        return "redirect:" + (redirectTo != null ? redirectTo : "/risks/" + riskId + "?tab=tasks");
    }

    @PostMapping("/risks/{riskId}/tasks/{taskId}/updates")
    public ResponseEntity<?> addUpdate(@PathVariable Long riskId,
                                       @PathVariable Long taskId,
                                       @RequestParam(value = "content", required = true) String content,
                                       Principal principal) {
        String authorId = principal != null ? principal.getName() : "system";
        String authorName = getActorName(authorId);
        TaskUpdate newUpdate = taskService.addUpdate(taskId, content, authorId, authorName);

        // NOTE: No audit trail for comments/updates
        return ResponseEntity.ok(newUpdate);
    }

    @GetMapping("/api/tasks/{taskId}/updates")
    public ResponseEntity<List<TaskUpdate>> getUpdates(@PathVariable Long taskId) {
        List<TaskUpdate> updates = taskService.findUpdatesByTask(taskId);
        return ResponseEntity.ok(updates);
    }

    @DeleteMapping("/risks/{riskId}/tasks/{taskId}/updates/{updateId}")
    public ResponseEntity<Map<String, String>> deleteUpdate(
            @PathVariable Long riskId,
            @PathVariable Long taskId,
            @PathVariable Long updateId) {
        taskService.deleteUpdate(updateId);
        return ResponseEntity.ok(Map.of("success", "true"));
    }

    private String getActorName(String email) {
        if (email == null || email.equals("system")) {
            return "System";
        }
        var user = userRepository.findByEmail(email);
        return user != null ? user.getDisplayName() : email;
    }

    private Task snapshotTask(Task t) {
        Task snap = new Task();
        snap.setId(t.getId());
        snap.setRiskId(t.getRiskId());
        snap.setTitle(t.getTitle());
        snap.setDescription(t.getDescription());
        snap.setStatus(t.getStatus());
        snap.setPriority(t.getPriority());
        snap.setAssigneeId(t.getAssigneeId());
        snap.setAssigneeName(t.getAssigneeName());
        snap.setDueDate(t.getDueDate());
        return snap;
    }
}
