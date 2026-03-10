package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for insight cards used in reports
 */
import java.util.List;

/**
 * DTO for insight cards used in reports
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsightCardDTO {
    private String title;
    private Object value;
    private String subtitle;
    private TrendDTO trend;
    private String icon;
    private String color;
    private String type;
    private Double progress;
    private String secondaryValue;
    private String secondaryLabel;
    
    // New compact table properties
    private String layoutType;
    private String summaryText;
    private List<TableRowDTO> tableRows;

    // Constructor to maintain backward compatibility
    public InsightCardDTO(String title, Object value, String subtitle, TrendDTO trend,
                         String icon, String color, String type, Double progress,
                         String secondaryValue, String secondaryLabel) {
        this.title = title;
        this.value = value;
        this.subtitle = subtitle;
        this.trend = trend;
        this.icon = icon;
        this.color = color;
        this.type = type;
        this.progress = progress;
        this.secondaryValue = secondaryValue;
        this.secondaryLabel = secondaryLabel;
        this.layoutType = "default"; // Default to old layout
        this.summaryText = null;
        this.tableRows = null;
    }

    /**
     * DTO for trend information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDTO {
        private Double value;
        private String direction;
        private String label;
    }
    
    /**
     * DTO for table row data in compact table layout
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableRowDTO {
        private String category;
        private Object count;
        private String icon;
        private String color;
    }
}