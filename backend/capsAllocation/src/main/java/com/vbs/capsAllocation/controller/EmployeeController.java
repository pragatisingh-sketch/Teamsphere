package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.BaseResponse;
import com.vbs.capsAllocation.dto.EmployeeDTO;
import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.LeadsRequest;
import com.vbs.capsAllocation.dto.UserEditLogDTO;
import com.vbs.capsAllocation.service.EmployeeService;
import com.vbs.capsAllocation.service.EmployeeDataFileService;
import com.vbs.capsAllocation.service.UserEditLogService;
import com.vbs.capsAllocation.util.EmployeeDataMigrationUtil;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Controller for employee operations in the system
 *
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserEditLogService userEditLogService;

    @Autowired
    private EmployeeDataMigrationUtil migrationUtil;

    @Autowired
    private EmployeeDataFileService employeeDataFileService;

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<BaseResponse<Employee>> addEmployee(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("employee") EmployeeDTO employeeDTO,
            @RequestPart(value = "profilePic", required = false) MultipartFile profilePic) {
        try {
            LoggerUtil.logDebug("Adding employee: {}", employeeDTO.getLdap());
            Employee result = employeeService.addEmployee(employeeDTO, userDetails, profilePic);
            return ResponseEntity.ok(BaseResponse.success(
                    result != null ? "Employee added successfully" : "Request submitted for approval", result));
        } catch (Exception e) {
            LoggerUtil.logError("Error adding employee: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to add employee: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping
    public ResponseEntity<BaseResponse<List<Employee>>> getAllEmployees(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching employees for user: {}", userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Employees retrieved successfully",
                    employeeService.getEmployeesForUser(userDetails)));
        } catch (AccessDeniedException e) {
            LoggerUtil.logError("Access denied for user: {}", userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN.value()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching employees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve employees: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<List<Employee>>> getAllEmployeesSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching employee summary for user: {}", userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Employee summary retrieved successfully",
                    employeeService.getAllEmployeesSummary()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching employee summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve employee summary: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/getAllLeadsRequest")
    public ResponseEntity<BaseResponse<List<LeadsRequest>>> getAllLeadsRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LoggerUtil.logDebug("Fetching leads requests for user: {}", userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Leads requests retrieved successfully",
                    employeeService.getAllLeadsRequests(userDetails, status, startDate, endDate)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching leads requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve leads requests: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/approve-reject")
    public ResponseEntity<BaseResponse<String>> processLeadsRequest(
            @RequestParam Long requestId,
            @RequestParam String action) {
        try {
            LoggerUtil.logDebug("Processing leads request: {} with action: {}", requestId, action);
            return ResponseEntity.ok(BaseResponse.success("Request processed successfully",
                    employeeService.processLeadsRequest(requestId, action)));
        } catch (Exception e) {
            LoggerUtil.logError("Error processing leads request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Error processing request: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/leads-request/{requestId}/employee-data")
    public ResponseEntity<BaseResponse<String>> getEmployeeDataForRequest(
            @PathVariable Long requestId) {
        try {
            LoggerUtil.logDebug("Getting employee data for leads request: {}", requestId);
            String employeeData = employeeService.getEmployeeDataForRequest(requestId);
            if (employeeData != null) {
                return ResponseEntity.ok(BaseResponse.success("Employee data retrieved successfully", employeeData));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error("Employee data not found for request", HttpStatus.NOT_FOUND.value()));
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error getting employee data for request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Error retrieving employee data: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/leads-request/{requestId}/employee-data-with-original")
    public ResponseEntity<BaseResponse<java.util.Map<String, String>>> getEmployeeDataWithOriginal(
            @PathVariable Long requestId) {
        try {
            LoggerUtil.logDebug("Getting employee data with original for leads request: {}", requestId);
            java.util.Map<String, String> dataMap = employeeService.getEmployeeDataWithOriginal(requestId);
            if (dataMap != null && dataMap.containsKey("employeeData")) {
                return ResponseEntity
                        .ok(BaseResponse.success("Employee data with original retrieved successfully", dataMap));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error("Employee data not found for request", HttpStatus.NOT_FOUND.value()));
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error getting employee data with original for request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Error retrieving employee data: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or  hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/getAll")
    public ResponseEntity<BaseResponse<List<Employee>>> getAllEmployees() {
        try {
            LoggerUtil.logDebug("Fetching all employees");
            return ResponseEntity
                    .ok(BaseResponse.success("Employees retrieved successfully", employeeService.getAllEmployees()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching employees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve employees", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<Employee>> getEmployeeById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "false") boolean isInactive) {

        try {
            LoggerUtil.logDebug("Fetching employee by ID: {}", userId);
            return ResponseEntity.ok(BaseResponse.success("Employee retrieved successfully",
                    employeeService.getEmployeeByIdWithPermission(userId, isInactive, userDetails)));
        } catch (AccessDeniedException e) {
            LoggerUtil.logError("Access denied for user: {} to view employee: {}", userDetails.getUsername(), userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN.value()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching employee by ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve employee: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/by-ldap/{ldap}")
    public ResponseEntity<BaseResponse<Employee>> getEmployeeByLdap(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String ldap) {

        try {
            LoggerUtil.logDebug("Fetching employee by LDAP: {}", ldap);
            return ResponseEntity.ok(BaseResponse.success("Employee retrieved successfully",
                    employeeService.getEmployeeByLdapWithPermission(ldap, userDetails)));
        } catch (AccessDeniedException e) {
            LoggerUtil.logError("Access denied for user: {} to view employee: {}", userDetails.getUsername(), ldap);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN.value()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching employee by LDAP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve employee: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @DeleteMapping("{userId}")
    public ResponseEntity<BaseResponse<String>> deleteEmployeeById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId) {

        try {
            LoggerUtil.logDebug("Deleting employee with ID: {}", userId);
            return ResponseEntity
                    .ok(BaseResponse.success(employeeService.deleteEmployeeWithPermission(userId, userDetails)));
        } catch (Exception e) {
            LoggerUtil.logError("Error deleting employee: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to delete employee: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/add/csv")
    public ResponseEntity<BaseResponse<List<Employee>>> addEmployees(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody List<EmployeeDTO> employees) {

        try {
            LoggerUtil.logDebug("Adding {} employees from CSV", employees.size());
            List<Employee> result = employeeService.addEmployeesFromCsv(employees, userDetails);
            if (!result.isEmpty()) {
                return ResponseEntity.ok(BaseResponse.success("Employees added successfully", result));
            } else {
                return ResponseEntity.ok(BaseResponse.success("Request submitted for approval", result));
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error adding employees from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to add employees: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<String>> deleteEmployeesByIds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody List<Long> userIds) {

        try {
            return ResponseEntity
                    .ok(BaseResponse.success(employeeService.deleteMultipleEmployees(userIds, userDetails)));
        } catch (Exception e) {
            LoggerUtil.logError("Error deleting multiple employees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to delete employees: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<BaseResponse<Employee>> updateEmployee(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestPart("employee") EmployeeDTO employeeDTO,
            @RequestPart(value = "profilePic", required = false) MultipartFile profilePic) {

        try {
            LoggerUtil.logDebug("Updating employee with ID: {}", id);
            Employee result = employeeService.updateEmployee(employeeDTO, id, userDetails, profilePic);
            if (result != null) {
                return ResponseEntity.ok(BaseResponse.success("Employee updated successfully", result));
            } else {
                return ResponseEntity.ok(BaseResponse.success("Update request submitted for approval"));
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error updating employee: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to update employee: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/chart-data")
    public ResponseEntity<BaseResponse<Object>> getChartData() {
        try {
            LoggerUtil.logDebug("Fetching chart data");
            return ResponseEntity.ok(BaseResponse.success("Chart data retrieved successfully",
                    employeeService.constructChartData(employeeService.getAllEmployees())));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching chart data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to fetch chart data: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/leads")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getLeads() {
        try {
            LoggerUtil.logDebug("Fetching leads data");
            return ResponseEntity
                    .ok(BaseResponse.success("Leads data retrieved successfully", employeeService.getLeadsData()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching leads: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseResponse
                    .error("Failed to fetch leads: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("getLeadsOnly")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getLeadsOnly() {
        try {
            LoggerUtil.logDebug("Fetching leads only data");
            return ResponseEntity.ok(
                    BaseResponse.success("Leads only data retrieved successfully", employeeService.getLeadsOnlyData()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching leads only: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseResponse
                    .error("Failed to fetch leads only: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("getManagersOnly")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getManagersOnly() {
        try {
            LoggerUtil.logDebug("Fetching managers only data");
            return ResponseEntity.ok(BaseResponse.success("Managers only data retrieved successfully",
                    employeeService.getManagersOnlyData()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching managers only: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseResponse.error(
                    "Failed to fetch managers only: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/my-team-leads/{ldap}")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getMyTeamLeads(
            @PathVariable(required = false) String ldap, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching leads and managers for user's team: {}", ldap);
            return ResponseEntity.ok(BaseResponse.success("Team leads retrieved successfully",
                    employeeService.getMyTeamLeadsData(ldap, userDetails)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching team leads: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseResponse
                    .error("Failed to fetch team leads: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/my-team")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getMyTeam(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching team data for user: {}", userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Team data retrieved successfully",
                    employeeService.getMyTeamData(userDetails)));
        } catch (AccessDeniedException e) {
            LoggerUtil.logError("Access denied for user: {}", userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching team data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseResponse
                    .error("Failed to fetch team data: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/change-role")
    public ResponseEntity<BaseResponse<String>> changeRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {

        try {
            LoggerUtil.logDebug("Changing user role by admin: {}", userDetails.getUsername());
            System.out.println(request);
            return ResponseEntity.ok(BaseResponse.success("Role changed successfully", employeeService
                    .changeUserRole(request.get("ldap"), request.get("newRole"), userDetails.getUsername())));
        } catch (Exception e) {
            LoggerUtil.logError("Error changing user role: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to change user role: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get edit logs for a specific user
     *
     * @param ldap The LDAP of the user to get logs for
     * @return List of edit logs
     */
    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/edit-logs/{ldap}")
    public ResponseEntity<BaseResponse<List<UserEditLogDTO>>> getUserEditLogs(@PathVariable String ldap) {
        try {
            LoggerUtil.logDebug("Fetching edit logs for user: {}", ldap);
            return ResponseEntity.ok(BaseResponse.success("Edit logs retrieved successfully",
                    userEditLogService.getLogsByUserLdap(ldap)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching edit logs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to fetch edit logs: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Export user data with edit logs as CSV
     *
     * @return CSV file with user data and edit logs
     */
    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/export-with-logs")
    public ResponseEntity<BaseResponse<byte[]>> exportUsersWithLogs() {
        try {
            return ResponseEntity.ok(BaseResponse.success("Users with logs exported successfully",
                    employeeService.exportUsersWithLogs()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Migrate specific request with employee data (Admin only)
     * This endpoint is used to migrate existing blob data to JSON file storage
     */
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/migrate-request-data")
    public ResponseEntity<BaseResponse<String>> migrateRequestData(
            @RequestParam Long requestId,
            @RequestBody String employeeDataJson) {
        try {
            LoggerUtil.logDebug("Migrating employee data for request ID: {}", requestId);
            migrationUtil.migrateSpecificRequest(requestId, employeeDataJson);
            return ResponseEntity.ok(BaseResponse.success("Request data migrated successfully",
                    "Request ID " + requestId + " migrated"));
        } catch (Exception e) {
            LoggerUtil.logError("Error migrating request data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Error migrating request data: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Clean up old employee data files (Admin only)
     * This endpoint removes employee data older than specified days
     */
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/cleanup-old-data")
    public ResponseEntity<BaseResponse<String>> cleanupOldData(
            @RequestParam(defaultValue = "90") int daysOld) {
        try {
            LoggerUtil.logDebug("Cleaning up employee data older than {} days", daysOld);
            int removedCount = employeeDataFileService.cleanupOldData(daysOld);
            String message = String.format("Cleaned up %d old employee data entries", removedCount);
            return ResponseEntity.ok(BaseResponse.success(message, message));
        } catch (Exception e) {
            LoggerUtil.logError("Error cleaning up old data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Error cleaning up old data: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get statistics about stored employee data (Admin only)
     */
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/employee-data-stats")
    public ResponseEntity<BaseResponse<String>> getEmployeeDataStats() {
        try {
            LoggerUtil.logDebug("Getting employee data statistics");
            java.util.Set<String> allKeys = employeeDataFileService.getAllKeys();
            String message = String.format("Total employee data entries stored: %d", allKeys.size());
            return ResponseEntity.ok(BaseResponse.success(message, message));
        } catch (Exception e) {
            LoggerUtil.logError("Error getting employee data stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Error getting stats: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get request counts by status for the current user
     */
    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/request-counts")
    public ResponseEntity<BaseResponse<Map<String, Long>>> getRequestCounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching request counts for user: {}", userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Request counts retrieved successfully",
                    employeeService.getRequestCounts(userDetails)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching request counts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to fetch request counts: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
