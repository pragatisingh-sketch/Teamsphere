package com.vbs.capsAllocation.dto;

public class ComplianceDetailsDTO {
    private Long employeeId;
    private String employeeName;
    private String department;
    private Integer timeEntryIssues;
    private Integer attendanceIssues;
    private Integer leaveIssues;
    private Integer totalNonCompliance;

    // Default constructor
    public ComplianceDetailsDTO() {
    }

    // All-args constructor
    public ComplianceDetailsDTO(Long employeeId, String employeeName, String department,
            Integer timeEntryIssues, Integer attendanceIssues,
            Integer leaveIssues, Integer totalNonCompliance) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.timeEntryIssues = timeEntryIssues;
        this.attendanceIssues = attendanceIssues;
        this.leaveIssues = leaveIssues;
        this.totalNonCompliance = totalNonCompliance;
    }

    // Getters and Setters
    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getTimeEntryIssues() {
        return timeEntryIssues;
    }

    public void setTimeEntryIssues(Integer timeEntryIssues) {
        this.timeEntryIssues = timeEntryIssues;
    }

    public Integer getAttendanceIssues() {
        return attendanceIssues;
    }

    public void setAttendanceIssues(Integer attendanceIssues) {
        this.attendanceIssues = attendanceIssues;
    }

    public Integer getLeaveIssues() {
        return leaveIssues;
    }

    public void setLeaveIssues(Integer leaveIssues) {
        this.leaveIssues = leaveIssues;
    }

    public Integer getTotalNonCompliance() {
        return totalNonCompliance;
    }

    public void setTotalNonCompliance(Integer totalNonCompliance) {
        this.totalNonCompliance = totalNonCompliance;
    }
}
