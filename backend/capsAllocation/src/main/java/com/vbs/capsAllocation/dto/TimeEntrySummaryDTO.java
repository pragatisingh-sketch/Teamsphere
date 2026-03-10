package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntrySummaryDTO {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long userId;
    private String username;
    private Integer totalTimeInMins;
    private Integer totalEntries;
}
