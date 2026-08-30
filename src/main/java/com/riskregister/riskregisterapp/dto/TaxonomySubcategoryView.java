package com.riskregister.riskregisterapp.dto;

import com.riskregister.riskregisterapp.entities.RiskSubcategory;

/**
 * A subcategory row on the Risk Taxonomy admin page.
 *
 * @param riskCount        active risks in this organisation — the number shown to the user
 * @param referencingCount every risk still pointing at this subcategory, soft-deleted ones
 *                         included, which is what actually blocks a delete
 */
public record TaxonomySubcategoryView(
    RiskSubcategory subcategory,
    long riskCount,
    long referencingCount
) {
    public boolean deletable() {
        return referencingCount == 0;
    }

    /** References that exist but aren't visible in the register — deleted or other-org risks. */
    public long archivedCount() {
        return Math.max(0, referencingCount - riskCount);
    }

    public boolean hasHiddenReferences() {
        return archivedCount() > 0;
    }
}
