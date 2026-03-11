package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.Task;
import com.riskregister.riskregisterapp.entities.TaskUpdate;
import com.riskregister.riskregisterapp.enums.TaskStatus;
import com.riskregister.riskregisterapp.repositories.TaskRepository;
import com.riskregister.riskregisterapp.repositories.TaskUpdateRepository;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskUpdateRepository taskUpdateRepository;

    public List<Task> findAllByRisk(Long riskId) {
        return taskRepository.findByRiskIdAndDeletedAtIsNullOrderByCreatedAtDesc(riskId);
    }

    public List<Task> findAll() {
        return taskRepository.findAllWithRiskAndDeletedAtIsNull();
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findByIdAndDeletedAtIsNull(id);
    }

    public Task save(Task task) {
        return taskRepository.save(task);
    }

    public void softDelete(Long id) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            task.setDeletedAt(Instant.now());
            taskRepository.save(task);
        }
    }

    public TaskUpdate addUpdate(Long taskId, String content, String authorId, String authorName) {
        TaskUpdate update = new TaskUpdate();
        update.setTaskId(taskId);
        update.setContent(content);
        update.setAuthorId(authorId);
        update.setAuthorName(authorName);
        update.setCreatedAt(Instant.now());
        return taskUpdateRepository.save(update);
    }

    public List<TaskUpdate> findUpdatesByTask(Long taskId) {
        return taskUpdateRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public void deleteUpdate(Long updateId) {
        taskUpdateRepository.deleteById(updateId);
    }

    public Map<String, Long> getTasksByStatus() {
        List<Task> allTasks = findAll();
        Map<String, Long> statusCounts = new java.util.LinkedHashMap<>();

        long backlog = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.BACKLOG).count();
        long inProgress = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long completed = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

        statusCounts.put("Backlog", backlog);
        statusCounts.put("In Progress", inProgress);
        statusCounts.put("Completed", completed);

        return statusCounts;
    }

    public long countOverdueTasks() {
        LocalDate today = LocalDate.now();
        return findAll().stream()
            .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getDueDate() != null && t.getDueDate().isBefore(today))
            .count();
    }

    public List<Task> getOverdueTasks(int limit) {
        LocalDate today = LocalDate.now();
        return findAll().stream()
            .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getDueDate() != null && t.getDueDate().isBefore(today))
            .sorted((t1, t2) -> {
                if (t1.getDueDate() == null) return 1;
                if (t2.getDueDate() == null) return -1;
                return t1.getDueDate().compareTo(t2.getDueDate());
            })
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    public long getCompletionRate() {
        List<Task> allTasks = findAll();
        if (allTasks.isEmpty()) return 0;
        long completed = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        return (completed * 100) / allTasks.size();
    }

    public long countTasksInProgress() {
        return findAll().stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
    }

    public long countTotalTasks() {
        return findAll().size();
    }

    public List<Task> getTasksByAssignee(String assigneeId) {
        if (assigneeId == null || assigneeId.isEmpty()) {
            return List.of();
        }
        return taskRepository.findByAssigneeIdWithRiskAndDeletedAtIsNullOrderByCreatedAtDesc(assigneeId);
    }
}
