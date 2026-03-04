package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.*;
import com.vbs.capsAllocation.service.TimeEntryService;
import com.vbs.capsAllocation.service.UserProjectService;
import com.vbs.capsAllocation.model.TimeEntryStatus;
import com.vbs.capsAllocation.util.LoggerUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for managing time entries in the Time Sheet System
 *
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/api/time-entries")
public class TimeEntryController {

        @Autowired
        private TimeEntryService timeEntryService;

        @Autowired
        private UserProjectService userProjectService;

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping
        public ResponseEntity<BaseResponse<TimeEntryDTO>> createTimeEntry(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody CreateTimeEntryDTO createTimeEntryDTO) {
                try {
                        LoggerUtil.logDebug("Creating time entry for LDAP: {} by user: {}",
                                        createTimeEntryDTO.getLdap(),
                                        userDetails.getUsername());
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(BaseResponse.success("Time entry created successfully",
                                                        timeEntryService.createTimeEntry(createTimeEntryDTO,
                                                                        createTimeEntryDTO.getLdap()),
                                                        HttpStatus.CREATED.value()));
                } catch (Exception e) {
                        LoggerUtil.logError("Error creating time entry: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to create time entry: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        /**
         * Creates multiple time entries for different projects/processes on the same
         * day
         *
         * @param bulkTimeEntriesDTO List of time entries to create
         * @return List of created time entries
         */
        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/bulk")
        public ResponseEntity<BaseResponse<List<TimeEntryDTO>>> createBulkTimeEntries(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody List<CreateTimeEntryDTO> bulkTimeEntriesDTO) {
                try {
                        LoggerUtil.logDebug("Creating {} bulk time entries by user: {}", bulkTimeEntriesDTO.size(),
                                        userDetails.getUsername());
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(BaseResponse.success("Bulk time entries created successfully",
                                                        timeEntryService.createBulkTimeEntries(bulkTimeEntriesDTO,
                                                                        userDetails.getUsername()),
                                                        HttpStatus.CREATED.value()));
                } catch (Exception e) {
                        LoggerUtil.logError("Error creating bulk time entries: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error(
                                                        "Failed to create bulk time entries: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PutMapping("/{timeEntryId}")
        public ResponseEntity<BaseResponse<TimeEntryDTO>> updateTimeEntry(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long timeEntryId,
                        @Valid @RequestBody CreateTimeEntryDTO updateDTO) {
                try {
                        LoggerUtil.logDebug("Updating time entry ID: {} by user: {}", timeEntryId,
                                        userDetails.getUsername());
                        return ResponseEntity.ok(BaseResponse.success("Time entry updated successfully",
                                        timeEntryService.updateTimeEntry(timeEntryId, updateDTO,
                                                        userDetails.getUsername())));
                } catch (Exception e) {
                        LoggerUtil.logError("Error updating time entry: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to update time entry: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @DeleteMapping("/{timeEntryId}")
        public ResponseEntity<BaseResponse<String>> deleteTimeEntry(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long timeEntryId) {
                try {
                        LoggerUtil.logDebug("Deleting time entry ID: {} by user: {}", timeEntryId,
                                        userDetails.getUsername());
                        timeEntryService.deleteTimeEntry(timeEntryId, userDetails.getUsername());
                        return ResponseEntity.ok(BaseResponse.success("Time entry deleted successfully"));
                } catch (Exception e) {
                        LoggerUtil.logError("Error deleting time entry: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to delete time entry: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/{timeEntryId}/approve")
        public ResponseEntity<BaseResponse<TimeEntryDTO>> approveTimeEntry(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long timeEntryId,
                        @Valid @RequestBody TimeEntryApprovalDTO approvalDTO) {
                try {
                        LoggerUtil.logDebug("Approving time entry ID: {} by user: {}", timeEntryId,
                                        userDetails.getUsername());
                        return ResponseEntity.ok(BaseResponse.success("Time entry approved successfully",
                                        timeEntryService.approveTimeEntry(approvalDTO, userDetails.getUsername())));
                } catch (Exception e) {
                        LoggerUtil.logError("Error approving time entry: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to approve time entry: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/approveAll")
        public ResponseEntity<BaseResponse<List<TimeEntryDTO>>> approveAllTimeEntry(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody TimeEntryApprovalIdsListDTO approvalIdsListDTO) {
                try {
                        LoggerUtil.logDebug("Approving multiple time entries by user: {}", userDetails.getUsername());
                        return ResponseEntity.ok(BaseResponse.success("Time entries approved successfully",
                                        timeEntryService.approveAllTimeEntry(approvalIdsListDTO,
                                                        userDetails.getUsername())));
                } catch (Exception e) {
                        LoggerUtil.logError("Error approving multiple time entries: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to approve time entries: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/{timeEntryId}/reject")
        public ResponseEntity<BaseResponse<TimeEntryDTO>> rejectTimeEntry(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long timeEntryId,
                        @Valid @RequestBody TimeEntryRejectionDTO rejectionDTO) {
                try {
                        LoggerUtil.logDebug("Rejecting time entry ID: {} by user: {}", timeEntryId,
                                        userDetails.getUsername());
                        return ResponseEntity.ok(BaseResponse.success("Time entry rejected successfully",
                                        timeEntryService.rejectTimeEntry(rejectionDTO, userDetails.getUsername())));
                } catch (Exception e) {
                        LoggerUtil.logError("Error rejecting time entry: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to reject time entry: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/rejectAll")
        public ResponseEntity<BaseResponse<List<TimeEntryDTO>>> rejectAllTimeEntry(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody TimeEntryRejectionIdsListDTO rejectionIdsListDTO) {
                try {
                        LoggerUtil.logDebug("Rejecting multiple time entries by user: {}", userDetails.getUsername());
                        return ResponseEntity.ok(BaseResponse.success("Time entries rejected successfully",
                                        timeEntryService.rejectAllTimeEntry(rejectionIdsListDTO,
                                                        userDetails.getUsername())));
                } catch (Exception e) {
                        LoggerUtil.logError("Error rejecting multiple time entries: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to reject time entries: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping
        public ResponseEntity<BaseResponse<List<TimeEntryDTO>>> getMyTimeEntries(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                try {
                        LoggerUtil.logDebug("Fetching time entries for user: {} from {} to {}",
                                        userDetails.getUsername(),
                                        startDate, endDate);
                        return ResponseEntity.ok(BaseResponse.success("Time entries retrieved successfully",
                                        timeEntryService.getTimeEntriesByUser(userDetails.getUsername(), startDate,
                                                        endDate)));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching time entries: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to retrieve time entries: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/pending")
        public ResponseEntity<BaseResponse<List<TimeEntryDTO>>> getPendingTimeEntries(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                try {
                        LoggerUtil.logDebug("Fetching pending time entries for lead: {} from {} to {}",
                                        userDetails.getUsername(),
                                        startDate, endDate);
                        return ResponseEntity.ok(BaseResponse.success("Pending time entries retrieved successfully",
                                        timeEntryService.getPendingTimeEntriesByLead(userDetails.getUsername(),
                                                        startDate, endDate)));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching pending time entries: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error(
                                                        "Failed to retrieve pending time entries: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/team")
        public ResponseEntity<BaseResponse<List<TimeEntryDTO>>> getTeamTimeEntries(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(required = false) TimeEntryStatus status,
                        @RequestParam(required = false, defaultValue = "false") boolean directOnly) {
                try {
                        LoggerUtil.logDebug(
                                        "Fetching team time entries for lead: {} from {} to {} with status: {} directOnly: {}",
                                        userDetails.getUsername(), startDate, endDate, status, directOnly);
                        return ResponseEntity.ok(BaseResponse.success("Team time entries retrieved successfully",
                                        timeEntryService
                                                        .getTeamTimeEntries(userDetails.getUsername(), startDate,
                                                                        endDate, status, directOnly)));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching team time entries: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error(
                                                        "Failed to retrieve team time entries: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/summary")
        public ResponseEntity<BaseResponse<List<TimeEntrySummaryDTO>>> getTimeEntrySummary(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(required = false) Long userId,
                        @RequestParam(required = false) Long projectId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                try {
                        LoggerUtil.logDebug("Fetching time entry summary by user: {} for userId: {}, projectId: {}",
                                        userDetails.getUsername(), userId, projectId);
                        return ResponseEntity.ok(BaseResponse.success("Time entry summary retrieved successfully",
                                        timeEntryService.getTimeEntrySummary(userId, projectId, startDate, endDate)));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching time entry summary: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error(
                                                        "Failed to retrieve time entry summary: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/remaining-time")
        public ResponseEntity<BaseResponse<Integer>> getRemainingTimeForDay(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String ldap) {
                try {
                        LoggerUtil.logDebug("Fetching remaining time for LDAP: {} on date: {} by user: {}", ldap, date,
                                        userDetails.getUsername());
                        return ResponseEntity.ok(BaseResponse.success("Remaining time retrieved successfully",
                                        timeEntryService.getRemainingTimeForDay(userDetails, ldap, date)));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching remaining time: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to retrieve remaining time: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/my-projects")
        public ResponseEntity<BaseResponse<List<UserProjectDTO>>> getMyProjects(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(required = false) String ldap) {
                try {
                        String targetLdap = (ldap != null && !ldap.isEmpty()) ? ldap : userDetails.getUsername();
                        LoggerUtil.logDebug("Fetching projects for user: {}", targetLdap);
                        return ResponseEntity.ok(BaseResponse.success("Projects retrieved successfully",
                                        userProjectService.getProjectsByUser(targetLdap)));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching user projects: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to retrieve projects: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/hierarchical-summary")
        public ResponseEntity<BaseResponse<List<TimeEntryHierarchicalSummaryDTO>>> getHierarchicalTimeSummary(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(required = false) Long userId,
                        @RequestParam(required = false) Long projectId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                try {
                        LoggerUtil.logDebug(
                                        "Fetching hierarchical time summary by user: {} for userId: {}, projectId: {}",
                                        userDetails.getUsername(), userId, projectId);
                        return ResponseEntity
                                        .ok(BaseResponse.success("Hierarchical time summary retrieved successfully",
                                                        timeEntryService.getHierarchicalTimeSummary(userId, projectId,
                                                                        startDate, endDate)));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching hierarchical time summary: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error(
                                                        "Failed to retrieve hierarchical time summary: "
                                                                        + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/ldaps")
        public ResponseEntity<BaseResponse<List<UserDTO>>> getAllLdaps(
                        @AuthenticationPrincipal UserDetails userDetails) {
                try {
                        LoggerUtil.logDebug("Fetching all LDAPs by user: {}", userDetails.getUsername());
                        return ResponseEntity
                                        .ok(BaseResponse.success("LDAPs retrieved successfully",
                                                        timeEntryService.getAllLdaps()));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching LDAPs: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to retrieve LDAPs: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/team-members")
        public ResponseEntity<BaseResponse<List<UserDTO>>> getTeamMembers(
                        @AuthenticationPrincipal UserDetails userDetails) {
                try {
                        LoggerUtil.logDebug("Fetching team members for user: {}", userDetails.getUsername());
                        return ResponseEntity.ok(BaseResponse.success("Team members retrieved successfully",
                                        timeEntryService.getTeamMembers(userDetails.getUsername())));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching team members: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to retrieve team members: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/{timeEntryId}")
        public ResponseEntity<BaseResponse<TimeEntryDTO>> getTimeEntryById(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long timeEntryId) {
                try {
                        LoggerUtil.logDebug("Fetching time entry ID: {} by user: {}", timeEntryId,
                                        userDetails.getUsername());
                        return ResponseEntity.ok(BaseResponse.success("Time entry retrieved successfully",
                                        timeEntryService.getTimeEntryById(timeEntryId, userDetails.getUsername())));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching time entry by ID: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to retrieve time entry: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        /**
         * Creates multiple time entries based on a source entry for different dates
         *
         * @param batchTimeEntryDTO DTO containing source entry and target dates
         * @return List of created time entries
         */
        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/batch")
        public ResponseEntity<BaseResponse<List<TimeEntryDTO>>> createBatchTimeEntries(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody BatchTimeEntryDTO batchTimeEntryDTO) {
                try {
                        LoggerUtil.logDebug("Creating batch time entries by user: {}", userDetails.getUsername());
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(BaseResponse.success("Batch time entries created successfully",
                                                        timeEntryService.createBatchTimeEntries(batchTimeEntryDTO,
                                                                        userDetails.getUsername()),
                                                        HttpStatus.CREATED.value()));
                } catch (Exception e) {
                        LoggerUtil.logError("Error creating batch time entries: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error(
                                                        "Failed to create batch time entries: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        /**
         * Creates multiple holiday time entries for Google holidays
         *
         * @param holidayBatchRequest DTO containing list of holiday entries to create
         * @return List of created holiday time entries
         */
        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/batch-holidays")
        public ResponseEntity<BaseResponse<List<TimeEntryDTO>>> createBatchHolidayEntries(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody HolidayBatchRequestDTO holidayBatchRequest) {
                try {
                        LoggerUtil.logDebug("Creating batch holiday entries by user: {}", userDetails.getUsername());
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(BaseResponse.success("Batch holiday entries created successfully",
                                                        timeEntryService.createBatchHolidayEntries(holidayBatchRequest,
                                                                        userDetails.getUsername()),
                                                        HttpStatus.CREATED.value()));
                } catch (Exception e) {
                        LoggerUtil.logError("Error creating batch holiday entries: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error(
                                                        "Failed to create batch holiday entries: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        /**
         * Get user's most recent project from last 15 days of time entries
         * Used for leave application time entry preview
         *
         * @param ldap User's LDAP
         * @return Recent project information
         */
        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/recent-project/{ldap}")
        public ResponseEntity<BaseResponse<RecentProjectDTO>> getRecentProject(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable String ldap) {
                try {
                        LoggerUtil.logDebug("Fetching recent project for LDAP: {} by user: {}", ldap,
                                        userDetails.getUsername());
                        RecentProjectDTO recentProject = timeEntryService.getRecentProject(ldap);
                        return ResponseEntity.ok(BaseResponse.success("Recent project retrieved successfully",
                                        recentProject));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching recent project: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to retrieve recent project: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }
}
