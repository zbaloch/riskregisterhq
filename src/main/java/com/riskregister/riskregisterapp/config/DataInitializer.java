package com.riskregister.riskregisterapp.config;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.riskregister.riskregisterapp.entities.EffectivenessScore;
import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.entities.RiskCategory;
import com.riskregister.riskregisterapp.entities.RiskDimension;
import com.riskregister.riskregisterapp.entities.RiskStatus;
import com.riskregister.riskregisterapp.entities.RiskSubcategory;
import com.riskregister.riskregisterapp.entities.Role;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.enums.RiskReviewFrequency;
import com.riskregister.riskregisterapp.lookups.RiskTreatment;
import com.riskregister.riskregisterapp.repositories.EffectivenessScoreRepository;
import com.riskregister.riskregisterapp.repositories.RiskCategoryRepository;
import com.riskregister.riskregisterapp.repositories.RiskDimensionRepository;
import com.riskregister.riskregisterapp.repositories.RiskRepository;
import com.riskregister.riskregisterapp.repositories.RiskStatusRepository;
import com.riskregister.riskregisterapp.repositories.RiskSubcategoryRepository;
import com.riskregister.riskregisterapp.repositories.UserRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private RiskCategoryRepository riskCategoryRepository;

    @Autowired
    private RiskSubcategoryRepository riskSubcategoryRepository;

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

    @Override
    public void run(ApplicationArguments args) {
        seedRiskCategories();
        seedRiskSubcategories();
        seedRiskDimensions();
        seedRiskStatuses();
        seedAdminUser();
        seedRisks();
        seedEffectivenessScores();
    }

    private void seedRiskCategories() {
        if (riskCategoryRepository.count() > 0) return;

        riskCategoryRepository.saveAll(List.of(
            category(1L, "Operational Risk",
                "Risk of loss, resulting from inadequate or failed processes, people and systems or from external events"),
            category(2L, "Market Risk (Currency Rate Risk)",
                "Risk of loss arising from movements in market prices, including foreign exchange rates, interest rates and commodity prices"),
            category(3L, "Counterparty Risk",
                "Risk that the other party to an agreement may default; requires identification, measurement, monitoring and control of exposure limits prior to and throughout the business relationship"),
            category(4L, "Compliance Risk",
                "Exposure to legal penalties, financial penalties and material losses when failing to act in accordance with applicable Laws, Rules, Regulations, Notices and Standards"),
            category(5L, "Reputational Risk",
                "Risk of loss resulting from damage to an organization's reputation, whether through negative publicity, customer dissatisfaction, or other factors that can erode trust and confidence in the organization"),
            category(6L, "Security Risk",
                "Risk of loss resulting from unauthorized access to or disruption of information systems, including risks related to data breaches, cyber attacks, and other security incidents"),
            category(7L, "Money Laundering/Terrorist Financing Risk",
                "Risk of loss resulting from involvement, whether deliberate or not, in transforming proceeds of crime into legitimate assets, or in financing terrorism directly or indirectly; governed by AML/CFT compliance requirements")
        ));
    }

    private void seedRiskSubcategories() {
        if (riskSubcategoryRepository.count() > 0) return;

        riskSubcategoryRepository.saveAll(List.of(
            subcategory(1L,  "Access Control",        "Authentication, authorization, user management"),
            subcategory(3L,  "Asset Management",      "Hardware, software, data asset tracking"),
            subcategory(4L,  "Business Continuity",   "Disaster recovery, backup procedures"),
            subcategory(5L,  "Change Management",     "System changes, deployment processes"),
            subcategory(6L,  "Cryptography",          "Encryption, key management, certificates"),
            subcategory(7L,  "Data Protection",       "Data handling, classification, retention"),
            subcategory(8L,  "Human Resources",       "Personnel security, training, background checks"),
            subcategory(9L,  "Incident Response",     "Security incidents, breach response"),
            subcategory(10L, "Information Security",  "Security policies, awareness, governance"),
            subcategory(11L, "Network Security",      "Firewalls, network monitoring, segmentation"),
            subcategory(12L, "Physical Security",     "Facility access, equipment protection"),
            subcategory(13L, "Risk Management",       "Risk assessment, treatment, monitoring"),
            subcategory(14L, "System Operations",     "System monitoring, maintenance, logging"),
            subcategory(15L, "Vendor Management",     "Third-party risk, supplier assessment")
        ));
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

    private static RiskSubcategory subcategory(Long id, String name, String description) {
        RiskSubcategory s = new RiskSubcategory();
        s.setId(id);
        s.setName(name);
        s.setDescription(description);
        return s;
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

    private void seedRisks() {
        if (riskRepository.count() > 0) return;

        User adminUser = userRepository.findByEmail("john@doe.com");
        if (adminUser == null) return;

        String adminEmail = adminUser.getEmail();
        Instant now = Instant.now();
        int riskCounter = 1;

        riskRepository.saveAll(List.of(
            // Operational Risks
            createRisk(riskCounter++, "System downtime during peak hours",
                "Unexpected system outages affecting business operations", adminEmail, now,
                1L, 3L, 2L, 5, 4, "Legacy infrastructure and insufficient redundancy",
                3, 3, "Implemented redundant systems and automated failover", 1L),
            createRisk(riskCounter++, "Data entry errors in critical systems",
                "Manual data entry errors leading to incorrect records", adminEmail, now,
                1L, 5L, 3L, 3, 2, "High transaction volume with manual processes",
                2, 1, "Implemented validation rules and automated checks", 2L),
            createRisk(riskCounter++, "Inadequate disaster recovery procedures",
                "Lack of documented and tested disaster recovery plans", adminEmail, now,
                1L, 4L, 6L, 4, 5, "Limited backup systems and recovery testing",
                2, 2, "Established DR site and quarterly testing schedule", 1L),
            createRisk(riskCounter++, "Vendor dependency for critical services",
                "Over-reliance on single vendor for essential operations", adminEmail, now,
                1L, 2L, 5L, 3, 4, "No alternative vendor agreements in place",
                2, 2, "Negotiated backup vendor contracts", 2L),
            createRisk(riskCounter++, "Inefficient process workflows",
                "Outdated manual processes causing delays and inefficiencies", adminEmail, now,
                1L, 3L, 2L, 2, 3, "Limited process automation and documentation",
                1, 1, "Implemented workflow automation tools", 3L),
            createRisk(riskCounter++, "Supply chain disruptions",
                "Interruptions in supply chain affecting product delivery", adminEmail, now,
                1L, 6L, 2L, 4, 5, "Limited supplier diversification and inventory buffers",
                3, 2, "Diversified suppliers and increased safety stock", 2L),

            // Security Risks
            createRisk(riskCounter++, "Data breach from external hackers",
                "Unauthorized access to sensitive customer or company data", adminEmail, now,
                6L, 1L, 3L, 5, 5, "Evolving cyber threats and advanced attack techniques",
                3, 3, "Implemented multi-layer security and 24/7 monitoring", 1L),
            createRisk(riskCounter++, "Insider threat from disgruntled employee",
                "Malicious activity by employee with system access", adminEmail, now,
                6L, 1L, 1L, 4, 5, "Limited monitoring of privileged user activities",
                2, 3, "Enhanced access controls and behavioral monitoring", 1L),
            createRisk(riskCounter++, "Weak password policies",
                "Inadequate password strength and change requirements", adminEmail, now,
                6L, 1L, 11L, 3, 4, "Legacy systems without modern password requirements",
                2, 2, "Implemented strong password policy and MFA", 3L),
            createRisk(riskCounter++, "Unpatched software vulnerabilities",
                "Systems running outdated software with known vulnerabilities", adminEmail, now,
                6L, 1L, 10L, 4, 5, "Irregular patch management and testing cycles",
                2, 3, "Automated patch management system implemented", 1L),
            createRisk(riskCounter++, "Social engineering attacks",
                "Phishing and pretexting attacks targeting employees", adminEmail, now,
                6L, 1L, 2L, 3, 3, "Insufficient security awareness training",
                1, 1, "Implemented security training and email filtering", 3L),
            createRisk(riskCounter++, "Ransomware infection",
                "Ransomware attack encrypting critical business systems", adminEmail, now,
                6L, 1L, 4L, 5, 5, "Limited backup segregation and recovery procedures",
                3, 4, "Air-gapped backups and incident response plan", 2L),

            // Compliance & Regulatory Risks
            createRisk(riskCounter++, "Non-compliance with GDPR regulations",
                "Failure to comply with GDPR data protection requirements", adminEmail, now,
                4L, 10L, 7L, 5, 5, "Complex international operations and data transfers",
                3, 3, "Implemented privacy by design and data governance", 1L),
            createRisk(riskCounter++, "Regulatory audit findings",
                "Non-compliance issues identified during regulatory audits", adminEmail, now,
                4L, 10L, 8L, 4, 4, "Gaps in compliance controls and documentation",
                2, 2, "Remediation plan and enhanced compliance monitoring", 2L),
            createRisk(riskCounter++, "Failure to maintain required certifications",
                "Loss of ISO or industry-specific certifications", adminEmail, now,
                4L, 10L, 13L, 3, 5, "Insufficient resources for compliance maintenance",
                2, 2, "Dedicated compliance team assigned", 2L),
            createRisk(riskCounter++, "Sanctions or export control violations",
                "Violation of international sanctions or export control regulations", adminEmail, now,
                4L, 10L, 4L, 4, 5, "Limited screening procedures for international transactions",
                2, 2, "Implemented sanctions screening system", 1L),

            // Financial Risks
            createRisk(riskCounter++, "Foreign exchange rate fluctuations",
                "Adverse currency movements affecting international operations", adminEmail, now,
                2L, 9L, 3L, 3, 3, "Significant revenue in multiple currencies",
                2, 2, "Implemented hedging strategies", 2L),
            createRisk(riskCounter++, "Credit default by major customers",
                "Default payment from significant customer accounts", adminEmail, now,
                2L, 9L, 6L, 3, 5, "Concentrated customer base with large exposures",
                2, 2, "Enhanced credit monitoring and payment terms", 2L),
            createRisk(riskCounter++, "Fraud in financial reporting",
                "Intentional or unintentional misstatement of financial results", adminEmail, now,
                2L, 9L, 1L, 4, 5, "Complex transactions and limited segregation of duties",
                2, 3, "Implemented SOX controls and internal audit", 1L),
            createRisk(riskCounter++, "Inadequate working capital management",
                "Insufficient liquidity to meet operational needs", adminEmail, now,
                2L, 9L, 2L, 3, 4, "Unpredictable cash flow and seasonal variations",
                2, 2, "Implemented cash forecasting and credit facilities", 2L),
            createRisk(riskCounter++, "Interest rate fluctuations",
                "Adverse impact from changes in interest rates", adminEmail, now,
                2L, 9L, 5L, 2, 2, "Floating rate debt exposure",
                1, 1, "Negotiated fixed-rate facilities", 3L),

            // Reputational & Market Risks
            createRisk(riskCounter++, "Negative media coverage",
                "Damaging news stories affecting brand reputation", adminEmail, now,
                5L, 9L, 12L, 4, 4, "Crisis in operations or product quality",
                2, 2, "Established crisis communication plan", 2L),
            createRisk(riskCounter++, "Loss of key customer accounts",
                "Departure of major customers to competitors", adminEmail, now,
                5L, 9L, 6L, 3, 5, "Competitive pressures and changing customer preferences",
                2, 3, "Implemented customer retention programs", 2L),
            createRisk(riskCounter++, "Product quality issues",
                "Defects or failures in products reaching market", adminEmail, now,
                5L, 9L, 3L, 3, 4, "Insufficient quality control processes",
                2, 2, "Enhanced QA testing and inspections", 2L),
            createRisk(riskCounter++, "Market share loss to competitors",
                "Declining market share due to competitive threats", adminEmail, now,
                5L, 9L, 2L, 2, 3, "Slow product innovation and market adaptation",
                1, 2, "Accelerated R&D and marketing initiatives", 2L),
            createRisk(riskCounter++, "Executive leadership departure",
                "Loss of key executives affecting company direction", adminEmail, now,
                5L, 9L, 8L, 3, 4, "Insufficient succession planning",
                2, 2, "Implemented leadership succession plan", 2L),

            // Counterparty & Vendor Risks
            createRisk(riskCounter++, "Supplier financial distress",
                "Critical supplier facing financial difficulties", adminEmail, now,
                3L, 15L, 4L, 3, 4, "Limited supplier financial monitoring",
                2, 2, "Quarterly financial health assessments implemented", 2L),
            createRisk(riskCounter++, "Third-party service failures",
                "Service outages from outsourced providers", adminEmail, now,
                3L, 15L, 11L, 4, 4, "Limited SLA enforcement and backup arrangements",
                2, 2, "Enhanced SLA terms and backup provider contracts", 2L),
            createRisk(riskCounter++, "Joint venture partner underperformance",
                "Partner failing to meet commitments in joint ventures", adminEmail, now,
                3L, 15L, 5L, 3, 3, "Limited performance monitoring mechanisms",
                2, 2, "Enhanced governance and KPI tracking", 2L),

            // Human Resources & Organizational Risks
            createRisk(riskCounter++, "Key talent retention challenges",
                "Loss of critical skilled employees to competitors", adminEmail, now,
                7L, 8L, 1L, 3, 4, "Competitive job market and limited career growth",
                2, 2, "Implemented competitive compensation and development programs", 3L),
            createRisk(riskCounter++, "Insufficient staff training and development",
                "Lack of skills for evolving business requirements", adminEmail, now,
                7L, 8L, 10L, 2, 3, "Limited training budget and time allocation",
                1, 1, "Established formal training program", 3L),
            createRisk(riskCounter++, "Workplace safety incidents",
                "Accidents or injuries in workplace", adminEmail, now,
                7L, 8L, 12L, 3, 4, "Inadequate safety protocols and training",
                2, 2, "Enhanced safety program and regular inspections", 2L),
            createRisk(riskCounter++, "Labor disputes or strikes",
                "Employee grievances leading to industrial action", adminEmail, now,
                7L, 8L, 9L, 2, 4, "Union relations and compensation disagreements",
                1, 2, "Established labor relations team", 2L),
            createRisk(riskCounter++, "High employee turnover",
                "Elevated staff departure rates affecting operations", adminEmail, now,
                7L, 8L, 2L, 3, 3, "Poor working environment or compensation",
                2, 2, "Improved HR practices and engagement initiatives", 2L),

            // Strategic & Growth Risks
            createRisk(riskCounter++, "Failed new product launch",
                "New product not meeting market expectations or targets", adminEmail, now,
                5L, 14L, 1L, 3, 4, "Insufficient market research or product development",
                2, 2, "Enhanced market testing and product validation", 2L),
            createRisk(riskCounter++, "Merger and acquisition integration failures",
                "Failed to achieve synergies from M&A activities", adminEmail, now,
                5L, 14L, 9L, 4, 5, "Cultural differences and operational misalignment",
                3, 2, "Dedicated M&A integration team and planning", 1L),
            createRisk(riskCounter++, "Technology obsolescence",
                "Core business technology becoming outdated", adminEmail, now,
                1L, 14L, 7L, 3, 4, "Rapid technological change and limited innovation",
                2, 2, "Ongoing technology refresh and modernization plan", 2L),
            createRisk(riskCounter++, "Market entry failure in new geography",
                "Unsuccessful expansion into new geographic markets", adminEmail, now,
                5L, 14L, 3L, 2, 3, "Limited local market knowledge and partnerships",
                1, 1, "Established local partnerships and market research", 3L),

            // Legal & Intellectual Property Risks
            createRisk(riskCounter++, "Intellectual property infringement claims",
                "Third parties alleging IP violation of patents or trademarks", adminEmail, now,
                5L, 13L, 6L, 3, 4, "Limited IP monitoring and management",
                2, 2, "Established IP portfolio management", 2L),
            createRisk(riskCounter++, "Litigation and legal disputes",
                "Involvement in lawsuits or contractual disputes", adminEmail, now,
                5L, 13L, 7L, 3, 4, "Complex business relationships and contracts",
                2, 2, "Enhanced contract review and legal governance", 2L),
            createRisk(riskCounter++, "Contract breach by counterparty",
                "Other party failing to perform contractual obligations", adminEmail, now,
                3L, 13L, 8L, 3, 3, "Limited contract enforcement mechanisms",
                2, 2, "Enhanced contract monitoring and remedies", 2L),

            // Environmental & Social Risks
            createRisk(riskCounter++, "Environmental compliance failures",
                "Non-compliance with environmental regulations", adminEmail, now,
                1L, 4L, 5L, 3, 4, "Complex environmental regulations and monitoring",
                2, 2, "Implemented environmental management system", 2L),
            createRisk(riskCounter++, "Climate change impacts on operations",
                "Physical climate risks affecting facilities or supply chains", adminEmail, now,
                1L, 4L, 14L, 3, 4, "Increased frequency of extreme weather events",
                2, 2, "Climate risk assessment and adaptation plan", 2L),
            createRisk(riskCounter++, "Negative ESG perception",
                "Poor environmental, social or governance perception", adminEmail, now,
                5L, 4L, 11L, 3, 3, "Changing stakeholder expectations on ESG",
                2, 2, "Established ESG reporting and improvement initiatives", 2L)
        ));
    }

    private Risk createRisk(int number, String title, String description, String adminEmail,
            Instant now, Long categoryId, Long dimensionId, Long subcategoryId,
            int inherentLikelihood, int inherentImpact, String inherentRationale,
            int residualLikelihood, int residualImpact, String residualRationale, Long statusId) {
        Risk risk = new Risk();
        risk.setRiskId("RISK-" + String.format("%03d", number));
        risk.setTitle(title);
        risk.setDescription(description);
        risk.setRiskOwnerName("Admin");
        risk.setRiskCategoryId(categoryId);
        risk.setRiskDimensionId(dimensionId);
        risk.setRiskSubcategoryId(subcategoryId);
        risk.setInherentLikelihood(inherentLikelihood);
        risk.setInherentImpact(inherentImpact);
        risk.setInherentRationale(inherentRationale);
        risk.setResidualLikelihood(residualLikelihood);
        risk.setResidualImpact(residualImpact);
        risk.setResidualRationale(residualRationale);
        risk.setReviewFrequency(RiskReviewFrequency.QUARTERLY);
        risk.setRiskTreatment(RiskTreatment.AWAITING_ASSESSMENT);
        risk.setStatusId(statusId);
        risk.setCreatedAt(now);
        risk.setCreatedByEmail(adminEmail);
        return risk;
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
