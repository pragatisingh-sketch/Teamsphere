package com.vbs.capsAllocation.dto;

public class UtilizationDetailsDTO {
    private Long employeeId;
    private String employeeName;
    private String department;
    private Integer fullyUtilizedDays;
    private Integer partiallyUtilizedDays;
    private Integer zeroUtilizationDays;
    private Double utilizationPercentage;

    private String manager;
    private String project;
    private String program;

    // Default constructor
    public UtilizationDetailsDTO() {
    }

    // All-args constructor
    public UtilizationDetailsDTO(Long employeeId, String employeeName, String department, String manager,
            String project, String program,
            Integer fullyUtilizedDays, Integer partiallyUtilizedDays,
            Integer zeroUtilizationDays, Double utilizationPercentage) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.manager = manager;
        this.project = project;
        this.program = program;
        this.fullyUtilizedDays = fullyUtilizedDays;
        this.partiallyUtilizedDays = partiallyUtilizedDays;
        this.zeroUtilizationDays = zeroUtilizationDays;
        this.utilizationPercentage = utilizationPercentage;
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

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public Integer getFullyUtilizedDays() {
        return fullyUtilizedDays;
    }

    public void setFullyUtilizedDays(Integer fullyUtilizedDays) {
        this.fullyUtilizedDays = fullyUtilizedDays;
    }

    public Integer getPartiallyUtilizedDays() {
        return partiallyUtilizedDays;
    }

    public void setPartiallyUtilizedDays(Integer partiallyUtilizedDays) {
        this.partiallyUtilizedDays = partiallyUtilizedDays;
    }

    public Integer getZeroUtilizationDays() {
        return zeroUtilizationDays;
    }

    public void setZeroUtilizationDays(Integer zeroUtilizationDays) {
        this.zeroUtilizationDays = zeroUtilizationDays;
    }

    public Double getUtilizationPercentage() {
        return utilizationPercentage;
    }

    public void setUtilizationPercentage(Double utilizationPercentage) {
        this.utilizationPercentage = utilizationPercentage;
    }
}
