package com.vbs.capsAllocation.service.impl;

import com.vbs.capsAllocation.dto.DefaulterDetailDTO;
import com.vbs.capsAllocation.dto.TopDefaulterDTO;
import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.model.VunnoResponse;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.repository.VunnoResponseRepository;
import com.vbs.capsAllocation.service.DefaulterTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to handle Leave defaulters (UNPLANNED leaves).
 */
@Service
public class LeaveDefaulterService implements DefaulterTypeService<VunnoResponse> {

    @Autowired
    private VunnoResponseRepository vunnoResponseRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Long getDefaulterCount(LocalDate start, LocalDate end, String level, User loggedInUser) {
        switch (level) {
            case "LEAD":
                return vunnoResponseRepository.countUnplannedLeavesForLead(start, end, loggedInUser.getUsername());
            case "MANAGER":
                return vunnoResponseRepository.countUnplannedLeavesForManager(start, end, loggedInUser.getUsername());
            case "ADMIN_OPS_MANAGER":
                return vunnoResponseRepository.countUnplannedLeavesForAdmin(start, end);
            default:
                throw new RuntimeException("Invalid role: " + level);
        }
    }

    @Override
    public JpaRepository<VunnoResponse, Long> getRepository() {
        return vunnoResponseRepository;
    }

    @Override
    public String getEntityTypeName() {
        return "Leaves";
    }

    @Override
    @Transactional(readOnly = true)
    public List<DefaulterDetailDTO> getDefaultersList(LocalDate start, LocalDate end, String level, User loggedInUser,
            Map<String, Object> filters) {
        List<VunnoResponse> defaulters;

        switch (level) {
            case "LEAD":
                defaulters = vunnoResponseRepository.findUnplannedLeavesForLead(start, end, loggedInUser.getUsername());
                break;
            case "MANAGER":
                defaulters = vunnoResponseRepository.findUnplannedLeavesForManager(start, end,
                        loggedInUser.getUsername());
                break;
            case "ADMIN_OPS_MANAGER":
                defaulters = vunnoResponseRepository.findUnplannedLeavesForAdmin(start, end);
                break;
            default:
                throw new RuntimeException("Invalid role: " + level);
        }

        if (defaulters.isEmpty()) {
            return new ArrayList<>();
        }

        // Group by user and count
        // VunnoResponse has direct Employee reference, so we might not need to fetch
        // map separately if eager loaded or accessible
        // But to be safe and efficient with what's loaded, let's just group by Employee
        // if possible, or LDAP

        Map<String, List<VunnoResponse>> groupedByLdap = defaulters.stream()
                .filter(v -> v.getEmployee() != null && v.getEmployee().getLdap() != null)
                .collect(Collectors.groupingBy(v -> v.getEmployee().getLdap()));

        return groupedByLdap.entrySet().stream()
                .map(entry -> {
                    String ldap = entry.getKey();
                    List<VunnoResponse> responses = entry.getValue();
                    long count = responses.size(); // Counting occurrences (requests)

                    LocalDate lastIncident = responses.stream()
                            .map(VunnoResponse::getFromDate) // Use fromDate as incident date
                            .max(LocalDate::compareTo)
                            .orElse(null);

                    Employee employee = responses.get(0).getEmployee(); // Get from first response since grouped by LDAP

                    String name = employee.getFirstName() + " " + employee.getLastName();

                    return new DefaulterDetailDTO(
                            employee.getId(),
                            name,
                            employee.getEmail(),
                            employee.getTeam(),
                            employee.getProgramManager(),
                            employee.getProcess(),
                            employee.getPnseProgram(),
                            count,
                            count > 3 ? "Critical" : (count > 1 ? "Warning" : "Good"), // Arbitrary thresholds for
                                                                                       // Leaves
                            lastIncident);
                })
                .filter(dto -> applyFilters(dto, filters))
                .sorted(Comparator.comparingLong(DefaulterDetailDTO::getIssueCount).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopDefaulterDTO> getTopDefaulters(LocalDate start, LocalDate end, String level, User loggedInUser,
            Map<String, Object> filters) {
        return getDefaultersList(start, end, level, loggedInUser, filters).stream()
                .limit(3)
                .map(d -> new TopDefaulterDTO(
                        d.getEmployeeId(),
                        d.getEmployeeName(),
                        d.getDepartment(),
                        d.getIssueCount(),
                        null))
                .collect(Collectors.toList());
    }

    private boolean applyFilters(DefaulterDetailDTO dto, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty())
            return true;

        boolean match = true;
        if (filters.containsKey("team")) {
            match &= dto.getDepartment() != null
                    && dto.getDepartment().toLowerCase().contains(((String) filters.get("team")).toLowerCase());
        }
        if (filters.containsKey("project")) {
            match &= dto.getProject() != null
                    && dto.getProject().toLowerCase().contains(((String) filters.get("project")).toLowerCase());
        }
        if (filters.containsKey("program")) {
            match &= dto.getProgram() != null
                    && dto.getProgram().toLowerCase().contains(((String) filters.get("program")).toLowerCase());
        }
        if (filters.containsKey("manager")) {
            match &= dto.getManager() != null
                    && dto.getManager().toLowerCase().contains(((String) filters.get("manager")).toLowerCase());
        }
        return match;
    }
}
