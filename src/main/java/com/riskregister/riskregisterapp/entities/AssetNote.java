package com.riskregister.riskregisterapp.entities;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "asset_notes")
@Getter
@Setter
public class AssetNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String authorName;
    private String authorEmail;

    private Instant createdAt;

    // Computed getter for formatted creation date
    public String getCreatedAtFormatted() {
        if (createdAt == null) return "—";
        LocalDateTime ldt = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault());
        return ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));
    }
}
