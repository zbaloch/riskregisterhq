package com.riskregister.riskregisterapp.dto;

import java.util.List;

import com.riskregister.riskregisterapp.entities.RiskCategory;

/**
 * A category group on the Risk Taxonomy admin page.
 *
 * @param riskCount        active risks in this organisation — the number shown to the user
 * @param referencingCount every risk still pointing at this category, soft-deleted ones
 *                         included, which is what actually blocks a delete
 */
public record TaxonomyCategoryView(
    RiskCategory category,
    long riskCount,
    long referencingCount,
    List<TaxonomySubcategoryView> subcategories
) {
    public boolean deletable() {
        return referencingCount == 0 && subcategories.isEmpty();
    }

    /** References that exist but aren't visible in the register — deleted or other-org risks. */
    public long archivedCount() {
        return Math.max(0, referencingCount - riskCount);
    }

    public boolean hasHiddenReferences() {
        return archivedCount() > 0;
    }
}
