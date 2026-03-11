package com.riskregister.riskregisterapp.entities;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "task_updates")
@Getter
@Setter
public class TaskUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;  // FK to tasks.id

    @Column(columnDefinition = "TEXT")
    private String content;

    private String authorId;    // User.id (UUID)
    private String authorName;

    private Instant createdAt;

    // Computed getter (not persisted)

    public String getCreatedAtFormatted() {
        if (createdAt == null) return null;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")
                                                   .withZone(ZoneId.systemDefault());
        return fmt.format(createdAt);
    }
}
