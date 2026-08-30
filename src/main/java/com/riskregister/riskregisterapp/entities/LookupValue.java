package com.riskregister.riskregisterapp.entities;

import java.time.Instant;

import com.riskregister.riskregisterapp.enums.LookupType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * One selectable option of an admin-managed dropdown field (see {@link LookupType}).
 *
 * <p>Records store the {@link #code}, not the id or the display name, so an administrator can
 * rename an option without rewriting history. The code is fixed at creation for that reason.</p>
 */
@Entity
@Table(name = "lookup_values")
@Getter
@Setter
public class LookupValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Which managed field this option belongs to — a {@link LookupType} name. */
    private String lookupType;

    /** Stable machine key stored on records. Set once at creation and never edited. */
    private String code;

    /** Display label. Safe to change at any time; existing records follow it. */
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer sortOrder;

    /** Inactive options stay resolvable on existing records but disappear from pickers. */
    private Boolean active;

    /** Optional yes/no attribute whose meaning is defined by the LookupType's flag label. */
    private Boolean flagValue;

    /** Seeded with the product. Can be renamed or deactivated, but is restored if absent. */
    private Boolean systemDefault;

    private Long organizationId;

    private Instant createdAt;
    private Instant updatedAt;

    // --- Convenience accessors (not persisted) ---

    public boolean isActive() {
        return !Boolean.FALSE.equals(active);
    }

    public boolean isFlagged() {
        return Boolean.TRUE.equals(flagValue);
    }

    public boolean isSystemDefault() {
        return Boolean.TRUE.equals(systemDefault);
    }

    public LookupType getType() {
        return LookupType.fromCode(lookupType);
    }
}
