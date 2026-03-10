package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for per-day leave configuration in mixed leave type requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayConfigurationDto {
    private String date; // Format: yyyy-MM-dd
    private String leaveType; // Sick Leave, Casual Leave, Earned Leave
    private String duration; // Full Day, Half Day AM, Half Day PM
    private Double durationValue; // 1.0 for full day, 0.5 for half day
}
