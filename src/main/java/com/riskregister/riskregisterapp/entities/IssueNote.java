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

/**
 * One comment on an issue's discussion thread.
 *
 * <p>Kept out of the audit trail: the trail records what changed about the finding, while this
 * is the conversation around it. Comments cannot be edited, and only their author can remove
 * one, so the thread stays usable as a record of who said what and when.</p>
 */
@Entity
@Table(name = "issue_notes")
@Getter
@Setter
public class IssueNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    private Long organizationId;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** The author's email — the stable identity used to decide who may delete a comment. */
    private String authorId;

    /** Display name captured at the time of writing, so it survives later renames. */
    private String authorName;

    private Instant createdAt;

    // --- Display helpers (not persisted) ---

    private static final DateTimeFormatter STAMP_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    public String getCreatedAtFormatted() {
        return createdAt != null ? STAMP_FMT.format(createdAt) : "";
    }

    /** Initials for the avatar bubble, derived from whatever name we captured. */
    public String getInitials() {
        if (authorName == null || authorName.isBlank()) return "?";
        String[] parts = authorName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
