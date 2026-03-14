-- V2: Sample Data for Financial Institute Risk Register
-- This migration adds realistic sample data for a fictional bank: "SecureBank Ltd."
-- Includes: Risks, Tasks, Assets, Notes, and 6-month Effectiveness Score history

-- =====================================================================
-- SAMPLE RISKS
-- =====================================================================
INSERT INTO risks (
    risk_id, title, description, risk_owner_name,
    risk_category_id, risk_subcategory_id, risk_dimension_id,
    categories, linked_asset_ids,
    review_frequency,
    inherent_likelihood, inherent_impact, inherent_rationale,
    residual_likelihood, residual_impact, residual_rationale,
    risk_treatment, status_id,
    created_at, updated_at, deleted_at,
    created_by_email, updated_by_email
) VALUES
-- Risk 1: Data Breach - Customer Information
(
    'RISK-001',
    'Customer Data Breach via Compromised API Endpoint',
    'Potential unauthorized access to sensitive customer personal and financial information through inadequately secured API endpoints. Could expose account numbers, SSNs, transaction history, and contact details.',
    'Sarah Johnson',
    6, 1, 11,  -- Security Risk, Access Control, Technology
    'PII,API,Cybersecurity',
    '1,2,3',
    'QUARTERLY',
    5, 5, 'Advanced threat actors continuously targeting financial institutions. Legacy API endpoints lack modern authentication. Historical breaches in sector show high success rates.',
    2, 3, 'Implemented OAuth 2.0, rate limiting, and API gateway WAF. 24/7 SOC monitoring. Regular penetration testing quarterly.',
    'MITIGATE', 2,  -- Status: Assessed
    '2025-09-15 08:30:00', '2026-03-10 14:20:00', NULL,
    'john@doe.com', 'john@doe.com'
),
-- Risk 2: Regulatory Non-Compliance
(
    'RISK-002',
    'Non-Compliance with PCI-DSS Standards',
    'Payment card data storage and processing does not fully meet PCI-DSS v3.2.1 requirements. Could result in regulatory fines up to $100K per month and loss of card processing capabilities.',
    'Michael Chen',
    4, 8, 8,  -- Compliance Risk, Information Security, Regulatory
    'Compliance,PCI,CardProcessing',
    '2,5',
    'SEMI-ANNUAL',
    4, 5, 'Annual compliance audit identified 12 outstanding findings. Remediation timeline extends beyond current deadline. Third-party processor also non-compliant.',
    2, 2, 'Remediation plan in progress. Network segmentation 80% complete. Encryption upgrade scheduled Q2 2026. Third-party audit scheduled.',
    'MITIGATE', 3,  -- Status: Mitigated
    '2025-10-20 10:15:00', '2026-03-05 09:45:00', NULL,
    'john@doe.com', 'john@doe.com'
),
-- Risk 3: System Outage
(
    'RISK-003',
    'Core Banking System Outage - Extended Downtime',
    'Single point of failure in core banking system could result in inability to process transactions, customer service disruption, and significant revenue loss. Estimated impact: $500K per hour of downtime.',
    'James Wilson',
    1, 4, 12,  -- Operational Risk, Business Continuity, Operational
    'Availability,CriticalSystem,Operational',
    '1,4,6',
    'QUARTERLY',
    3, 5, 'System is 15+ years old, nearing end of life. No active-active redundancy. Recovery time objective is 4 hours. Limited vendor support.',
    2, 3, 'High-availability cluster implemented. Automated failover in place. RTO reduced to 15 minutes. Vendor support contract renewed for 2 years.',
    'MITIGATE', 2,  -- Status: Assessed
    '2025-08-10 07:00:00', '2026-02-28 11:30:00', NULL,
    'john@doe.com', 'john@doe.com'
),
-- Risk 4: Insider Threat
(
    'RISK-004',
    'Unauthorized Data Exfiltration by Privileged User',
    'Disgruntled employee or contractor with elevated system access could copy and exfiltrate customer data, trade secrets, or financial records.',
    'Lisa Rodriguez',
    6, 8, 11,  -- Security Risk, Human Resources, Technology
    'InsiderThreat,PrivilegedAccess',
    '2,3',
    'QUARTERLY',
    4, 5, 'High turnover in IT department (25% annual). Employees have access to unencrypted customer data. Limited DLP controls. No behavior analytics.',
    2, 3, 'DLP system deployed. Privileged access monitoring (PAM) implemented. User behavior analytics (UBA) in pilot phase. Background checks enhanced.',
    'MITIGATE', 2,  -- Status: Assessed
    '2025-11-05 13:45:00', '2026-03-08 10:20:00', NULL,
    'john@doe.com', 'john@doe.com'
),
-- Risk 5: Third-Party Vendor Risk
(
    'RISK-005',
    'Critical Vendor Service Degradation',
    'Payment processor or cloud provider outage could disrupt banking services. Lack of service level agreement enforcement and backup arrangements.',
    'David Kumar',
    3, 15, 1,  -- Counterparty Risk, Vendor Management, Commercial
    'VendorRisk,ThirdParty',
    '5',
    'SEMI-ANNUAL',
    3, 4, 'Single vendor for payment processing. No fallback. Vendor has had 2 incidents in past 18 months. SLA penalties not enforced.',
    1, 2, 'Secondary payment processor contracted and integrated. Automated failover testing monthly. Vendor SLA penalties now enforced contractually.',
    'MITIGATE', 3,  -- Status: Mitigated
    '2025-09-22 15:30:00', '2026-01-15 12:00:00', NULL,
    'john@doe.com', 'john@doe.com'
),
-- Risk 6: Fraud and Money Laundering
(
    'RISK-006',
    'Inadequate AML/CFT Controls - Sanctions Screening Gaps',
    'Existing AML screening process has false negative rate of 15%. Potential sanctions violations and regulatory penalties for processing transactions for sanctioned entities.',
    'Patricia Hayes',
    7, 15, 14,  -- Money Laundering Risk, Vendor Management, Financial Crime
    'AML,Sanctions,Fraud',
    '2',
    'QUARTERLY',
    4, 5, 'Manual screening process. Sanctions list updated monthly (should be real-time). OFAC false positives causing customer friction. Regulatory guidance unclear.',
    2, 2, 'Automated sanctions screening system deployed. Real-time OFAC/UN/EU list updates. False positive rate reduced to 2%. Training program initiated.',
    'MITIGATE', 3,  -- Status: Mitigated
    '2025-07-18 09:00:00', '2026-02-20 14:15:00', NULL,
    'john@doe.com', 'john@doe.com'
);

-- =====================================================================
-- SAMPLE TASKS (Mitigation Actions)
-- =====================================================================
INSERT INTO tasks (
    risk_id, title, description, status, priority,
    assignee_id, assignee_name, due_date,
    created_at, updated_at, deleted_at,
    created_by_email, updated_by_email
) VALUES
-- Tasks for Risk 001 (Data Breach)
(
    1,
    'Implement API Security Gateway',
    'Deploy industry-standard API management solution with authentication, rate limiting, and DDoS protection across all customer-facing endpoints.',
    'IN_PROGRESS',
    'CRITICAL',
    NULL, NULL, '2026-04-30',
    '2026-01-10 08:00:00', '2026-03-12 16:45:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    1,
    'Complete API Penetration Testing',
    'Engage external security firm to perform comprehensive penetration testing on all APIs. Document findings and track remediation.',
    'BACKLOG',
    'HIGH',
    NULL, NULL, '2026-05-15',
    '2026-01-15 09:30:00', NULL, NULL,
    'john@doe.com', 'john@doe.com'
),
-- Tasks for Risk 002 (PCI-DSS)
(
    2,
    'Complete Network Segmentation Implementation',
    'Isolate cardholder data environment (CDE) from corporate network. Ensure all traffic is encrypted and monitored.',
    'IN_PROGRESS',
    'CRITICAL',
    NULL, NULL, '2026-04-15',
    '2025-11-20 10:00:00', '2026-03-08 11:20:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    2,
    'Conduct Remediation Verification Audit',
    'Schedule and conduct formal audit to verify all PCI-DSS findings have been remediated per requirements.',
    'BACKLOG',
    'HIGH',
    NULL, NULL, '2026-06-01',
    '2026-01-22 14:30:00', NULL, NULL,
    'john@doe.com', 'john@doe.com'
),
-- Tasks for Risk 003 (System Outage)
(
    3,
    'Deploy Database Replication Setup',
    'Implement synchronous database replication to secondary data center with automated failover mechanism.',
    'COMPLETED',
    'CRITICAL',
    NULL, NULL, '2026-03-01',
    '2025-09-01 08:00:00', '2026-02-28 17:00:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    3,
    'Conduct Disaster Recovery Drill',
    'Execute full failover test simulating complete primary data center failure. Verify RTO of 15 minutes is achievable.',
    'IN_PROGRESS',
    'HIGH',
    NULL, NULL, '2026-04-30',
    '2026-02-15 09:00:00', '2026-03-11 13:25:00', NULL,
    'john@doe.com', 'john@doe.com'
),
-- Tasks for Risk 004 (Insider Threat)
(
    4,
    'Deploy Privileged Access Management (PAM) Solution',
    'Implement centralized PAM system to monitor, control, and log all privileged account access with multi-factor authentication.',
    'IN_PROGRESS',
    'CRITICAL',
    NULL, NULL, '2026-05-01',
    '2025-12-01 10:30:00', '2026-03-09 15:40:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    4,
    'User Behavior Analytics Pilot Program',
    'Complete pilot phase of UBA system, measure effectiveness, and plan enterprise rollout.',
    'BACKLOG',
    'MEDIUM',
    NULL, NULL, '2026-06-15',
    '2026-02-20 11:00:00', NULL, NULL,
    'john@doe.com', 'john@doe.com'
),
-- Tasks for Risk 005 (Vendor Risk)
(
    5,
    'Integrate Secondary Payment Processor',
    'Complete technical integration and testing with backup payment processor. Ensure seamless failover capability.',
    'COMPLETED',
    'CRITICAL',
    NULL, NULL, '2026-01-31',
    '2025-10-15 08:00:00', '2026-01-30 16:30:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    5,
    'Automated Failover Testing Monthly',
    'Establish monthly schedule for automated failover tests to ensure backup processor is always ready.',
    'IN_PROGRESS',
    'MEDIUM',
    NULL, NULL, '2026-12-31',
    '2026-02-01 09:00:00', '2026-03-10 10:15:00', NULL,
    'john@doe.com', 'john@doe.com'
),
-- Tasks for Risk 006 (AML/CFT)
(
    6,
    'Deploy Automated Sanctions Screening System',
    'Implement real-time sanctions screening tool with automatic list updates and reduced false positive rates.',
    'COMPLETED',
    'CRITICAL',
    NULL, NULL, '2025-12-31',
    '2025-08-10 09:00:00', '2025-12-20 14:45:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    6,
    'Conduct AML Compliance Training for All Staff',
    'Mandatory training for all employees on updated AML/CFT procedures and compliance requirements.',
    'IN_PROGRESS',
    'HIGH',
    NULL, NULL, '2026-04-30',
    '2026-01-15 10:00:00', '2026-03-11 11:30:00', NULL,
    'john@doe.com', 'john@doe.com'
);

-- =====================================================================
-- SAMPLE ASSETS
-- =====================================================================
INSERT INTO assets (
    name, description, type, status, location, notes,
    owner_email, owner_name,
    confidentiality, integrity, availability,
    created_at, updated_at, deleted_at,
    created_by_email, updated_by_email
) VALUES
(
    'Core Banking System - Production',
    'Mission-critical system handling all customer transactions, account management, and general ledger processing. Runs 24/7/365.',
    'Software',
    'Active',
    'Primary Data Center - New York',
    'Custom-built application running on enterprise Java stack. Business continuity critical. In-house support team.',
    'it-ops@securebank.com', 'IT Operations Team',
    5, 5, 5,  -- CIA 5/5/5 - Maximum classification
    '2025-06-01 08:00:00', '2026-03-01 10:30:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    'Payment Processing Gateway',
    'Interfaces with external payment networks (Visa, Mastercard, ACH). Handles payment routing and settlement.',
    'Software',
    'Active',
    'Primary Data Center - New York',
    'PCI-DSS compliant. Third-party hosted with redundant connections. High availability required.',
    'payments@securebank.com', 'Payments Team',
    5, 5, 5,  -- CIA 5/5/5 - Maximum classification
    '2025-07-15 09:00:00', '2026-02-28 14:20:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    'Customer Data Warehouse',
    'Central repository of all customer personal, financial, and behavioral data. Supports analytics and reporting.',
    'Data',
    'Active',
    'Secondary Data Center - Chicago',
    'Contains highly sensitive PII and PHI. Encrypted at rest and in transit. Access heavily restricted.',
    'data-governance@securebank.com', 'Data Governance Officer',
    5, 4, 4,
    '2025-05-10 08:30:00', '2026-03-05 11:45:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    'Email and Collaboration Platform',
    'Microsoft Exchange and Teams - used by 2000+ employees for internal communications.',
    'Software',
    'Active',
    'Cloud (Microsoft 365)',
    'SaaS solution. Requires MFA. Data retention policies in place.',
    'it-security@securebank.com', 'IT Security Team',
    3, 3, 4,
    '2025-08-20 10:00:00', '2026-02-15 13:15:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    'Network Security - Firewall Cluster',
    'Enterprise-grade firewall protecting all ingress/egress traffic. Palo Alto Networks PA-7050s in active-active configuration.',
    'Hardware',
    'Active',
    'Primary Data Center - New York',
    'Redundant pair with automatic failover. Centrally managed and monitored. Regular firmware updates.',
    'network-ops@securebank.com', 'Network Operations',
    3, 5, 5,
    '2025-04-01 07:00:00', '2026-03-08 09:30:00', NULL,
    'john@doe.com', 'john@doe.com'
),
(
    'Physical Facilities - Main Office Building',
    'Primary office building housing IT infrastructure and ~500 employees. Contains server rooms and data centers.',
    'Facility',
    'Active',
    '123 Finance Street, New York, NY',
    'Biometric access control. Video surveillance. HVAC and power redundancy. Annual security audit.',
    'facilities@securebank.com', 'Facilities Management',
    2, 3, 5,
    '2025-03-15 08:00:00', '2026-01-30 16:20:00', NULL,
    'john@doe.com', 'john@doe.com'
);

-- =====================================================================
-- SAMPLE RISK NOTES
-- =====================================================================
INSERT INTO risk_notes (risk_id, content, author_id, author_name, created_at) VALUES
(
    1,
    'Penetration test completed 2026-02-15. Found 3 critical vulnerabilities in API authentication layer. All have been prioritized for immediate remediation. External vendor estimate: 6 weeks for full remediation.',
    'emp-001', 'Sarah Johnson',
    '2026-02-20 10:30:00'
),
(
    1,
    'Executive steering committee approved $250K budget allocation for API security infrastructure. Procurement process initiated. Expected delivery by end of Q2.',
    'emp-002', 'Mark Thompson',
    '2026-03-01 14:15:00'
),
(
    2,
    'PCI-DSS assessment update: 10 of 12 findings now resolved. Remaining 2 findings require network architecture changes targeting Q2 2026 completion.',
    'emp-003', 'Michael Chen',
    '2026-03-08 09:45:00'
),
(
    3,
    'High-availability infrastructure deployment successful. Failover testing showed RTO of 12 minutes (target was 15 minutes). Exceeding expectations.',
    'emp-004', 'James Wilson',
    '2026-02-28 11:20:00'
),
(
    4,
    'PAM implementation 75% complete. User acceptance testing underway with IT team. No blockers identified. On track for May 2026 production deployment.',
    'emp-005', 'Lisa Rodriguez',
    '2026-03-09 15:50:00'
),
(
    5,
    'Payment processor failover test conducted 2026-03-10. All transactions successfully routed to backup processor within 30 seconds. Excellent result.',
    'emp-006', 'David Kumar',
    '2026-03-11 13:30:00'
),
(
    6,
    'New AML system in production. False positive rate dropped from 15% to 2%. Customer service team reports significant reduction in friction.',
    'emp-007', 'Patricia Hayes',
    '2026-01-20 10:00:00'
);

-- =====================================================================
-- SAMPLE ASSET NOTES
-- =====================================================================
INSERT INTO asset_notes (asset_id, content, author_name, author_email, created_at) VALUES
(
    1,
    'Core banking system patch management: All critical security patches applied as of 2026-03-10. Next patching window: 2026-03-24 (monthly maintenance window).',
    'James Wilson', 'j.wilson@securebank.com',
    '2026-03-10 16:45:00'
),
(
    2,
    'Payment gateway capacity analysis: Current peak throughput is 50,000 TPS. Projected growth requires upgrade to 100,000 TPS by Q4 2026. Budget request submitted.',
    'Sarah Chen', 's.chen@securebank.com',
    '2026-03-05 14:20:00'
),
(
    3,
    'Customer data warehouse: Archival policy implemented. Data older than 7 years moved to cold storage (AWS Glacier). Estimated cost savings: $100K annually.',
    'Robert Martinez', 'r.martinez@securebank.com',
    '2026-02-28 11:15:00'
),
(
    4,
    'Exchange Server: Migrated to latest version (Exchange 2021). Teams integration enhanced. Mobile app security hardened. Rollout 95% complete.',
    'Lisa Anderson', 'l.anderson@securebank.com',
    '2026-03-08 09:30:00'
),
(
    5,
    'Firewall firmware: Updated to version 9.1.11 on 2026-02-20. All security patches applied. Threat prevention signatures updated to latest (dated 2026-03-10).',
    'Kevin O''Brien', 'k.obrien@securebank.com',
    '2026-03-10 13:45:00'
),
(
    6,
    'Main office security audit completed. All findings addressed. Badge access system upgraded. New backup power generator installed (1.5 MW capacity).',
    'Jennifer White', 'j.white@securebank.com',
    '2026-03-01 15:30:00'
);

-- =====================================================================
-- SAMPLE TASK UPDATES / COMMENTS
-- =====================================================================
INSERT INTO task_updates (task_id, content, author_id, author_name, created_at) VALUES
(
    1,
    'Day 1: Project kickoff meeting held. Vendor (Apigee) selected. License agreement executed. Team training scheduled for next week.',
    'emp-001', 'Sarah Johnson',
    '2026-01-12 09:00:00'
),
(
    1,
    'Development environment setup complete. Initial configuration templates created. Development team starting implementation of authentication policies.',
    'emp-008', 'Alex Turner',
    '2026-01-26 14:30:00'
),
(
    1,
    'API gateway successfully deployed to staging environment. Initial testing shows improved latency (25ms reduction). DDoS mitigation thresholds being tuned.',
    'emp-008', 'Alex Turner',
    '2026-02-20 11:15:00'
),
(
    1,
    'Rate limiting policies configured. OAuth 2.0 token validation working. Pending: certificate pinning implementation and production readiness assessment.',
    'emp-001', 'Sarah Johnson',
    '2026-03-12 16:45:00'
),
(
    2,
    'Scheduled external penetration testing firm engagement. RFP responses received from 3 qualified vendors. Vendor selection in progress.',
    'emp-009', 'Michael Brown',
    '2026-02-15 10:30:00'
),
(
    3,
    'Database replication configured. Testing shows zero data loss in failover scenarios. Automated failover triggers validated. Ready for production deployment.',
    'emp-010', 'David Park',
    '2026-02-25 13:20:00'
),
(
    3,
    'Full disaster recovery drill scheduled for 2026-04-15. All stakeholders notified. Success criteria defined: RTO ≤15 min, RPO ≤5 min.',
    'emp-004', 'James Wilson',
    '2026-03-11 09:45:00'
),
(
    4,
    'PAM system procurement completed. Vendor onboarding underway. Credential vault setup in progress. Integration with LDAP/AD in planning phase.',
    'emp-005', 'Lisa Rodriguez',
    '2026-02-10 14:00:00'
),
(
    4,
    'PAM pilot: Deployed to 50-user test group (IT administrators). Zero issues reported. Positive feedback on audit trail functionality.',
    'emp-011', 'Jennifer Lee',
    '2026-03-01 11:30:00'
),
(
    5,
    'Secondary payment processor integration testing complete. All transaction types successfully tested: ACH, Wire, Card, International. Performance metrics exceed requirements.',
    'emp-006', 'David Kumar',
    '2026-01-25 15:45:00'
),
(
    6,
    'Sanctions screening system deployed to production. Real-time updates from OFAC/UN/EU databases enabled. False positive rate: 2% (down from 15%).',
    'emp-007', 'Patricia Hayes',
    '2025-12-20 10:15:00'
),
(
    6,
    'Customer service impact assessment: Fraud investigation time reduced by 40%. Customer complaints about false positives decreased 85%. High satisfaction.',
    'emp-012', 'Susan Martinez',
    '2026-01-30 14:00:00'
);

-- =====================================================================
-- SAMPLE EFFECTIVENESS SCORES (6 months of data)
-- Monthly snapshots showing improvement in risk management effectiveness
-- =====================================================================
INSERT INTO effectiveness_scores (score, total_inherent_score, total_residual_score, risk_count, calculated_at) VALUES
-- September 2025: Starting point (45% effectiveness)
(45.0, 2800, 1540, 6, '2025-09-30 23:59:59'),

-- October 2025: Early improvements from baseline work (48% effectiveness)
(48.0, 2800, 1456, 6, '2025-10-31 23:59:59'),

-- November 2025: Completion of first control implementations (52% effectiveness)
(52.0, 2800, 1344, 6, '2025-11-30 23:59:59'),

-- December 2025: Vendor security improvements take effect (55% effectiveness)
(55.0, 2800, 1260, 6, '2025-12-31 23:59:59'),

-- January 2026: Major compliance remediation milestones reached (61% effectiveness)
(61.0, 2800, 1092, 6, '2026-01-31 23:59:59'),

-- February 2026: API gateway and DR infrastructure complete (68% effectiveness)
(68.0, 2800, 896, 6, '2026-02-28 23:59:59'),

-- March 2026: Current state - PAM and AML systems operational (72% effectiveness)
(72.0, 2800, 784, 6, '2026-03-14 23:59:59');

-- =====================================================================
-- OPTIONAL: Verify sample data inserted successfully
-- =====================================================================
-- SELECT COUNT(*) as total_risks FROM risks WHERE deleted_at IS NULL;
-- SELECT COUNT(*) as total_tasks FROM tasks WHERE deleted_at IS NULL;
-- SELECT COUNT(*) as total_assets FROM assets WHERE deleted_at IS NULL;
-- SELECT COUNT(*) as total_effectiveness_scores FROM effectiveness_scores;
-- SELECT COUNT(*) as total_risk_notes FROM risk_notes;
-- SELECT COUNT(*) as total_asset_notes FROM asset_notes;
-- SELECT COUNT(*) as total_task_updates FROM task_updates;
