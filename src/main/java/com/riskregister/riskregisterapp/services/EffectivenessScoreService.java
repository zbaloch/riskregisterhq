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

@Service
public class EffectivenessScoreService {

    @Autowired
    private EffectivenessScoreRepository effectivenessScoreRepository;

    @Autowired
    private RiskService riskService;

    /**
     * Calculates the Risk Management Effectiveness score.
     * Formula: (1 - Total Residual / Total Inherent) × 100%
     * Only includes risks with status Assessed (2), Mitigated (3), or Accepted (4)
     */
    public EffectivenessScore calculateEffectivenessScore() {
        List<Risk> allRisks = riskService.findAll();

        // Filter for approved risks (Assessed, Mitigated, Accepted)
        List<Risk> approvedRisks = allRisks.stream()
            .filter(r -> r.getStatusId() != null && r.getStatusId() >= 2 && r.getStatusId() <= 4)
            .toList();

        if (approvedRisks.isEmpty()) {
            return new EffectivenessScore(0.0, 0L, 0L, 0);
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
        return effectivenessScoreRepository.save(score);
    }

    /**
     * Monthly scheduled job to calculate and store effectiveness score
     * Runs at 2 AM on the first day of each month
     */
    @Scheduled(cron = "0 2 1 * * *")
    public void scheduleMonthlyCalculation() {
        calculateEffectivenessScore();
    }

    /**
     * Get the latest effectiveness score
     */
    public EffectivenessScore getLatestScore() {
        return effectivenessScoreRepository.findFirstByOrderByCalculatedAtDesc();
    }

    /**
     * Get effectiveness scores for the last N days
     */
    public List<EffectivenessScore> getScoresForLastDays(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return effectivenessScoreRepository.findByCalculatedAtAfterOrderByCalculatedAtAsc(cutoff);
    }

    /**
     * Get all effectiveness scores
     */
    public List<EffectivenessScore> getAllScores() {
        return effectivenessScoreRepository.findAll();
    }
}
