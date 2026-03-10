package com.vbs.capsAllocation.dto;

import com.vbs.capsAllocation.common.DateRange;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO for defaulter comparison specific to an entity type (TimeEntry, Attendance, etc.)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityDefaulterComparison {
    private String entityTypeName;
    private long currentCount;
    private long previousCount;
    private DateRange previousRange;
    
    /**
     * Calculate trend percentage
     * @return trend percentage formatted to 1 decimal place
     */
    public double calculateTrendPercentage() {
        // If there was no previous data → 100% growth by default
        if (previousCount == 0) {
            return currentCount > 0 ? 100.0 : 0.0;
        }
        
        // (current - previous) / previous * 100
        double trend = ((double) (currentCount - previousCount) / previousCount) * 100.0;
        
        // Use BigDecimal for precise decimal formatting
        BigDecimal bd = new BigDecimal(trend);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
}