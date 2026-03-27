package com.riskregister.riskregisterapp.entities;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.riskregister.riskregisterapp.enums.TaskPriority;
import com.riskregister.riskregisterapp.enums.TaskStatus;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long riskId;  // FK to risks.id (bare Long, same pattern as riskCategoryId)

    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "riskId", insertable = false, updatable = false)
    private Risk risk;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority;

    private String assigneeId;    // User.id (UUID string), nullable
    private String assigneeName;  // Display name, stored at assignment time

    private LocalDate dueDate;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;    // null = active, soft delete pattern

    private String createdByEmail;
    private String updatedByEmail;

    // Computed getters (not persisted)

    public String getCreatedAtFormatted() {
        if (createdAt == null) return null;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                                                   .withZone(ZoneId.systemDefault());
        return fmt.format(createdAt);
    }

    public String getDueDateFormatted() {
        if (dueDate == null) return "";
        return dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    public String getStatusLabel() {
        if (status == null) return "";
        String raw = status.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public String getPriorityLabel() {
        if (priority == null) return "";
        String raw = priority.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
