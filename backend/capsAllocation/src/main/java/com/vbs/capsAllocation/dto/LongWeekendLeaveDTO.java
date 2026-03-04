package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for Long Weekend Leave Pattern Report
 * Contains employee information and their long weekend leave patterns
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LongWeekendLeaveDTO {
    private Long employeeId;
    private String employeeName;
    private String ldap;
    private String email;
    private String department;
    private String manager;
    private int occurrenceCount;
    private List<LongWeekendInstance> instances;

    /**
     * Nested class representing a single long weekend leave instance
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LongWeekendInstance {
        private LocalDate startDate;
        private LocalDate endDate;
        private String leaveType;
        private int totalDays;
        private String pattern; // Description like "Friday leave + Weekend + Holiday (Monday)"
    }
}
