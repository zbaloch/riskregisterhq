package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.Risk;
import com.riskregister.riskregisterapp.entities.RiskCategory;
import com.riskregister.riskregisterapp.entities.RiskStatus;
import com.riskregister.riskregisterapp.repositories.RiskRepository;
import com.riskregister.riskregisterapp.repositories.RiskCategoryRepository;
import com.riskregister.riskregisterapp.repositories.RiskStatusRepository;

@Service
public class RiskService {

    @Autowired
    private RiskRepository riskRepository;

    @Autowired
    private RiskStatusRepository riskStatusRepository;

    @Autowired
    private RiskCategoryRepository riskCategoryRepository;

    public List<Risk> findAll() {
        return riskRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
    }

    public Optional<Risk> findById(Long id) {
        return riskRepository.findByIdAndDeletedAtIsNull(id);
    }

    public Risk save(Risk risk) {
        Instant now = Instant.now();
        if (risk.getId() == null) {
            risk.setCreatedAt(now);
        }
        risk.setUpdatedAt(now);
        return riskRepository.save(risk);
    }

    public void softDelete(Long id) {
        riskRepository.findByIdAndDeletedAtIsNull(id).ifPresent(risk -> {
            risk.setDeletedAt(Instant.now());
            riskRepository.save(risk);
        });
    }

    public long countActive() {
        return riskRepository.countByDeletedAtIsNull();
    }

    public List<RiskStatus> getAllStatuses() {
        return riskStatusRepository.findAll();
    }

    public Map<RiskStatus, Long> countRisksByStatus() {
        Map<RiskStatus, Long> counts = new java.util.LinkedHashMap<>();
        List<RiskStatus> allStatuses = riskStatusRepository.findAll();

        // Define desired order: Accepted (4), Identified (1), Assessed (2), Mitigated (3), Closed (5)
        java.util.List<Long> desiredOrder = java.util.Arrays.asList(4L, 1L, 2L, 3L, 5L);

        for (Long statusId : desiredOrder) {
            RiskStatus status = allStatuses.stream()
                .filter(s -> s.getId().equals(statusId))
                .findFirst()
                .orElse(null);
            if (status != null) {
                long count = riskRepository.countByStatusIdAndDeletedAtIsNull(status.getId());
                counts.put(status, count);
            }
        }
        return counts;
    }

    public Map<RiskCategory, Long> countRisksByCategory() {
        Map<RiskCategory, Long> counts = new java.util.LinkedHashMap<>();
        List<RiskCategory> categories = riskCategoryRepository.findAll();
        for (RiskCategory category : categories) {
            long count = riskRepository.countByRiskCategoryIdAndDeletedAtIsNull(category.getId());
            counts.put(category, count);
        }
        return counts;
    }

    public Map<String, Object> getRiskScoreDistribution() {
        List<Risk> activeRisks = findAll();

        // Count risks by inherent likelihood
        long inherent1 = activeRisks.stream().filter(r -> r.getInherentLikelihood() != null && r.getInherentLikelihood() == 1).count();
        long inherent2 = activeRisks.stream().filter(r -> r.getInherentLikelihood() != null && r.getInherentLikelihood() == 2).count();
        long inherent3 = activeRisks.stream().filter(r -> r.getInherentLikelihood() != null && r.getInherentLikelihood() == 3).count();
        long inherent4 = activeRisks.stream().filter(r -> r.getInherentLikelihood() != null && r.getInherentLikelihood() == 4).count();
        long inherent5 = activeRisks.stream().filter(r -> r.getInherentLikelihood() != null && r.getInherentLikelihood() == 5).count();

        // Count risks by residual likelihood
        long residual1 = activeRisks.stream().filter(r -> r.getResidualLikelihood() != null && r.getResidualLikelihood() == 1).count();
        long residual2 = activeRisks.stream().filter(r -> r.getResidualLikelihood() != null && r.getResidualLikelihood() == 2).count();
        long residual3 = activeRisks.stream().filter(r -> r.getResidualLikelihood() != null && r.getResidualLikelihood() == 3).count();
        long residual4 = activeRisks.stream().filter(r -> r.getResidualLikelihood() != null && r.getResidualLikelihood() == 4).count();
        long residual5 = activeRisks.stream().filter(r -> r.getResidualLikelihood() != null && r.getResidualLikelihood() == 5).count();

        Map<String, Object> distribution = new java.util.LinkedHashMap<>();
        distribution.put("inherentData", java.util.Arrays.asList(inherent1, inherent2, inherent3, inherent4, inherent5));
        distribution.put("residualData", java.util.Arrays.asList(residual1, residual2, residual3, residual4, residual5));
        return distribution;
    }

    public Map<String, Object> getRisksByStatusAndCategory() {
        List<RiskStatus> statuses = riskStatusRepository.findAll();
        // Order statuses: Accepted (4), Identified (1), Assessed (2), Mitigated (3), Closed (5)
        java.util.List<Long> desiredOrder = java.util.Arrays.asList(4L, 1L, 2L, 3L, 5L);
        List<RiskStatus> orderedStatuses = new java.util.ArrayList<>();
        for (Long statusId : desiredOrder) {
            statuses.stream()
                .filter(s -> s.getId().equals(statusId))
                .findFirst()
                .ifPresent(orderedStatuses::add);
        }

        List<RiskCategory> categories = riskCategoryRepository.findAll();

        // Build data: for each status, count risks per category
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        Map<Long, java.util.List<Long>> statusData = new java.util.LinkedHashMap<>();

        for (RiskStatus status : orderedStatuses) {
            java.util.List<Long> countsByCategory = new java.util.ArrayList<>();
            for (RiskCategory category : categories) {
                long count = riskRepository.countByStatusIdAndRiskCategoryIdAndDeletedAtIsNull(status.getId(), category.getId());
                countsByCategory.add(count);
            }
            statusData.put(status.getId(), countsByCategory);
        }

        data.put("statuses", orderedStatuses);
        data.put("statusNames", orderedStatuses.stream().map(RiskStatus::getName).collect(java.util.stream.Collectors.toList()));
        data.put("categories", categories);
        data.put("categoryNames", categories.stream().map(RiskCategory::getName).collect(java.util.stream.Collectors.toList()));
        data.put("statusData", statusData);

        return data;
    }

    public Map<String, Object> getRiskScoresByCategory() {
        List<Risk> activeRisks = findAll();
        List<RiskCategory> categories = riskCategoryRepository.findAll();

        // Build data structure: for each category, count risks by score level for both inherent and residual
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        Map<Long, java.util.List<Long>> inherentData = new java.util.LinkedHashMap<>();
        Map<Long, java.util.List<Long>> residualData = new java.util.LinkedHashMap<>();

        for (RiskCategory category : categories) {
            List<Risk> categoryRisks = activeRisks.stream()
                .filter(r -> r.getRiskCategoryId() != null && r.getRiskCategoryId().equals(category.getId()))
                .collect(java.util.stream.Collectors.toList());

            // Count inherent scores for this category
            java.util.List<Long> inherentCounts = new java.util.ArrayList<>();
            for (int score = 1; score <= 5; score++) {
                final int currentScore = score;
                long count = categoryRisks.stream()
                    .filter(r -> r.getInherentLikelihood() != null && r.getInherentLikelihood() == currentScore)
                    .count();
                inherentCounts.add(count);
            }
            inherentData.put(category.getId(), inherentCounts);

            // Count residual scores for this category
            java.util.List<Long> residualCounts = new java.util.ArrayList<>();
            for (int score = 1; score <= 5; score++) {
                final int currentScore = score;
                long count = categoryRisks.stream()
                    .filter(r -> r.getResidualLikelihood() != null && r.getResidualLikelihood() == currentScore)
                    .count();
                residualCounts.add(count);
            }
            residualData.put(category.getId(), residualCounts);
        }

        data.put("categories", categories);
        data.put("categoryNames", categories.stream().map(RiskCategory::getName).collect(java.util.stream.Collectors.toList()));
        data.put("inherentData", inherentData);
        data.put("residualData", residualData);

        return data;
    }

    public Map<String, Long> getRisksByScoreLevel() {
        List<Risk> activeRisks = findAll();
        Map<String, Long> scoreDistribution = new java.util.LinkedHashMap<>();

        long veryLow = activeRisks.stream()
            .filter(r -> {
                Integer score = r.getResidualScore() != null ? r.getResidualScore() : r.getInherentScore();
                return score != null && score <= 2;
            })
            .count();

        long low = activeRisks.stream()
            .filter(r -> {
                Integer score = r.getResidualScore() != null ? r.getResidualScore() : r.getInherentScore();
                return score != null && score >= 3 && score <= 4;
            })
            .count();

        long medium = activeRisks.stream()
            .filter(r -> {
                Integer score = r.getResidualScore() != null ? r.getResidualScore() : r.getInherentScore();
                return score != null && score >= 5 && score <= 9;
            })
            .count();

        long high = activeRisks.stream()
            .filter(r -> {
                Integer score = r.getResidualScore() != null ? r.getResidualScore() : r.getInherentScore();
                return score != null && score >= 10 && score <= 16;
            })
            .count();

        long veryHigh = activeRisks.stream()
            .filter(r -> {
                Integer score = r.getResidualScore() != null ? r.getResidualScore() : r.getInherentScore();
                return score != null && score >= 17;
            })
            .count();

        scoreDistribution.put("Very Low", veryLow);
        scoreDistribution.put("Low", low);
        scoreDistribution.put("Medium", medium);
        scoreDistribution.put("High", high);
        scoreDistribution.put("Very High", veryHigh);

        return scoreDistribution;
    }

    public long countHighRisks() {
        List<Risk> activeRisks = findAll();
        return activeRisks.stream()
            .filter(r -> {
                Integer score = r.getResidualScore() != null ? r.getResidualScore() : r.getInherentScore();
                return score != null && score >= 15;
            })
            .count();
    }

    public List<Risk> getHighestRisks(int limit) {
        return findAll().stream()
            .sorted((r1, r2) -> {
                Integer score1 = r1.getResidualScore() != null ? r1.getResidualScore() : r1.getInherentScore();
                Integer score2 = r2.getResidualScore() != null ? r2.getResidualScore() : r2.getInherentScore();
                if (score1 == null) score1 = 0;
                if (score2 == null) score2 = 0;
                return score2.compareTo(score1);
            })
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    private int[][] buildHeatmapMatrix(List<Risk> risks, boolean isResidual) {
        int[][] heatmapMatrix = new int[5][5];

        for (Risk risk : risks) {
            Integer likelihood = isResidual ? risk.getResidualLikelihood() : risk.getInherentLikelihood();
            Integer impact = isResidual ? risk.getResidualImpact() : risk.getInherentImpact();

            // Only count risks with both likelihood and impact set
            if (likelihood != null && impact != null && likelihood >= 1 && likelihood <= 5 && impact >= 1 && impact <= 5) {
                heatmapMatrix[likelihood - 1][impact - 1]++;
            }
        }
        return heatmapMatrix;
    }

    private Map<Integer, String> getScoreColorMap() {
        Map<Integer, String> scoreColors = new java.util.LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 5; j++) {
                int score = i * j;
                String colorClass;
                if (score <= 2) {
                    colorClass = "bg-green-50 text-green-600";
                } else if (score <= 4) {
                    colorClass = "bg-yellow-50 text-yellow-600";
                } else if (score <= 9) {
                    colorClass = "bg-orange-50 text-orange-600";
                } else if (score <= 16) {
                    colorClass = "bg-rose-50 text-rose-600";
                } else {
                    colorClass = "bg-red-50 text-red-600";
                }
                scoreColors.put(score, colorClass);
            }
        }
        return scoreColors;
    }

    public Map<String, Object> getRiskHeatmaps() {
        List<Risk> activeRisks = findAll();

        // Filter risks: exclude Identified (1) and Closed (5), keep only Assessed (2), Mitigated (3), Accepted (4)
        List<Risk> filteredRisks = activeRisks.stream()
            .filter(r -> r.getStatusId() != null && r.getStatusId() >= 2 && r.getStatusId() <= 4)
            .collect(java.util.stream.Collectors.toList());

        // Build inherent and residual heatmap matrices
        int[][] inherentMatrix = buildHeatmapMatrix(filteredRisks, false);
        int[][] residualMatrix = buildHeatmapMatrix(filteredRisks, true);

        // Get color mapping
        Map<Integer, String> scoreColors = getScoreColorMap();

        // Build response
        Map<String, Object> heatmaps = new java.util.LinkedHashMap<>();
        Map<String, Object> inherentHeatmap = new java.util.LinkedHashMap<>();
        inherentHeatmap.put("matrix", inherentMatrix);
        inherentHeatmap.put("scoreColors", scoreColors);
        heatmaps.put("inherent", inherentHeatmap);

        Map<String, Object> residualHeatmap = new java.util.LinkedHashMap<>();
        residualHeatmap.put("matrix", residualMatrix);
        residualHeatmap.put("scoreColors", scoreColors);
        heatmaps.put("residual", residualHeatmap);

        return heatmaps;
    }
}
