package com.riskregister.riskregisterapp.dto.reports;

import java.util.List;

/**
 * Average inherent vs residual exposure for one taxonomy group (a category, or a
 * subcategory nested under it). Shows whether controls are actually moving the needle.
 */
public record ReductionRow(
    String name,
    boolean isCategory,          // true = category header row, false = subcategory row
    long riskCount,
    Double avgInherent,          // null when nothing in the group is scored
    Double avgResidual,
    List<ReductionRow> children  // subcategory rows; empty for subcategory rows themselves
) {
    /** Absolute drop from inherent to residual, or null when either side is unscored. */
    public Double reduction() {
        if (avgInherent == null || avgResidual == null) return null;
        return avgInherent - avgResidual;
    }

    /** Percentage of inherent exposure removed by controls, or null when not computable. */
    public Double reductionPct() {
        if (avgInherent == null || avgResidual == null || avgInherent == 0) return null;
        return ((avgInherent - avgResidual) / avgInherent) * 100.0;
    }

    /** Rounded helpers so templates stay free of formatting logic. */
    public String avgInherentLabel() { return format(avgInherent); }
    public String avgResidualLabel() { return format(avgResidual); }
    public String reductionLabel()   { return format(reduction()); }

    public String reductionPctLabel() {
        Double pct = reductionPct();
        return pct == null ? "—" : Math.round(pct) + "%";
    }

    /** Controls that increase exposure, or barely move it, deserve attention. */
    public boolean isWeak() {
        Double pct = reductionPct();
        return pct != null && pct < 20.0;
    }

    private static String format(Double v) {
        return v == null ? "—" : String.format("%.1f", v);
    }
}
