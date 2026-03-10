        package com.vbs.capsAllocation.service.impl;

        import com.vbs.capsAllocation.dto.AssignProjectDTO;
        import com.vbs.capsAllocation.dto.AssignProjectsToUserDTO;
        import com.vbs.capsAllocation.dto.UserProjectDTO;
        import com.vbs.capsAllocation.model.*;
        import com.vbs.capsAllocation.repository.EmployeeRepository;
        import com.vbs.capsAllocation.repository.ProjectRepository;
        import com.vbs.capsAllocation.repository.UserProjectRepository;
        import com.vbs.capsAllocation.repository.UserRepository;
        import com.vbs.capsAllocation.service.UserProjectService;
        import jakarta.transaction.Transactional;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.http.HttpStatus;
        import org.springframework.stereotype.Service;
        import org.springframework.web.server.ResponseStatusException;

        import java.util.ArrayList;
        import java.util.List;
        import java.util.Optional;
        import java.util.stream.Collectors;

        @Service
        @Transactional
        public class UserProjectServiceImpl implements UserProjectService {

            private final UserProjectRepository userProjectRepository;
            private final UserRepository userRepository;
            private final ProjectRepository projectRepository;
            private final EmployeeRepository employeeRepository;

            @Autowired
            public UserProjectServiceImpl(UserProjectRepository userProjectRepository,
                                          UserRepository userRepository,
                                          ProjectRepository projectRepository,
                                          EmployeeRepository employeeRepository) {
                this.userProjectRepository = userProjectRepository;
                this.userRepository = userRepository;
                this.projectRepository = projectRepository;
                this.employeeRepository = employeeRepository;
            }

            @Override
            public List<UserProjectDTO> assignUsersToProject(AssignProjectDTO assignProjectDTO, String ldap) {
                User lead = userRepository.findByUsername(ldap)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

                if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads and managers can assign projects");
                }

                Project project = projectRepository.findById(assignProjectDTO.getProjectId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

                List<UserProjectDTO> result = new ArrayList<>();

                for (Long userId : assignProjectDTO.getUserIds()) {
                    Employee employee = employeeRepository.findByIdExcludingLobsNative(userId);
                    User user = userRepository.findByUsername(employee.getLdap())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));

                    // Check if mapping already exists
                    if (userProjectRepository.findByUserAndProject(user, project).isPresent()) {
                        continue; // Skip if already mapped
                    }

                    UserProject userProject = new UserProject();
                    userProject.setUser(user);
                    userProject.setProject(project);
                    userProject.setAssignedDate(assignProjectDTO.getAssignedDate());
                    userProject.setStatus(assignProjectDTO.getStatus());
                    UserProject savedMapping = userProjectRepository.save(userProject);
                    result.add(convertToDTO(savedMapping));
                }

                return result;
            }
            @Override
            public List<UserProjectDTO> assignProjectsToUser(AssignProjectsToUserDTO assignProjectsToUserDTO, String ldap) {
                User lead = userRepository.findByUsername(ldap)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

                if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads and managers can assign projects to users");
                }

                Employee employee = employeeRepository.findByIdExcludingLobsNative(assignProjectsToUserDTO.getUserId());
                User user = userRepository.findByUsername(employee.getLdap())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                List<Project> projects = projectRepository.findAllById(assignProjectsToUserDTO.getProjectIds());

                if (projects.size() != assignProjectsToUserDTO.getProjectIds().size()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more projects not found");
                }

                List<UserProject> createdMappings = new ArrayList<>();

                for (Project project : projects) {
                    // Check if mapping already exists
                    Optional<UserProject> existingMapping = userProjectRepository.findByUserAndProject(user, project);
                    if (existingMapping.isEmpty()) {
                        UserProject userProject = new UserProject();
                        userProject.setUser(user);
                        userProject.setProject(project);
                        userProject.setAssignedDate(assignProjectsToUserDTO.getAssignedDate());
                        userProject.setStatus(assignProjectsToUserDTO.getStatus());
                        createdMappings.add(userProjectRepository.save(userProject));
                    }
                }

                return createdMappings.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }
            @Override
            public void removeUserFromProject(Long userId, Long projectId, String ldap) {
                User lead = userRepository.findByUsername(ldap)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

                if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads can unassign projects");
                }

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                Project project = projectRepository.findById(projectId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

                UserProject userProject = userProjectRepository.findByUserAndProject(user, project)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not assigned to this project"));

                userProjectRepository.delete(userProject);
            }

            @Override
            public List<UserProjectDTO> getProjectsByUser(String ldap) {
                User user = userRepository.findByUsername(ldap)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                return userProjectRepository.findByUser(user).stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }
            @Override
            public List<UserProjectDTO> getUsersByProject(Long projectId) {
                Project project = projectRepository.findById(projectId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

                return userProjectRepository.findByProject(project).stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            @Override
            public List<UserProjectDTO> getTeamMembersWithProjects(String leadLdap) {
                User lead = userRepository.findByUsername(leadLdap)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead/Manager not found"));

                if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads or managers can view team assignments");
                }

                if (lead.getRole() == Role.ADMIN_OPS_MANAGER) {
                    return userProjectRepository.findAll().stream()
                            .map(this::convertToDTO)
                            .collect(Collectors.toList());
                }

                // Find all employees who report to this lead/manager
                List<Employee> teamMembers;
                if (lead.getRole() == Role.MANAGER) {
                    teamMembers = employeeRepository.findByProgramManager(leadLdap);
                } else {
                    // For LEAD role
                    teamMembers = employeeRepository.findByLead(leadLdap);
                }

                if (teamMembers.isEmpty()) {
                    return new ArrayList<>();
                }

                // Get the usernames (ldap) of all team members
                List<String> teamMemberLdaps = teamMembers.stream()
                        .map(Employee::getLdap)
                        .collect(Collectors.toList());

                // Find all users corresponding to these ldaps
                List<User> teamUsers = userRepository.findByUsernameIn(teamMemberLdaps);

                if (teamUsers.isEmpty()) {
                    return new ArrayList<>();
                }

                // Get all project assignments for these team members
                List<UserProject> allAssignments = new ArrayList<>();
                for (User teamUser : teamUsers) {
                    allAssignments.addAll(userProjectRepository.findByUser(teamUser));
                }

                return allAssignments.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            private UserProjectDTO convertToDTO(UserProject userProject) {
                UserProjectDTO dto = new UserProjectDTO();
                dto.setId(userProject.getId());
                dto.setUserId(userProject.getUser().getId());
                dto.setUsername(userProject.getUser().getUsername());
                dto.setProjectId(userProject.getProject().getId());
                dto.setProjectCode(userProject.getProject().getProjectCode());
                dto.setProjectName(userProject.getProject().getProjectName());
                dto.setAssignedDate(userProject.getAssignedDate());
                dto.setStatus(userProject.getStatus());
                dto.setIsOvertimeEligible(userProject.getProject().getIsOvertimeEligible());
                return dto;
            }
        }
