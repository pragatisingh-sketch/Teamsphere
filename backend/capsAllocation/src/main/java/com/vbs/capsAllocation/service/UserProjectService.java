package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.*;

import java.util.List;

/**
 * Service interface for managing user-project assignments.
 */
public interface UserProjectService {

    /**
     * Assign users to a project.
     */
    List<UserProjectDTO> assignUsersToProject(AssignProjectDTO assignProjectDTO, String assignedBy);

    /**
     * Assign projects to a user.
     */
    List<UserProjectDTO> assignProjectsToUser(AssignProjectsToUserDTO assignProjectsToUserDTO, String assignedBy);

    /**
     * Remove a user from a project.
     */
    void removeUserFromProject(Long userId, Long projectId, String removedBy);

    /**
     * Get users assigned to a project.
     */
    List<UserProjectDTO> getUsersByProject(Long projectId);

    /**
     * Get team members with their project assignments.
     */
    List<UserProjectDTO> getTeamMembersWithProjects(String ldap);


    List<UserProjectDTO> getProjectsByUser(String username);
}
