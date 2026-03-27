package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.EffectivenessScore;
import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.repositories.EffectivenessScoreRepository;
import com.riskregister.riskregisterapp.repositories.OrganizationRepository;

@Service
public class EffectivenessScoreService {

    @Autowired
    private EffectivenessScoreRepository effectivenessScoreRepository;

    @Autowired
    private RiskService riskService;

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Calculates the Risk Management Effectiveness score for a specific organization.
     * Formula: (1 - Total Residual / Total Inherent) × 100%
     * Only includes risks with status Assessed (2), Mitigated (3), or Accepted (4)
     */
    public EffectivenessScore calculateEffectivenessScore(Long organizationId) {
        List<Risk> allRisks = riskService.findAll(organizationId);

        // Filter for approved risks (Assessed, Mitigated, Accepted)
        List<Risk> approvedRisks = allRisks.stream()
            .filter(r -> r.getStatusId() != null && r.getStatusId() >= 2 && r.getStatusId() <= 4)
            .toList();

        if (approvedRisks.isEmpty()) {
            EffectivenessScore score = new EffectivenessScore(0.0, 0L, 0L, 0);
            score.setOrganizationId(organizationId);
            return score;
        }

        // Calculate total inherent and residual scores
        long totalInherentScore = 0;
        long totalResidualScore = 0;

        for (Risk risk : approvedRisks) {
            Integer inherentScore = risk.getInherentScore();
            Integer residualScore = risk.getResidualScore();

            if (inherentScore != null) {
                totalInherentScore += inherentScore;
            }

            if (residualScore != null) {
                totalResidualScore += residualScore;
            }
        }

        // Calculate effectiveness: (1 - Total Residual / Total Inherent) × 100%
        Double effectiveness = 0.0;
        if (totalInherentScore > 0) {
            effectiveness = (1.0 - (double) totalResidualScore / totalInherentScore) * 100.0;
            effectiveness = Math.max(0.0, Math.min(100.0, effectiveness)); // Clamp to 0-100
        }

        EffectivenessScore score = new EffectivenessScore(effectiveness, totalInherentScore, totalResidualScore, approvedRisks.size());
        score.setOrganizationId(organizationId);
        return effectivenessScoreRepository.save(score);
    }

    /**
     * Monthly scheduled job to calculate and store effectiveness score for all organizations
     * Runs at 2 AM on the first day of each month
     */
    @Scheduled(cron = "0 2 1 * * *")
    public void scheduleMonthlyCalculation() {
        organizationRepository.findAll().forEach(org -> calculateEffectivenessScore(org.getId()));
    }

    /**
     * Get the latest effectiveness score for a specific organization
     */
    public EffectivenessScore getLatestScore(Long organizationId) {
        return effectivenessScoreRepository.findFirstByOrganizationIdOrderByCalculatedAtDesc(organizationId);
    }

    /**
     * Get effectiveness scores for the last N days for a specific organization
     */
    public List<EffectivenessScore> getScoresForLastDays(int days, Long organizationId) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return effectivenessScoreRepository.findByOrganizationIdAndCalculatedAtAfterOrderByCalculatedAtAsc(organizationId, cutoff);
    }

    /**
     * Get all effectiveness scores
     */
    public List<EffectivenessScore> getAllScores() {
        return effectivenessScoreRepository.findAll();
    }
}
