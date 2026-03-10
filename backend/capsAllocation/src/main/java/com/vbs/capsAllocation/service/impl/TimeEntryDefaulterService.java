package com.vbs.capsAllocation.service.impl;

import com.vbs.capsAllocation.model.TimeEntry;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.repository.TimeEntryRepository;
import com.vbs.capsAllocation.service.DefaulterTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * TimeEntry-specific implementation of DefaulterTypeService
 */
@Service
public class TimeEntryDefaulterService implements DefaulterTypeService<TimeEntry> {

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Override
    public Long getDefaulterCount(LocalDate start, LocalDate end, String level, User loggedInUser) {
        switch (level) {
            case "LEAD":
                return timeEntryRepository.countDefaultersForLead(
                        start, end, loggedInUser.getUsername());

            case "MANAGER":
                return timeEntryRepository.countDefaultersForManager(
                        start, end, loggedInUser.getUsername());

            case "ADMIN_OPS_MANAGER":
                return timeEntryRepository.countDefaultersForAdmin(start, end);

            default:
                throw new RuntimeException("Invalid role: " + level);
        }
    }

    @Override
    public TimeEntryRepository getRepository() {
        return timeEntryRepository;
    }

    @Override
    public String getEntityTypeName() {
        return "TimeEntry";
    }

    @Autowired
    private com.vbs.capsAllocation.repository.EmployeeRepository employeeRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<com.vbs.capsAllocation.dto.DefaulterDetailDTO> getDefaultersList(
            LocalDate start,
            LocalDate end,
            String level,
            User loggedInUser,
            java.util.Map<String, Object> filters) {

        java.util.List<TimeEntry> defaulters;

        switch (level) {
            case "LEAD":
                defaulters = timeEntryRepository.findDefaultersForLead(start, end, loggedInUser.getUsername());
                break;
            case "MANAGER":
                defaulters = timeEntryRepository.findDefaultersForManager(start, end, loggedInUser.getUsername());
                break;
            case "ADMIN_OPS_MANAGER":
                defaulters = timeEntryRepository.findDefaultersForAdmin(start, end);
                break;
            default:
                throw new RuntimeException("Invalid role: " + level);
        }

        if (defaulters.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // Fetch employee details
        java.util.List<String> ldaps = defaulters.stream()
                .map(TimeEntry::getLdap)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<String, com.vbs.capsAllocation.model.Employee> employeeMap = employeeRepository
                .findByLdapIn(ldaps).stream()
                .collect(java.util.stream.Collectors.toMap(com.vbs.capsAllocation.model.Employee::getLdap, e -> e));

        // Group by user and count issues
        return defaulters.stream()
                .collect(java.util.stream.Collectors.groupingBy(TimeEntry::getLdap))
                .entrySet().stream()
                .map(entry -> {
                    String ldap = entry.getKey();
                    long count = entry.getValue().size();
                    LocalDate lastIncident = entry.getValue().stream()
                            .map(TimeEntry::getEntryDate)
                            .max(LocalDate::compareTo)
                            .orElse(null);

                    com.vbs.capsAllocation.model.Employee employee = employeeMap.get(ldap);
                    String name = employee != null ? employee.getFirstName() + " " + employee.getLastName() : ldap;
                    String email = employee != null ? employee.getEmail() : "N/A";
                    String department = employee != null ? employee.getTeam() : "N/A"; // Assuming Team is Department
                    String manager = employee != null ? employee.getProgramManager() : "N/A";
                    String project = employee != null ? employee.getProcess() : "N/A"; // Assuming Process is Project
                    String program = employee != null ? employee.getPnseProgram() : "N/A";

                    return new com.vbs.capsAllocation.dto.DefaulterDetailDTO(
                            employee != null ? employee.getId() : 0L,
                            name,
                            email,
                            department,
                            manager,
                            project,
                            program,
                            count,
                            count > 5 ? "Critical" : (count > 2 ? "Warning" : "Good"),
                            lastIncident);
                })
                .filter(dto -> {
                    if (filters == null || filters.isEmpty())
                        return true;

                    boolean match = true;
                    if (filters.containsKey("team")) {
                        match &= dto.getDepartment().toLowerCase()
                                .contains(((String) filters.get("team")).toLowerCase());
                    }
                    if (filters.containsKey("project")) {
                        match &= dto.getProject().toLowerCase()
                                .contains(((String) filters.get("project")).toLowerCase());
                    }
                    if (filters.containsKey("program")) {
                        match &= dto.getProgram().toLowerCase()
                                .contains(((String) filters.get("program")).toLowerCase());
                    }
                    if (filters.containsKey("manager")) {
                        match &= dto.getManager().toLowerCase()
                                .contains(((String) filters.get("manager")).toLowerCase());
                    }
                    return match;
                })
                .sorted(java.util.Comparator.comparingLong(com.vbs.capsAllocation.dto.DefaulterDetailDTO::getIssueCount)
                        .reversed())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<com.vbs.capsAllocation.dto.TopDefaulterDTO> getTopDefaulters(
            LocalDate start,
            LocalDate end,
            String level,
            User loggedInUser,
            java.util.Map<String, Object> filters) {

        return getDefaultersList(start, end, level, loggedInUser, filters).stream()
                .limit(3)
                .map(d -> new com.vbs.capsAllocation.dto.TopDefaulterDTO(
                        d.getEmployeeId(),
                        d.getEmployeeName(),
                        d.getDepartment(),
                        d.getIssueCount(),
                        null // avatarUrl
                ))
                .collect(java.util.stream.Collectors.toList());
    }
}