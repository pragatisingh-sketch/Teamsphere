package com.vbs.capsAllocation.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbs.capsAllocation.dto.EmployeeDTO;
import com.vbs.capsAllocation.dto.EmployeeRelationDTO;
import com.vbs.capsAllocation.dto.UserDTO;
import com.vbs.capsAllocation.dto.UserEditLogDTO;
import com.vbs.capsAllocation.model.*;
import com.vbs.capsAllocation.repository.EmployeeRelationRepository;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.repository.InactiveEmployeeRepository;
import com.vbs.capsAllocation.repository.LeadRepository;
import com.vbs.capsAllocation.repository.RelationTypeRepository;
import com.vbs.capsAllocation.service.EmployeeService;
import com.vbs.capsAllocation.service.EmployeeDataFileService;
import com.vbs.capsAllocation.service.UserEditLogService;
import com.vbs.capsAllocation.util.LoggerUtil;

import jakarta.persistence.EntityNotFoundException;

import com.vbs.capsAllocation.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
//import jakarta.transaction.Transactional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final String BACKUP_FILE_PATH = "/home/piyushm/Documents/teamsphere/backend/capsAllocation/dataBackup/deleted_employees.json"; // Path
                                                                                                                                                  // to
                                                                                                                                                  // store
                                                                                                                                                  // the
                                                                                                                                                  // file

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private InactiveEmployeeRepository inactiveEmployeeRepository;

    @Autowired
    private UserEditLogService userEditLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmployeeRelationRepository employeeRelationRepository;

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private EmployeeDataFileService employeeDataFileService;

    @Autowired
    private com.vbs.capsAllocation.service.DelegationService delegationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Employee saveEmployee(EmployeeDTO employeeDTO, Long id) {
        // Get the current user's LDAP from the security context
        String currentUserLdap = getCurrentUserLdap();

        // If this is an update (id is not null), get the existing employee to compare
        // changes
        if (id != null) {
            Employee existingEmployee = employeeRepository.findByIdExcludingLobsNative(id);
            if (existingEmployee != null) {
                // Track changes before updating
                trackChanges(existingEmployee, employeeDTO, currentUserLdap);
            }
        }

        // Create or update the employee entity
        Employee employee = convertToEntity(employeeDTO, id);
        employee = employeeRepository.save(employee);

        // If status is Inactive, disable the corresponding User account to prevent
        // login
        if ("Inactive".equalsIgnoreCase(employee.getStatus()) || Boolean.TRUE.equals(employee.getInactive())) {
            try {
                Optional<User> userOpt = userRepository.findByUsername(employee.getLdap());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // Scramble password to prevent login
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    // Set role to USER to remove elevated privileges
                    user.setRole(Role.USER);
                    userRepository.save(user);
                    LoggerUtil.logInfo(EmployeeServiceImpl.class, "Disabled user account for inactive employee: {}",
                            employee.getLdap());
                }
            } catch (Exception e) {
                LoggerUtil.logError("Error disabling user account for inactive employee: {}", e.getMessage());
            }
        }

        // Handle employee relations if provided in DTO
        if (employeeDTO.getEmployeeRelations() != null && !employeeDTO.getEmployeeRelations().isEmpty()) {
            // If updating, clear existing relations
            if (id != null) {
                employeeRelationRepository.deleteByEmployeeId(employee.getId());
            }

            // Add new relations
            for (EmployeeRelationDTO relationDTO : employeeDTO.getEmployeeRelations()) {
                EmployeeRelation relation = new EmployeeRelation();
                relation.setEmployee(employee);
                relation.setRelationType(relationTypeRepository.findById(relationDTO.getRelationTypeId())
                        .orElseThrow(() -> new RuntimeException("RelationType not found")));
                relation.setRelationValue(relationDTO.getRelationValue());
                relation.setEffectiveDate(relationDTO.getEffectiveDate());
                relation.setEndDate(relationDTO.getEndDate());
                relation.setIsActive(relationDTO.getIsActive() != null ? relationDTO.getIsActive() : true);
                employeeRelationRepository.save(relation);
            }
        }

        return employee;
    }

    /**
     * Get the current user's LDAP from the security context
     */
    private String getCurrentUserLdap() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getName();
        } catch (Exception e) {
            return "system"; // Default value if unable to get current user
        }
    }

    /**
     * Track changes between existing employee and new employee data
     */
    private void trackChanges(Employee existingEmployee, EmployeeDTO newData, String changedBy) {
        // Track changes for each field
        if (!Objects.equals(existingEmployee.getFirstName(), newData.getFirstName())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "firstName",
                    existingEmployee.getFirstName(), newData.getFirstName(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getLastName(), newData.getLastName())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "lastName",
                    existingEmployee.getLastName(), newData.getLastName(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getEmail(), newData.getEmail())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "email",
                    existingEmployee.getEmail(), newData.getEmail(), changedBy);
        }

        // Track changes for other important fields
        if (!Objects.equals(existingEmployee.getTeam(), newData.getTeam())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "team",
                    existingEmployee.getTeam(), newData.getTeam(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getLevel(), newData.getLevel())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "level",
                    existingEmployee.getLevel(), newData.getLevel(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getLead(), newData.getLead())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "lead",
                    existingEmployee.getLead(), newData.getLead(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getProgramManager(), newData.getProgramManager())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "programManager",
                    existingEmployee.getProgramManager(), newData.getProgramManager(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getStatus(), newData.getStatus())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "status",
                    existingEmployee.getStatus(), newData.getStatus(), changedBy);
        }

        // Track changes for pending fields
        if (!Objects.equals(existingEmployee.getLdap(), newData.getLdap())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "ldap",
                    existingEmployee.getLdap(), newData.getLdap(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getStartDate(), newData.getStartDate())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "startDate",
                    existingEmployee.getStartDate(), newData.getStartDate(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getNewLevel(), newData.getNewLevel())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "newLevel",
                    existingEmployee.getNewLevel(), newData.getNewLevel(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getVendor(), newData.getVendor())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "vendor",
                    existingEmployee.getVendor(), newData.getVendor(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getLwdMlStartDate(), newData.getLwdMlStartDate())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "lwdMlStartDate",
                    existingEmployee.getLwdMlStartDate(), newData.getLwdMlStartDate(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getProcess(), newData.getProcess())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "process",
                    existingEmployee.getProcess(), newData.getProcess(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getResignationDate(), newData.getResignationDate())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "resignationDate",
                    existingEmployee.getResignationDate(), newData.getResignationDate(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getRoleChangeEffectiveDate(), newData.getRoleChangeEffectiveDate())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "roleChangeEffectiveDate",
                    existingEmployee.getRoleChangeEffectiveDate(), newData.getRoleChangeEffectiveDate(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getLevelBeforeChange(), newData.getLevelBeforeChange())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "levelBeforeChange",
                    existingEmployee.getLevelBeforeChange(), newData.getLevelBeforeChange(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getLevelAfterChange(), newData.getLevelAfterChange())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "levelAfterChange",
                    existingEmployee.getLevelAfterChange(), newData.getLevelAfterChange(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getLastBillingDate(), newData.getLastBillingDate())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "lastBillingDate",
                    existingEmployee.getLastBillingDate(), newData.getLastBillingDate(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getBackfillLdap(), newData.getBackfillLdap())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "backfillLdap",
                    existingEmployee.getBackfillLdap(), newData.getBackfillLdap(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getBillingStartDate(), newData.getBillingStartDate())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "billingStartDate",
                    existingEmployee.getBillingStartDate(), newData.getBillingStartDate(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getLanguage(), newData.getLanguage())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "language",
                    existingEmployee.getLanguage(), newData.getLanguage(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getTenureTillDate(), newData.getTenureTillDate())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "tenureTillDate",
                    existingEmployee.getTenureTillDate(), newData.getTenureTillDate(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getInactiveReason(), newData.getInactiveReason())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "inactiveReason",
                    existingEmployee.getInactiveReason(), newData.getInactiveReason(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getPnseProgram(), newData.getPnseProgram())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "pnseProgram",
                    existingEmployee.getPnseProgram(), newData.getPnseProgram(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getLocation(), newData.getLocation())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "location",
                    existingEmployee.getLocation(), newData.getLocation(), changedBy);
        }

        if (!Objects.equals(existingEmployee.getShift(), newData.getShift())) {
            userEditLogService.logChange(existingEmployee.getLdap(), "shift",
                    existingEmployee.getShift(), newData.getShift(), changedBy);
        }
    }

    @Override
    @Transactional
    public List<Employee> getAllEmployees() {
        try {
            List<Employee> employees = new ArrayList<>();

            // Get employees using native query to avoid LOB fields completely
            List<Object[]> activeEmployeeRows = employeeRepository.findAllExcludingLobsNative();
            for (Object[] row : activeEmployeeRows) {
                Employee employee = mapRowToEmployee(row);
                employees.add(employee);
            }

            List<Object[]> inactiveEmployeeRows = inactiveEmployeeRepository.findAllExcludingLobsNative();
            for (Object[] row : inactiveEmployeeRows) {
                Employee employee = mapRowToInactiveEmployee(row);
                employees.add(employee);
            }
            return employees;
        } catch (Exception e) {
            LoggerUtil.logError("Error in getAllEmployees: {}", e.getMessage());
            // Return empty list if there's an error to prevent application crash
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public List<Employee> getAllEmployees(boolean includeInactive) {
        if (includeInactive) {
            return getAllEmployees();
        } else {
            List<Object[]> activeEmployees = employeeRepository.findAllExcludingLobsNative();
            List<Employee> employees = new ArrayList<>();
            for (Object[] row : activeEmployees) {
                Employee employee = mapRowToEmployee(row);
                employees.add(employee);
            }

            // Fetch and set employee relations
            if (!employees.isEmpty()) {
                List<Long> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toList());
                List<EmployeeRelation> allRelations = employeeRelationRepository.findByEmployeeIdIn(employeeIds);
                Map<Long, List<EmployeeRelation>> groupedRelations = allRelations.stream()
                        .collect(Collectors.groupingBy(r -> r.getEmployee().getId()));
                for (Employee emp : employees) {
                    emp.setEmployeeRelations(groupedRelations.getOrDefault(emp.getId(), new ArrayList<>()));
                }
            }

            return employees;
        }
    }

    @Override
    @Transactional
    public List<Employee> getAllActiveEmp() {
        try {
            List<Employee> employees = new ArrayList<>();
            List<Object[]> activeEmployeeRows = employeeRepository.findAllExcludingLobsNative();
            for (Object[] row : activeEmployeeRows) {
                Employee employee = mapRowToEmployee(row);
                employees.add(employee);
            }

            // Fetch and set employee relations
            if (!employees.isEmpty()) {
                List<Long> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toList());
                List<EmployeeRelation> allRelations = employeeRelationRepository.findByEmployeeIdIn(employeeIds);
                Map<Long, List<EmployeeRelation>> groupedRelations = allRelations.stream()
                        .collect(Collectors.groupingBy(r -> r.getEmployee().getId()));
                for (Employee emp : employees) {
                    emp.setEmployeeRelations(groupedRelations.getOrDefault(emp.getId(), new ArrayList<>()));
                }
            }

            return employees;
        } catch (Exception e) {
            LoggerUtil.logError("Error in getAllActiveEmp: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public Employee mapInactiveEmployeeToEmployee(InactiveEmployee inactiveEmployee) {
        Employee employee = new Employee();
        employee.setFirstName(inactiveEmployee.getFirstName());
        employee.setLastName(inactiveEmployee.getLastName());
        employee.setLdap(inactiveEmployee.getLdap());
        employee.setTeam(inactiveEmployee.getTeam());
        employee.setNewLevel(inactiveEmployee.getNewLevel());
        employee.setLead(inactiveEmployee.getLead());
        employee.setProgramManager(inactiveEmployee.getProgramManager());
        employee.setVendor(inactiveEmployee.getVendor());
        employee.setEmail(inactiveEmployee.getEmail());
        employee.setStatus(inactiveEmployee.getStatus());
        employee.setProcess(inactiveEmployee.getProcess());
        employee.setLevelBeforeChange(inactiveEmployee.getLevelBeforeChange());
        employee.setLevelAfterChange(inactiveEmployee.getLevelAfterChange());
        employee.setBackfillLdap(inactiveEmployee.getBackfillLdap());
        employee.setLanguage(inactiveEmployee.getLanguage());
        employee.setTenureTillDate(inactiveEmployee.getTenureTillDate());
        employee.setProfilePic(inactiveEmployee.getProfilePic());
        employee.setInactiveReason(inactiveEmployee.getInactiveReason());
        employee.setPnseProgram(inactiveEmployee.getPnseProgram());
        employee.setStartDate(inactiveEmployee.getStartDate());
        employee.setRoleChangeEffectiveDate(inactiveEmployee.getRoleChangeEffectiveDate());
        employee.setLastBillingDate(inactiveEmployee.getLastBillingDate());
        employee.setBillingStartDate(inactiveEmployee.getBillingStartDate());
        employee.setLwdMlStartDate(inactiveEmployee.getLwdMlStartDate());
        employee.setParent(inactiveEmployee.getParent());
        employee.setLevel(inactiveEmployee.getLevel());
        employee.setShift(inactiveEmployee.getShift());
        employee.setLocation(inactiveEmployee.getLocation());
        employee.setResignationDate(inactiveEmployee.getResignationDate());
        employee.setId(inactiveEmployee.getId());
        employee.setInactive(true);
        return employee;
    }

    // Helper method to create a safe employee object without LOB fields
    private Employee createSafeEmployee(Employee original) {
        Employee safe = new Employee();
        safe.setId(original.getId());
        safe.setFirstName(original.getFirstName());
        safe.setLastName(original.getLastName());
        safe.setLdap(original.getLdap());
        safe.setStartDate(original.getStartDate());
        safe.setTeam(original.getTeam());
        safe.setNewLevel(original.getNewLevel());
        safe.setLead(original.getLead());
        safe.setProgramManager(original.getProgramManager());
        safe.setVendor(original.getVendor());
        safe.setEmail(original.getEmail());
        safe.setStatus(original.getStatus());
        safe.setLwdMlStartDate(original.getLwdMlStartDate());
        safe.setProcess(original.getProcess());
        safe.setResignationDate(original.getResignationDate());
        safe.setRoleChangeEffectiveDate(original.getRoleChangeEffectiveDate());
        safe.setLevelBeforeChange(original.getLevelBeforeChange());
        safe.setLevelAfterChange(original.getLevelAfterChange());
        safe.setLastBillingDate(original.getLastBillingDate());
        safe.setBackfillLdap(original.getBackfillLdap());
        safe.setBillingStartDate(original.getBillingStartDate());
        safe.setLanguage(original.getLanguage());
        safe.setTenureTillDate(original.getTenureTillDate());
        safe.setLevel(original.getLevel());
        safe.setInactiveReason(original.getInactiveReason());
        safe.setParent(original.getParent());
        safe.setIsDeleted(original.getIsDeleted());
        safe.setPnseProgram(original.getPnseProgram());
        safe.setLocation(original.getLocation());
        safe.setShift(original.getShift());
        safe.setInactive(original.getInactive());
        // Explicitly set profilePic to null to avoid LOB issues
        safe.setProfilePic(null);
        return safe;
    }

    // Helper method to safely map inactive employee to employee without LOB fields
    public Employee mapInactiveEmployeeToEmployeeSafe(InactiveEmployee inactiveEmployee) {
        Employee employee = new Employee();
        employee.setFirstName(inactiveEmployee.getFirstName());
        employee.setLastName(inactiveEmployee.getLastName());
        employee.setLdap(inactiveEmployee.getLdap());
        employee.setTeam(inactiveEmployee.getTeam());
        employee.setNewLevel(inactiveEmployee.getNewLevel());
        employee.setLead(inactiveEmployee.getLead());
        employee.setProgramManager(inactiveEmployee.getProgramManager());
        employee.setVendor(inactiveEmployee.getVendor());
        employee.setEmail(inactiveEmployee.getEmail());
        employee.setStatus(inactiveEmployee.getStatus());
        employee.setProcess(inactiveEmployee.getProcess());
        employee.setLevelBeforeChange(inactiveEmployee.getLevelBeforeChange());
        employee.setLevelAfterChange(inactiveEmployee.getLevelAfterChange());
        employee.setBackfillLdap(inactiveEmployee.getBackfillLdap());
        employee.setLanguage(inactiveEmployee.getLanguage());
        employee.setTenureTillDate(inactiveEmployee.getTenureTillDate());
        // Explicitly set profilePic to null to avoid LOB issues
        employee.setProfilePic(null);
        employee.setInactiveReason(inactiveEmployee.getInactiveReason());
        employee.setPnseProgram(inactiveEmployee.getPnseProgram());
        employee.setStartDate(inactiveEmployee.getStartDate());
        employee.setRoleChangeEffectiveDate(inactiveEmployee.getRoleChangeEffectiveDate());
        employee.setLastBillingDate(inactiveEmployee.getLastBillingDate());
        employee.setBillingStartDate(inactiveEmployee.getBillingStartDate());
        employee.setLwdMlStartDate(inactiveEmployee.getLwdMlStartDate());
        employee.setParent(inactiveEmployee.getParent());
        employee.setLevel(inactiveEmployee.getLevel());
        employee.setShift(inactiveEmployee.getShift());
        employee.setLocation(inactiveEmployee.getLocation());
        employee.setResignationDate(inactiveEmployee.getResignationDate());
        employee.setId(inactiveEmployee.getId());
        employee.setInactive(true);
        return employee;
    }

    // Helper method to create a safe LeadsRequest object without LOB fields
    private LeadsRequest createSafeLeadsRequest(LeadsRequest original) {
        LeadsRequest safe = new LeadsRequest();
        safe.setId(original.getId());
        safe.setStatus(original.getStatus());
        safe.setRequestedBy(original.getRequestedBy());
        safe.setRequestType(original.getRequestType());
        safe.setLdap(original.getLdap());
        safe.setRequestedAt(original.getRequestedAt());
        safe.setIsSignUp(original.getIsSignUp());
        // Copy the employee data key (no LOB issues)
        safe.setEmployeeDataKey(original.getEmployeeDataKey());
        return safe;
    }

    // Helper method to map Object[] from native query to LeadsRequest entity
    private LeadsRequest mapRowToLeadsRequest(Object[] row) {
        LeadsRequest leadsRequest = new LeadsRequest();
        int i = 0;
        leadsRequest.setId(row[i++] != null ? ((Number) row[i - 1]).longValue() : null);
        leadsRequest.setStatus((String) row[i++]);
        leadsRequest.setRequestedBy((String) row[i++]);
        leadsRequest.setRequestType((String) row[i++]);
        leadsRequest.setLdap((String) row[i++]);
        leadsRequest.setRequestedAt(row[i++] != null ? (java.sql.Timestamp) row[i - 1] : null);
        leadsRequest.setIsSignUp(row[i++] != null ? (Boolean) row[i - 1] : false);
        // Set employee data key if available
        leadsRequest.setEmployeeDataKey(row.length > i ? (String) row[i] : null);
        return leadsRequest;
    }

    // Helper method to map Object[] from native query to Employee entity
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

    // Helper method to map Object[] from native query to Employee entity (from
    // inactive_employees table)
    private Employee mapRowToInactiveEmployee(Object[] row) {
        Employee employee = new Employee();
        int i = 0;
        employee.setId(row[i++] != null ? ((Number) row[i - 1]).longValue() : null);
        employee.setFirstName((String) row[i++]);
        employee.setLastName((String) row[i++]);
        employee.setLdap((String) row[i++]);
        employee.setTeam((String) row[i++]);
        employee.setNewLevel((String) row[i++]);
        employee.setLead((String) row[i++]);
        employee.setProgramManager((String) row[i++]);
        employee.setVendor((String) row[i++]);
        employee.setEmail((String) row[i++]);
        employee.setStatus((String) row[i++]);
        employee.setProcess((String) row[i++]);
        employee.setLevelBeforeChange((String) row[i++]);
        employee.setLevelAfterChange((String) row[i++]);
        employee.setBackfillLdap((String) row[i++]);
        employee.setLanguage((String) row[i++]);
        employee.setTenureTillDate((String) row[i++]);
        employee.setLevel((String) row[i++]);
        employee.setParent(row[i++] != null ? ((Number) row[i - 1]).longValue() : null);
        employee.setInactiveReason((String) row[i++]);
        employee.setPnseProgram((String) row[i++]);
        employee.setStartDate((String) row[i++]);
        employee.setRoleChangeEffectiveDate((String) row[i++]);
        employee.setLastBillingDate((String) row[i++]);
        employee.setBillingStartDate((String) row[i++]);
        employee.setLwdMlStartDate((String) row[i++]);
        employee.setShift((String) row[i++]);
        employee.setLocation((String) row[i++]);
        employee.setResignationDate((String) row[i++]);
        // Skip deleted_at field (i++)
        i++;
        employee.setInactive(row[i++] != null ? (Boolean) row[i - 1] : true);
        // profilePic is intentionally not set (avoiding LOB)
        return employee;
    }

    @Override
    public Employee getEmployeeById(String userId, boolean isInactive) {
        if (isInactive) {
            // If isInactive is true, look in inactive employees table
            InactiveEmployee inactiveEmployee = inactiveEmployeeRepository
                    .findByIdExcludingLobsNative(Long.valueOf(userId));

            // Convert InactiveEmployee to Employee for consistent response
            Employee employee = new Employee();
            employee.setId(inactiveEmployee.getId());
            employee.setFirstName(inactiveEmployee.getFirstName());
            employee.setLastName(inactiveEmployee.getLastName());
            employee.setLdap(inactiveEmployee.getLdap());
            employee.setStartDate(inactiveEmployee.getStartDate());
            employee.setTeam(inactiveEmployee.getTeam());
            employee.setNewLevel(inactiveEmployee.getNewLevel());
            employee.setProgramManager(inactiveEmployee.getProgramManager());
            employee.setLead(inactiveEmployee.getLead());
            employee.setVendor(inactiveEmployee.getVendor());
            employee.setEmail(inactiveEmployee.getEmail());
            employee.setStatus("Inactive");
            employee.setLwdMlStartDate(inactiveEmployee.getLwdMlStartDate());
            employee.setProcess(inactiveEmployee.getProcess());
            employee.setResignationDate(inactiveEmployee.getResignationDate());
            employee.setRoleChangeEffectiveDate(inactiveEmployee.getRoleChangeEffectiveDate());
            employee.setLevelBeforeChange(inactiveEmployee.getLevelBeforeChange());
            employee.setLevelAfterChange(inactiveEmployee.getLevelAfterChange());
            employee.setLastBillingDate(inactiveEmployee.getLastBillingDate());
            employee.setBackfillLdap(inactiveEmployee.getBackfillLdap());
            employee.setBillingStartDate(inactiveEmployee.getBillingStartDate());
            employee.setLanguage(inactiveEmployee.getLanguage());
            employee.setTenureTillDate(inactiveEmployee.getTenureTillDate());
            employee.setLevel(inactiveEmployee.getLevel());
            employee.setInactiveReason(inactiveEmployee.getInactiveReason());
            employee.setPnseProgram(inactiveEmployee.getPnseProgram());
            employee.setLocation(inactiveEmployee.getLocation());
            employee.setShift(inactiveEmployee.getShift());
            employee.setId(inactiveEmployee.getId());
            employee.setEmployeeRelations(new ArrayList<>()); // No relations for inactive
            return employee;
        } else {
            // If isInactive is false, look in active employees table
            // return employeeRepository.findById(Long.valueOf(userId))
            // .orElseThrow(() -> new RuntimeException("Active employee not found"));
            Employee employee = employeeRepository.findByIdExcludingLobsNative(Long.valueOf(userId));
            if (employee != null) {
                // Fetch relations for this employee
                List<EmployeeRelation> relations = employeeRelationRepository.findByEmployeeId(employee.getId());
                employee.setEmployeeRelations(relations);
            }
            return employee;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEmployeeByLdap(String ldap) {
        if (ldap == null) {
            throw new RuntimeException("LDAP is null");
        }

        // Clean up whitespace and surrounding quotes
        String cleanedLdap = ldap.trim();
        if (cleanedLdap.startsWith("\"") && cleanedLdap.endsWith("\"") && cleanedLdap.length() > 1) {
            cleanedLdap = cleanedLdap.substring(1, cleanedLdap.length() - 1).trim();
        }

        String finalCleanedLdap = cleanedLdap;
        return employeeRepository.findByLdap(cleanedLdap)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + finalCleanedLdap));
    }

    @Override
    public void deleteEmployeeById(Long userId) {
        employeeRepository.deleteById(userId);
    }

    // New saveEmployees method
    @Override
    public List<Employee> saveEmployees(List<EmployeeDTO> employeeDTOs) {
        if (employeeDTOs == null || employeeDTOs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Employee> employees = new ArrayList<>();
        for (EmployeeDTO employeeDTO : employeeDTOs) {
            // Skip inactive employees
            if ("inactive".equalsIgnoreCase(employeeDTO.getStatus())) {
                InactiveEmployee employee = createInactiveEmployee(employeeDTO);
                inactiveEmployeeRepository.save(employee);
                continue;
            }

            // Check if LDAP already exists in DB
            Optional<Employee> existingEmployee = employeeRepository.findByLdap(employeeDTO.getLdap().toLowerCase());
            if (existingEmployee.isPresent()) {
                continue; // Skip saving duplicate LDAPs
            }

            Employee employee = convertToEntity(employeeDTO, null);
            employees.add(employee);
        }

        return employeeRepository.saveAll(employees);
    }

    private InactiveEmployee createInactiveEmployee(EmployeeDTO employeeDTO) {
        InactiveEmployee inactiveEmployee = new InactiveEmployee();
        inactiveEmployee.setFirstName(employeeDTO.getFirstName());
        inactiveEmployee.setLastName(employeeDTO.getLastName());
        inactiveEmployee.setLdap(employeeDTO.getLdap());
        inactiveEmployee.setTeam(employeeDTO.getTeam());
        inactiveEmployee.setNewLevel(employeeDTO.getNewLevel());
        inactiveEmployee.setLead(employeeDTO.getLead());
        inactiveEmployee.setProgramManager(employeeDTO.getProgramManager());
        inactiveEmployee.setVendor(employeeDTO.getVendor());
        inactiveEmployee.setEmail(employeeDTO.getEmail());
        inactiveEmployee.setStatus(employeeDTO.getStatus());
        inactiveEmployee.setProcess(employeeDTO.getProcess());
        inactiveEmployee.setLevelBeforeChange(employeeDTO.getLevelBeforeChange());
        inactiveEmployee.setLevelAfterChange(employeeDTO.getLevelAfterChange());
        inactiveEmployee.setBackfillLdap(employeeDTO.getBackfillLdap());
        inactiveEmployee.setLanguage(employeeDTO.getLanguage());
        inactiveEmployee.setTenureTillDate(employeeDTO.getTenureTillDate());
        inactiveEmployee.setLevel(employeeDTO.getLevel());
        inactiveEmployee.setParent(employeeDTO.getParent());
        inactiveEmployee.setProfilePic(employeeDTO.getProfilePic() != null ? employeeDTO.getProfilePic() : new byte[0]);
        inactiveEmployee.setInactiveReason(employeeDTO.getInactiveReason());
        inactiveEmployee.setPnseProgram(employeeDTO.getPnseProgram());
        inactiveEmployee.setStartDate(employeeDTO.getStartDate());
        inactiveEmployee.setRoleChangeEffectiveDate(employeeDTO.getRoleChangeEffectiveDate());
        inactiveEmployee.setLastBillingDate(employeeDTO.getLastBillingDate());
        inactiveEmployee.setBillingStartDate(employeeDTO.getBillingStartDate());
        inactiveEmployee.setLwdMlStartDate(employeeDTO.getLwdMlStartDate());
        inactiveEmployee.setShift(employeeDTO.getShift());
        inactiveEmployee.setLocation(employeeDTO.getLocation());
        inactiveEmployee.setDeletedAt(LocalDate.now());
        inactiveEmployee.setResignationDate(employeeDTO.getResignationDate());
        return inactiveEmployee;
    }

    private Employee convertToEntity(EmployeeDTO employeeDTO, Long id) {
        Employee employee = null;
        if (id == null) {
            employee = new Employee();

        } else {
            employee = employeeRepository.findByIdExcludingLobsNative(id);
        }
        System.out.println("employeeDTO.ldap" + employeeDTO.getLdap());
        LocalDate startDate = LocalDate.parse(convertToDate(employeeDTO.getStartDate()));
        employee.setStartDate(convertToDate(employeeDTO.getStartDate()));
        employee.setRoleChangeEffectiveDate(convertToDate(employeeDTO.getRoleChangeEffectiveDate()));
        employee.setLastBillingDate(convertToDate(employeeDTO.getLastBillingDate()));
        employee.setBillingStartDate(convertToDate(employeeDTO.getBillingStartDate()));
        employee.setLwdMlStartDate(convertToDate(employeeDTO.getLwdMlStartDate()));
        employee.setFirstName(employeeDTO.getFirstName());
        employee.setLastName(employeeDTO.getLastName());
        employee.setTeam(employeeDTO.getTeam());
        employee.setNewLevel(employeeDTO.getNewLevel());
        employee.setLead(employeeDTO.getLead());
        employee.setProgramManager(employeeDTO.getProgramManager());
        employee.setVendor(employeeDTO.getVendor());
        employee.setEmail(employeeDTO.getEmail());
        employee.setStatus(employeeDTO.getStatus());
        employee.setProcess(employeeDTO.getProcess());
        employee.setLevelBeforeChange(employeeDTO.getLevelBeforeChange());
        employee.setLevelAfterChange(employeeDTO.getLevelAfterChange());
        employee.setBackfillLdap(employeeDTO.getBackfillLdap());
        employee.setLanguage(employeeDTO.getLanguage());
        LocalDate currentDate = LocalDate.now(); // Get today's date
        if (employeeDTO.getResignationDate() != null && !employeeDTO.getResignationDate().isEmpty()
                && !employeeDTO.getResignationDate().equals("")) {
            LocalDate resignationDate = LocalDate.parse(convertToDate(employeeDTO.getResignationDate()));
            long tenure = ChronoUnit.YEARS.between(startDate, resignationDate); // Calculate difference in years
            employee.setTenureTillDate(String.valueOf(tenure));
        } else {
            employee.setTenureTillDate(String.valueOf(ChronoUnit.YEARS.between(startDate, currentDate)));
        }
        employee.setProfilePic(employeeDTO.getProfilePic());
        employee.setLdap(employeeDTO.getLdap());
        employee.setLevel(employeeDTO.getLevel());
        employee.setInactiveReason(employeeDTO.getInactiveReason());
        employee.setPnseProgram(employeeDTO.getPnseProgram());
        employee.setLocation(employeeDTO.getLocation());
        employee.setShift(employeeDTO.getShift());
        employee.setResignationDate(employeeDTO.getResignationDate());
        if (employeeDTO.getProgramManager() == null || employeeDTO.getProgramManager().isEmpty()) {
            employee.setParent(1L);
        } else {
            Optional<Employee> manager = employeeRepository.findByLdap(employeeDTO.getProgramManager());
            if (manager.isPresent()) {
                employee.setParent(manager.get().getId());
            } else {
                employee.setParent(1L);
            }
        }
        if (employeeDTO.getLead() != null && employeeDTO.getLead().equals(employeeDTO.getProgramManager())) {
            Optional<Employee> manager = employeeRepository.findByLdap(employeeDTO.getProgramManager());
            if (manager.isPresent()) {
                employee.setParent(manager.get().getId());
            }
        } else if (employeeDTO.getLead() != null && !employeeDTO.getLead().equals(employeeDTO.getProgramManager())) {
            Optional<Employee> lead = employeeRepository.findByLdap(employeeDTO.getLead());
            if (lead.isPresent()) {
                employee.setParent(lead.get().getId());
            }
        }

        return employee;
    }

    private String convertToDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            // If it's an Excel Serial Number (contains only digits or decimal)
            if (dateStr.matches("\\d+(\\.\\d+)?")) {
                LocalDate date = convertExcelSerialToDate(Double.parseDouble(dateStr));
                return date.toString(); // Convert to YYYY-MM-DD format
            }

            // If it's already in YYYY-MM-DD format, return as is
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return dateStr;
            }

            // Handle ISO 8601 format like "2025-01-31T18:30:00.000Z"
            try {
                Instant instant = Instant.parse(dateStr); // Parse the ISO timestamp
                LocalDate date = instant.atZone(ZoneId.of("UTC")).toLocalDate(); // Convert to LocalDate
                return date.toString(); // Return YYYY-MM-DD
            } catch (DateTimeParseException ignored) {
            }

            // Try parsing as MM/dd/yyyy or dd/MM/yyyy
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd")
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    LocalDate parsedDate = LocalDate.parse(dateStr, formatter);
                    return parsedDate.toString();
                } catch (DateTimeParseException ignored) {
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing date: " + dateStr);
        }

        return null; // Return null if no valid format found
    }

    public LocalDate convertExcelSerialToDate(double excelSerial) {
        return LocalDate.of(1899, 12, 30).plusDays((long) excelSerial - 1);
    }

    @Override
    public void deleteEmployeesByIds(List<Long> userIds) {
        employeeRepository.deleteAllById(userIds);
    }

    @Override
    @Transactional
    public List<Employee> findAllById(List<Long> userIds) {
        return findAllByIdExcludingLobs(userIds);
    }

    @Override
    @Transactional
    public List<Employee> findAllByIdExcludingLobs(List<Long> userIds) {
        try {
            List<Employee> employees = new ArrayList<>();
            List<Object[]> employeeRows = employeeRepository.findAllByIdExcludingLobsNative(userIds);
            for (Object[] row : employeeRows) {
                Employee employee = mapRowToEmployee(row);
                employees.add(employee);
            }

            // Fetch and set employee relations
            if (!employees.isEmpty()) {
                List<Long> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toList());
                List<EmployeeRelation> allRelations = employeeRelationRepository.findByEmployeeIdIn(employeeIds);
                Map<Long, List<EmployeeRelation>> groupedRelations = allRelations.stream()
                        .collect(Collectors.groupingBy(r -> r.getEmployee().getId()));
                for (Employee emp : employees) {
                    emp.setEmployeeRelations(groupedRelations.getOrDefault(emp.getId(), new ArrayList<>()));
                }
            }

            return employees;
        } catch (Exception e) {
            LoggerUtil.logError("Error in findAllByIdExcludingLobs: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void handleDeleteEmployee(List<Employee> deletedEmployees) throws IOException {
        File backupFile = new File(BACKUP_FILE_PATH);
        JSONObject backupData = new JSONObject();

        // Check if file exists and is not empty
        if (backupFile.exists() && backupFile.length() > 0) {
            try {
                String content = new String(Files.readAllBytes(backupFile.toPath())).trim();
                if (!content.isEmpty() && content.startsWith("{")) {
                    backupData = new JSONObject(content);
                } else {
                    // If content is invalid, start with an empty JSON object
                    backupData = new JSONObject();
                }
            } catch (JSONException e) {
                System.err.println("Invalid JSON format in backup file, starting fresh.");
                backupData = new JSONObject(); // Reset to an empty object
            }
        }

        // Get current timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());

        // Get existing backups or create a new array
        JSONArray allBackups = backupData.optJSONArray(timestamp);
        if (allBackups == null) {
            allBackups = new JSONArray();
        }

        // Add deleted employees
        for (Employee emp : deletedEmployees) {
            JSONObject obj = new JSONObject();
            obj.put("id", emp.getId());
            obj.put("firstName", emp.getFirstName());
            obj.put("lastName", emp.getLastName());
            obj.put("ldap", emp.getLdap());
            obj.put("startDate", emp.getStartDate());
            obj.put("team", emp.getTeam());
            obj.put("newLevel", emp.getNewLevel());
            obj.put("lead", emp.getLead());
            obj.put("programManager", emp.getProgramManager());
            obj.put("vendor", emp.getVendor());
            obj.put("email", emp.getEmail());
            obj.put("status", emp.getStatus());
            obj.put("lwdMlStartDate", emp.getLwdMlStartDate());
            obj.put("process", emp.getProcess());
            obj.put("roleChangeEffectiveDate", emp.getRoleChangeEffectiveDate());
            obj.put("levelBeforeChange", emp.getLevelBeforeChange());
            obj.put("levelAfterChange", emp.getLevelAfterChange());
            obj.put("lastBillingDate", emp.getLastBillingDate());
            obj.put("backfillLdap", emp.getBackfillLdap());
            obj.put("billingStartDate", emp.getBillingStartDate());
            obj.put("language", emp.getLanguage());
            obj.put("tenureTillDate", emp.getTenureTillDate());
            obj.put("level", emp.getLevel());
            obj.put("parent", emp.getParent());
            obj.put("deletedAt", timestamp);
            obj.put("shift", emp.getShift());
            obj.put("location", emp.getLocation());
            obj.put("resignationDate", emp.getResignationDate());
            allBackups.put(obj);
        }

        // Save updated data
        backupData.put(timestamp, allBackups);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))) {
            writer.write(backupData.toString(4)); // Pretty print JSON
        }
    }

    @Override
    @Transactional
    public List<Employee> getEmployeesByLead(String leadLdap, boolean isInactive) {
        try {
            List<Employee> employees = new ArrayList<>();

            // Get active employees by lead using native query
            List<Object[]> activeEmployeeRows = employeeRepository.findByLeadExcludingLobsNative(leadLdap);
            for (Object[] row : activeEmployeeRows) {
                Employee employee = mapRowToEmployee(row);
                employees.add(employee);
            }

            if (isInactive) {
                List<Object[]> inactiveEmployeeRows = inactiveEmployeeRepository
                        .findByLeadExcludingLobsNative(leadLdap);
                for (Object[] row : inactiveEmployeeRows) {
                    Employee employee = mapRowToInactiveEmployee(row);
                    employees.add(employee);
                }
            }
            return employees;
        } catch (Exception e) {
            LoggerUtil.logError("Error in getEmployeesByLead: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Employee> getEmployeesByProgramManager(String programManager, boolean isInactive) {
        try {
            List<Employee> employees = new ArrayList<>();

            // Get active employees by program manager
            List<Employee> activeEmployees = employeeRepository.findByProgramManager(programManager);
            employees.addAll(activeEmployees);

            // if(isInactive){
            // List<InactiveEmployee> inactiveEmployees =
            // inactiveEmployeeRepository.findByProgramManager(programManager);
            // for (InactiveEmployee inactiveEmployee : inactiveEmployees) {
            // employees.add(mapInactiveEmployeeToEmployee(inactiveEmployee));
            // }
            // }
            return employees;
        } catch (Exception e) {
            LoggerUtil.logError("Error in getEmployeesByProgramManager: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void saveLeadsRequest(LeadsRequest leadsRequest) {
        leadRepository.save(leadsRequest);
    }

    /**
     * Helper method to get employee data for a leads request
     * This is used by the frontend to display employee data
     */
    public String getEmployeeDataForRequest(Long requestId) {
        try {
            Optional<LeadsRequest> requestOpt = getLeadsRequestById(requestId);
            if (!requestOpt.isPresent()) {
                return null;
            }

            LeadsRequest request = requestOpt.get();
            String employeeDataKey = request.getEmployeeDataKey();
            if (employeeDataKey == null) {
                return null;
            }

            Optional<String> employeeDataOpt = employeeDataFileService.getEmployeeData(employeeDataKey);
            return employeeDataOpt.orElse(null);
        } catch (Exception e) {
            LoggerUtil.logError("Error getting employee data for request {}: {}", requestId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get employee data with original data for comparison
     */
    public java.util.Map<String, String> getEmployeeDataWithOriginal(Long requestId) {
        try {
            Optional<LeadsRequest> requestOpt = getLeadsRequestById(requestId);
            if (!requestOpt.isPresent()) {
                return null;
            }

            LeadsRequest request = requestOpt.get();
            String employeeDataKey = request.getEmployeeDataKey();
            if (employeeDataKey == null) {
                return null;
            }

            // Get current employee data
            Optional<String> employeeDataOpt = employeeDataFileService.getEmployeeData(employeeDataKey);
            if (!employeeDataOpt.isPresent()) {
                return null;
            }

            // Get original employee data
            Optional<String> originalDataOpt = employeeDataFileService.getOriginalEmployeeData(employeeDataKey);

            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("employeeData", employeeDataOpt.get());
            result.put("originalData", originalDataOpt.orElse(null));
            result.put("requestType", request.getRequestType());

            return result;
        } catch (Exception e) {
            LoggerUtil.logError("Error getting employee data with original for request {}: {}", requestId,
                    e.getMessage(), e);
            return null;
        }
    }

    /**
     * Export users with their edit logs as CSV
     *
     * @return CSV data as byte array
     * @throws IOException if there's an error generating the CSV
     */
    @Override
    public byte[] exportUsersWithLogs() throws IOException {
        List<Employee> employees = new ArrayList<>();
        List<Object[]> activeEmployeeRows = employeeRepository.findAllExcludingLobsNative();
        for (Object[] row : activeEmployeeRows) {
            Employee employee = mapRowToEmployee(row);
            employees.add(employee);
        }

        StringBuilder csvContent = new StringBuilder();

        // Add CSV header with all employee fields
        csvContent.append("firstName,lastName,ldap,startDate,team,newLevel,programManager,lead,");
        csvContent.append("vendor,email,status,lwdMlStartDate,process,resignationDate,roleChangeEffectiveDate,");
        csvContent.append("levelBeforeChange,levelAfterChange,lastBillingDate,backfillLdap,billingStartDate,");
        csvContent.append("language,tenureTillDate,level,comments,pnseProgram,location,shift,");
        csvContent.append("changeField,changedBy,changedAt\n");

        // Add data rows
        for (Employee employee : employees) {
            // Get edit logs for this employee
            List<UserEditLogDTO> logs = userEditLogService.getLogsByUserLdap(employee.getLdap());

            if (logs.isEmpty()) {
                // If no logs, just add the current employee data
                csvContent.append(formatCurrentEmployeeRow(employee));
            } else {
                // First add the current state of the employee
                csvContent.append(formatCurrentEmployeeRow(employee));

                // Then add historical rows for each change
                for (UserEditLogDTO log : logs) {
                    csvContent.append(formatHistoricalEmployeeRow(employee, log));
                }
            }
        }

        List<InactiveEmployee> employeeList = inactiveEmployeeRepository.findAll();
        for (InactiveEmployee employee : employeeList) {
            csvContent.append(formatCurrentEmployeeRow(mapInactiveEmployeeToEmployee(employee)));
        }

        return csvContent.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Format a CSV row with the current employee data
     *
     * @param employee The employee data
     * @return Formatted CSV row
     */
    private String formatCurrentEmployeeRow(Employee employee) {
        StringBuilder row = new StringBuilder();

        // Add all employee fields with current values
        row.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,,,%n",
                // employee.getParent() != null ? employee.getParent() : "",
                // employee.getId(),
                escapeCsvField(employee.getFirstName()),
                escapeCsvField(employee.getLastName()),
                escapeCsvField(employee.getLdap()),
                escapeCsvField(employee.getStartDate()),
                escapeCsvField(employee.getTeam()),
                escapeCsvField(employee.getNewLevel()),
                escapeCsvField(employee.getProgramManager()),
                escapeCsvField(employee.getLead()),
                escapeCsvField(employee.getVendor()),
                escapeCsvField(employee.getEmail()),
                escapeCsvField(employee.getStatus()),
                escapeCsvField(employee.getLwdMlStartDate() != null ? employee.getLwdMlStartDate() : "-"),
                escapeCsvField(employee.getProcess()),
                escapeCsvField(employee.getResignationDate() != null ? employee.getResignationDate() : "-"),
                escapeCsvField(
                        employee.getRoleChangeEffectiveDate() != null ? employee.getRoleChangeEffectiveDate() : "-"),
                escapeCsvField(employee.getLevelBeforeChange()),
                escapeCsvField(employee.getLevelAfterChange()),
                escapeCsvField(employee.getLastBillingDate() != null ? employee.getLastBillingDate() : "-"),
                escapeCsvField(employee.getBackfillLdap() != null ? employee.getBackfillLdap() : ""),
                escapeCsvField(employee.getBillingStartDate() != null ? employee.getBillingStartDate() : "-"),
                escapeCsvField(employee.getLanguage()),
                escapeCsvField(employee.getTenureTillDate()),
                escapeCsvField(employee.getLevel()),
                escapeCsvField(employee.getInactiveReason() != null ? employee.getInactiveReason() : ""),
                escapeCsvField(employee.getPnseProgram()),
                escapeCsvField(employee.getLocation()),
                escapeCsvField(employee.getShift())));

        return row.toString();
    }

    /**
     * Format a CSV row with historical employee data based on a log entry
     *
     * @param employee The current employee data
     * @param log      The log entry with historical change
     * @return Formatted CSV row with historical data
     */
    private String formatHistoricalEmployeeRow(Employee employee, UserEditLogDTO log) {
        StringBuilder row = new StringBuilder();

        // Create a copy of the current employee data
        Map<String, String> historicalData = new HashMap<>();
        // historicalData.put("parent", employee.getParent() != null ?
        // employee.getParent().toString() : "");
        // historicalData.put("id", employee.getId().toString());
        historicalData.put("firstName", employee.getFirstName());
        historicalData.put("lastName", employee.getLastName());
        historicalData.put("ldap", employee.getLdap());
        historicalData.put("startDate", employee.getStartDate());
        historicalData.put("team", employee.getTeam());
        historicalData.put("newLevel", employee.getNewLevel());
        historicalData.put("programManager", employee.getProgramManager());
        historicalData.put("lead", employee.getLead());
        historicalData.put("vendor", employee.getVendor());
        historicalData.put("email", employee.getEmail());
        historicalData.put("status", employee.getStatus());
        historicalData.put("lwdMlStartDate", employee.getLwdMlStartDate() != null ? employee.getLwdMlStartDate() : "-");
        historicalData.put("process", employee.getProcess());
        historicalData.put("resignationDate",
                employee.getResignationDate() != null ? employee.getResignationDate() : "-");
        historicalData.put("roleChangeEffectiveDate",
                employee.getRoleChangeEffectiveDate() != null ? employee.getRoleChangeEffectiveDate() : "-");
        historicalData.put("levelBeforeChange", employee.getLevelBeforeChange());
        historicalData.put("levelAfterChange", employee.getLevelAfterChange());
        historicalData.put("lastBillingDate",
                employee.getLastBillingDate() != null ? employee.getLastBillingDate() : "-");
        historicalData.put("backfillLdap", employee.getBackfillLdap() != null ? employee.getBackfillLdap() : "");
        historicalData.put("billingStartDate",
                employee.getBillingStartDate() != null ? employee.getBillingStartDate() : "-");
        historicalData.put("language", employee.getLanguage());
        historicalData.put("tenureTillDate", employee.getTenureTillDate());
        historicalData.put("level", employee.getLevel());
        historicalData.put("comments", employee.getInactiveReason() != null ? employee.getInactiveReason() : "");
        historicalData.put("pnseProgram", employee.getPnseProgram());
        historicalData.put("location", employee.getLocation());
        historicalData.put("shift", employee.getShift());

        // Replace the changed field with its old value
        if (log.getFieldName() != null && log.getOldValue() != null) {
            historicalData.put(log.getFieldName(), log.getOldValue());
        }

        // Format the row with historical data
        row.append(String.format(
                "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                // escapeCsvField(historicalData.get("parent")),
                // escapeCsvField(historicalData.get("id")),
                escapeCsvField(historicalData.get("firstName")),
                escapeCsvField(historicalData.get("lastName")),
                escapeCsvField(historicalData.get("ldap")),
                escapeCsvField(historicalData.get("startDate")),
                escapeCsvField(historicalData.get("team")),
                escapeCsvField(historicalData.get("newLevel")),
                escapeCsvField(historicalData.get("programManager")),
                escapeCsvField(historicalData.get("lead")),
                escapeCsvField(historicalData.get("vendor")),
                escapeCsvField(historicalData.get("email")),
                escapeCsvField(historicalData.get("status")),
                escapeCsvField(historicalData.get("lwdMlStartDate")),
                escapeCsvField(historicalData.get("process")),
                escapeCsvField(historicalData.get("resignationDate")),
                escapeCsvField(historicalData.get("roleChangeEffectiveDate")),
                escapeCsvField(historicalData.get("levelBeforeChange")),
                escapeCsvField(historicalData.get("levelAfterChange")),
                escapeCsvField(historicalData.get("lastBillingDate")),
                escapeCsvField(historicalData.get("backfillLdap")),
                escapeCsvField(historicalData.get("billingStartDate")),
                escapeCsvField(historicalData.get("language")),
                escapeCsvField(historicalData.get("tenureTillDate")),
                escapeCsvField(historicalData.get("level")),
                escapeCsvField(historicalData.get("comments")),
                escapeCsvField(historicalData.get("pnseProgram")),
                escapeCsvField(historicalData.get("location")),
                escapeCsvField(historicalData.get("shift")),
                escapeCsvField(log.getFieldName()),
                escapeCsvField(log.getChangedBy()),
                log.getChangedAt()));

        return row.toString();
    }

    /**
     * Escape CSV field to handle commas, quotes, and null values
     *
     * @param field The field to escape
     * @return Escaped field
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // If field contains comma, quote, or newline, wrap in quotes and escape quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }

    @Transactional(readOnly = true)
    public List<LeadsRequest> findAllLeads(String loggedInUser) {
        if (loggedInUser != null) {
            return leadRepository.findByLdap(loggedInUser);
        }
        return leadRepository.findAll();
    }

    @Override
    public List<LeadsRequest> getAllLeadsRequests(UserDetails userDetails, String status, String startDate,
            String endDate) {
        String loggedInuser = userDetails.getUsername(); // Get logged-in user
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);
        System.out.println("Logged in user: " + loggedInuser);
        System.out.println("Role: " + role);
        System.out.println("Filters - Status: " + status + ", Start Date: " + startDate + ", End Date: " + endDate);

        // Check if there are any leads requests at all
        List<LeadsRequest> allRequests = findAllLeads(null);

        System.out.println("Total leads requests in database: " + allRequests.size());

        if (allRequests.isEmpty()) {
            System.out.println("No leads requests found in the database");
            return Collections.emptyList();
        }

        // Get initial list based on user role
        List<LeadsRequest> filteredRequests;
        if (role != null && role.equals("ROLE_ADMIN_OPS_MANAGER")) {
            System.out.println("Admin role detected, starting with all requests");
            filteredRequests = new ArrayList<>(allRequests);
        } else {
            System.out.println("Non-admin role, starting with user's requests");
            // Use LDAP field instead of requestedBy since LDAP contains the username
            filteredRequests = new ArrayList<>(leadRepository.findByLdap(loggedInuser));
            System.out.println("User requests found: " + filteredRequests.size());
        }

        // Apply status filter if provided
        if (status != null && !status.isEmpty()) {
            filteredRequests = filteredRequests.stream()
                    .filter(request -> status.equalsIgnoreCase(request.getStatus()))
                    .collect(Collectors.toList());
            System.out.println("After status filter: " + filteredRequests.size());
        }

        // Apply date filters if provided
        if (startDate != null && !startDate.isEmpty()) {
            try {
                LocalDate parsedStartDate = LocalDate.parse(startDate);
                filteredRequests = filteredRequests.stream()
                        .filter(request -> {
                            LocalDate requestDate = request.getRequestedAt().toLocalDateTime().toLocalDate();
                            return !requestDate.isBefore(parsedStartDate);
                        })
                        .collect(Collectors.toList());
                System.out.println("After start date filter: " + filteredRequests.size());
            } catch (Exception e) {
                System.out.println("Error parsing start date: " + e.getMessage());
            }
        }

        if (endDate != null && !endDate.isEmpty()) {
            try {
                LocalDate parsedEndDate = LocalDate.parse(endDate);
                filteredRequests = filteredRequests.stream()
                        .filter(request -> {
                            LocalDate requestDate = request.getRequestedAt().toLocalDateTime().toLocalDate();
                            return !requestDate.isAfter(parsedEndDate);
                        })
                        .collect(Collectors.toList());
                System.out.println("After end date filter: " + filteredRequests.size());
            } catch (Exception e) {
                System.out.println("Error parsing end date: " + e.getMessage());
            }
        }

        return filteredRequests;
    }

    @Override
    public Optional<LeadsRequest> getLeadsRequestById(Long requestId) {
        return leadRepository.findById(requestId);
    }

    // Removed duplicate getAllEmployees() method

    @Transactional
    @Override
    public Map<String, Object> constructChartData(List<Employee> employees) {
        Map<String, Map<String, Long>> groupedData = new HashMap<>();
        Map<String, Long> pieChartData = new HashMap<>();

        // Filter out employees with NA or null PnseProgram
        List<Employee> validEmployees = employees.stream()
                .filter(emp -> emp.getPnseProgram() != null && !emp.getPnseProgram().equals("NA")
                        && !emp.getStatus().equals("Inactive"))
                .collect(Collectors.toList());

        for (Employee emp : validEmployees) {
            String program = emp.getPnseProgram();
            String level = emp.getLevel();

            groupedData
                    .computeIfAbsent(program, k -> new HashMap<>())
                    .merge(level, 1L, Long::sum);

            pieChartData.merge(program, 1L, Long::sum);
        }

        // Construct Stacked Bar Chart JSON
        List<String> categories = new ArrayList<>(groupedData.keySet());
        Set<String> allLevels = groupedData.values().stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toSet());

        List<Map<String, Object>> seriesData = new ArrayList<>();
        for (String level : allLevels) {
            Map<String, Object> series = new HashMap<>();
            series.put("name", level);
            series.put("data", categories.stream()
                    .map(cat -> groupedData.getOrDefault(cat, new HashMap<>()).getOrDefault(level, 0L))
                    .collect(Collectors.toList()));
            seriesData.add(series);
        }

        // Construct Pie Chart JSON
        List<Map<String, Object>> pieData = new ArrayList<>();
        for (Map.Entry<String, Long> entry : pieChartData.entrySet()) {
            Map<String, Object> pieEntry = new HashMap<>();
            pieEntry.put("name", entry.getKey());
            pieEntry.put("y", entry.getValue());
            pieData.add(pieEntry);
        }

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("categories", categories);
        chartData.put("series", seriesData);
        chartData.put("pieData", pieData);

        return chartData;
    }

    @Override
    public boolean requestAlreadyExists(String loggedInUser, String userOperation, String employeeLdap) {
        List<LeadsRequest> existingRequests = leadRepository.findAll();

        for (LeadsRequest request : existingRequests) {
            try {
                // Get employee data from file using the key
                String employeeDataKey = request.getEmployeeDataKey();
                if (employeeDataKey == null) {
                    continue; // Skip if no employee data key
                }

                Optional<String> jsonDataOpt = employeeDataFileService.getEmployeeData(employeeDataKey);
                if (!jsonDataOpt.isPresent()) {
                    continue; // Skip if employee data not found
                }

                String jsonData = jsonDataOpt.get();
                String reqString = request.getRequestType();

                if (jsonData.trim().startsWith("[")) {
                    // JSON is an array → Deserialize as List<Employee>
                    List<Employee> employeeDataList = objectMapper.readValue(jsonData,
                            new TypeReference<List<Employee>>() {
                            });
                    for (Employee employeeData : employeeDataList) {
                        if (employeeData != null && employeeLdap.equals(employeeData.getLdap())) {
                            // Check if requestType and userOperation match
                            if (reqString.equals(userOperation)) {
                                return true; // Both match, return false
                            } else {
                                return false; // Different operations for the same user, return true
                            }
                        }
                    }
                } else {
                    // JSON is a single object → Deserialize as Employee
                    Employee employeeData = objectMapper.readValue(jsonData, Employee.class);
                    if (employeeData != null && employeeLdap.equals(employeeData.getLdap())) {
                        // Check if requestType and userOperation match
                        if (reqString.equals(userOperation)) {
                            return true; // Both match, return false
                        } else {
                            return false; // Different operations for the same user, return true
                        }
                    }
                }
            } catch (Exception e) {
                LoggerUtil.logError("Error checking existing request: {}", e.getMessage(), e);
            }
        }

        return false; // No matching requests found
    }

    @Override
    public Employee addEmployee(EmployeeDTO employeeDTO, UserDetails userDetails, MultipartFile profilePic) {
        try {
            String loggedInUser = userDetails.getUsername();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            if (profilePic != null && !profilePic.isEmpty()) {
                if (profilePic.getSize() > 1048576) {
                    throw new IllegalArgumentException("Profile picture size should be less than 1 MB");
                }
                employeeDTO.setProfilePic(profilePic.getBytes());
            }
            LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "addEmployee", employeeDTO, loggedInUser, roles);
            if (roles.contains("ROLE_ADMIN_OPS_MANAGER")) {
                // Admin can directly add employee
                Employee savedEmployee = saveEmployee(employeeDTO, employeeDTO.getId());

                // Create user account if doesn't exist
                if (!userRepository.existsByUsername(savedEmployee.getLdap())) {
                    createUserAccount(savedEmployee);
                }

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "addEmployee", savedEmployee);
                return savedEmployee;
            } else {
                // Non-admin users create a request
                // if (requestAlreadyExists(loggedInUser, "ADD NEW USER",
                // employeeDTO.getLdap())) {
                // throw new RuntimeException("Request already exists for this employee");
                // }

                String employeeJson = objectMapper.writeValueAsString(employeeDTO);
                Employee requestingEmployee = getEmployeeByLdap(loggedInUser);

                // Store employee data in file and get the key
                String employeeDataKey = employeeDataFileService.storeEmployeeData(employeeJson);

                LeadsRequest leadsRequest = new LeadsRequest();
                leadsRequest.setEmployeeDataKey(employeeDataKey);
                leadsRequest.setRequestedBy(requestingEmployee.getFirstName() + " " + requestingEmployee.getLastName());
                leadsRequest.setStatus("PENDING");
                leadsRequest.setRequestType("ADD NEW USER");
                leadsRequest.setLdap(loggedInUser);

                saveLeadsRequest(leadsRequest);

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "addEmployee", "Request created");
                return null; // Return null to indicate request was created
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error adding employee: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add employee: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public List<Employee> getEmployeesForUser(UserDetails userDetails) {
        String loggedInUser = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "getEmployeesForUser", loggedInUser, roles);

        try {
            if (roles.contains("ROLE_MANAGER") || roles.contains("ROLE_ACCOUNT_MANAGER")
                    || roles.contains("ROLE_ADMIN_OPS_MANAGER")) {
                List<Employee> employees = getAllEmployees();
                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "getEmployeesForUser", employees.size());
                return employees;
            } else if (roles.contains("ROLE_LEAD")) {
                // Get delegators for the current user
                List<String> delegators = delegationService.getDelegatorsForDelegatee(loggedInUser);
                List<String> allLdaps = new ArrayList<>();
                allLdaps.add(loggedInUser);
                allLdaps.addAll(delegators);

                // Use findByLeadIn to get employees for all LDAPs (self + delegators)
                List<Employee> employees = employeeRepository.findByLeadIn(allLdaps);
                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "getEmployeesForUser", employees.size());
                return employees;
            } else {
                LoggerUtil.logError("Access denied for user: {} with roles: {}", loggedInUser, roles);
                throw new AccessDeniedException("You do not have permission to view employee data.");
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error getting employees for user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get employees: " + e.getMessage());
        }
    }

    @Override
    public Employee getEmployeeByIdWithPermission(String userId, boolean isInactive, UserDetails userDetails) {
        String loggedInUser = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "getEmployeeByIdWithPermission", userId, isInactive,
                loggedInUser, roles);

        try {
            Employee employee = getEmployeeById(userId, isInactive);

            // Check permissions
            boolean isAdminRole = roles.contains("ROLE_ADMIN_OPS_MANAGER") ||
                    roles.contains("ROLE_ACCOUNT_MANAGER") ||
                    roles.contains("ROLE_MANAGER");

            boolean isLead = roles.contains("ROLE_LEAD");
            boolean isOwnRecord = employee.getLdap().equals(loggedInUser);
            boolean isTeamMember = isLead && employee.getLead() != null &&
                    employee.getLead().equals(loggedInUser);

            if (isAdminRole || isOwnRecord || isTeamMember) {
                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "getEmployeeByIdWithPermission", employee);
                return employee;
            } else {
                LoggerUtil.logError("Access denied for user: {} to view employee: {}", loggedInUser, userId);
                throw new AccessDeniedException("You do not have permission to access this employee's data");
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error getting employee by ID with permission: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get employee: " + e.getMessage());
        }
    }

    @Override
    public Employee getEmployeeByLdapWithPermission(String ldap, UserDetails userDetails) {
        String loggedInUser = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "getEmployeeByLdapWithPermission", ldap, loggedInUser,
                roles);

        try {
            Employee employee = getEmployeeByLdap(ldap);

            // Check permissions
            boolean isAdminRole = roles.contains("ROLE_ADMIN_OPS_MANAGER") ||
                    roles.contains("ROLE_ACCOUNT_MANAGER") ||
                    roles.contains("ROLE_MANAGER");

            boolean isLead = roles.contains("ROLE_LEAD");
            boolean isOwnRecord = ldap.equals(loggedInUser);
            boolean isTeamMember = isLead && employee.getLead() != null &&
                    employee.getLead().equals(loggedInUser);

            if (isAdminRole || isOwnRecord || isTeamMember) {
                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "getEmployeeByLdapWithPermission", employee);
                return employee;
            } else {
                LoggerUtil.logError("Access denied for user: {} to view employee: {}", loggedInUser, ldap);
                throw new AccessDeniedException("You do not have permission to access this employee's data");
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error getting employee by LDAP with permission: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get employee: " + e.getMessage());
        }
    }

    /**
     * Helper method to create user account
     */
    private void createUserAccount(Employee employee) {
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "createUserAccount", employee.getLdap());

        try {
            User newUser = new User();
            newUser.setUsername(employee.getLdap());
            newUser.setPassword(passwordEncoder.encode("vbsllp"));

            Role userRole;
            String level = employee.getLevel();
            if (level == null || level.trim().isEmpty()) {
                level = employee.getNewLevel();
            }
            if (level == null || level.trim().isEmpty()) {
                level = "user";
            }

            switch (level.trim().toLowerCase()) {
                case "team lead":
                    userRole = Role.LEAD;
                    break;
                case "program manager":
                    userRole = Role.MANAGER;
                    break;
                case "account manager":
                    userRole = Role.ACCOUNT_MANAGER;
                    break;
                default:
                    userRole = Role.USER;
            }

            newUser.setRole(userRole);
            newUser.setPasswordChangeRequired(true);
            userRepository.save(newUser);

            LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "createUserAccount");
        } catch (Exception e) {
            LoggerUtil.logError("Error creating user account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user account: " + e.getMessage());
        }
    }

    @Override
    public String deleteEmployeeWithPermission(Long userId, UserDetails userDetails) {
        LoggerUtil.logDebug("Deleting employee with ID: {}", userId);
        String loggedInUser = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "deleteEmployeeWithPermission", userId, loggedInUser,
                roles);

        try {
            if (roles.contains("ROLE_ADMIN_OPS_MANAGER")) {
                // Admin can directly delete
                Employee employeeToDelete = getEmployeeById(String.valueOf(userId), false);
                deleteEmployeeById(userId);
                userRepository.deleteByUsername(employeeToDelete.getLdap());

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "deleteEmployeeWithPermission",
                        "Deleted successfully");
                return "Deleted successfully";
            } else {
                // Non-admin users create a request
                Employee employeeToDelete = getEmployeeById(String.valueOf(userId), false);
                // if (requestAlreadyExists(loggedInUser, "DELETE USER",
                // employeeToDelete.getLdap())) {
                // throw new RuntimeException("Delete request already exists for this
                // employee");
                // }

                String employeeJson = objectMapper.writeValueAsString(employeeToDelete);
                Employee requestingEmployee = getEmployeeByLdap(loggedInUser);

                // Store employee data in file and get the key
                String employeeDataKey = employeeDataFileService.storeEmployeeData(employeeJson);

                LeadsRequest leadsRequest = new LeadsRequest();
                leadsRequest.setEmployeeDataKey(employeeDataKey);
                leadsRequest.setRequestedBy(requestingEmployee.getFirstName() + " " + requestingEmployee.getLastName());
                leadsRequest.setStatus("PENDING");
                leadsRequest.setRequestType("DELETE USER");
                leadsRequest.setLdap(loggedInUser);

                saveLeadsRequest(leadsRequest);

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "deleteEmployeeWithPermission",
                        "Delete request submitted");
                return "Delete request submitted for approval";
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error deleting employee: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete employee: " + e.getMessage());
        }
    }

    @Override
    public List<Employee> addEmployeesFromCsv(List<EmployeeDTO> employees, UserDetails userDetails) {

        String loggedInUser = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "addEmployeesFromCsv", employees.size(), loggedInUser,
                roles);

        try {
            if (roles.contains("ROLE_ADMIN_OPS_MANAGER")) {
                // Admin can directly add employees
                List<Employee> savedEmployees = saveEmployees(employees);

                // Create user accounts for new employees
                for (Employee emp : savedEmployees) {
                    if (!userRepository.existsByUsername(emp.getLdap())) {
                        createUserAccount(emp);
                    }
                }

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "addEmployeesFromCsv", savedEmployees.size());
                return savedEmployees;
            } else {
                // Non-admin users create a request
                String employeesJson = objectMapper.writeValueAsString(employees);
                Employee requestingEmployee = getEmployeeByLdap(loggedInUser);

                // Store employee data in file and get the key
                String employeeDataKey = employeeDataFileService.storeEmployeeData(employeesJson);

                LeadsRequest leadsRequest = new LeadsRequest();
                leadsRequest.setEmployeeDataKey(employeeDataKey);
                leadsRequest.setRequestedBy(requestingEmployee.getFirstName() + " " + requestingEmployee.getLastName());
                leadsRequest.setStatus("PENDING");
                leadsRequest.setRequestType("ADD USERS CSV");
                leadsRequest.setLdap(loggedInUser);

                saveLeadsRequest(leadsRequest);

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "addEmployeesFromCsv", "Request created");
                return Collections.emptyList(); // Return empty list to indicate request was created
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error adding employees from CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add employees from CSV: " + e.getMessage());
        }
    }

    @Override
    public String deleteMultipleEmployees(List<Long> userIds, UserDetails userDetails) {
        LoggerUtil.logDebug("Deleting {} employees", userIds.size());

        String loggedInUser = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "deleteMultipleEmployees", userIds, loggedInUser, roles);

        try {
            if (userIds == null || userIds.isEmpty()) {
                throw new RuntimeException("No user IDs provided");
            }

            if (roles.contains("ROLE_ADMIN_OPS_MANAGER")) {
                // Admin can directly delete
                List<Employee> deletedEmployees = findAllById(userIds);
                handleDeleteEmployee(deletedEmployees);
                deleteEmployeesByIds(userIds);

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "deleteMultipleEmployees",
                        "Users deleted successfully");
                return "Users deleted successfully";
            } else {
                // Non-admin users create a request
                List<Employee> employeesToDelete = findAllById(userIds);
                for (Employee employee : employeesToDelete) {
                    // if (requestAlreadyExists(loggedInUser, "DELETE MULTIPLE USERS",
                    // employee.getLdap())) {
                    // throw new RuntimeException("Delete request already exists for one or more
                    // employees");
                    // }
                }

                String employeesJson = objectMapper.writeValueAsString(employeesToDelete);
                Employee requestingEmployee = getEmployeeByLdap(loggedInUser);

                // Store employee data in file and get the key
                String employeeDataKey = employeeDataFileService.storeEmployeeData(employeesJson);

                LeadsRequest leadsRequest = new LeadsRequest();
                leadsRequest.setEmployeeDataKey(employeeDataKey);
                leadsRequest.setRequestedBy(requestingEmployee.getFirstName() + " " + requestingEmployee.getLastName());
                leadsRequest.setStatus("PENDING");
                leadsRequest.setRequestType("DELETE MULTIPLE USERS");
                leadsRequest.setLdap(loggedInUser);

                saveLeadsRequest(leadsRequest);

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "deleteMultipleEmployees",
                        "Delete request submitted");
                return "Delete request submitted for approval";
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error deleting multiple employees: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete employees: " + e.getMessage());
        }
    }

    @Override
    public Employee updateEmployee(EmployeeDTO employeeDTO, Long id, UserDetails userDetails,
            MultipartFile profilePic) {
        LoggerUtil.logDebug("Updating employee with ID: {}", id);
        if (!id.equals(employeeDTO.getId())) {
            throw new IllegalArgumentException("ID mismatch: The provided ID does not match the employee DTO ID");
        }

        if (profilePic != null && !profilePic.isEmpty()) {
            if (profilePic.getSize() > 1048576) {
                throw new IllegalArgumentException("Profile picture size should be less than 1 MB");
            }
            try {
                employeeDTO.setProfilePic(profilePic.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String loggedInUser = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "updateEmployee", employeeDTO, id, loggedInUser, roles);

        try {
            if (roles.contains("ROLE_ADMIN_OPS_MANAGER")) {
                // Admin can directly update
                updateUserRoleIfChanged(employeeDTO);
                Employee savedEmployee = saveEmployee(employeeDTO, id);

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "updateEmployee", savedEmployee);
                return savedEmployee;
            } else {
                // Non-admin users create a request
                String employeeJson = objectMapper.writeValueAsString(employeeDTO);
                Employee requestingEmployee = getEmployeeByLdap(loggedInUser);

                // Get the original employee data for comparison
                Employee originalEmployee = getEmployeeById(String.valueOf(id), false);
                String originalEmployeeJson = objectMapper.writeValueAsString(originalEmployee);

                // Store employee data with original data for comparison
                String employeeDataKey = employeeDataFileService.storeEmployeeDataWithOriginal(employeeJson,
                        originalEmployeeJson);

                LeadsRequest leadsRequest = new LeadsRequest();
                leadsRequest.setEmployeeDataKey(employeeDataKey);
                leadsRequest.setRequestedBy(requestingEmployee.getFirstName() + " " + requestingEmployee.getLastName());
                leadsRequest.setStatus("PENDING");
                leadsRequest.setRequestType("EDIT EXISTING USER");
                leadsRequest.setLdap(loggedInUser);

                saveLeadsRequest(leadsRequest);

                LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "updateEmployee", "Request created");
                return null; // Return null to indicate request was created
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error updating employee: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update employee: " + e.getMessage());
        }
    }

    /**
     * Helper method to update user role if level has changed
     */
    private void updateUserRoleIfChanged(EmployeeDTO employeeDTO) {
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "updateUserRoleIfChanged", employeeDTO.getLdap());

        try {
            Optional<User> userOptional = userRepository.findByUsername(employeeDTO.getLdap());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                Role newRole = determineRole(employeeDTO.getLevel());

                if (!user.getRole().equals(newRole)) {
                    user.setRole(newRole);
                    userRepository.save(user);
                    LoggerUtil.logDebug("Updated user role for {}: {} -> {}", employeeDTO.getLdap(), user.getRole(),
                            newRole);
                }
            }

            LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "updateUserRoleIfChanged");
        } catch (Exception e) {
            LoggerUtil.logError("Error updating user role: {}", e.getMessage(), e);
            // Don't throw exception here as it's not critical
        }
    }

    /**
     * Helper method to determine role based on level
     */
    private Role determineRole(String level) {
        if (level == null)
            return Role.USER;

        switch (level.trim().toLowerCase()) {
            case "team lead":
                return Role.LEAD;
            case "program manager":
                return Role.MANAGER;
            case "account manager":
                return Role.ACCOUNT_MANAGER;
            default:
                return Role.USER;
        }
    }

    @Override
    @Transactional
    public String processLeadsRequest(Long requestId, String action) {
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "processLeadsRequest", requestId, action);

        try {
            Optional<LeadsRequest> optionalRequest = getLeadsRequestById(requestId);
            if (optionalRequest.isEmpty()) {
                return "Leads Request not found.";
            }

            LeadsRequest leadsRequest = optionalRequest.get();
            if (!"PENDING".equalsIgnoreCase(leadsRequest.getStatus())) {
                return "Request is already processed.";
            }

            if ("APPROVE".equalsIgnoreCase(action)) {
                return processApprovalRequest(leadsRequest);
            } else if ("REJECT".equalsIgnoreCase(action)) {
                leadsRequest.setStatus("REJECTED");
                saveLeadsRequest(leadsRequest);
                LoggerUtil.logInfo(EmployeeServiceImpl.class,
                        "Request {} rejected. Employee data retained for audit trail.", leadsRequest.getId());
                return "Request Rejected.";
            } else {
                return "Invalid action. Use 'APPROVE' or 'REJECT'.";
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error processing leads request: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing request: " + e.getMessage());
        }
    }

    private String processApprovalRequest(LeadsRequest leadsRequest) {
        try {
            // Get employee data from file using the key
            String employeeDataKey = leadsRequest.getEmployeeDataKey();
            if (employeeDataKey == null) {
                return "No employee data found for this request";
            }

            Optional<String> employeeDataOpt = employeeDataFileService.getEmployeeData(employeeDataKey);
            if (!employeeDataOpt.isPresent()) {
                return "Employee data not found in file storage";
            }

            String employeeDataJson = employeeDataOpt.get();

            switch (leadsRequest.getRequestType()) {
                case "ADD NEW USER":
                    EmployeeDTO newEmployee = objectMapper.readValue(employeeDataJson, EmployeeDTO.class);
                    Employee savedEmployee = saveEmployee(newEmployee, newEmployee.getId());

                    // Create user account if doesn't exist
                    if (!userRepository.existsByUsername(savedEmployee.getLdap())) {
                        createUserAccount(savedEmployee);
                    }
                    break;

                case "DELETE USER":
                    Employee employeeToDelete = objectMapper.readValue(employeeDataJson, Employee.class);
                    deleteEmployeeById(employeeToDelete.getId());
                    userRepository.deleteByUsername(employeeToDelete.getLdap());
                    break;

                case "DELETE MULTIPLE USERS":
                    List<Employee> employeesToDelete = objectMapper.readValue(
                            employeeDataJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Employee.class));
                    for (Employee emp : employeesToDelete) {
                        deleteEmployeeById(emp.getId());
                        userRepository.deleteByUsername(emp.getLdap());
                    }
                    break;

                case "ADD USERS CSV":
                    List<EmployeeDTO> employeesToAdd = objectMapper.readValue(
                            employeeDataJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, EmployeeDTO.class));
                    List<Employee> savedEmployees = saveEmployees(employeesToAdd);

                    // Create user accounts for new employees only
                    for (Employee emp : savedEmployees) {
                        if (!userRepository.existsByUsername(emp.getLdap())) {
                            createUserAccount(emp);
                        }
                    }
                    break;

                case "EDIT EXISTING USER":
                    EmployeeDTO updatedEmployee = objectMapper.readValue(employeeDataJson, EmployeeDTO.class);
                    // Update user role if level has changed
                    updateUserRoleIfChanged(updatedEmployee);
                    saveEmployee(updatedEmployee, updatedEmployee.getId());
                    break;

                case "SIGN UP":
                    if (leadsRequest.getIsSignUp()) {
                        User user = objectMapper.readValue(employeeDataJson, User.class);
                        userRepository.save(user);
                    }
                    break;

                default:
                    return "Unknown request type: " + leadsRequest.getRequestType();
            }

            leadsRequest.setStatus("APPROVED");
            saveLeadsRequest(leadsRequest);

            // Keep the employee data for audit trail and historical reference
            // Do not remove the data so it can be viewed later
            LoggerUtil.logInfo(EmployeeServiceImpl.class,
                    "Request {} approved successfully. Employee data retained for audit trail.", leadsRequest.getId());

            return "Request Approved and processed successfully.";

        } catch (Exception e) {
            LoggerUtil.logError("Error processing approval request: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing approval: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getLeadsData() {
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "getLeadsData");

        List<User> leads = userRepository.findByRoleIn(List.of(Role.LEAD, Role.MANAGER, Role.ADMIN_OPS_MANAGER));
        return leads.stream()
                .map(user -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", user.getId());
                    dto.put("ldap", user.getUsername());
                    dto.put("role", user.getRole().name());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getLeadsOnlyData() {
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "getLeadsOnlyData");

        List<User> leads = userRepository.findByRoleIn(List.of(Role.LEAD, Role.MANAGER, Role.ADMIN_OPS_MANAGER));
        List<Map<String, Object>> leadDTOs = leads.stream()
                .map(user -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", user.getId());
                    dto.put("ldap", user.getUsername());
                    dto.put("role", user.getRole().name());
                    return dto;
                })
                .collect(Collectors.toList());

        // Add hardcoded admin ops manager
        Map<String, Object> adminOps = new HashMap<>();
        adminOps.put("id", "00001");
        adminOps.put("ldap", "sanjeevkumar");
        adminOps.put("role", "ADMIN_OPS_MANAGER");
        leadDTOs.add(adminOps);

        return leadDTOs;
    }

    @Override
    public List<Map<String, Object>> getManagersOnlyData() {
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "getManagersOnlyData");

        List<User> managers = userRepository.findByRoleIn(List.of(Role.LEAD, Role.MANAGER, Role.ADMIN_OPS_MANAGER));
        List<Map<String, Object>> managerDTOs = managers.stream()
                .map(user -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", user.getId());
                    dto.put("ldap", user.getUsername());
                    dto.put("role", user.getRole().name());
                    return dto;
                })
                .collect(Collectors.toList());

        // Add hardcoded admin ops manager
        Map<String, Object> adminOps = new HashMap<>();
        adminOps.put("id", "00001");
        adminOps.put("ldap", "sanjeevkumar");
        adminOps.put("role", "ADMIN_OPS_MANAGER");
        managerDTOs.add(adminOps);

        return managerDTOs;
    }

    @Override
    public List<Map<String, Object>> getMyTeamData(UserDetails userDetails) {
        LoggerUtil.logDebug("Fetching team data for user: {}", userDetails.getUsername());
        String loggedInUser = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "getMyTeamData", loggedInUser, roles);

        if (!roles.contains("ROLE_LEAD") && !roles.contains("ROLE_MANAGER")
                && !roles.contains("ROLE_ADMIN_OPS_MANAGER")) {
            throw new AccessDeniedException("Access denied: insufficient permissions");
        }

        if (roles.contains("ROLE_ADMIN_OPS_MANAGER")) {
            return getAllActiveEmp().stream()
                    .map(employee -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", employee.getId());
                        dto.put("ldap", employee.getLdap());
                        dto.put("firstName", employee.getFirstName());
                        dto.put("lastName", employee.getLastName());
                        return dto;
                    })
                    .collect(Collectors.toList());
        }

        // Get delegators for the current user
        List<String> delegators = delegationService.getDelegatorsForDelegatee(loggedInUser);
        List<String> allLdaps = new ArrayList<>();
        allLdaps.add(loggedInUser);
        allLdaps.addAll(delegators);

        if (roles.contains("ROLE_MANAGER")) {
            return employeeRepository.findByProgramManagerIn(allLdaps).stream()
                    .map(employee -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", employee.getId());
                        dto.put("ldap", employee.getLdap());
                        dto.put("firstName", employee.getFirstName());
                        dto.put("lastName", employee.getLastName());
                        return dto;
                    })
                    .collect(Collectors.toList());
        }

        if (roles.contains("ROLE_LEAD")) {
            return employeeRepository.findByLeadIn(allLdaps).stream()
                    .map(employee -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", employee.getId());
                        dto.put("ldap", employee.getLdap());
                        dto.put("firstName", employee.getFirstName());
                        dto.put("lastName", employee.getLastName());
                        return dto;
                    })
                    .collect(Collectors.toList());
        }

        // Get all employees under the logged-in lead
        List<Employee> teamMembers = getEmployeesByLead(loggedInUser, false);

        // Convert to DTO format
        return teamMembers.stream()
                .map(employee -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", employee.getId());
                    dto.put("ldap", employee.getLdap());
                    dto.put("firstName", employee.getFirstName());
                    dto.put("lastName", employee.getLastName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String changeUserRole(String ldap, String newRole, String adminUser) {
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "changeUserRole", ldap, newRole, adminUser);

        try {
            if (ldap == null || newRole == null) {
                return "LDAP and new role must be provided.";
            }

            // Validate that the new role is a valid enum value
            try {
                Role.valueOf(newRole);
            } catch (IllegalArgumentException e) {
                return "Invalid role specified.";
            }

            Optional<User> userOptional = userRepository.findByUsername(ldap);
            if (userOptional.isEmpty()) {
                return "User not found";
            }

            User user = userOptional.get();
            String oldRole = user.getRole().name();

            // Log the role change for audit purposes
            LoggerUtil.logDebug("Role change: User {} role changed from {} to {} by {}",
                    ldap, oldRole, newRole, adminUser);

            user.setRole(Role.valueOf(newRole));
            userRepository.save(user);

            LoggerUtil.logMethodExit(EmployeeServiceImpl.class, "changeUserRole", "Role changed successfully");
            return "Role changed successfully.";
        } catch (Exception e) {
            LoggerUtil.logError("Error changing user role: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to change user role: " + e.getMessage());
        }
    }

    @Override
    public UserDTO getUser(String username) {
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "getUser", username);

        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return null;
            }

            User user = userOptional.get();
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setLdap(user.getUsername());
            userDTO.setRole(user.getRole().name());
            return userDTO;
        } catch (Exception e) {
            LoggerUtil.logError("Error getting user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Long> getRequestCounts(UserDetails userDetails) {
        LoggerUtil.logMethodEntry(EmployeeServiceImpl.class, "getRequestCounts", userDetails.getUsername());

        try {
            String loggedInUser = userDetails.getUsername();
            String role = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse(null);

            Map<String, Long> counts = new HashMap<>();

            // Get all requests based on user role
            List<LeadsRequest> allRequests;
            if (role != null && role.equals("ROLE_ADMIN_OPS_MANAGER")) {
                // Admin can see all requests
                allRequests = leadRepository.findAll();
            } else {
                // Non-admin users see only their requests
                allRequests = leadRepository.findByLdap(loggedInUser);
            }

            // Count requests by status
            long pendingCount = allRequests.stream()
                    .filter(request -> "PENDING".equals(request.getStatus()))
                    .count();

            long approvedCount = allRequests.stream()
                    .filter(request -> "APPROVED".equals(request.getStatus()))
                    .count();

            long rejectedCount = allRequests.stream()
                    .filter(request -> "REJECTED".equals(request.getStatus()))
                    .count();

            counts.put("pending", pendingCount);
            counts.put("approved", approvedCount);
            counts.put("rejected", rejectedCount);
            counts.put("total", (long) allRequests.size());

            LoggerUtil.logDebug("Request counts for user {}: {}", loggedInUser, counts);
            return counts;

        } catch (Exception e) {
            LoggerUtil.logError("Error getting request counts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get request counts: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getMyTeamLeadsData(String ldap, UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching leads and managers for user's team: {}", ldap);

            // Get current user's team
            String currentUserLdap;
            if (ldap == null || ldap.isEmpty() || ldap.equals("null")) {
                currentUserLdap = userDetails.getUsername();
            } else {
                currentUserLdap = ldap;
            }
            Optional<User> optionalUser = userRepository.findByUsername(currentUserLdap);
            if (optionalUser.get().getRole().equals(Role.LEAD)) {
                List<Role> targetRoles = Arrays.asList(Role.MANAGER, Role.ADMIN_OPS_MANAGER);
                return userRepository.findByRoleIn(targetRoles).stream().map(
                        leads -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", leads.getId());
                            map.put("ldap", leads.getUsername());
                            map.put("role", leads.getRole().name());
                            return map;
                        }

                ).collect(Collectors.toList());
            }
            if (optionalUser.get().getRole().equals(Role.MANAGER)
                    || optionalUser.get().getRole().equals(Role.ADMIN_OPS_MANAGER)) {
                List<Role> targetRoles = Arrays.asList(Role.ADMIN_OPS_MANAGER, Role.MANAGER);
                return userRepository.findByRoleIn(targetRoles).stream().map(
                        leads -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", leads.getId());
                            map.put("ldap", leads.getUsername());
                            map.put("role", leads.getRole().name());
                            return map;
                        }

                ).collect(Collectors.toList());
            }

            Employee currentUser = employeeRepository.findByLdap(currentUserLdap)
                    .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                            "Employee not found for LDAP: " + currentUserLdap));

            String team = currentUser.getTeam();
            if (team == null || team.isEmpty()) {
                throw new IllegalStateException("Current user is not assigned to any team");
            }

            // Fetch all active employees in the same team
            List<Employee> teamEmployees = getAllActiveEmp().stream()
                    .filter(emp -> team.equals(emp.getTeam()))
                    .collect(Collectors.toList());

            // Get LDAPs of team members and initialize a set to track added leads
            List<String> teamLdaps = teamEmployees.stream()
                    .map(Employee::getLdap)
                    .collect(Collectors.toList());
            Set<String> leadLdaps = new HashSet<>();

            // Fetch all users with lead/manager roles
            List<Role> targetRoles = Arrays.asList(Role.LEAD, Role.MANAGER, Role.ADMIN_OPS_MANAGER);
            List<User> users = userRepository.findByRoleIn(targetRoles);

            // Filter users by team membership and convert to map format
            List<Map<String, Object>> usersData = users.stream()
                    .filter(user -> teamLdaps.contains(user.getUsername()))
                    .map(user -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", user.getId());
                        map.put("ldap", user.getUsername());
                        map.put("role", user.getRole().name());
                        leadLdaps.add(user.getUsername());
                        return map;
                    })
                    .collect(Collectors.toList());

            // Add immediate lead if exists
            if (currentUser.getLead() != null && !currentUser.getLead().isEmpty()) {
                Optional<User> leadUser = userRepository.findByUsername(currentUser.getLead());
                if (leadUser.isPresent() && !leadLdaps.contains(currentUser.getLead())) {
                    Map<String, Object> leadMap = new HashMap<>();
                    leadMap.put("id", leadUser.get().getId());
                    leadMap.put("ldap", leadUser.get().getUsername());
                    leadMap.put("role", leadUser.get().getRole().name());
                    usersData.add(leadMap);
                    leadLdaps.add(currentUser.getLead());
                }
            }

            if (usersData.isEmpty()) {
                User user = userRepository.findByUsername("sanjeevkumar")
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin Ops Manager not found"));
                Map<String, Object> adminMap = new HashMap<>();
                adminMap.put("id", user.getId());
                adminMap.put("ldap", user.getUsername());
                adminMap.put("role", user.getRole().name());
                usersData.add(adminMap);
            }
            return usersData;
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching team leads: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch team leads: " + e.getMessage());
        }
    }

    @Override
    public List<Employee> getAllEmployeesSummary() {
        try {
            return getAllEmployees();
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching all employees: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch all employees: " + e.getMessage());
        }
    }

}
