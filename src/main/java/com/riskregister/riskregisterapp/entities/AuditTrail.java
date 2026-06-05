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
@Table(name = "audit_trails")
@Getter
@Setter
public class AuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Generic entity pointer — allows reuse for Tasks, Documents, Assets, etc.
    private String entityType;   // e.g. "Risk", "Task", "Document"
    private Long   entityId;     // FK value (not a real JPA FK — intentional for generic use)

    // Action: "CREATED", "UPDATED", "DELETED"
    private String action;

    // Human-readable one-line summary stored at write time, e.g.:
    // "Risk updated (RISK-001): Title, Inherent Impact, Review Frequency"
    @Column(columnDefinition = "TEXT")
    private String summary;

    // JSON array of field-level changes — null for CREATED/DELETED actions.
    // Structure: [{"field":"Title","label":"Title","oldValue":"Foo","newValue":"Bar"}, ...]
    @Column(columnDefinition = "TEXT")
    private String changesJson;

    // Who and when
    private String actorEmail;
    private String actorName;  // Full name of the user who performed the action
    private Instant createdAt;

    private Long organizationId;

    // --- Display helpers (not persisted) ---

    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")
                         .withZone(ZoneId.systemDefault());

    public String getCreatedAtFormatted() {
        return createdAt != null ? DISPLAY_FMT.format(createdAt) : null;
    }

    public String getActorShortName() {
        if (actorName == null || actorName.isBlank()) return actorName;
        int space = actorName.indexOf(' ');
        if (space < 0 || space == actorName.length() - 1) return actorName;
        return actorName.substring(0, space) + " " + actorName.charAt(space + 1) + ".";
    }
}
