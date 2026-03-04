package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.BaseResponse;
import com.vbs.capsAllocation.dto.ComplianceDetailsDTO;
import com.vbs.capsAllocation.dto.UtilizationDetailsDTO;
import com.vbs.capsAllocation.dto.LeavesWFHDetailsDTO;
import com.vbs.capsAllocation.dto.InsightCardDTO;
import com.vbs.capsAllocation.dto.ReportDTO;
import com.vbs.capsAllocation.service.ReportsService;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller for generating various reports and insights
 */
@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    @Autowired
    private ReportsService reportsService;

    /**
     * Get dashboard insight cards
     */
    @GetMapping("/insights")
    public ResponseEntity<BaseResponse<List<InsightCardDTO>>> getDashboardInsights(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching dashboard insights with filters -  startDate: {}, endDate: {}, level: {}",
                    startDate, endDate, userDetails.getUsername());

            List<InsightCardDTO> insights = reportsService.getDashboardInsights(startDate, endDate,
                    userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success("Dashboard insights retrieved successfully", insights));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching dashboard insights: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve dashboard insights: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get time entry summary for a date range
     */
    @GetMapping("/time-entry-summary")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getTimeEntrySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching time entry summary from {} to {}", startDate, endDate);
            Map<String, Object> summary = reportsService.getTimeEntrySummary(startDate, endDate);
            return ResponseEntity.ok(BaseResponse.success("Time entry summary retrieved successfully", summary));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching time entry summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve time entry summary: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get project-wise time allocation
     */
    @GetMapping("/project-allocation")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getProjectAllocation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching project allocation from {} to {}", startDate, endDate);
            Map<String, Object> allocation = reportsService.getProjectAllocation(startDate, endDate);
            return ResponseEntity.ok(BaseResponse.success("Project allocation retrieved successfully", allocation));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching project allocation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve project allocation: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get employee productivity report
     */
    @GetMapping("/employee-productivity")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getEmployeeProductivity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching employee productivity from {} to {}", startDate, endDate);
            Map<String, Object> productivity = reportsService.getEmployeeProductivity(startDate, endDate);
            return ResponseEntity
                    .ok(BaseResponse.success("Employee productivity retrieved successfully", productivity));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching employee productivity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve employee productivity: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get attendance report
     */
    @GetMapping("/attendance")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getAttendanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching attendance report from {} to {}", startDate, endDate);
            Map<String, Object> attendance = reportsService.getAttendanceReport(startDate, endDate);
            return ResponseEntity.ok(BaseResponse.success("Attendance report retrieved successfully", attendance));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching attendance report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve attendance report: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get project status report
     */
    @GetMapping("/project-status")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getProjectStatusReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching project status report from {} to {}", startDate, endDate);
            Map<String, Object> statusReport = reportsService.getProjectStatusReport(startDate, endDate);
            return ResponseEntity
                    .ok(BaseResponse.success("Project status report retrieved successfully", statusReport));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching project status report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve project status report: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get time entry approval statistics
     */
    @GetMapping("/approval-stats")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getApprovalStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching approval statistics from {} to {}", startDate, endDate);
            Map<String, Object> approvalStats = reportsService.getApprovalStatistics(startDate, endDate);
            return ResponseEntity.ok(BaseResponse.success("Approval statistics retrieved successfully", approvalStats));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching approval statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve approval statistics: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get department-wise time allocation
     */
    @GetMapping("/department-allocation")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getDepartmentAllocation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching department allocation from {} to {}", startDate, endDate);
            Map<String, Object> departmentAllocation = reportsService.getDepartmentAllocation(startDate, endDate);
            return ResponseEntity
                    .ok(BaseResponse.success("Department allocation retrieved successfully", departmentAllocation));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching department allocation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve department allocation: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get overtime report
     */
    @GetMapping("/overtime")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getOvertimeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching overtime report from {} to {}", startDate, endDate);
            Map<String, Object> overtimeReport = reportsService.getOvertimeReport(startDate, endDate);
            return ResponseEntity.ok(BaseResponse.success("Overtime report retrieved successfully", overtimeReport));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching overtime report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve overtime report: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get custom report with filters
     */
    @PostMapping("/custom")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getCustomReport(@RequestBody CustomReportRequest request) {
        try {
            LoggerUtil.logDebug("Fetching custom report with filters: {}", request);
            Map<String, Object> customReport = reportsService.getCustomReport(request);
            return ResponseEntity.ok(BaseResponse.success("Custom report retrieved successfully", customReport));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching custom report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve custom report: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get compliance details (non-compliance issues by employee)
     */
    @GetMapping("/compliance-details")
    public ResponseEntity<BaseResponse<List<ComplianceDetailsDTO>>> getComplianceDetails(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching compliance details from {} to {}", startDate, endDate);
            List<ComplianceDetailsDTO> complianceDetails = reportsService.getComplianceDetails(startDate, endDate);
            return ResponseEntity
                    .ok(BaseResponse.success("Compliance details retrieved successfully", complianceDetails));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching compliance details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve compliance details: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get utilization details (resource utilization by employee)
     */
    /**
     * Get utilization details (resource utilization by employee)
     */
    @GetMapping("/utilization-details")
    public ResponseEntity<BaseResponse<List<UtilizationDetailsDTO>>> getUtilizationDetails(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String manager,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching utilization details from {} to {}", startDate, endDate);

            Map<String, Object> filters = new java.util.HashMap<>();
            if (team != null && !team.isEmpty())
                filters.put("team", team);
            if (project != null && !project.isEmpty())
                filters.put("project", project);
            if (program != null && !program.isEmpty())
                filters.put("program", program);
            if (manager != null && !manager.isEmpty())
                filters.put("manager", manager);

            List<UtilizationDetailsDTO> utilizationDetails = reportsService.getUtilizationDetails(startDate, endDate,
                    userDetails.getUsername(), filters);
            return ResponseEntity
                    .ok(BaseResponse.success("Utilization details retrieved successfully", utilizationDetails));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching utilization details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve utilization details: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get top 3 low utilization users
     */
    @GetMapping("/top-low-utilization")
    public ResponseEntity<BaseResponse<List<UtilizationDetailsDTO>>> getTopLowUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String manager,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching top low utilization users from {} to {}", startDate, endDate);

            Map<String, Object> filters = new java.util.HashMap<>();
            if (team != null && !team.isEmpty())
                filters.put("team", team);
            if (project != null && !project.isEmpty())
                filters.put("project", project);
            if (program != null && !program.isEmpty())
                filters.put("program", program);
            if (manager != null && !manager.isEmpty())
                filters.put("manager", manager);

            List<UtilizationDetailsDTO> topUsers = reportsService.getTopLowUtilization(startDate, endDate,
                    userDetails.getUsername(), filters);
            return ResponseEntity
                    .ok(BaseResponse.success("Top low utilization users retrieved successfully", topUsers));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching top low utilization users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve top low utilization users: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get leaves and WFH details (leave and work from home counts by employee)
     */
    @GetMapping("/leaves-wfh-details")
    public ResponseEntity<BaseResponse<List<LeavesWFHDetailsDTO>>> getLeavesWFHDetails(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching leaves/WFH details from {} to {}", startDate, endDate);
            List<LeavesWFHDetailsDTO> leavesWFHDetails = reportsService.getLeavesWFHDetails(startDate, endDate);
            return ResponseEntity
                    .ok(BaseResponse.success("Leaves/WFH details retrieved successfully", leavesWFHDetails));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching leaves/WFH details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve leaves/WFH details: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get report by type
     */
    @GetMapping("/{type}")
    public ResponseEntity<BaseResponse<ReportDTO>> getReportByType(@PathVariable String type) {
        try {
            LoggerUtil.logDebug("Fetching report of type: {}", type);
            ReportDTO report = reportsService.getReportByType(type);
            return ResponseEntity.ok(BaseResponse.success("Report retrieved successfully", report));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching report by type {}: {}", type, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve report: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get top 3 defaulters
     */
    @GetMapping("/top-defaulters")
    public ResponseEntity<BaseResponse<List<com.vbs.capsAllocation.dto.TopDefaulterDTO>>> getTopDefaulters(
            @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String manager,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching top defaulters for type: {}, from {} to {}", type, startDate, endDate);

            Map<String, Object> filters = new java.util.HashMap<>();
            if (team != null && !team.isEmpty())
                filters.put("team", team);
            if (project != null && !project.isEmpty())
                filters.put("project", project);
            if (program != null && !program.isEmpty())
                filters.put("program", program);
            if (manager != null && !manager.isEmpty())
                filters.put("manager", manager);

            List<com.vbs.capsAllocation.dto.TopDefaulterDTO> topDefaulters = reportsService.getTopDefaulters(type,
                    startDate, endDate, userDetails.getUsername(), filters);
            return ResponseEntity.ok(BaseResponse.success("Top defaulters retrieved successfully", topDefaulters));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching top defaulters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve top defaulters: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get all defaulters detailed list
     */
    @GetMapping("/all-defaulters")
    public ResponseEntity<BaseResponse<List<com.vbs.capsAllocation.dto.DefaulterDetailDTO>>> getAllDefaulters(
            @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String manager,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching all defaulters for type: {}, from {} to {}", type, startDate, endDate);

            Map<String, Object> filters = new java.util.HashMap<>();
            if (team != null && !team.isEmpty())
                filters.put("team", team);
            if (project != null && !project.isEmpty())
                filters.put("project", project);
            if (program != null && !program.isEmpty())
                filters.put("program", program);
            if (manager != null && !manager.isEmpty())
                filters.put("manager", manager);

            List<com.vbs.capsAllocation.dto.DefaulterDetailDTO> allDefaulters = reportsService.getAllDefaulters(type,
                    startDate, endDate, userDetails.getUsername(), filters);
            return ResponseEntity.ok(BaseResponse.success("All defaulters retrieved successfully", allDefaulters));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching all defaulters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve all defaulters: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get filter options
     */
    @GetMapping("/filter-options")
    public ResponseEntity<BaseResponse<Map<String, List<String>>>> getFilterOptions() {
        try {
            LoggerUtil.logDebug("Fetching filter options");
            Map<String, List<String>> options = reportsService.getFilterOptions();
            return ResponseEntity.ok(BaseResponse.success("Filter options retrieved successfully", options));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching filter options: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve filter options: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get detailed issues for a specific user by type
     * Used for the issue details modal when clicking on issue counts
     */
    @GetMapping("/user-issues")
    public ResponseEntity<BaseResponse<List<com.vbs.capsAllocation.dto.IssueDetailDTO>>> getUserIssues(
            @RequestParam String type,
            @RequestParam String employeeLdap,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching user issues - type: {}, ldap: {}, from {} to {}",
                    type, employeeLdap, startDate, endDate);

            List<com.vbs.capsAllocation.dto.IssueDetailDTO> issues = reportsService.getUserIssues(
                    type, employeeLdap, startDate, endDate);
            return ResponseEntity.ok(BaseResponse.success("User issues retrieved successfully", issues));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching user issues: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve user issues: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Autowired
    private com.vbs.capsAllocation.service.TimeEntryReminderService timeEntryReminderService;

    /**
     * Get weekly time-entry defaulters report
     * Returns list of employees who haven't filled time entries, grouped by weeks
     */
    @GetMapping("/weekly-time-entry-defaulters")
    public ResponseEntity<BaseResponse<List<com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO>>> getWeeklyTimeEntryDefaulters(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String manager,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching weekly time entry defaulters from {} to {}", startDate, endDate);

            Map<String, Object> filters = new java.util.HashMap<>();
            if (team != null && !team.isEmpty())
                filters.put("team", team);
            if (manager != null && !manager.isEmpty())
                filters.put("manager", manager);

            List<com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO> defaulters = reportsService
                    .getWeeklyTimeEntryDefaulters(startDate, endDate,
                            userDetails.getUsername(), filters);
            return ResponseEntity.ok(BaseResponse.success(
                    "Weekly time entry defaulters retrieved successfully", defaulters));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching weekly time entry defaulters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve weekly time entry defaulters: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Send time-entry reminder emails
     * Supports both individual and bulk reminder sending
     */
    @PostMapping("/send-time-entry-reminder")
    public ResponseEntity<BaseResponse<Map<String, Integer>>> sendTimeEntryReminder(
            @RequestBody com.vbs.capsAllocation.dto.TimeEntryReminderRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logInfo(ReportsController.class, "Sending time entry reminders - bulk: {}, recipients: {}",
                    request.isBulk(), request.getRecipientLdaps().size());

            Map<String, Integer> result = timeEntryReminderService.sendBulkReminders(request);

            String message = String.format("Reminders sent successfully. Success: %d, Failed: %d",
                    result.get("success"), result.get("failed"));
            return ResponseEntity.ok(BaseResponse.success(message, result));
        } catch (Exception e) {
            LoggerUtil.logError("Error sending time entry reminders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to send reminders: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get daily attendance defaulters for a specific date
     */
    @GetMapping("/daily-attendance-defaulters")
    public ResponseEntity<BaseResponse<List<com.vbs.capsAllocation.dto.DailyAttendanceDefaulterDTO>>> getDailyAttendanceDefaulters(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String manager,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching daily attendance defaulters for date: {}", date);

            Map<String, Object> filters = new java.util.HashMap<>();
            if (team != null && !team.isEmpty())
                filters.put("team", team);
            if (manager != null && !manager.isEmpty())
                filters.put("manager", manager);

            List<com.vbs.capsAllocation.dto.DailyAttendanceDefaulterDTO> defaulters = reportsService
                    .getDailyAttendanceDefaulters(date, userDetails.getUsername(), filters);
            return ResponseEntity.ok(BaseResponse.success(
                    "Daily attendance defaulters retrieved successfully", defaulters));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching daily attendance defaulters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve daily attendance defaulters: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Autowired
    private com.vbs.capsAllocation.service.AttendanceReminderService attendanceReminderService;

    /**
     * Send attendance reminder emails
     * Supports both individual and bulk reminder sending
     */
    @PostMapping("/send-attendance-reminder")
    public ResponseEntity<BaseResponse<Map<String, Integer>>> sendAttendanceReminder(
            @RequestBody com.vbs.capsAllocation.dto.AttendanceReminderRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logInfo(ReportsController.class, "Sending attendance reminders - bulk: {}, recipients: {}",
                    request.isBulk(), request.getRecipientLdaps().size());

            Map<String, Integer> result = attendanceReminderService.sendBulkReminders(request);

            String message = String.format("Reminders sent successfully. Success: %d, Failed: %d",
                    result.get("success"), result.get("failed"));
            return ResponseEntity.ok(BaseResponse.success(message, result));
        } catch (Exception e) {
            LoggerUtil.logError("Error sending attendance reminders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to send reminders: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get long weekend leave patterns
     */
    @GetMapping("/long-weekend-leave-patterns")
    public ResponseEntity<BaseResponse<List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO>>> getLongWeekendLeavePatterns(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String manager,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Fetching long weekend leave patterns from {} to {}", startDate, endDate);

            Map<String, Object> filters = new java.util.HashMap<>();
            if (team != null && !team.isEmpty())
                filters.put("team", team);
            if (manager != null && !manager.isEmpty())
                filters.put("manager", manager);

            List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO> patterns = reportsService
                    .getLongWeekendLeavePatterns(startDate, endDate, userDetails.getUsername(), filters);
            return ResponseEntity.ok(BaseResponse.success(
                    "Long weekend leave patterns retrieved successfully", patterns));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching long weekend leave patterns: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve long weekend leave patterns: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Request DTO for custom reports
     */
    public static class CustomReportRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<String> reportType;
        private Map<String, Object> filters;
        private String groupBy;
        private String sortBy;
        private String sortOrder;

        // Getters and Setters
        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public List<String> getReportType() {
            return reportType;
        }

        public void setReportType(List<String> reportType) {
            this.reportType = reportType;
        }

        public Map<String, Object> getFilters() {
            return filters;
        }

        public void setFilters(Map<String, Object> filters) {
            this.filters = filters;
        }

        public String getGroupBy() {
            return groupBy;
        }

        public void setGroupBy(String groupBy) {
            this.groupBy = groupBy;
        }

        public String getSortBy() {
            return sortBy;
        }

        public void setSortBy(String sortBy) {
            this.sortBy = sortBy;
        }

        public String getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}