package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.*;
import java.util.List;

/**
 * Service interface for managing projects and user-project relationships.
 */
public interface ProjectService {

    /**
     * Create a new project.
     */
    ProjectDTO createProject(CreateProjectDTO createProjectDTO, String createdBy);

    /**
     * Get all projects.
     */
    List<ProjectDTO> getAllProjects();

    /**
     * Delete project by ID.
     */
    void deleteProject(Long projectId);

    /**
     * Get project by lead.
     */
    List<ProjectDTO> getProjectsByLead(String leadLdap);

    /**
     * Get project by ID.
     */
    ProjectDTO getProjectById(Long projectId);

    /**
     * Get project by code.
     */
    ProjectDTO getProjectByCode(String projectCode);

    /**
     * Update project by ID.
     */
    ProjectDTO updateProject(Long projectId, CreateProjectDTO updateProjectDTO, String updatedBy);
}