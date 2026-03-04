package com.vbs.capsAllocation.service.impl;

import java.util.*;
import com.vbs.capsAllocation.repository.EmployeeRelationRepository;

import com.vbs.capsAllocation.dto.*;
import com.vbs.capsAllocation.model.*;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.repository.ProjectRepository;
import com.vbs.capsAllocation.repository.TimeEntryRepository;
import com.vbs.capsAllocation.repository.UserProjectRepository;
import com.vbs.capsAllocation.repository.UserRepository;
import com.vbs.capsAllocation.service.TimeEntryService;
import com.vbs.capsAllocation.util.LoggerUtil;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimeEntryServiceImpl implements TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeRelationRepository employeeRelationRepository;
    private final com.vbs.capsAllocation.service.DelegationService delegationService;

    @Autowired
    public TimeEntryServiceImpl(TimeEntryRepository timeEntryRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            UserProjectRepository userProjectRepository,
            EmployeeRepository employeeRepository,
            EmployeeRelationRepository employeeRelationRepository,
            com.vbs.capsAllocation.service.DelegationService delegationService) {
        this.timeEntryRepository = timeEntryRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.userProjectRepository = userProjectRepository;
        this.employeeRepository = employeeRepository;
        this.employeeRelationRepository = employeeRelationRepository;
        this.delegationService = delegationService;
    }

    @Override
    public TimeEntryDTO createTimeEntry(CreateTimeEntryDTO createTimeEntryDTO, String username) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "createTimeEntry", createTimeEntryDTO, username);
        String targetLdap = (createTimeEntryDTO.getLdap() != null && !createTimeEntryDTO.getLdap().isEmpty())
                ? createTimeEntryDTO.getLdap()
                : username;

        User user = userRepository.findByUsername(targetLdap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Project project = projectRepository.findById(createTimeEntryDTO.getProjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        // Check if user is assigned to this project
        if (userProjectRepository.findByUserAndProject(user, project).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not assigned to this project");
        }

        // Calculate total normal time for the day (excluding overtime entries)
        Integer totalNormalTimeForDay = timeEntryRepository.getTotalNormalTimeByUserAndDate(user,
                createTimeEntryDTO.getEntryDate());
        if (totalNormalTimeForDay == null) {
            totalNormalTimeForDay = 0;
        }

        // Check if adding this entry would exceed 8 hours (480 minutes), but skip check
        // for overtime entries
        Boolean isOvertime = createTimeEntryDTO.getIsOvertime();
        if ((isOvertime == null || !isOvertime) && totalNormalTimeForDay + createTimeEntryDTO.getTimeInMins() > 480) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Total time for the day cannot exceed 8 hours. Please use overtime toggle for additional hours.");
        }

        TimeEntry timeEntry = new TimeEntry();
        timeEntry.setUser(user);
        timeEntry.setProject(project);
        timeEntry.setEntryDate(createTimeEntryDTO.getEntryDate());
        timeEntry.setLdap(user.getUsername()); // Using username as LDAP for simplicity
        timeEntry.setProcess(createTimeEntryDTO.getProcess());
        timeEntry.setActivity(createTimeEntryDTO.getActivity());
        timeEntry.setTimeInMins(createTimeEntryDTO.getTimeInMins());
        timeEntry.setAttendanceType(createTimeEntryDTO.getAttendanceType());
        timeEntry.setComment(createTimeEntryDTO.getComment());
        timeEntry.setStatus(TimeEntryStatus.PENDING);
        timeEntry.setIsOvertime(createTimeEntryDTO.getIsOvertime());

        // Set lead as the project creator
        User userLead = userRepository.findById(createTimeEntryDTO.getLeadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
        timeEntry.setLead(userLead);

        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        timeEntry.setCreatedAt(now);
        timeEntry.setUpdatedAt(now);

        // Determine if this is a defaulter entry
        timeEntry.setIsDefaulter(isTimeEntryDefaulter(timeEntry.getEntryDate(), now));

        TimeEntry savedEntry = timeEntryRepository.save(timeEntry);
        return convertToDTO(savedEntry);
    }

    @Override
    public List<TimeEntryDTO> createBulkTimeEntries(List<CreateTimeEntryDTO> bulkTimeEntriesDTO, String username) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "createBulkTimeEntries", bulkTimeEntriesDTO, username);

        if (bulkTimeEntriesDTO == null || bulkTimeEntriesDTO.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No time entries provided");
        }

        // Validate all entries are for the same date and user
        CreateTimeEntryDTO firstEntry = bulkTimeEntriesDTO.get(0);
        LocalDate entryDate = firstEntry.getEntryDate();
        String ldap = firstEntry.getLdap();

        for (CreateTimeEntryDTO dto : bulkTimeEntriesDTO) {
            if (!dto.getEntryDate().equals(entryDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All time entries must be for the same date");
            }
            if (!dto.getLdap().equals(ldap)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All time entries must be for the same user");
            }
        }

        // Get the user
        String targetLdap = (ldap != null && !ldap.isEmpty()) ? ldap : username;
        User user = userRepository.findByUsername(targetLdap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Calculate total time from all entries
        int totalTime = bulkTimeEntriesDTO.stream()
                .mapToInt(CreateTimeEntryDTO::getTimeInMins)
                .sum();

        // Calculate existing normal time for the day
        Integer existingNormalTime = timeEntryRepository.getTotalNormalTimeByUserAndDate(user, entryDate);
        if (existingNormalTime == null) {
            existingNormalTime = 0;
        }

        // Check if any entry is overtime
        boolean hasOvertime = bulkTimeEntriesDTO.stream()
                .anyMatch(dto -> dto.getIsOvertime() != null && dto.getIsOvertime());

        // If not overtime, validate daily limit
        if (!hasOvertime && (existingNormalTime + totalTime) > 480) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Total time for the day (%d minutes) would exceed 8 hours (480 minutes). " +
                            "Please enable overtime or adjust times. Existing: %d, Adding: %d",
                            existingNormalTime + totalTime, existingNormalTime, totalTime));
        }

        // Create all entries
        List<TimeEntryDTO> createdEntries = new ArrayList<>();
        for (CreateTimeEntryDTO dto : bulkTimeEntriesDTO) {
            TimeEntryDTO created = createTimeEntry(dto, username);
            createdEntries.add(created);
        }

        return createdEntries;
    }

    @Override
    public TimeEntryDTO updateTimeEntry(Long timeEntryId, CreateTimeEntryDTO updateDTO, String ldap) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "updateTimeEntry", timeEntryId, updateDTO, ldap);
        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found"));

        // // Check if the entry belongs to the user
        // if (!timeEntry.getUser().getUsername().equals(ldap)) {
        // throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update
        // your own time entries");
        // }

        // // Check if the entry is in REJECTED status
        // if (timeEntry.getStatus() != TimeEntryStatus.REJECTED) {
        // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only rejected time
        // entries can be updated");
        // }

        Project project = projectRepository.findById(updateDTO.getProjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        // Check if user is assigned to this project
        User user = userRepository.findByUsername(updateDTO.getLdap())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (userProjectRepository.findByUserAndProject(user, project).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not assigned to this project");
        }

        // Calculate total normal time for the day excluding this entry (only if current
        // entry is not overtime)
        Integer totalNormalTimeForDay = timeEntryRepository.getTotalNormalTimeByUserAndDate(user,
                updateDTO.getEntryDate());
        if (totalNormalTimeForDay == null) {
            totalNormalTimeForDay = 0;
        }
        // Only subtract current entry time if it's not an overtime entry
        if (timeEntry.getIsOvertime() == null || !timeEntry.getIsOvertime()) {
            totalNormalTimeForDay -= timeEntry.getTimeInMins();
        }

        // Check if updating this entry would exceed 8 hours (480 minutes), but skip
        // check for overtime entries
        Boolean isOvertime = updateDTO.getIsOvertime();
        if ((isOvertime == null || !isOvertime) && totalNormalTimeForDay + updateDTO.getTimeInMins() > 480) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Total time for the day cannot exceed 8 hours. Please use overtime toggle for additional hours.");
        }

        timeEntry.setProject(project);
        timeEntry.setEntryDate(updateDTO.getEntryDate());
        timeEntry.setProcess(updateDTO.getProcess());
        timeEntry.setActivity(updateDTO.getActivity());
        timeEntry.setTimeInMins(updateDTO.getTimeInMins());
        timeEntry.setAttendanceType(updateDTO.getAttendanceType());
        timeEntry.setComment(updateDTO.getComment());
        timeEntry.setStatus(TimeEntryStatus.PENDING); // Reset to pending after update
        timeEntry.setRejectionComment(null); // Clear rejection comment
        timeEntry.setIsOvertime(updateDTO.getIsOvertime());
        timeEntry.setLead(userRepository.findById(updateDTO.getLeadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found")));

        // Update timestamp
        timeEntry.setUpdatedAt(LocalDateTime.now());

        // Re-determine defaulter status based on current time
        timeEntry.setIsDefaulter(isTimeEntryDefaulter(timeEntry.getEntryDate(), LocalDateTime.now()));

        TimeEntry updatedEntry = timeEntryRepository.save(timeEntry);
        return convertToDTO(updatedEntry);
    }

    @Override
    public void deleteTimeEntry(Long timeEntryId, String username) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "deleteTimeEntry", timeEntryId, username);
        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found"));

        // // Check if the entry belongs to the user
        // if (!timeEntry.getUser().getUsername().equals(username)) {
        // throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete
        // your own time entries");
        // }

        // // Check if the entry is in REJECTED status
        // if (timeEntry.getStatus() != TimeEntryStatus.REJECTED) {
        // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only rejected time
        // entries can be deleted");
        // }

        timeEntryRepository.delete(timeEntry);
    }

    @Override
    public TimeEntryDTO approveTimeEntry(TimeEntryApprovalDTO approvalDTO, String ldap) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "approveTimeEntry", approvalDTO, ldap);
        User lead = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

        if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads can approve time entries");
        }
        TimeEntry timeEntry = timeEntryRepository.findById(approvalDTO.getTimeEntryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found"));

        // Check if the lead is assigned to approve this entry
        // if (!timeEntry.getLead().getUsername().equals(ldap)) {
        // throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not
        // authorized to approve this time entry");
        // }

        // Check if the entry is in PENDING or SUBMITTED status
        if (timeEntry.getStatus() != TimeEntryStatus.PENDING && timeEntry.getStatus() != TimeEntryStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only pending or submitted time entries can be approved");
        }

        timeEntry.setStatus(TimeEntryStatus.APPROVED);

        // Unlock auto-generated entries upon approval
        if (timeEntry.getIsLocked() != null && timeEntry.getIsLocked()) {
            timeEntry.setIsLocked(false);
            timeEntry.setLockedBy(null);
            timeEntry.setLockedAt(null);
        }

        timeEntry.setUpdatedAt(LocalDateTime.now());
        TimeEntry updatedEntry = timeEntryRepository.save(timeEntry);
        return convertToDTO(updatedEntry);
    }

    @Override
    public TimeEntryDTO rejectTimeEntry(TimeEntryRejectionDTO rejectionDTO, String ldap) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "rejectTimeEntry", rejectionDTO, ldap);
        User lead = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

        if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads can reject time entries");
        }

        TimeEntry timeEntry = timeEntryRepository.findById(rejectionDTO.getTimeEntryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found"));

        // Check if the lead is assigned to approve this entry
        // if (!timeEntry.getLead().getUsername().equals(ldap)) {
        // throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not
        // authorized to reject this time entry");
        // }

        // Check if the entry is in PENDING or SUBMITTED status
        if (timeEntry.getStatus() != TimeEntryStatus.PENDING && timeEntry.getStatus() != TimeEntryStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only pending or submitted time entries can be rejected");
        }

        timeEntry.setStatus(TimeEntryStatus.REJECTED);
        timeEntry.setRejectionComment(rejectionDTO.getRejectionComment());
        timeEntry.setUpdatedAt(LocalDateTime.now());
        TimeEntry updatedEntry = timeEntryRepository.save(timeEntry);
        return convertToDTO(updatedEntry);
    }

    @Override
    public List<TimeEntryDTO> approveAllTimeEntry(@Valid TimeEntryApprovalIdsListDTO approvalIdsListDTO, String ldap) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "approveAllTimeEntry", approvalIdsListDTO, ldap);
        List<Long> timeEntryIds = approvalIdsListDTO.getTimeEntryId();
        User lead = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

        if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads can approve time entries");
        }

        List<TimeEntry> timeEntries = timeEntryRepository.findAllById(timeEntryIds);
        List<TimeEntryDTO> approvedEntries = new ArrayList<>();

        for (TimeEntry timeEntry : timeEntries) {
            if (timeEntry.getStatus() == TimeEntryStatus.PENDING
                    || timeEntry.getStatus() == TimeEntryStatus.SUBMITTED) {
                timeEntry.setStatus(TimeEntryStatus.APPROVED);

                // Unlock auto-generated entries upon approval
                if (timeEntry.getIsLocked() != null && timeEntry.getIsLocked()) {
                    timeEntry.setIsLocked(false);
                    timeEntry.setLockedBy(null);
                    timeEntry.setLockedAt(null);
                }

                timeEntry.setUpdatedAt(LocalDateTime.now());
                TimeEntry updatedEntry = timeEntryRepository.save(timeEntry);
                approvedEntries.add(convertToDTO(updatedEntry));
            } else {
                continue;
            }

        }

        return approvedEntries;

    }

    @Override
    public List<TimeEntryDTO> getTimeEntriesByUser(String ldap, LocalDate startDate, LocalDate endDate) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getTimeEntriesByUser", ldap, startDate, endDate);
        User user = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        System.out.println("USER " + user);
        List<TimeEntry> entries;
        if (startDate != null && endDate != null) {
            entries = timeEntryRepository.findByUserAndEntryDateBetween(user, startDate, endDate);
        } else {
            entries = timeEntryRepository.findByUser(user);
        }

        return entries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimeEntryDTO> getPendingTimeEntriesByLead(String ldap, LocalDate startDate, LocalDate endDate) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getPendingTimeEntriesByLead", ldap, startDate, endDate);
        User lead = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
        if (lead.getRole() != Role.LEAD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads can view pending time entries");
        }

        // Get delegators for the current user
        List<String> delegators = delegationService.getDelegatorsForDelegatee(ldap);
        List<String> allLdaps = new ArrayList<>();
        allLdaps.add(ldap);
        allLdaps.addAll(delegators);

        List<TimeEntry> entries = timeEntryRepository.findPendingTimeEntriesByLeadIn(
                startDate != null ? startDate : LocalDate.MIN,
                endDate != null ? endDate : LocalDate.MAX,
                allLdaps);

        return entries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimeEntrySummaryDTO> getTimeEntrySummary(Long userId, Long projectId, LocalDate startDate,
            LocalDate endDate) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getTimeEntrySummary", userId, projectId, startDate,
                endDate);
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }

        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<TimeEntry> entries = new ArrayList<>();

        if (userId != null && projectId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

            entries = timeEntryRepository.findByUserAndProjectAndEntryDateBetween(user, project, startDate, endDate);
        } else if (userId != null) {
            // Get entries for specific user across all projects
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            entries = timeEntryRepository.findByUserAndEntryDateBetween(user, startDate, endDate);
        } else if (projectId != null) {
            // Get entries for specific project across all users
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

            entries = timeEntryRepository.findByProjectAndEntryDateBetween(project, startDate, endDate);
        } else {
            entries = timeEntryRepository.findByEntryDateBetween(startDate, endDate);
        }

        // Group entries by project and user
        Map<String, TimeEntrySummaryDTO> summaryMap = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getProject().getId() + "-" + entry.getUser().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                entryList -> {
                                    TimeEntry firstEntry = entryList.get(0);
                                    TimeEntrySummaryDTO summary = new TimeEntrySummaryDTO();
                                    summary.setProjectId(firstEntry.getProject().getId());
                                    summary.setProjectCode(firstEntry.getProject().getProjectCode());
                                    summary.setProjectName(firstEntry.getProject().getProjectName());
                                    summary.setUserId(firstEntry.getUser().getId());
                                    summary.setUsername(firstEntry.getUser().getUsername());
                                    summary.setTotalTimeInMins(
                                            entryList.stream().mapToInt(TimeEntry::getTimeInMins).sum());
                                    summary.setTotalEntries(entryList.size());
                                    return summary;
                                })));

        return new ArrayList<>(summaryMap.values());
    }

    @Override
    public Integer getRemainingTimeForDay(UserDetails userDetails, String inputldap, LocalDate date) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getRemainingTimeForDay", userDetails, inputldap, date);

        if (date == null) {
            date = LocalDate.now();
        }
        String ldap = (inputldap != null && !inputldap.isEmpty()) ? inputldap : userDetails.getUsername();
        User user = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Integer totalNormalTimeForDay = timeEntryRepository.getTotalNormalTimeByUserAndDate(user, date);
        if (totalNormalTimeForDay == null) {
            totalNormalTimeForDay = 0;
        }

        // 8 hours = 480 minutes
        return 480 - totalNormalTimeForDay;
    }

    @Override
    public List<TimeEntryDTO> getTeamTimeEntries(String leadLdap, LocalDate startDate, LocalDate endDate,
            TimeEntryStatus status, boolean directOnly) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getTeamTimeEntries", leadLdap, startDate, endDate,
                status, directOnly);
        User lead = userRepository.findByUsername(leadLdap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead/Manager not found"));

        if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only leads or managers can view team time entries");
        }
        // Get delegators for the current user
        List<String> delegators = delegationService.getDelegatorsForDelegatee(leadLdap);
        List<String> leadLdaps = new ArrayList<>();
        leadLdaps.add(leadLdap);
        leadLdaps.addAll(delegators);

        Set<String> allTeamMemberLdaps = new HashSet<>();

        if (lead.getRole() == Role.ADMIN_OPS_MANAGER) {
            // Admin sees everything, delegation doesn't restrict/expand this usually,
            // but if they are delegating FOR someone else, they might want to see that
            // person's view?
            // Usually Admin has full access anyway.
            // Original logic for Admin:
            if (status != null) {
                return timeEntryRepository.findAllByStatusStrictAndEntryDateBetween(
                        startDate != null ? startDate : LocalDate.MIN,
                        endDate != null ? endDate : LocalDate.MAX,
                        status).stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }
            return timeEntryRepository.findAllByStatusOptionalAndEntryDateBetween(
                    startDate != null ? startDate : LocalDate.MIN,
                    endDate != null ? endDate : LocalDate.MAX,
                    null).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        // For Manager/Lead, collect team members for all lead LDAPs
        for (String currentLeadLdap : leadLdaps) {
            List<Employee> teamMembers;
            // We need to check the role of the *delegator* to know if we should fetch by
            // Lead or ProgramManager
            // But here we only have the currentLeadLdap string.
            // We can assume if the *current user* is Manager, they act as Manager for
            // delegators too?
            // Or we should check each delegator's role.
            // For simplicity, let's check the role of the user associated with
            // currentLeadLdap.

            User currentLeadUser = userRepository.findByUsername(currentLeadLdap).orElse(null);
            if (currentLeadUser == null)
                continue;

            if (currentLeadUser.getRole() == Role.MANAGER) {
                teamMembers = employeeRepository.findByProgramManager(currentLeadLdap);
            } else {
                teamMembers = employeeRepository.findByLead(currentLeadLdap);
            }

            if (teamMembers != null) {
                allTeamMemberLdaps.addAll(teamMembers.stream().map(Employee::getLdap).collect(Collectors.toList()));
            }

            if (!directOnly) {
                // Include secondary leads/managers from EmployeeRelation
                List<String> relationTypeNames = Arrays.asList("LEAD", "MANAGER");
                List<EmployeeRelation> secondaryRelations = employeeRelationRepository
                        .findSecondaryByLeadLdapAndRelationTypeNames(currentLeadLdap, relationTypeNames);
                allTeamMemberLdaps.addAll(
                        secondaryRelations.stream().map(er -> er.getEmployee().getLdap()).collect(Collectors.toList()));
            }
        }

        if (allTeamMemberLdaps.isEmpty()) {
            return new ArrayList<>();
        }

        // Get the usernames (ldap) of all team members
        List<String> teamMemberLdapsList = new ArrayList<>(allTeamMemberLdaps);

        // Find all users corresponding to these ldaps
        List<User> teamUsers = userRepository.findByUsernameIn(teamMemberLdapsList);

        List<TimeEntry> entries;
        if (status != null) {
            // Use strict status filtering when status is provided
            entries = timeEntryRepository.findByUsersAndEntryDateBetweenAndStatusStrict(
                    teamUsers,
                    startDate != null ? startDate : LocalDate.MIN,
                    endDate != null ? endDate : LocalDate.MAX,
                    status);
        } else {
            // Use the original query when no status filter is needed
            entries = timeEntryRepository.findByUsersAndEntryDateBetweenAndStatus(
                    teamUsers,
                    startDate != null ? startDate : LocalDate.MIN,
                    endDate != null ? endDate : LocalDate.MAX,
                    null);
        }

        return entries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimeEntryHierarchicalSummaryDTO> getHierarchicalTimeSummary(Long userId, Long projectId,
            LocalDate startDate, LocalDate endDate) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getHierarchicalTimeSummary", userId, projectId,
                startDate, endDate);
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get time entries based on filters
        List<TimeEntry> entries;
        if (userId != null && projectId != null) {
            entries = timeEntryRepository.findByUser_IdAndProject_IdAndEntryDateBetween(userId, projectId, startDate,
                    endDate);
        } else if (userId != null) {
            entries = timeEntryRepository.findByUser_IdAndEntryDateBetween(userId, startDate, endDate);
        } else if (projectId != null) {
            entries = timeEntryRepository.findByProject_IdAndEntryDateBetween(projectId, startDate, endDate);
        } else {
            entries = timeEntryRepository.findByEntryDateBetween(startDate, endDate);
        }

        // Group entries by user and project
        Map<Long, Map<Long, List<TimeEntry>>> groupedEntries = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getUser().getId(),
                        Collectors.groupingBy(entry -> entry.getProject().getId())));

        // Create hierarchical summaries
        List<TimeEntryHierarchicalSummaryDTO> summaries = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, List<TimeEntry>>> userEntry : groupedEntries.entrySet()) {
            Long currentUserId = userEntry.getKey();
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            for (Map.Entry<Long, List<TimeEntry>> projectEntry : userEntry.getValue().entrySet()) {
                Long currentProjectId = projectEntry.getKey();
                Project project = projectRepository.findById(currentProjectId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

                List<TimeEntry> projectEntries = projectEntry.getValue();

                // Calculate total time and entries
                int totalTimeInMins = projectEntries.stream()
                        .mapToInt(TimeEntry::getTimeInMins)
                        .sum();
                int totalEntries = projectEntries.size();

                // Create breakdowns based on time unit
                List<TimeEntryBreakdownDTO> breakdowns = new ArrayList<>();
                Map<LocalDate, List<TimeEntry>> dailyEntries = projectEntries.stream()
                        .collect(Collectors.groupingBy(TimeEntry::getEntryDate));

                for (Map.Entry<LocalDate, List<TimeEntry>> dayEntry : dailyEntries.entrySet()) {
                    LocalDate date = dayEntry.getKey();
                    List<TimeEntry> dayEntries = dayEntry.getValue();

                    int dayTimeInMins = dayEntries.stream()
                            .mapToInt(TimeEntry::getTimeInMins)
                            .sum();

                    List<TimeEntryDetailDTO> details = dayEntries.stream()
                            .map(entry -> {
                                TimeEntryDetailDTO detail = new TimeEntryDetailDTO();
                                detail.setProcess(entry.getProcess());
                                detail.setActivity(entry.getActivity().toString());
                                detail.setTimeInMins(entry.getTimeInMins());
                                detail.setComment(entry.getComment());
                                detail.setStatus(entry.getStatus().toString());
                                return detail;
                            })
                            .collect(Collectors.toList());

                    TimeEntryBreakdownDTO breakdown = new TimeEntryBreakdownDTO();
                    breakdown.setDate(date);
                    breakdown.setTimeInMins(dayTimeInMins);
                    breakdown.setEntries(dayEntries.size());
                    breakdown.setDetails(details);
                    breakdowns.add(breakdown);
                }

                // Create and add summary
                TimeEntryHierarchicalSummaryDTO summary = new TimeEntryHierarchicalSummaryDTO();
                summary.setProjectId(project.getId());
                summary.setProjectCode(project.getProjectCode());
                summary.setProjectName(project.getProjectName());
                summary.setUserId(user.getId());
                summary.setUsername(user.getUsername());
                summary.setTotalTimeInMins(totalTimeInMins);
                summary.setTotalEntries(totalEntries);
                summary.setStartDate(startDate);
                summary.setEndDate(endDate);
                summary.setBreakdowns(breakdowns);
                summaries.add(summary);
            }
        }

        return summaries;
    }

    private TimeEntryDTO convertToDTO(TimeEntry timeEntry) {
        TimeEntryDTO dto = new TimeEntryDTO();
        dto.setId(timeEntry.getId());
        dto.setUserId(timeEntry.getUser().getId());
        dto.setUsername(timeEntry.getUser().getUsername());
        dto.setProjectId(timeEntry.getProject().getId());
        dto.setProjectCode(timeEntry.getProject().getProjectCode());
        dto.setProjectName(timeEntry.getProject().getProjectName());
        dto.setEntryDate(timeEntry.getEntryDate());
        dto.setLdap(timeEntry.getLdap());

        if (timeEntry.getLead() != null) {
            dto.setLeadId(timeEntry.getLead().getId());
            dto.setLeadUsername(timeEntry.getLead().getUsername());
        }

        dto.setProcess(timeEntry.getProcess());
        dto.setActivity(timeEntry.getActivity());
        dto.setTimeInMins(timeEntry.getTimeInMins());
        dto.setAttendanceType(timeEntry.getAttendanceType());
        dto.setComment(timeEntry.getComment());
        dto.setStatus(timeEntry.getStatus());
        dto.setRejectionComment(timeEntry.getRejectionComment());
        dto.setIsOvertime(timeEntry.getIsOvertime());
        dto.setIsDefaulter(timeEntry.getIsDefaulter());
        dto.setCreatedAt(timeEntry.getCreatedAt());
        dto.setUpdatedAt(timeEntry.getUpdatedAt());
        return dto;
    }

    @Override
    public List<UserDTO> getAllLdaps() {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getAllLdaps");
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOs = new ArrayList<>();
        for (User user : users) {
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setLdap(user.getUsername());
            userDTOs.add(dto);
        }
        return userDTOs;
    }

    @Override
    public List<UserDTO> getTeamMembers(String ldap) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getTeamMembers", ldap);
        User user = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if user is a lead, manager, or admin_ops_manager
        if (user.getRole() != Role.LEAD && user.getRole() != Role.MANAGER && user.getRole() != Role.ADMIN_OPS_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only leads, managers, or admin ops managers can view team members");
        }

        List<Employee> teamMembers = new ArrayList<>();

        // If user is an admin_ops_manager, return all employees
        if (user.getRole() == Role.ADMIN_OPS_MANAGER) {
            List<Object[]> activeEmployeeRows = employeeRepository.findAllExcludingLobsNative();
            for (Object[] row : activeEmployeeRows) {
                Employee employee = mapRowToEmployee(row);
                teamMembers.add(employee);
            }
        }
        // If user is a manager, get all employees under their leads
        else if (user.getRole() == Role.MANAGER) {
            List<Employee> directReports = employeeRepository.findByProgramManager(ldap);
            for (Employee employee : directReports) {
                teamMembers.add(employee);
            }
        }
        // If user is a lead, return only direct team members
        else {
            teamMembers = employeeRepository.findByLead(ldap);
        }

        // Convert to DTOs
        List<UserDTO> teamMemberDTOs = new ArrayList<>();
        for (Employee employee : teamMembers) {
            UserDTO dto = new UserDTO();
            // Try to find the corresponding user in the users table
            Optional<User> teamUser = userRepository.findByUsername(employee.getLdap());
            if (teamUser.isPresent()) {
                dto.setId(teamUser.get().getId());
            }
            dto.setLdap(employee.getLdap());
            teamMemberDTOs.add(dto);
        }

        return teamMemberDTOs;
    }

    @Override
    public List<TimeEntryDTO> rejectAllTimeEntry(@Valid TimeEntryRejectionIdsListDTO rejectionIdsListDTO, String ldap) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "rejectAllTimeEntry", rejectionIdsListDTO, ldap);
        List<Long> timeEntryIds = rejectionIdsListDTO.getTimeEntryId();
        User lead = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

        if (lead.getRole() != Role.LEAD && lead.getRole() != Role.MANAGER && lead.getRole() != Role.ADMIN_OPS_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only leads can reject time entries");
        }

        List<TimeEntry> timeEntries = timeEntryRepository.findAllById(timeEntryIds);
        List<TimeEntryDTO> rejectedEntries = new ArrayList<>();

        for (TimeEntry timeEntry : timeEntries) {
            if (timeEntry.getStatus() == TimeEntryStatus.PENDING) {
                timeEntry.setStatus(TimeEntryStatus.REJECTED);
                timeEntry.setRejectionComment(rejectionIdsListDTO.getRejectionComment());
                timeEntry.setUpdatedAt(LocalDateTime.now());
                TimeEntry updatedEntry = timeEntryRepository.save(timeEntry);
                rejectedEntries.add(convertToDTO(updatedEntry));
            }
            continue;
        }

        return rejectedEntries;
    }

    @Override
    public TimeEntryDTO getTimeEntryById(Long timeEntryId, String ldap) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getTimeEntryById", timeEntryId, ldap);
        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found"));

        // Check if the entry belongs to the user or if the user is a lead/manager
        User user = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!timeEntry.getUser().getUsername().equals(ldap) &&
                user.getRole() != Role.LEAD &&
                user.getRole() != Role.MANAGER &&
                user.getRole() != Role.ADMIN_OPS_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own time entries");
        }

        return convertToDTO(timeEntry);
    }

    /**
     * Creates multiple time entries based on a source entry for different dates
     *
     * @param batchTimeEntryDTO DTO containing source entry and target dates
     * @param username          Username of the user creating the entries
     * @return List of created time entries
     */
    @Override
    public List<TimeEntryDTO> createBatchTimeEntries(BatchTimeEntryDTO batchTimeEntryDTO, String username) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "createBatchTimeEntries", batchTimeEntryDTO, username);
        CreateTimeEntryDTO sourceEntry = batchTimeEntryDTO.getSourceEntry();
        List<String> targetDates = batchTimeEntryDTO.getTargetDates();
        List<TimeEntryDTO> createdEntries = new ArrayList<>();

        // Validate the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate the project
        Project project = projectRepository.findById(sourceEntry.getProjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        // Check if user is assigned to this project
        if (userProjectRepository.findByUserAndProject(user, project).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not assigned to this project");
        }

        // Validate the lead exists
        userRepository.findById(sourceEntry.getLeadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

        // Create a time entry for each target date
        for (String dateStr : targetDates) {
            try {
                LocalDate targetDate = LocalDate.parse(dateStr);

                // Create a copy of the source entry with the new date
                CreateTimeEntryDTO newEntry = new CreateTimeEntryDTO();
                newEntry.setProjectId(sourceEntry.getProjectId());
                newEntry.setEntryDate(targetDate);
                newEntry.setProcess(sourceEntry.getProcess());
                newEntry.setActivity(sourceEntry.getActivity());
                newEntry.setTimeInMins(sourceEntry.getTimeInMins());
                newEntry.setAttendanceType(sourceEntry.getAttendanceType());
                newEntry.setLeadId(sourceEntry.getLeadId());
                newEntry.setComment(sourceEntry.getComment());
                newEntry.setLdap(sourceEntry.getLdap());
                newEntry.setIsOvertime(sourceEntry.getIsOvertime());

                // Check if adding this entry would exceed 8 hours (480 minutes) for the day
                // Skip check for overtime entries
                if (newEntry.getIsOvertime() == null || !newEntry.getIsOvertime()) {
                    Integer totalNormalTimeForDay = timeEntryRepository.getTotalNormalTimeByUserAndDate(user,
                            targetDate);
                    if (totalNormalTimeForDay == null) {
                        totalNormalTimeForDay = 0;
                    }

                    if (totalNormalTimeForDay + newEntry.getTimeInMins() > 480) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Total time for " + dateStr
                                        + " would exceed 8 hours. Please use overtime toggle for additional hours.");
                    }
                }

                // Create the time entry
                TimeEntryDTO createdEntry = createTimeEntry(newEntry, username);
                createdEntries.add(createdEntry);
            } catch (Exception e) {
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Failed to create time entry for date " + dateStr + ": " + e.getMessage());
            }
        }

        return createdEntries;
    }

    /**
     * Creates multiple holiday time entries for Google holidays
     *
     * @param holidayBatchRequest DTO containing list of holiday entries to create
     * @param username            Username of the user creating the entries
     * @return List of created holiday time entries
     */
    @Override
    public List<TimeEntryDTO> createBatchHolidayEntries(HolidayBatchRequestDTO holidayBatchRequest, String username) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "createBatchHolidayEntries", holidayBatchRequest,
                username);
        List<CreateTimeEntryDTO> holidayEntries = holidayBatchRequest.getEntries();
        List<TimeEntryDTO> createdEntries = new ArrayList<>();

        // Validate the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        for (CreateTimeEntryDTO holidayEntry : holidayEntries) {
            try {
                // Validate the project
                Project project = projectRepository.findById(holidayEntry.getProjectId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

                // Check if user is assigned to this project
                if (userProjectRepository.findByUserAndProject(user, project).isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not assigned to this project");
                }

                // Validate the lead exists
                userRepository.findById(holidayEntry.getLeadId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

                // For holiday entries, skip the 8-hour time limit check since they are
                // typically short entries
                // and don't count toward regular working hours

                // Create the holiday time entry
                TimeEntryDTO createdEntry = createTimeEntry(holidayEntry, username);
                createdEntries.add(createdEntry);
            } catch (Exception e) {
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Failed to create holiday time entry: " + e.getMessage());
            }
        }

        return createdEntries;
    }

    private Employee mapRowToEmployee(Object[] row) {
        Employee employee = new Employee();
        int i = 0;
        employee.setId(row[i++] != null ? ((Number) row[i - 1]).longValue() : null);
        employee.setFirstName((String) row[i++]);
        employee.setLastName((String) row[i++]);
        employee.setLdap((String) row[i++]);
        employee.setStartDate((String) row[i++]);
        employee.setTeam((String) row[i++]);
        employee.setNewLevel((String) row[i++]);
        employee.setLead((String) row[i++]);
        employee.setProgramManager((String) row[i++]);
        employee.setVendor((String) row[i++]);
        employee.setEmail((String) row[i++]);
        employee.setStatus((String) row[i++]);
        employee.setLwdMlStartDate((String) row[i++]);
        employee.setProcess((String) row[i++]);
        employee.setResignationDate((String) row[i++]);
        employee.setRoleChangeEffectiveDate((String) row[i++]);
        employee.setLevelBeforeChange((String) row[i++]);
        employee.setLevelAfterChange((String) row[i++]);
        employee.setLastBillingDate((String) row[i++]);
        employee.setBackfillLdap((String) row[i++]);
        employee.setBillingStartDate((String) row[i++]);
        employee.setLanguage((String) row[i++]);
        employee.setTenureTillDate((String) row[i++]);
        employee.setLevel((String) row[i++]);
        employee.setInactiveReason((String) row[i++]);
        // Skip profile_pic (NULL value from query)
        i++;
        employee.setParent(row[i++] != null ? ((Number) row[i - 1]).longValue() : null);
        employee.setIsDeleted(row[i++] != null ? (Boolean) row[i - 1] : false);
        employee.setPnseProgram((String) row[i++]);
        employee.setLocation((String) row[i++]);
        employee.setShift((String) row[i++]);
        employee.setInactive(row[i++] != null ? (Boolean) row[i - 1] : false);
        // profilePic is intentionally not set (avoiding LOB)
        return employee;
    }

    /**
     * Determines if a time entry is considered a defaulter based on the entry date
     * and creation timestamp.
     * A time entry is marked as a defaulter if it's created after the end of the
     * week it belongs to.
     *
     * @param entryDate The date the time entry is for
     * @param createdAt The timestamp when the time entry was created
     * @return true if the entry is a defaulter, false otherwise
     */
    private boolean isTimeEntryDefaulter(LocalDate entryDate, LocalDateTime createdAt) {
        // Get the week of the entry date (assuming Monday to Sunday week)
        LocalDate entryWeekStart = entryDate.minusDays(entryDate.getDayOfWeek().getValue() - 1);
        LocalDate entryWeekEnd = entryWeekStart.plusDays(6);

        // Get the week of creation date
        LocalDate creationWeekStart = createdAt.toLocalDate().minusDays(createdAt.getDayOfWeek().getValue() - 1);
        LocalDate creationWeekEnd = creationWeekStart.plusDays(6);

        // If the entry date is in a previous week compared to creation date, it's a
        // defaulter
        return creationWeekStart.isAfter(entryWeekEnd);
    }

    @Override
    public RecentProjectDTO getRecentProject(String ldap) {
        LoggerUtil.logMethodEntry(TimeEntryServiceImpl.class, "getRecentProject", ldap);

        // Step 1: Try to find recent project from last 15 days of time entries
        LocalDate sinceDate = LocalDate.now().minusDays(15);
        List<TimeEntry> recentEntries = timeEntryRepository.findRecentByLdap(ldap, sinceDate);

        if (!recentEntries.isEmpty()) {
            // Find the most frequently used project
            Map<Long, Long> projectFrequency = recentEntries.stream()
                    .collect(Collectors.groupingBy(
                            entry -> entry.getProject().getId(),
                            Collectors.counting()));

            // Get the project ID with highest frequency
            Long mostFrequentProjectId = projectFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (mostFrequentProjectId != null) {
                Project project = projectRepository.findById(mostFrequentProjectId).orElse(null);
                if (project != null) {
                    return new RecentProjectDTO(project.getProjectName(), project.getId());
                }
            }
        }

        // Step 2: Fallback - check user_project_mapping table
        User user = userRepository.findByUsername(ldap).orElse(null);
        if (user != null) {
            List<UserProject> userProjects = userProjectRepository.findByUser(user);
            if (!userProjects.isEmpty()) {
                // Return the first project from mapping
                Project project = userProjects.get(0).getProject();
                return new RecentProjectDTO(project.getProjectName(), project.getId());
            }
        }

        // Step 3: No project found - return N/A
        return new RecentProjectDTO("N/A", null);
    }

}
