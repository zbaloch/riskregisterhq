package com.riskregister.riskregisterapp.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riskregister.riskregisterapp.dto.TaxonomyCategoryView;
import com.riskregister.riskregisterapp.dto.TaxonomySubcategoryView;
import com.riskregister.riskregisterapp.entities.RiskCategory;
import com.riskregister.riskregisterapp.entities.RiskSubcategory;
import com.riskregister.riskregisterapp.repositories.RiskCategoryRepository;
import com.riskregister.riskregisterapp.repositories.RiskRepository;
import com.riskregister.riskregisterapp.repositories.RiskSubcategoryRepository;

/**
 * Manages the risk taxonomy (categories and their subcategories) edited in
 * Settings → Risk Taxonomy. All mutations are audit trailed, and deletes are
 * blocked while any risk (including soft-deleted ones) still references the
 * entry, so historical records never lose their category names.
 */
@Service
public class RiskTaxonomyService {

    @Autowired
    private RiskCategoryRepository riskCategoryRepository;

    @Autowired
    private RiskSubcategoryRepository riskSubcategoryRepository;

    @Autowired
    private RiskRepository riskRepository;

    @Autowired
    private AuditTrailService auditTrailService;

    // -----------------------------------------------------------------------
    // Read side
    // -----------------------------------------------------------------------

    /**
     * Categories (name order) with their subcategories and usage counts, for the admin accordion.
     * Displayed counts are this organisation's ACTIVE risks, so they agree with the register;
     * the separate referencing count is what governs whether an entry can be deleted.
     */
    public List<TaxonomyCategoryView> getTaxonomy(Long organizationId) {
        Map<Long, Long> categoryUsage = toCountMap(riskRepository.countActiveRisksGroupedByCategory(organizationId));
        Map<Long, Long> subcategoryUsage = toCountMap(riskRepository.countActiveRisksGroupedBySubcategory(organizationId));

        Map<Long, List<RiskSubcategory>> subsByCategory = riskSubcategoryRepository.findAllByOrderByNameAsc().stream()
            .filter(s -> s.getCategoryId() != null)
            .collect(Collectors.groupingBy(RiskSubcategory::getCategoryId));

        return riskCategoryRepository.findAllByOrderByNameAsc().stream()
            .map(cat -> new TaxonomyCategoryView(
                cat,
                categoryUsage.getOrDefault(cat.getId(), 0L),
                riskRepository.countByRiskCategoryId(cat.getId()),
                subsByCategory.getOrDefault(cat.getId(), List.of()).stream()
                    .map(sub -> new TaxonomySubcategoryView(
                        sub,
                        subcategoryUsage.getOrDefault(sub.getId(), 0L),
                        riskRepository.countByRiskSubcategoryId(sub.getId())))
                    .toList()))
            .toList();
    }

    /** Legacy subcategories that could not be matched to a parent category (shown as "Uncategorized"). */
    public List<TaxonomySubcategoryView> getUncategorizedSubcategories(Long organizationId) {
        Map<Long, Long> subcategoryUsage = toCountMap(riskRepository.countActiveRisksGroupedBySubcategory(organizationId));
        return riskSubcategoryRepository.findByCategoryIdIsNullOrderByNameAsc().stream()
            .map(sub -> new TaxonomySubcategoryView(
                sub,
                subcategoryUsage.getOrDefault(sub.getId(), 0L),
                riskRepository.countByRiskSubcategoryId(sub.getId())))
            .toList();
    }

    // -----------------------------------------------------------------------
    // Category CRUD
    // -----------------------------------------------------------------------

    @Transactional
    public RiskCategory createCategory(String name, String description,
                                       String actorEmail, String actorName, Long organizationId) {
        String cleanName = requireName(name, "Category");
        ensureCategoryNameAvailable(cleanName, null);

        RiskCategory category = new RiskCategory();
        category.setId(nextCategoryId());
        category.setName(cleanName);
        category.setDescription(clean(description));
        category = riskCategoryRepository.save(category);

        auditTrailService.logCategoryCreated(category, actorEmail, actorName, organizationId);
        return category;
    }

    @Transactional
    public RiskCategory updateCategory(Long id, String name, String description,
                                       String actorEmail, String actorName, Long organizationId) {
        RiskCategory category = riskCategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        String cleanName = requireName(name, "Category");
        ensureCategoryNameAvailable(cleanName, id);

        RiskCategory before = snapshot(category);
        category.setName(cleanName);
        category.setDescription(clean(description));
        category = riskCategoryRepository.save(category);

        auditTrailService.logCategoryUpdated(before, category, actorEmail, actorName, organizationId);
        return category;
    }

    @Transactional
    public void deleteCategory(Long id, String actorEmail, String actorName, Long organizationId) {
        RiskCategory category = riskCategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        long referencing = riskRepository.countByRiskCategoryId(id);
        if (referencing > 0) {
            long active = countActive(riskRepository.countActiveRisksGroupedByCategory(organizationId), id);
            throw new IllegalStateException(blockedMessage(category.getName(), active, referencing));
        }
        long subCount = riskSubcategoryRepository.countByCategoryId(id);
        if (subCount > 0) {
            throw new IllegalStateException("Cannot delete \"" + category.getName() + "\": it still has "
                + subCount + " subcategor" + (subCount == 1 ? "y" : "ies") + ". Delete or move them first.");
        }

        riskCategoryRepository.delete(category);
        auditTrailService.logCategoryDeleted(category, actorEmail, actorName, organizationId);
    }

    // -----------------------------------------------------------------------
    // Subcategory CRUD
    // -----------------------------------------------------------------------

    @Transactional
    public RiskSubcategory createSubcategory(String name, String description, Long categoryId,
                                             String actorEmail, String actorName, Long organizationId) {
        String cleanName = requireName(name, "Subcategory");
        requireCategory(categoryId);
        ensureSubcategoryNameAvailable(cleanName, null);

        RiskSubcategory sub = new RiskSubcategory();
        sub.setId(nextSubcategoryId());
        sub.setName(cleanName);
        sub.setDescription(clean(description));
        sub.setCategoryId(categoryId);
        sub = riskSubcategoryRepository.save(sub);

        auditTrailService.logSubcategoryCreated(sub, categoryNameMap(), actorEmail, actorName, organizationId);
        return sub;
    }

    @Transactional
    public RiskSubcategory updateSubcategory(Long id, String name, String description, Long categoryId,
                                             String actorEmail, String actorName, Long organizationId) {
        RiskSubcategory sub = riskSubcategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subcategory not found."));
        String cleanName = requireName(name, "Subcategory");
        requireCategory(categoryId);
        ensureSubcategoryNameAvailable(cleanName, id);

        RiskSubcategory before = snapshot(sub);
        sub.setName(cleanName);
        sub.setDescription(clean(description));
        sub.setCategoryId(categoryId);
        sub = riskSubcategoryRepository.save(sub);

        auditTrailService.logSubcategoryUpdated(before, sub, categoryNameMap(), actorEmail, actorName, organizationId);
        return sub;
    }

    @Transactional
    public void deleteSubcategory(Long id, String actorEmail, String actorName, Long organizationId) {
        RiskSubcategory sub = riskSubcategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subcategory not found."));

        long referencing = riskRepository.countByRiskSubcategoryId(id);
        if (referencing > 0) {
            long active = countActive(riskRepository.countActiveRisksGroupedBySubcategory(organizationId), id);
            throw new IllegalStateException(blockedMessage(sub.getName(), active, referencing));
        }

        riskSubcategoryRepository.delete(sub);
        auditTrailService.logSubcategoryDeleted(sub, actorEmail, actorName, organizationId);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Map<Long, String> categoryNameMap() {
        return riskCategoryRepository.findAll().stream()
            .collect(Collectors.toMap(RiskCategory::getId, RiskCategory::getName));
    }

    /** Pull one id's count out of a grouped-count result set. */
    private static long countActive(List<Object[]> rows, Long id) {
        return toCountMap(rows).getOrDefault(id, 0L);
    }

    /**
     * Explain a blocked delete. A entry can show zero active risks and still be undeletable
     * because deleted risks continue to reference it, so say so rather than leaving the user
     * staring at a "0 risks" badge and a refusal.
     */
    private static String blockedMessage(String name, long active, long referencing) {
        if (active > 0) {
            String msg = "Cannot delete \"" + name + "\": it is in use by "
                + active + " active risk" + (active == 1 ? "" : "s") + ". Reassign those risks first.";
            long hidden = referencing - active;
            if (hidden > 0) {
                msg += " A further " + hidden + " deleted risk record"
                     + (hidden == 1 ? "" : "s") + " also reference" + (hidden == 1 ? "s" : "") + " it.";
            }
            return msg;
        }
        return "Cannot delete \"" + name + "\": no active risks use it, but " + referencing
             + " deleted risk record" + (referencing == 1 ? "" : "s") + " still reference"
             + (referencing == 1 ? "s" : "") + " it. Removing it would leave those records without a category name.";
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    // Ids are assigned manually (seed data uses fixed ids), so new rows continue from the max.
    private Long nextCategoryId() {
        return riskCategoryRepository.findTopByOrderByIdDesc().map(c -> c.getId() + 1).orElse(1L);
    }

    private Long nextSubcategoryId() {
        return riskSubcategoryRepository.findTopByOrderByIdDesc().map(s -> s.getId() + 1).orElse(1L);
    }

    private void requireCategory(Long categoryId) {
        if (categoryId == null || !riskCategoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("Please choose a parent category.");
        }
    }

    private void ensureCategoryNameAvailable(String name, Long excludeId) {
        ensureNameAvailable(name, excludeId, riskCategoryRepository.findAll(),
            RiskCategory::getId, RiskCategory::getName, "category");
    }

    private void ensureSubcategoryNameAvailable(String name, Long excludeId) {
        ensureNameAvailable(name, excludeId, riskSubcategoryRepository.findAll(),
            RiskSubcategory::getId, RiskSubcategory::getName, "subcategory");
    }

    private static <T> void ensureNameAvailable(String name, Long excludeId, List<T> all,
                                                Function<T, Long> idOf, Function<T, String> nameOf, String kind) {
        boolean taken = all.stream().anyMatch(e ->
            !idOf.apply(e).equals(excludeId)
            && nameOf.apply(e) != null
            && nameOf.apply(e).trim().equalsIgnoreCase(name));
        if (taken) {
            throw new IllegalArgumentException("A " + kind + " named \"" + name + "\" already exists.");
        }
    }

    private static String requireName(String name, String kind) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(kind + " name is required.");
        }
        return name.trim();
    }

    private static String clean(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static RiskCategory snapshot(RiskCategory c) {
        RiskCategory copy = new RiskCategory();
        copy.setId(c.getId());
        copy.setName(c.getName());
        copy.setDescription(c.getDescription());
        return copy;
    }

    private static RiskSubcategory snapshot(RiskSubcategory s) {
        RiskSubcategory copy = new RiskSubcategory();
        copy.setId(s.getId());
        copy.setName(s.getName());
        copy.setDescription(s.getDescription());
        copy.setCategoryId(s.getCategoryId());
        return copy;
    }
}
