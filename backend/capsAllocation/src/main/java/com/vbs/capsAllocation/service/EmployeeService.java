package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.EmployeeDTO;
import com.vbs.capsAllocation.dto.UserDTO;
import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.LeadsRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for Employee operations
 */
public interface EmployeeService {

    /**
     * Save or update an employee
     */
    Employee saveEmployee(EmployeeDTO employeeDTO, Long id);

    /**
     * Get all employees
     */
    List<Employee> getAllEmployees();

    /**
     * Get all employees including inactive ones
     */
    List<Employee> getAllEmployees(boolean includeInactive);

    /**
     * Get employee by ID
     */
    Employee getEmployeeById(String userId, boolean isInactive);

    /**
     * Get employees by lead
     */
    List<Employee> getEmployeesByLead(String leadLdap, boolean isInactive);

    /**
     * Delete employee by ID
     */
    void deleteEmployeeById(Long userId);

    /**
     * Save leads request
     */
    void saveLeadsRequest(LeadsRequest leadsRequest);

    /**
     * Get all leads requests
     */
    List<LeadsRequest> getAllLeadsRequests(UserDetails userDetails, String status, String startDate, String endDate);

    /**
     * Check if request already exists
     */
    boolean requestAlreadyExists(String ldap, String requestType, String targetLdap);

    /**
     * Export employees with edit logs to CSV
     */
    byte[] exportUsersWithLogs() throws IOException;

    /**
     * Construct chart data for employees
     */
    Map<String, Object> constructChartData(List<Employee> employees);

    /**
     * Get employee by LDAP
     */
    Employee getEmployeeByLdap(String ldap);

    /**
     * Get leads request by ID
     */
    Optional<LeadsRequest> getLeadsRequestById(Long requestId);

    /**
     * Save multiple employees
     */
    List<Employee> saveEmployees(List<EmployeeDTO> employeeDTOs);

    /**
     * Find employees by IDs
     */
    List<Employee> findAllById(List<Long> userIds);

    /**
     * Find employees by IDs excluding LOB fields
     */
    List<Employee> findAllByIdExcludingLobs(List<Long> userIds);

    /**
     * Handle delete employee (backup)
     */
    void handleDeleteEmployee(List<Employee> deletedEmployees) throws IOException;

    /**
     * Delete employees by IDs
     */
    void deleteEmployeesByIds(List<Long> userIds);

    /**
     * Get all active employees
     */
    List<Employee> getAllActiveEmp();

    /**
     * Add employee with role-based logic
     */
    Employee addEmployee(EmployeeDTO employeeDTO, UserDetails userDetails, MultipartFile profilePic);

    /**
     * Get employees based on user role and permissions
     */
    List<Employee> getEmployeesForUser(UserDetails userDetails);

    /**
     * Get employee by ID with permission check
     */
    Employee getEmployeeByIdWithPermission(String userId, boolean isInactive, UserDetails userDetails);

    /**
     * Get employee by LDAP with permission check
     */
    Employee getEmployeeByLdapWithPermission(String ldap, UserDetails userDetails);

    /**
     * Delete employee with role-based logic
     */
    String deleteEmployeeWithPermission(Long userId, UserDetails userDetails);

    /**
     * Add multiple employees from CSV with role-based logic
     */
    List<Employee> addEmployeesFromCsv(List<EmployeeDTO> employees, UserDetails userDetails);

    /**
     * Delete multiple employees with role-based logic
     */
    String deleteMultipleEmployees(List<Long> userIds, UserDetails userDetails);

    /**
     * Update employee with role-based logic
     */
    Employee updateEmployee(EmployeeDTO employeeDTO, Long id, UserDetails userDetails, MultipartFile profilePic);

    /**
     * Process leads request (approve/reject)
     */
    String processLeadsRequest(Long requestId, String action);

    /**
     * Get leads data with role filtering
     */
    List<Map<String, Object>> getLeadsData();

    /**
     * Get leads only data with role filtering
     */
    List<Map<String, Object>> getLeadsOnlyData();

    /**
     * Get managers only data with role filtering
     */
    List<Map<String, Object>> getManagersOnlyData();

    /**
     * Get team members for a user based on their role
     */
    List<Map<String, Object>> getMyTeamData(UserDetails userDetails);

    /**
     * Change user role (admin only)
     */
    String changeUserRole(String ldap, String newRole, String adminUser);

    /**
     * Get user by username
     */
    UserDTO getUser(String username);

    /**
     * Get request counts by status for the current user
     */
    Map<String, Long> getRequestCounts(UserDetails userDetails);

    List<Map<String, Object>> getMyTeamLeadsData(String ldap, UserDetails userDetails);

    /**
     * Get employee data for a leads request (for frontend display)
     */
    String getEmployeeDataForRequest(Long requestId);

    /**
     * Get employee data with original data for comparison
     */
    java.util.Map<String, String> getEmployeeDataWithOriginal(Long requestId);

    /**
     * Get all employees for summary/autocomplete usage (unrestricted)
     */
    List<Employee> getAllEmployeesSummary();
}
