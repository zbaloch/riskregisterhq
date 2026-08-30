package com.riskregister.riskregisterapp.config;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.riskregister.riskregisterapp.entities.Asset;
import com.riskregister.riskregisterapp.entities.AuditTrail;
import com.riskregister.riskregisterapp.entities.EffectivenessScore;
import com.riskregister.riskregisterapp.entities.LookupValue;
import com.riskregister.riskregisterapp.entities.Organization;
import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.entities.RiskCategory;
import com.riskregister.riskregisterapp.entities.RiskDimension;
import com.riskregister.riskregisterapp.entities.RiskStatus;
import com.riskregister.riskregisterapp.entities.Role;
import com.riskregister.riskregisterapp.entities.Task;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.enums.LookupType;
import com.riskregister.riskregisterapp.enums.RiskReviewFrequency;
import com.riskregister.riskregisterapp.lookups.RiskTreatment;
import com.riskregister.riskregisterapp.repositories.AssetRepository;
import com.riskregister.riskregisterapp.repositories.AuditTrailRepository;
import com.riskregister.riskregisterapp.repositories.EffectivenessScoreRepository;
import com.riskregister.riskregisterapp.repositories.OrganizationRepository;
import com.riskregister.riskregisterapp.repositories.RiskCategoryRepository;
import com.riskregister.riskregisterapp.repositories.RiskDimensionRepository;
import com.riskregister.riskregisterapp.repositories.RiskRepository;
import com.riskregister.riskregisterapp.repositories.RiskStatusRepository;
import com.riskregister.riskregisterapp.repositories.TaskRepository;
import com.riskregister.riskregisterapp.repositories.UserRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private RiskCategoryRepository riskCategoryRepository;

    @Autowired
    private RiskDimensionRepository riskDimensionRepository;

    @Autowired
    private RiskStatusRepository riskStatusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RiskRepository riskRepository;

    @Autowired
    private EffectivenessScoreRepository effectivenessScoreRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AuditTrailRepository auditTrailRepository;

    @Autowired
    private com.riskregister.riskregisterapp.repositories.LookupValueRepository lookupValueRepository;

    @Override
    public void run(ApplicationArguments args) {
        ensureDefaultOrganization();
        fixUserNames();
        migrateRiskCategoriesToLookup();
        backfillLastReviewedAt();
        backfillRiskAppetiteThreshold();
        seedRiskDimensions();
        seedRiskStatuses();
        seedLookupValues();
        // seedAdminUser();
        // seedEffectivenessScores();
    }

    private void ensureDefaultOrganization() {
        // If any organization already exists, skip — migration already ran
        if (organizationRepository.count() > 0) return;

        // Create the default org
        Organization defaultOrg = new Organization();
        defaultOrg.setName("Default Organization");
        defaultOrg.setDescription("Migrated from single-tenant setup");
        defaultOrg.setCreatedAt(Instant.now());
        defaultOrg.setUpdatedAt(Instant.now());
        defaultOrg = organizationRepository.save(defaultOrg);

        final Long orgId = defaultOrg.getId();

        // Assign all existing users
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setOrganizationId(orgId));
        userRepository.saveAll(users);

        // Assign all existing risks
        List<Risk> risks = riskRepository.findAll();
        risks.forEach(r -> r.setOrganizationId(orgId));
        riskRepository.saveAll(risks);

        // Assign all existing tasks
        List<Task> tasks = taskRepository.findAll();
        tasks.forEach(t -> t.setOrganizationId(orgId));
        taskRepository.saveAll(tasks);

        // Assign all existing assets
        List<Asset> assets = assetRepository.findAll();
        assets.forEach(a -> a.setOrganizationId(orgId));
        assetRepository.saveAll(assets);

        // Assign all existing audit trails
        List<AuditTrail> audits = auditTrailRepository.findAll();
        audits.forEach(a -> a.setOrganizationId(orgId));
        auditTrailRepository.saveAll(audits);

        // Assign all existing effectiveness scores
        List<EffectivenessScore> scores = effectivenessScoreRepository.findAll();
        scores.forEach(s -> s.setOrganizationId(orgId));
        effectivenessScoreRepository.saveAll(scores);
    }

    private void fixUserNames() {
        List<User> users = userRepository.findAll();
        boolean updated = false;
        for (User user : users) {
            if (user.getFirstName() == null || user.getFirstName().isEmpty()) {
                user.setFirstName("");
                updated = true;
            }
            if (user.getLastName() == null || user.getLastName().isEmpty()) {
                user.setLastName("");
                updated = true;
            }
        }
        if (updated) {
            userRepository.saveAll(users);
        }
    }

    /**
     * Move risk categories onto the managed-fields framework, and fill each risk's new
     * category code from the old risk_category_id.
     *
     * Runs here rather than in Flyway because development has Flyway disabled and lets
     * Hibernate manage the schema, so a SQL-only migration would never execute there.
     *
     * Three cases:
     *   - options already exist  -> leave them alone; the administrator owns the list
     *   - legacy risk_categories -> carry those names and descriptions across, so nobody
     *                               loses categories they were already using
     *   - neither (fresh install) -> lay down the product defaults
     */
    private void migrateRiskCategoriesToLookup() {
        List<Organization> orgs = organizationRepository.findAll();
        if (orgs.isEmpty()) return;

        List<RiskCategory> legacy = riskCategoryRepository.findAllByOrderByNameAsc();
        List<Seed> seeds = legacy.isEmpty() ? defaultRiskCategorySeeds()
                                            : legacy.stream()
                                                    .map(c -> new Seed(codeFor(c.getName()), c.getName(),
                                                                       c.getDescription(), false))
                                                    .toList();
        seedLookupType(orgs, LookupType.RISK_CATEGORY, seeds);
        backfillRiskCategoryCodes(legacy);
    }

    private static List<Seed> defaultRiskCategorySeeds() {
        return List.of(
            new Seed("OPERATIONAL_RISK",   "Operational Risk",
                "Risk of loss resulting from inadequate or failed processes, people and systems, or from external events", false),
            new Seed("MARKET_RISK",        "Market Risk (Currency Rate Risk)",
                "Risk of loss arising from movements in market prices, including foreign exchange and interest rates", false),
            new Seed("COUNTERPARTY_RISK",  "Counterparty Risk",
                "Risk that the other party to an agreement defaults on its obligations", false),
            new Seed("COMPLIANCE_RISK",    "Compliance Risk",
                "Exposure to legal or financial penalties for failing to act in accordance with applicable laws and regulations", false),
            new Seed("REPUTATIONAL_RISK",  "Reputational Risk",
                "Risk of loss resulting from damage to the organisation's reputation and stakeholder trust", false),
            new Seed("SECURITY_RISK",      "Security Risk",
                "Risk of loss from unauthorised access to, or disruption of, information systems", false),
            new Seed("FINANCIAL_CRIME_RISK", "Money Laundering/Terrorist Financing Risk",
                "Risk of involvement, deliberate or otherwise, in laundering proceeds of crime or financing terrorism", false)
        );
    }

    /** UPPER_SNAKE code derived from a category name, matching LookupService's own scheme. */
    private static String codeFor(String name) {
        String base = (name == null ? "" : name).toUpperCase(java.util.Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
        if (base.isEmpty()) base = "CATEGORY";
        return base.length() > 50 ? base.substring(0, 50) : base;
    }

    /**
     * Copy each risk's old numeric category onto the new code column.
     *
     * The old column is read with native SQL because Risk no longer maps it. A fresh install
     * has no such column at all, which is why the whole thing is guarded — there is simply
     * nothing to carry across in that case.
     */
    private void backfillRiskCategoryCodes(List<RiskCategory> legacy) {
        if (legacy.isEmpty()) return;

        java.util.Map<Long, String> idToCode = new java.util.HashMap<>();
        for (RiskCategory c : legacy) {
            idToCode.put(c.getId(), codeFor(c.getName()));
        }

        List<Object[]> rows;
        try {
            rows = riskRepository.findLegacyCategoryAssignments();
        } catch (Exception e) {
            return; // No legacy column on this database — nothing to migrate
        }

        List<Risk> updated = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            Long riskId = ((Number) row[0]).longValue();
            Long legacyCategoryId = row[1] == null ? null : ((Number) row[1]).longValue();
            String code = idToCode.get(legacyCategoryId);
            if (code == null) continue;

            riskRepository.findById(riskId).ifPresent(risk -> {
                if (risk.getRiskCategory() == null || risk.getRiskCategory().isBlank()) {
                    risk.setRiskCategory(code);
                    updated.add(risk);
                }
            });
        }
        if (!updated.isEmpty()) {
            riskRepository.saveAll(updated);
        }
    }



    /**
     * One-time migration for risks created before review tracking existed. An edit is the
     * best available evidence the risk was actually looked at, so seed last_reviewed_at from
     * updated_at (falling back to created_at). Without this every existing risk would report
     * as "never reviewed" on day one, which is noise rather than signal.
     */
    private void backfillLastReviewedAt() {
        List<Risk> risks = riskRepository.findAll();
        List<Risk> updated = new java.util.ArrayList<>();
        for (Risk risk : risks) {
            if (risk.getLastReviewedAt() != null) continue;
            Instant seed = risk.getUpdatedAt() != null ? risk.getUpdatedAt() : risk.getCreatedAt();
            if (seed != null) {
                risk.setLastReviewedAt(seed);
                updated.add(risk);
            }
        }
        if (!updated.isEmpty()) {
            riskRepository.saveAll(updated);
        }
    }

    /** Give organisations created before the setting existed the default appetite threshold. */
    private void backfillRiskAppetiteThreshold() {
        List<Organization> orgs = organizationRepository.findAll();
        List<Organization> updated = new java.util.ArrayList<>();
        for (Organization org : orgs) {
            if (org.getRiskAppetiteThreshold() == null) {
                org.setRiskAppetiteThreshold(15);
                updated.add(org);
            }
        }
        if (!updated.isEmpty()) {
            organizationRepository.saveAll(updated);
        }
    }

    private void seedRiskDimensions() {
        if (riskDimensionRepository.count() > 0) return;

        riskDimensionRepository.saveAll(List.of(
            dimension("Financial",             "Risks related to financial performance, reporting, and compliance"),
            dimension("Customer",              "Risks related to customer interactions, satisfaction, and retention"),
            dimension("Opportunity",           "Risks related to missed business opportunities, market changes, and competitive threats"),
            dimension("Commercial",            "Risks related to contracts, suppliers, and third-party relationships"),
            dimension("Staff",                 "Risks related to employee management, talent acquisition, and workforce development"),
            dimension("Brand/Reputation",      "Risks related to brand reputation, public perception, and media coverage"),
            dimension("Media",                 "Risks related to media coverage, public relations, and communication strategies"),
            dimension("Regulatory",            "Risks related to regulatory compliance, legal issues, and government relations"),
            dimension("Environmental",         "Risks related to environmental impact, sustainability, and climate change"),
            dimension("Health & Safety",       "Risks related to workplace safety, employee health, and public health issues"),
            dimension("Technology",            "Risks related to technology infrastructure, cybersecurity, and data management"),
            dimension("Operational",           "Risks related to business operations, supply chain, and logistics"),
            dimension("Legal",                 "Risks related to legal disputes, intellectual property, and contract management"),
            dimension("Strategic",             "Risks related to strategic planning, market positioning, and long-term growth"),
            dimension("Project",               "Risks related to project management, timelines, and deliverables"),
            dimension("Financial Crime (AML/CFT)", "Risks related to fraud, money laundering, and other financial crimes")
        ));
    }

    private static RiskCategory category(Long id, String name, String description) {
        RiskCategory c = new RiskCategory();
        c.setId(id);
        c.setName(name);
        c.setDescription(description);
        return c;
    }


    private static RiskDimension dimension(String name, String description) {
        RiskDimension d = new RiskDimension();
        d.setName(name);
        d.setDescription(description);
        return d;
    }

    private void seedRiskStatuses() {
        if (riskStatusRepository.count() > 0) return;

        riskStatusRepository.saveAll(List.of(
            status(1L, "Identified",
                "Risk has been identified but not yet assessed"),
            status(2L, "Assessed",
                "Risk has been assessed with likelihood and impact scores assigned"),
            status(3L, "Mitigated",
                "Controls and mitigation measures have been applied to the risk"),
            status(4L, "Accepted",
                "Risk has been reviewed and accepted within the organisation's risk appetite"),
            status(5L, "Closed",
                "Risk is no longer active and has been formally closed")
        ));
    }

    /**
     * Seed the admin-managed dropdown options, per organisation.
     *
     * The Issue Source codes match the enum names that {@code issues.source} already stores,
     * so existing issues keep resolving without any data migration.
     *
     * Seeding runs only while a field has no options at all. After that the administrator owns
     * the list outright — otherwise deleting a supplied option would silently reappear on the
     * next restart. A later release that needs to add a built-in option should do it in a
     * migration rather than here.
     */
    /** One starting option for a managed field. */
    private record Seed(String code, String name, String description, boolean flag) {}

    private void seedLookupValues() {
        List<Organization> orgs = organizationRepository.findAll();
        if (orgs.isEmpty()) return;

        // Codes match the enum names issues.source already stores, so existing rows resolve unchanged
        seedLookupType(orgs, LookupType.ISSUE_SOURCE, List.of(
            new Seed("INTERNAL_AUDIT",  "Internal Audit",  "Raised by the internal audit function", true),
            new Seed("EXTERNAL_AUDIT",  "External Audit",  "Raised by the external auditor", true),
            new Seed("REGULATOR",       "Regulator",       "Raised by a regulator or supervisory body", true),
            new Seed("CONTROL_TESTING", "Control Testing", "Identified through control testing or monitoring", false),
            new Seed("SELF_IDENTIFIED", "Self-Identified", "Identified by the business itself", false),
            new Seed("INCIDENT",        "Incident",        "Arising from an incident or loss event", false),
            new Seed("OTHER",           "Other",           "Any other source", false)
        ));

        seedLookupType(orgs, LookupType.ISSUE_CATEGORY, List.of(
            new Seed("GOVERNANCE",        "Governance & Oversight",  "Committee structures, delegated authority, management information", false),
            new Seed("POLICY_PROCEDURE",  "Policy & Procedure",      "Missing, outdated or unfollowed policies and procedures", false),
            new Seed("ACCESS_MANAGEMENT", "Access Management",       "User access, privileged accounts, segregation of duties", false),
            new Seed("DATA_PROTECTION",   "Data Protection",         "Handling, retention, classification and privacy of data", false),
            new Seed("TECHNOLOGY",        "Technology & Infrastructure", "Platforms, resilience, patching and change control", false),
            new Seed("THIRD_PARTY",       "Third Party Management",  "Vendor due diligence, contracts and ongoing oversight", false),
            new Seed("PROCESS_CONTROLS",  "Process & Operational Controls", "Day-to-day operating controls and reconciliations", false),
            new Seed("REGULATORY",        "Regulatory Compliance",   "Compliance with applicable laws, rules and regulations", false),
            new Seed("FINANCIAL",         "Financial Controls",      "Financial reporting, reconciliation and accounting controls", false),
            new Seed("OTHER",             "Other",                   "Anything not covered by another category", false)
        ));

        // Mirrors the risk_dimensions list so findings and risks share one impact vocabulary
        seedLookupType(orgs, LookupType.ISSUE_DIMENSION, List.of(
            new Seed("FINANCIAL",        "Financial",                  "Direct financial loss or misstatement", false),
            new Seed("CUSTOMER",         "Customer",                   "Customer detriment, service quality or retention", false),
            new Seed("OPPORTUNITY",      "Opportunity",                "Missed opportunity or competitive disadvantage", false),
            new Seed("COMMERCIAL",       "Commercial",                 "Contracts, suppliers and commercial relationships", false),
            new Seed("STAFF",            "Staff",                      "Employees, capability and workforce impact", false),
            new Seed("BRAND_REPUTATION", "Brand/Reputation",           "Reputation and stakeholder confidence", false),
            new Seed("MEDIA",            "Media",                      "Media coverage and public relations", false),
            new Seed("REGULATORY",       "Regulatory",                 "Regulatory censure, penalties or enforcement", false),
            new Seed("ENVIRONMENTAL",    "Environmental",              "Environmental impact and sustainability", false),
            new Seed("HEALTH_SAFETY",    "Health & Safety",            "Workplace safety and health of people", false),
            new Seed("TECHNOLOGY",       "Technology",                 "Technology platforms, data and cyber security", false),
            new Seed("OPERATIONAL",      "Operational",                "Business operations and service delivery", false),
            new Seed("LEGAL",            "Legal",                      "Litigation, intellectual property and contracts", false),
            new Seed("STRATEGIC",        "Strategic",                  "Strategy, market position and long-term goals", false),
            new Seed("PROJECT",          "Project",                    "Project delivery, timelines and benefits", false),
            new Seed("FINANCIAL_CRIME",  "Financial Crime (AML/CFT)",  "Fraud, money laundering and terrorist financing", false)
        ));
    }

    /** Populate one managed field for every organisation that has none of its options yet. */
    private void seedLookupType(List<Organization> orgs, LookupType type, List<Seed> seeds) {
        List<LookupValue> toSave = new java.util.ArrayList<>();
        for (Organization org : orgs) {
            // Only populate an empty field; never re-add what an administrator removed
            long existing = lookupValueRepository
                .countByOrganizationIdAndLookupType(org.getId(), type.name());
            if (existing > 0) continue;

            int order = 0;
            for (Seed seed : seeds) {
                LookupValue value = new LookupValue();
                value.setLookupType(type.name());
                value.setCode(seed.code());
                value.setName(seed.name());
                value.setDescription(seed.description());
                value.setFlagValue(type.hasFlag() && seed.flag());
                value.setActive(true);
                value.setSystemDefault(true);
                value.setSortOrder(order++);
                value.setOrganizationId(org.getId());
                value.setCreatedAt(Instant.now());
                value.setUpdatedAt(Instant.now());
                toSave.add(value);
            }
        }
        if (!toSave.isEmpty()) {
            lookupValueRepository.saveAll(toSave);
        }
    }

    private static RiskStatus status(Long id, String name, String description) {
        RiskStatus s = new RiskStatus();
        s.setId(id);
        s.setName(name);
        s.setDescription(description);
        return s;
    }

    private void seedAdminUser() {
        User adminUser = userRepository.findByEmail("john@doe.com");
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setEmail("john@doe.com");
            adminUser.setFirstName("John");
            adminUser.setLastName("Doe");
            adminUser.setPassword(""); // Password should be set via registration or hashed
            adminUser.setRole(Role.ADMIN);
            adminUser.setApproved(true);
            userRepository.save(adminUser);
        }
    }



    private void seedEffectivenessScores() {
        if (effectivenessScoreRepository.count() > 0) return;

        Instant now = Instant.now();
        List<EffectivenessScore> scores = new java.util.ArrayList<>();

        // Generate monthly data points for the past 2 years
        // Starting from 24 months ago, progressing to present
        double[] scoreProgression = {
            45.0,  // Month 1 (24 months ago)
            48.0,  // Month 2
            50.0,  // Month 3
            52.0,  // Month 4
            55.0,  // Month 5
            58.0,  // Month 6
            60.0,  // Month 7
            62.0,  // Month 8
            65.0,  // Month 9
            66.0,  // Month 10
            68.0,  // Month 11
            70.0,  // Month 12 (1 year ago)
            71.0,  // Month 13
            72.0,  // Month 14
            73.0,  // Month 15
            72.5,  // Month 16
            71.0,  // Month 17
            73.0,  // Month 18
            75.0,  // Month 19
            76.0,  // Month 20
            77.0,  // Month 21
            78.0,  // Month 22
            78.5,  // Month 23
            79.0   // Month 24 (current month)
        };

        for (int i = 0; i < scoreProgression.length; i++) {
            // Calculate date: current - (24 - i) months
            long daysOffset = (24 - i) * 30L; // Approximate 30 days per month
            Instant scoreDate = now.minusSeconds(daysOffset * 86400);

            EffectivenessScore score = new EffectivenessScore();
            score.setScore(scoreProgression[i]);
            score.setTotalInherentScore(2500L);  // Sample totals
            score.setTotalResidualScore((long)(2500 * (1 - scoreProgression[i] / 100)));
            score.setRiskCount(35);  // Sample count of assessed/mitigated/accepted risks
            score.setCalculatedAt(scoreDate);

            scores.add(score);
        }

        effectivenessScoreRepository.saveAll(scores);
    }
}
