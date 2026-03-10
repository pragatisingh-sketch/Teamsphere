package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.Project;
import com.vbs.capsAllocation.model.TimeEntry;
import com.vbs.capsAllocation.model.TimeEntryStatus;
import com.vbs.capsAllocation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
        List<TimeEntry> findByUser(User user);

        List<TimeEntry> findByProject(Project project);

        List<TimeEntry> findByLead(User lead);

        List<TimeEntry> findByStatus(TimeEntryStatus status);

        List<TimeEntry> findByEntryDate(LocalDate entryDate);

        List<TimeEntry> findByEntryDateBetween(LocalDate startDate, LocalDate endDate);

        List<TimeEntry> findByUserAndEntryDate(User user, LocalDate entryDate);

        List<TimeEntry> findByUserAndEntryDateBetween(User user, LocalDate startDate, LocalDate endDate);

        List<TimeEntry> findByProjectAndEntryDateBetween(Project project, LocalDate startDate, LocalDate endDate);

        List<TimeEntry> findByUserAndProjectAndEntryDateBetween(User user, Project project, LocalDate startDate,
                        LocalDate endDate);

        @Query("SELECT t FROM TimeEntry t WHERE t.user IN ?1 AND t.entryDate BETWEEN ?2 AND ?3 AND (?4 IS NULL OR t.status = ?4)")
        List<TimeEntry> findByUsersAndEntryDateBetweenAndStatus(List<User> users, LocalDate startDate,
                        LocalDate endDate, TimeEntryStatus status);

        @Query("SELECT t FROM TimeEntry t WHERE t.user IN ?1 AND t.entryDate BETWEEN ?2 AND ?3 AND t.status = ?4")
        List<TimeEntry> findByUsersAndEntryDateBetweenAndStatusStrict(List<User> users, LocalDate startDate,
                        LocalDate endDate, TimeEntryStatus status);

        @Query("SELECT SUM(t.timeInMins) FROM TimeEntry t WHERE t.user = ?1 AND t.entryDate = ?2")
        Integer getTotalTimeByUserAndDate(User user, LocalDate date);

        @Query("SELECT SUM(t.timeInMins) FROM TimeEntry t WHERE t.user = ?1 AND t.entryDate = ?2 AND (t.isOvertime = false OR t.isOvertime IS NULL)")
        Integer getTotalNormalTimeByUserAndDate(User user, LocalDate date);

        @Query("SELECT SUM(t.timeInMins) FROM TimeEntry t WHERE t.user = ?1 AND t.project = ?2 AND t.entryDate BETWEEN ?3 AND ?4")
        Integer getTotalTimeByUserAndProjectAndDateRange(User user, Project project, LocalDate startDate,
                        LocalDate endDate);

        List<TimeEntry> findByUser_IdAndProject_IdAndEntryDateBetween(Long userId, Long projectId, LocalDate startDate,
                        LocalDate endDate);

        List<TimeEntry> findByUser_IdAndEntryDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

        List<TimeEntry> findByProject_IdAndEntryDateBetween(Long projectId, LocalDate startDate, LocalDate endDate);

        @Query("SELECT t FROM TimeEntry t WHERE t.entryDate BETWEEN :startDate AND :endDate AND t.status = :status")
        List<TimeEntry> findAllByStatusStrictAndEntryDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("status") TimeEntryStatus status);

        @Query("SELECT t FROM TimeEntry t WHERE t.entryDate BETWEEN :startDate AND :endDate AND (:status IS NULL OR t.status = :status)")
        List<TimeEntry> findAllByStatusOptionalAndEntryDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("status") TimeEntryStatus status);

        @Query("SELECT t FROM TimeEntry t WHERE t.isDefaulter = true AND t.entryDate BETWEEN :startDate AND :endDate")
        List<TimeEntry> findDefaultersByDateRange(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Lead-based filtering
        @Query("SELECT COUNT(t) FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.isDefaulter = TRUE " +
                        "AND t.user.username IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.lead = :leadUsername AND e.status = 'Active'" +
                        ")")
        Long countDefaultersForLead(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("leadUsername") String leadUsername);

        // Manager filtering
        @Query("SELECT COUNT(t) FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.isDefaulter = TRUE " +
                        "AND t.user.username IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.programManager = :managerUsername AND e.status = 'Active'"
                        +
                        ")")
        Long countDefaultersForManager(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("managerUsername") String managerUsername);

        // Admin ops manager — full visibility
        @Query("SELECT COUNT(t) FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.isDefaulter = TRUE")
        Long countDefaultersForAdmin(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Utilization Report Queries

        /**
         * Get time entries for utilization calculation for lead's team members
         */
        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.user.username IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.lead = :leadUsername AND e.status = 'Active'" +
                        ")")
        List<TimeEntry> findUtilizationDataForLead(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("leadUsername") String leadUsername);

        /**
         * Get time entries for utilization calculation for manager's team members
         */
        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.user.username IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.programManager = :managerUsername AND e.status = 'Active'"
                        +
                        ")")
        List<TimeEntry> findUtilizationDataForManager(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("managerUsername") String managerUsername);

        /**
         * Get time entries for utilization calculation for admin access
         */
        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate")
        List<TimeEntry> findUtilizationDataForAdmin(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Defaulter List Queries

        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.isDefaulter = TRUE " +
                        "AND t.user.username IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.lead = :leadUsername AND e.status = 'Active'" +
                        ")")
        List<TimeEntry> findDefaultersForLead(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("leadUsername") String leadUsername);

        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.isDefaulter = TRUE " +
                        "AND t.user.username IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.programManager = :managerUsername AND e.status = 'Active'"
                        +
                        ")")
        List<TimeEntry> findDefaultersForManager(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("managerUsername") String managerUsername);

        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.isDefaulter = TRUE")
        List<TimeEntry> findDefaultersForAdmin(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Find defaulter issues by LDAP and date range for issue details modal
        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.ldap = :ldap " +
                        "AND t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.isDefaulter = TRUE " +
                        "ORDER BY t.entryDate DESC")
        List<TimeEntry> findDefaulterIssuesByLdap(@Param("ldap") String ldap,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // List-based queries for Delegation support
        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.status IN ('PENDING', 'SUBMITTED') " +
                        "AND t.user.username IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.lead IN :leadUsernames AND e.status = 'Active'" +
                        ")")
        List<TimeEntry> findPendingTimeEntriesByLeadIn(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("leadUsernames") List<String> leadUsernames);

        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND (:status IS NULL OR t.status = :status) " +
                        "AND t.user.username IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.lead IN :leadUsernames AND e.status = 'Active'" +
                        ")")
        List<TimeEntry> findTeamTimeEntriesByLeadIn(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("status") TimeEntryStatus status,
                        @Param("leadUsernames") List<String> leadUsernames);

        @Query("SELECT t FROM TimeEntry t " +
                        "WHERE t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND (:status IS NULL OR t.status = :status) " +
                        "AND t.user.username IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.programManager IN :managerUsernames AND e.status = 'Active'"
                        +
                        ")")
        List<TimeEntry> findTeamTimeEntriesByManagerIn(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("status") TimeEntryStatus status,
                        @Param("managerUsernames") List<String> managerUsernames);

        // Auto-generated time entry support

        /**
         * Find most recent time entry for a user (for project selection)
         */
        @Query("SELECT t FROM TimeEntry t WHERE t.ldap = :ldap AND t.entryDate >= :sinceDate ORDER BY t.entryDate DESC, t.createdAt DESC")
        List<TimeEntry> findRecentByLdap(@Param("ldap") String ldap, @Param("sinceDate") LocalDate sinceDate);

        /**
         * Find all time entries generated from a specific leave request
         * Used for deletion when leave is rejected/revoked
         */
        List<TimeEntry> findBySourceLeaveId(Long sourceLeaveId);

        /**
         * Delete all time entries associated with a leave request
         */
        void deleteBySourceLeaveId(Long sourceLeaveId);

        /**
         * Find auto-generated time entries for linked approval workflow
         * These are locked entries with SUBMITTED status within a date range
         */
        @Query("SELECT t FROM TimeEntry t WHERE t.user.username = :ldap " +
                        "AND t.entryDate BETWEEN :startDate AND :endDate " +
                        "AND t.isLocked = true " +
                        "AND t.status = 'SUBMITTED'")
        List<TimeEntry> findAutoGeneratedEntriesForLeaveApproval(
                        @Param("ldap") String ldap,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Bulk update time entry statuses and lock state for linked approval
         */
        @Modifying
        @Query("UPDATE TimeEntry t SET t.status = :status, t.isLocked = :isLocked, " +
                        "t.lockedBy = :lockedBy, t.lockedAt = :lockedAt, t.updatedAt = :updatedAt " +
                        "WHERE t.id IN :ids")
        int bulkUpdateStatusAndLock(
                        @Param("ids") List<Long> ids,
                        @Param("status") TimeEntryStatus status,
                        @Param("isLocked") Boolean isLocked,
                        @Param("lockedBy") String lockedBy,
                        @Param("lockedAt") LocalDateTime lockedAt,
                        @Param("updatedAt") LocalDateTime updatedAt);
}
