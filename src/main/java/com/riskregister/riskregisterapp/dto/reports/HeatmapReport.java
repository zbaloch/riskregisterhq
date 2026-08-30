package com.riskregister.riskregisterapp.dto.reports;

import java.util.List;

/**
 * A full heatmap. Rows run impact 5→1 (top to bottom) and columns likelihood 1→5
 * (left to right), so the grid reads like the conventional risk matrix with the
 * worst quadrant in the top-right.
 */
public record HeatmapReport(
    String title,
    List<List<HeatmapCell>> rows,
    int plotted,        // risks placed on the grid
    int unscored        // risks skipped because likelihood/impact are not both set
) {}
