package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryHierarchicalSummaryDTO {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private String projectType;
    private String projectStatus;
    private String projectManager;
    private String projectOwner;
    private String projectDescription;
    private String projectStartDate;
    private String projectEndDate;
    private String projectBudget;
    private String projectActualCost;
    private String projectPlannedCost;
    private String projectVariance;
    private String projectVariancePercentage;
    private String projectPlannedHours;
    private String projectActualHours;
    private String projectVarianceHours;
    private String projectVarianceHoursPercentage;
    private String projectPlannedDays;
    private String projectActualDays;
    private String projectVarianceDays;
    private String projectVarianceDaysPercentage;
    private String projectPlannedMonths;
    private String projectActualMonths;
    private String projectVarianceMonths;
    private String projectVarianceMonthsPercentage;
    private String projectPlannedQuarters;
    private String projectActualQuarters;
    private String projectVarianceQuarters;
    private String projectVarianceQuartersPercentage;
    private String projectPlannedYears;
    private String projectActualYears;
    private String projectVarianceYears;
    private String projectVarianceYearsPercentage;
    private Long userId;
    private String username;
    private Integer totalTimeInMins;
    private Integer totalEntries;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<TimeEntryBreakdownDTO> breakdowns;
    private List<TimeEntryHierarchicalSummaryDTO> children;
}

