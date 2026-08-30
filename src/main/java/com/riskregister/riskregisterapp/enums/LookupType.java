package com.riskregister.riskregisterapp.enums;

/**
 * Registry of the dropdown fields an administrator can manage from Settings → Managed Fields.
 *
 * <p>This is the single extension point: to make another field admin-managed, add a constant
 * here and seed its starting values in {@code DataInitializer}. The admin screen, CRUD
 * endpoints, usage counting and delete protection all iterate this enum, so no new table,
 * controller or template is needed.</p>
 */
public enum LookupType {

    RISK_CATEGORY(
        "Risk Category",
        "Risk Categories",
        "How risks are classified in the register. Required on every risk, and the main axis for grouping exposure when reporting.",
        null,   // no yes/no attribute for this field
        null,
        "Risks"),

    ASSET_TYPE(
        "Asset Type",
        "Asset Types",
        "How assets are classified in the register — hardware, software, data and so on. Shown when recording an asset and used to filter the asset picker.",
        null,   // no yes/no attribute for this field
        null,
        "Assets"),

    ISSUE_SOURCE(
        "Issue Source",
        "Issue Sources",
        "Where a finding came from. Shown when raising an issue and used to separate independently raised findings from self-identified ones.",
        "Independently raised",
        "Tick when the source sits outside the function being assessed — audit, or a regulator. Independent findings carry more weight in assurance reporting.",
        "Issues"),

    ISSUE_CATEGORY(
        "Issue Category",
        "Issue Categories",
        "The theme a finding belongs to. Required on every issue, and what groups findings together when reporting to a committee.",
        null,   // no yes/no attribute for this field
        null,
        "Issues"),

    ISSUE_DIMENSION(
        "Issue Impact Area",
        "Issue Impact Areas",
        "Which part of the business a finding harms if it stays unremediated — the same axis as risk dimensions, so exposure can be compared across both registers.",
        null,
        null,
        "Issues");

    private final String singularName;
    private final String pluralName;
    private final String description;
    private final String flagLabel;   // null = this type has no yes/no attribute
    private final String flagHelp;
    private final String usedByLabel; // which records consume this field, for the usage column

    LookupType(String singularName, String pluralName, String description,
               String flagLabel, String flagHelp, String usedByLabel) {
        this.singularName = singularName;
        this.pluralName = pluralName;
        this.description = description;
        this.flagLabel = flagLabel;
        this.flagHelp = flagHelp;
        this.usedByLabel = usedByLabel;
    }

    public String getCode()         { return name(); }
    public String getSingularName() { return singularName; }
    public String getPluralName()   { return pluralName; }
    public String getDescription()  { return description; }
    public String getFlagLabel()    { return flagLabel; }
    public String getFlagHelp()     { return flagHelp; }
    public String getUsedByLabel()  { return usedByLabel; }

    /** Whether this field carries the optional yes/no attribute described by {@link #getFlagLabel()}. */
    public boolean hasFlag() {
        return flagLabel != null;
    }

    public static LookupType fromCode(String code) {
        if (code == null) return null;
        for (LookupType t : values()) {
            if (t.name().equalsIgnoreCase(code)) return t;
        }
        return null;
    }
}
