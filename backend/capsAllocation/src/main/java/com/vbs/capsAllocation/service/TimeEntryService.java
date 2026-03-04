package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.*;
import com.vbs.capsAllocation.model.TimeEntryStatus;
import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing time entries in the Time Sheet System
 *
 * @author Piyush Mishra
 * @version 1.0
 */
public interface TimeEntryService {

        /**
         * Create a new time entry
         */
        TimeEntryDTO createTimeEntry(CreateTimeEntryDTO createTimeEntryDTO, String username);

        /**
         * Create multiple time entries for different projects/processes on the same day
         */
        List<TimeEntryDTO> createBulkTimeEntries(List<CreateTimeEntryDTO> bulkTimeEntriesDTO, String username);

        /**
         * Update an existing time entry
         */
        TimeEntryDTO updateTimeEntry(Long timeEntryId, CreateTimeEntryDTO updateDTO, String ldap);

        /**
         * Delete a time entry
         */
        void deleteTimeEntry(Long timeEntryId, String username);

        /**
         * Approve a time entry
         */
        TimeEntryDTO approveTimeEntry(TimeEntryApprovalDTO approvalDTO, String ldap);

        /**
         * Reject a time entry
         */
        TimeEntryDTO rejectTimeEntry(TimeEntryRejectionDTO rejectionDTO, String ldap);

        /**
         * Approve multiple time entries
         */
        List<TimeEntryDTO> approveAllTimeEntry(@Valid TimeEntryApprovalIdsListDTO approvalIdsListDTO, String ldap);

        /**
         * Reject multiple time entries
         */
        List<TimeEntryDTO> rejectAllTimeEntry(@Valid TimeEntryRejectionIdsListDTO rejectionIdsListDTO, String ldap);

        /**
         * Get time entries by user
         */
        List<TimeEntryDTO> getTimeEntriesByUser(String ldap, LocalDate startDate, LocalDate endDate);

        /**
         * Get pending time entries by lead
         */
        List<TimeEntryDTO> getPendingTimeEntriesByLead(String ldap, LocalDate startDate, LocalDate endDate);

        /**
         * Get time entry summary
         */
        List<TimeEntrySummaryDTO> getTimeEntrySummary(Long userId, Long projectId, LocalDate startDate,
                        LocalDate endDate);

        /**
         * Get remaining time for a day
         */
        Integer getRemainingTimeForDay(UserDetails userDetails, String inputldap, LocalDate date);

        /**
         * Get team time entries
         */
        List<TimeEntryDTO> getTeamTimeEntries(String leadLdap, LocalDate startDate, LocalDate endDate,
                        TimeEntryStatus status, boolean directOnly);

        /**
         * Get hierarchical time summary
         */
        List<TimeEntryHierarchicalSummaryDTO> getHierarchicalTimeSummary(Long userId, Long projectId,
                        LocalDate startDate,
                        LocalDate endDate);

        /**
         * Get all LDAPs
         */
        List<UserDTO> getAllLdaps();

        /**
         * Get team members
         */
        List<UserDTO> getTeamMembers(String ldap);

        /**
         * Get time entry by ID
         */
        TimeEntryDTO getTimeEntryById(Long timeEntryId, String ldap);

        /**
         * Create batch time entries
         */
        List<TimeEntryDTO> createBatchTimeEntries(BatchTimeEntryDTO batchTimeEntryDTO, String username);

        /**
         * Create batch holiday time entries
         */
        List<TimeEntryDTO> createBatchHolidayEntries(HolidayBatchRequestDTO holidayBatchRequest, String username);

        /**
         * Get user's most recent project from last 15 days of time entries
         * Fallback to project_mapping table if no recent entries found
         */
        RecentProjectDTO getRecentProject(String ldap);
}
