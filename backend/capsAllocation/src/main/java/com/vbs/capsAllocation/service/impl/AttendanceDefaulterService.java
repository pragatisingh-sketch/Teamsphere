package com.vbs.capsAllocation.service.impl;

import com.vbs.capsAllocation.model.Attendance;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.repository.AttendanceRepository;
import com.vbs.capsAllocation.service.DefaulterTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Attendance-specific implementation of DefaulterTypeService
 */
@Service
public class AttendanceDefaulterService implements DefaulterTypeService<Attendance> {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Override
    public Long getDefaulterCount(LocalDate start, LocalDate end, String level, User loggedInUser) {
        switch (level) {
            case "LEAD":
                return attendanceRepository.countDefaultersForLead(
                        start, end, loggedInUser.getUsername());

            case "MANAGER":
                return attendanceRepository.countDefaultersForManager(
                        start, end, loggedInUser.getUsername());

            case "ADMIN_OPS_MANAGER":
                return attendanceRepository.countDefaultersForAdmin(start, end);

            default:
                throw new RuntimeException("Invalid role: " + level);
        }
    }

    @Override
    public AttendanceRepository getRepository() {
        return attendanceRepository;
    }

    @Override
    public String getEntityTypeName() {
        return "Attendance";
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<com.vbs.capsAllocation.dto.DefaulterDetailDTO> getDefaultersList(
            LocalDate start,
            LocalDate end,
            String level,
            User loggedInUser,
            java.util.Map<String, Object> filters) {

        java.util.List<Attendance> defaulters;

        switch (level) {
            case "LEAD":
                // Need to implement these methods in AttendanceRepository or use existing ones
                // if available
                // For now assuming we added them as per plan
                defaulters = attendanceRepository.countDefaultersForLeadList(start, end, loggedInUser.getUsername());
                break;
            case "MANAGER":
                defaulters = attendanceRepository.countDefaultersForManagerList(start, end, loggedInUser.getUsername());
                break;
            case "ADMIN_OPS_MANAGER":
                defaulters = attendanceRepository.countDefaultersForAdminList(start, end);
                break;
            default:
                throw new RuntimeException("Invalid role: " + level);
        }

        // Group by employee and count issues
        return defaulters.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getEmployee().getLdap()))
                .entrySet().stream()
                .map(entry -> {
                    String ldap = entry.getKey();
                    java.util.List<Attendance> attendanceList = entry.getValue();
                    com.vbs.capsAllocation.model.Employee employee = attendanceList.get(0).getEmployee();
                    long count = attendanceList.size();
                    LocalDate lastIncident = attendanceList.stream()
                            .map(Attendance::getEntryDate)
                            .max(LocalDate::compareTo)
                            .orElse(null);

                    String name = employee.getFirstName() + " " + employee.getLastName();

                    return new com.vbs.capsAllocation.dto.DefaulterDetailDTO(
                            employee.getId(),
                            name,
                            employee.getEmail(),
                            employee.getTeam(),
                            employee.getProgramManager(),
                            employee.getProcess(),
                            employee.getPnseProgram(),
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