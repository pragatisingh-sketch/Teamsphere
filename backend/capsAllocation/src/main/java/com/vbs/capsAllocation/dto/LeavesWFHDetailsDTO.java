package com.vbs.capsAllocation.dto;

public class LeavesWFHDetailsDTO {
    private Long employeeId;
    private String employeeName;
    private String department;
    private Integer leavesCount;
    private Integer wfhCount;
    private Integer totalDays;

    // Default constructor
    public LeavesWFHDetailsDTO() {
    }

    // All-args constructor
    public LeavesWFHDetailsDTO(Long employeeId, String employeeName, String department,
            Integer leavesCount, Integer wfhCount, Integer totalDays) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.leavesCount = leavesCount;
        this.wfhCount = wfhCount;
        this.totalDays = totalDays;
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

    public Integer getLeavesCount() {
        return leavesCount;
    }

    public void setLeavesCount(Integer leavesCount) {
        this.leavesCount = leavesCount;
    }

    public Integer getWfhCount() {
        return wfhCount;
    }

    public void setWfhCount(Integer wfhCount) {
        this.wfhCount = wfhCount;
    }

    public Integer getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Integer totalDays) {
        this.totalDays = totalDays;
    }
}
