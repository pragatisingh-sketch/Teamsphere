package com.vbs.capsAllocation.dto;

/**
 * DTO for returning user's recent project information
 * Used in leave application time entry preview
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class RecentProjectDTO {

    private String projectName;
    private Long projectId;

    public RecentProjectDTO() {
    }

    public RecentProjectDTO(String projectName, Long projectId) {
        this.projectName = projectName;
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
