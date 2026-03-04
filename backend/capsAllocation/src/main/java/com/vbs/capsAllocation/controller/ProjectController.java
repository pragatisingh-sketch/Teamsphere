package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.*;
import com.vbs.capsAllocation.service.EmployeeService;
import com.vbs.capsAllocation.service.ProjectService;
import com.vbs.capsAllocation.service.UserProjectService;
import com.vbs.capsAllocation.model.Activity;
import com.vbs.capsAllocation.util.LoggerUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Controller for managing projects in the Time Sheet System
 *
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserProjectService userProjectService;

    @Autowired
    private EmployeeService employeeService;

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping
    public ResponseEntity<BaseResponse<ProjectDTO>> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateProjectDTO createProjectDTO) {
        try {
            LoggerUtil.logDebug("Creating project: {} by user: {}", createProjectDTO.getProjectCode(), userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success("Project created successfully", projectService.createProject(createProjectDTO, userDetails.getUsername()), HttpStatus.CREATED.value()));
        } catch (Exception e) {
            LoggerUtil.logError("Error creating project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to create project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping
    public ResponseEntity<BaseResponse<List<ProjectDTO>>> getAllProjects() {
        try {
            LoggerUtil.logDebug("Fetching all projects");
            return ResponseEntity.ok(BaseResponse.success("Projects retrieved successfully", projectService.getAllProjects()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching projects: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve projects: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @PutMapping("/{projectId}")
    public ResponseEntity<BaseResponse<ProjectDTO>> updateProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectDTO updateProjectDTO) {
        try {
            LoggerUtil.logDebug("Updating project: {} by user: {}", projectId, userDetails.getUsername());
            return ResponseEntity.ok()
                    .body(BaseResponse.success("Project updated successfully", projectService.updateProject(projectId, updateProjectDTO, userDetails.getUsername())));
        } catch (Exception e) {
            LoggerUtil.logError("Error updating project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to update project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<BaseResponse<Integer>> deleteProject(@PathVariable Long projectId) {
        try {
            LoggerUtil.logDebug("Deleting project with ID: {}", projectId);
            projectService.deleteProject(projectId);
            return ResponseEntity.ok(BaseResponse.success("Project deleted successfully", HttpStatus.OK.value()));
        } catch (Exception e) {
            LoggerUtil.logError("Error deleting project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to delete project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/my-projects")
    public ResponseEntity<BaseResponse<List<ProjectDTO>>> getMyProjects(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching projects for user: {}", userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Projects retrieved successfully", projectService.getProjectsByLead(userDetails.getUsername())));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching user projects: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve projects: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/{projectId}")
    public ResponseEntity<BaseResponse<ProjectDTO>> getProjectById(@PathVariable Long projectId) {
        try {
            LoggerUtil.logDebug("Fetching project by ID: {}", projectId);
            return ResponseEntity.ok(BaseResponse.success("Project retrieved successfully", projectService.getProjectById(projectId)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching project by ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/code/{projectCode}")
    public ResponseEntity<BaseResponse<ProjectDTO>> getProjectByCode(@PathVariable String projectCode) {
        try {
            LoggerUtil.logDebug("Fetching project by code: {}", projectCode);
            return ResponseEntity.ok(BaseResponse.success("Project retrieved successfully", projectService.getProjectByCode(projectCode)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching project by code: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/{projectId}/assign")
    public ResponseEntity<BaseResponse<List<UserProjectDTO>>> assignUsersToProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody AssignProjectDTO assignProjectDTO) {
        try {
            LoggerUtil.logDebug("Assigning users to project: {} by user: {}", projectId, userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Users assigned to project successfully", userProjectService.assignUsersToProject(assignProjectDTO, userDetails.getUsername())));
        } catch (Exception e) {
            LoggerUtil.logError("Error assigning users to project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to assign users to project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/user/{userId}/assign-projects")
    public ResponseEntity<BaseResponse<List<UserProjectDTO>>> assignProjectsToUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId,
            @Valid @RequestBody AssignProjectsToUserDTO assignProjectsToUserDTO) {
        try {
            LoggerUtil.logDebug("Assigning projects to user: {} by user: {}", userId, userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Projects assigned to user successfully", userProjectService.assignProjectsToUser(assignProjectsToUserDTO, userDetails.getUsername())));
        } catch (Exception e) {
            LoggerUtil.logError("Error assigning projects to user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to assign projects to user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @DeleteMapping("/{projectId}/users/{userId}")
    public ResponseEntity<BaseResponse<String>> removeUserFromProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        try {
            LoggerUtil.logDebug("Removing user {} from project {} by user: {}", userId, projectId, userDetails.getUsername());
            userProjectService.removeUserFromProject(userId, projectId, userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("User removed from project successfully"));
        } catch (Exception e) {
            LoggerUtil.logError("Error removing user from project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to remove user from project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/{projectId}/users")
    public ResponseEntity<BaseResponse<List<UserProjectDTO>>> getUsersByProject(@PathVariable Long projectId) {
        try {
            LoggerUtil.logDebug("Fetching users for project: {}", projectId);
            return ResponseEntity.ok(BaseResponse.success("Users retrieved successfully", userProjectService.getUsersByProject(projectId)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching users for project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve users: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/team-assignments")
    public ResponseEntity<BaseResponse<List<UserProjectDTO>>> getTeamAssignments(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching team assignments for user: {}", userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Team assignments retrieved successfully", userProjectService.getTeamMembersWithProjects(userDetails.getUsername())));
        } catch (AccessDeniedException e) {
            LoggerUtil.logError("Access denied for user: {}", userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN.value()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching team assignments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve team assignments: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/activities")
    public ResponseEntity<BaseResponse<List<String>>> getAllActivities() {
        try {
            LoggerUtil.logDebug("Fetching all activities");
            return ResponseEntity.ok(BaseResponse.success("Activities retrieved successfully", Arrays.stream(Activity.values())
            .map(Activity::getName)
            .collect(Collectors.toList())));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching activities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve activities: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/current-user")
    public ResponseEntity<BaseResponse<UserDTO>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching current user: {}", userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Current user retrieved successfully", employeeService.getUser(userDetails.getUsername())));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching current user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve current user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
