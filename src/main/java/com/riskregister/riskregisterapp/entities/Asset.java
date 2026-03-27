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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assets")
@Getter
@Setter
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String type; // Hardware, Software, Data, Service, Facility, People

    @Column(nullable = false)
    private String status; // Active, Retired, Archived

    private String location;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String ownerEmail; // Stores the assigned user's email
    private String ownerName;  // Snapshot of name at assignment time

    @Column(nullable = false)
    private Integer confidentiality; // 1-5 scale

    @Column(nullable = false)
    private Integer integrity; // 1-5 scale

    @Column(nullable = false)
    private Integer availability; // 1-5 scale

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt; // null = active, soft-delete pattern

    private String createdByEmail;
    private String updatedByEmail;

    private Long organizationId;

    // Computed getter for CIA rating display
    public String getCiaRating() {
        return String.format("C:%d I:%d A:%d",
            confidentiality != null ? confidentiality : 0,
            integrity != null ? integrity : 0,
            availability != null ? availability : 0);
    }

    // Computed getter for created date display
    public String getCreatedAtFormatted() {
        if (createdAt == null) return "—";
        LocalDateTime ldt = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault());
        return ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    // Computed getter for type color classes (for use in Thymeleaf)
    public String getTypeColorClasses() {
        return getColorClassesForString(type);
    }

    // Helper method to generate consistent colors from strings
    private static String getColorClassesForString(String str) {
        if (str == null) return "bg-gray-50 text-gray-700 border border-gray-100";

        String[] colors = {
            "bg-blue-50 text-blue-700 border border-blue-100",
            "bg-indigo-50 text-indigo-700 border border-indigo-100",
            "bg-purple-50 text-purple-700 border border-purple-100",
            "bg-pink-50 text-pink-700 border border-pink-100",
            "bg-orange-50 text-orange-700 border border-orange-100",
            "bg-amber-50 text-amber-700 border border-amber-100",
            "bg-teal-50 text-teal-700 border border-teal-100",
            "bg-cyan-50 text-cyan-700 border border-cyan-100",
            "bg-emerald-50 text-emerald-700 border border-emerald-100",
            "bg-lime-50 text-lime-700 border border-lime-100",
        };

        int hash = Math.abs(str.hashCode());
        int index = hash % colors.length;
        return colors[index];
    }
}
