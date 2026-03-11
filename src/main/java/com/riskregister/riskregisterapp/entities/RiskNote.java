package com.riskregister.riskregisterapp.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "risk_notes")
@Getter
@Setter
public class RiskNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_id", nullable = false)
    private Long riskId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id")
    private String authorId;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getCreatedAtFormatted() {
        if (createdAt == null) return "";
        LocalDateTime localDateTime = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a");
        return localDateTime.format(formatter);
    }
}
