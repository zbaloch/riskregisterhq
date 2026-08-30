package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riskregister.riskregisterapp.entities.Issue;
import com.riskregister.riskregisterapp.entities.LookupValue;
import com.riskregister.riskregisterapp.enums.LookupType;
import com.riskregister.riskregisterapp.repositories.IssueRepository;
import com.riskregister.riskregisterapp.repositories.LookupValueRepository;

/**
 * Manages the options behind admin-editable dropdown fields.
 *
 * <p>Everything here is driven by {@link LookupType}, so a new managed field needs a new enum
 * constant and seed rows — not new code. The one type-specific piece is
 * {@link #usageCounts(LookupType, Long)}, which has to know which records consume the field.</p>
 */
@Service
public class LookupService {

    @Autowired
    private LookupValueRepository lookupValueRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private com.riskregister.riskregisterapp.repositories.RiskRepository riskRepository;

    @Autowired
    private com.riskregister.riskregisterapp.repositories.AssetRepository assetRepository;

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /** Every option for a field, active and inactive, in display order. For the admin screen. */
    public List<LookupValue> findAll(LookupType type, Long organizationId) {
        return lookupValueRepository
            .findByOrganizationIdAndLookupTypeOrderBySortOrderAscNameAsc(organizationId, type.name());
    }

    /** Selectable options only — what a picker on a form should offer. */
    public List<LookupValue> findActive(LookupType type, Long organizationId) {
        return findAll(type, organizationId).stream().filter(LookupValue::isActive).toList();
    }

    /**
     * Options for a form that is editing an existing record: the active ones, plus the
     * record's current value even if it has since been deactivated, so editing something
     * else on that record cannot silently blank the field.
     */
    public List<LookupValue> findActiveIncluding(LookupType type, Long organizationId, String currentCode) {
        return findAll(type, organizationId).stream()
            .filter(v -> v.isActive() || (currentCode != null && currentCode.equals(v.getCode())))
            .toList();
    }

    /** code → option, for resolving stored codes to display labels in templates. */
    public Map<String, LookupValue> map(LookupType type, Long organizationId) {
        Map<String, LookupValue> map = new LinkedHashMap<>();
        for (LookupValue v : findAll(type, organizationId)) {
            map.put(v.getCode(), v);
        }
        return map;
    }

    public Optional<LookupValue> find(LookupType type, Long organizationId, String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return lookupValueRepository.findByOrganizationIdAndLookupTypeAndCode(organizationId, type.name(), code);
    }

    // -----------------------------------------------------------------------
    // Usage — the one place that knows what consumes each field
    // -----------------------------------------------------------------------

    /** How many live records use each option code, so the admin screen can warn before deletion. */
    public Map<String, Long> usageCounts(LookupType type, Long organizationId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (type == LookupType.RISK_CATEGORY) {
            // Deleted risks still carry the code, so they count for delete protection
            for (com.riskregister.riskregisterapp.entities.Risk risk : riskRepository.findAll()) {
                if (!organizationId.equals(risk.getOrganizationId())) continue;
                String code = risk.getRiskCategory();
                if (code == null || code.isBlank()) continue;
                counts.merge(code, 1L, Long::sum);
            }
        } else if (type == LookupType.ASSET_TYPE) {
            // Deleted assets still carry the code, so they count for delete protection
            for (com.riskregister.riskregisterapp.entities.Asset asset : assetRepository.findAll()) {
                if (!organizationId.equals(asset.getOrganizationId())) continue;
                String code = asset.getType();
                if (code == null || code.isBlank()) continue;
                counts.merge(code, 1L, Long::sum);
            }
        } else if (type == LookupType.ISSUE_SOURCE
                || type == LookupType.ISSUE_CATEGORY
                || type == LookupType.ISSUE_DIMENSION) {
            // Deleted issues still carry the code, so they count for delete protection
            for (Issue issue : issueRepository.findAll()) {
                if (!organizationId.equals(issue.getOrganizationId())) continue;
                String code = switch (type) {
                    case ISSUE_SOURCE    -> issue.getSource();
                    case ISSUE_CATEGORY  -> issue.getCategory();
                    case ISSUE_DIMENSION -> issue.getDimension();
                    default -> null;   // not an issue field
                };
                if (code == null || code.isBlank()) continue;
                counts.merge(code, 1L, Long::sum);
            }
        }
        return counts;
    }

    // -----------------------------------------------------------------------
    // Mutations
    // -----------------------------------------------------------------------

    @Transactional
    public LookupValue create(LookupType type, Long organizationId,
                              String name, String description, boolean flagValue) {
        String cleanName = requireName(name);
        ensureNameAvailable(type, organizationId, cleanName, null);

        LookupValue value = new LookupValue();
        value.setLookupType(type.name());
        value.setCode(generateCode(type, organizationId, cleanName));
        value.setName(cleanName);
        value.setDescription(clean(description));
        value.setFlagValue(type.hasFlag() && flagValue);
        value.setActive(true);
        value.setSystemDefault(false);
        value.setOrganizationId(organizationId);
        value.setSortOrder(nextSortOrder(type, organizationId));
        value.setCreatedAt(Instant.now());
        value.setUpdatedAt(Instant.now());
        return lookupValueRepository.save(value);
    }

    /** Rename or re-describe an option. The code is deliberately not editable. */
    @Transactional
    public LookupValue update(Long organizationId, Long id,
                              String name, String description, boolean flagValue) {
        LookupValue value = require(organizationId, id);
        LookupType type = value.getType();
        String cleanName = requireName(name);
        ensureNameAvailable(type, organizationId, cleanName, id);

        value.setName(cleanName);
        value.setDescription(clean(description));
        if (type != null && type.hasFlag()) {
            value.setFlagValue(flagValue);
        }
        value.setUpdatedAt(Instant.now());
        return lookupValueRepository.save(value);
    }

    /**
     * Turn an option on or off. Deactivating always succeeds — it hides the option from
     * pickers while leaving existing records resolvable, which is why it is the safe
     * alternative to deleting something already in use.
     */
    @Transactional
    public LookupValue setActive(Long organizationId, Long id, boolean active) {
        LookupValue value = require(organizationId, id);
        if (!active) {
            LookupType type = value.getType();
            long remaining = findAll(type, organizationId).stream()
                .filter(LookupValue::isActive)
                .filter(v -> !v.getId().equals(id))
                .count();
            if (remaining == 0) {
                throw new IllegalStateException(
                    "Cannot deactivate the last remaining option — the field would have nothing to choose from.");
            }
        }
        value.setActive(active);
        value.setUpdatedAt(Instant.now());
        return lookupValueRepository.save(value);
    }

    @Transactional
    public LookupValue delete(Long organizationId, Long id) {
        LookupValue value = require(organizationId, id);
        LookupType type = value.getType();

        long inUse = usageCounts(type, organizationId).getOrDefault(value.getCode(), 0L);
        if (inUse > 0) {
            String records = type != null ? type.getUsedByLabel().toLowerCase(Locale.ROOT) : "records";
            throw new IllegalStateException("Cannot delete \"" + value.getName() + "\": "
                + inUse + " " + records + " still use it. Deactivate it instead — that hides it from "
                + "new records while existing ones keep their value.");
        }
        // Supplied options are deletable like any other; only an empty field is disallowed,
        // since a required picker with nothing in it would block record creation outright.
        if (findAll(type, organizationId).size() <= 1) {
            throw new IllegalStateException(
                "Cannot delete the last remaining option — the field would have nothing to choose from. "
                + "Add a replacement first, then delete this one.");
        }

        lookupValueRepository.delete(value);
        return value;
    }

    /** Move an option up or down the picker order. */
    @Transactional
    public void move(Long organizationId, Long id, int direction) {
        LookupValue value = require(organizationId, id);
        List<LookupValue> ordered = findAll(value.getType(), organizationId);

        int index = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(id)) { index = i; break; }
        }
        int target = index + direction;
        if (index < 0 || target < 0 || target >= ordered.size()) return; // already at the end

        // Renumber the whole list so ordering stays stable even if seeds shared a sort order
        LookupValue swap = ordered.get(target);
        ordered.set(target, value);
        ordered.set(index, swap);
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setSortOrder(i);
            ordered.get(i).setUpdatedAt(Instant.now());
        }
        lookupValueRepository.saveAll(ordered);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private LookupValue require(Long organizationId, Long id) {
        return lookupValueRepository.findByOrganizationIdAndId(organizationId, id)
            .orElseThrow(() -> new IllegalArgumentException("Option not found."));
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        return name.trim();
    }

    private void ensureNameAvailable(LookupType type, Long organizationId, String name, Long excludeId) {
        boolean taken = findAll(type, organizationId).stream()
            .anyMatch(v -> !v.getId().equals(excludeId)
                        && v.getName() != null
                        && v.getName().trim().equalsIgnoreCase(name));
        if (taken) {
            throw new IllegalArgumentException("An option named \"" + name + "\" already exists for this field.");
        }
    }

    /** Derive a stable UPPER_SNAKE code from the name, de-duplicated with a numeric suffix. */
    private String generateCode(LookupType type, Long organizationId, String name) {
        String base = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
        if (base.isEmpty()) base = "OPTION";
        if (base.length() > 50) base = base.substring(0, 50);

        List<LookupValue> existing = findAll(type, organizationId);
        String candidate = base;
        int suffix = 2;
        while (codeTaken(existing, candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private static boolean codeTaken(List<LookupValue> existing, String code) {
        return existing.stream().anyMatch(v -> code.equalsIgnoreCase(v.getCode()));
    }

    private int nextSortOrder(LookupType type, Long organizationId) {
        return findAll(type, organizationId).stream()
            .map(v -> v.getSortOrder() == null ? 0 : v.getSortOrder())
            .max(Integer::compareTo)
            .orElse(-1) + 1;
    }

    private static String clean(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
