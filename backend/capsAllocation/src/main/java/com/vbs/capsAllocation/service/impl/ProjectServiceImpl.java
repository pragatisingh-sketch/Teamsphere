package com.vbs.capsAllocation.service.impl;

import com.vbs.capsAllocation.dto.CreateProjectDTO;
import com.vbs.capsAllocation.dto.ProjectDTO;
import com.vbs.capsAllocation.model.Project;
import com.vbs.capsAllocation.model.Role;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.repository.ProjectRepository;
import com.vbs.capsAllocation.repository.UserRepository;
import com.vbs.capsAllocation.service.ProjectService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Autowired
    public ProjectServiceImpl(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ProjectDTO createProject(CreateProjectDTO createProjectDTO, String ldap) {
        User lead = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

        if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads can create projects");
        }

        if (projectRepository.findByProjectCode(createProjectDTO.getProjectCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project code already exists");
        }

        Project project = new Project();
        project.setProjectCode(createProjectDTO.getProjectCode());
        project.setProjectName(createProjectDTO.getProjectName());
        project.setCreatedBy(lead);
        project.setIsOvertimeEligible(createProjectDTO.getIsOvertimeEligible() != null ? createProjectDTO.getIsOvertimeEligible() : false);

        Project savedProject = projectRepository.save(project);
        return convertToDTO(savedProject);
    }
    @Override
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    @Override
    public List<ProjectDTO> getProjectsByLead(String leadName) {
        User lead = userRepository.findByUsername(leadName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

        return projectRepository.findByCreatedBy(lead).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    @Override
    public ProjectDTO getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        return convertToDTO(project);
    }
    @Override
    public ProjectDTO getProjectByCode(String projectCode) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        return convertToDTO(project);
    }
    private ProjectDTO convertToDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setProjectCode(project.getProjectCode());
        dto.setProjectName(project.getProjectName());
        dto.setCreatedById(project.getCreatedBy().getId());
        dto.setCreatedByUsername(project.getCreatedBy().getUsername());
        dto.setIsOvertimeEligible(project.getIsOvertimeEligible());
        return dto;
    }
    @Override
    public void deleteProject(Long projectId) {
        projectRepository.deleteById(projectId);
    }

    @Override
    public ProjectDTO updateProject(Long projectId, CreateProjectDTO updateProjectDTO, String updatedBy) {
        User user = userRepository.findByUsername(updatedBy)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.LEAD && user.getRole() != Role.MANAGER && user.getRole() != Role.ADMIN_OPS_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads, managers, and admin ops managers can update projects");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        // Check if project code is being changed and if it already exists
        if (!project.getProjectCode().equals(updateProjectDTO.getProjectCode())) {
            if (projectRepository.findByProjectCode(updateProjectDTO.getProjectCode()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Project code already exists");
            }
            project.setProjectCode(updateProjectDTO.getProjectCode());
        }

        project.setProjectName(updateProjectDTO.getProjectName());
        project.setIsOvertimeEligible(updateProjectDTO.getIsOvertimeEligible() != null ? updateProjectDTO.getIsOvertimeEligible() : false);

        Project savedProject = projectRepository.save(project);
        return convertToDTO(savedProject);
    }
}
