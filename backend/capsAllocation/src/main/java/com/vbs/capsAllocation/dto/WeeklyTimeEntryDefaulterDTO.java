package com.vbs.capsAllocation.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO representing a user with their weekly time-entry status.
 * Used for the Time-Entry Pending Report showing who hasn't filled timesheets.
 */
public class WeeklyTimeEntryDefaulterDTO {

    private Long employeeId;
    private String ldap;
    private String employeeName;
    private String email;
    private String department; // team
    private String manager;
    private int missingWeeksCount; // Number of weeks with missing entries
    private List<WeeklyBreakdown> weeklyBreakdowns;

    // Default constructor
    public WeeklyTimeEntryDefaulterDTO() {
    }

    // Full constructor
    public WeeklyTimeEntryDefaulterDTO(Long employeeId, String ldap, String employeeName,
            String email, String department, String manager,
            int missingWeeksCount, List<WeeklyBreakdown> weeklyBreakdowns) {
        this.employeeId = employeeId;
        this.ldap = ldap;
        this.employeeName = employeeName;
        this.email = email;
        this.department = department;
        this.manager = manager;
        this.missingWeeksCount = missingWeeksCount;
        this.weeklyBreakdowns = weeklyBreakdowns;
    }

    // Getters and Setters
    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getLdap() {
        return ldap;
    }

    public void setLdap(String ldap) {
        this.ldap = ldap;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public int getMissingWeeksCount() {
        return missingWeeksCount;
    }

    public void setMissingWeeksCount(int missingWeeksCount) {
        this.missingWeeksCount = missingWeeksCount;
    }

    public List<WeeklyBreakdown> getWeeklyBreakdowns() {
        return weeklyBreakdowns;
    }

    public void setWeeklyBreakdowns(List<WeeklyBreakdown> weeklyBreakdowns) {
        this.weeklyBreakdowns = weeklyBreakdowns;
    }

    /**
     * Inner class representing the breakdown of a single week's missing entries.
     */
    public static class WeeklyBreakdown {
        private LocalDate weekStartDate;
        private LocalDate weekEndDate;
        private String weekLabel; // e.g., "29 Dec 2025 - 2 Jan 2026"
        private boolean wholeWeekMissing;
        private List<LocalDate> missingDays; // Specific days if not whole week

        // Default constructor
        public WeeklyBreakdown() {
        }

        // Full constructor
        public WeeklyBreakdown(LocalDate weekStartDate, LocalDate weekEndDate,
                String weekLabel, boolean wholeWeekMissing, List<LocalDate> missingDays) {
            this.weekStartDate = weekStartDate;
            this.weekEndDate = weekEndDate;
            this.weekLabel = weekLabel;
            this.wholeWeekMissing = wholeWeekMissing;
            this.missingDays = missingDays;
        }

        // Getters and Setters
        public LocalDate getWeekStartDate() {
            return weekStartDate;
        }

        public void setWeekStartDate(LocalDate weekStartDate) {
            this.weekStartDate = weekStartDate;
        }

        public LocalDate getWeekEndDate() {
            return weekEndDate;
        }

        public void setWeekEndDate(LocalDate weekEndDate) {
            this.weekEndDate = weekEndDate;
        }

        public String getWeekLabel() {
            return weekLabel;
        }

        public void setWeekLabel(String weekLabel) {
            this.weekLabel = weekLabel;
        }

        public boolean isWholeWeekMissing() {
            return wholeWeekMissing;
        }

        public void setWholeWeekMissing(boolean wholeWeekMissing) {
            this.wholeWeekMissing = wholeWeekMissing;
        }

        public List<LocalDate> getMissingDays() {
            return missingDays;
        }

        public void setMissingDays(List<LocalDate> missingDays) {
            this.missingDays = missingDays;
        }
    }
}
