package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.dto.VunnoRequestDto;
import com.vbs.capsAllocation.model.VunnoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VunnoResponseRepository extends JpaRepository<VunnoResponse, Long> {

        // Get distinct employee LDAPs who have approved leave on a specific date
        @Query("""
                        SELECT DISTINCT e.ldap
                        FROM VunnoResponse v
                        JOIN v.employee e
                        WHERE v.status = 'APPROVED'
                          AND LOWER(v.applicationType) LIKE '%leave%'
                          AND :date BETWEEN v.fromDate AND v.toDate
                        """)
        List<String> findEmployeeLdapsOnApprovedLeaveForDate(@Param("date") LocalDate date);

        @Query("SELECT v FROM VunnoResponse v WHERE v.employee.ldap = :ldap")
        List<VunnoResponse> findByEmployeeLdap(@Param("ldap") String ldap);

        @Query("SELECT v FROM VunnoResponse v WHERE v.employee.ldap IN :ldaps AND v.status = 'APPROVED'")
        List<VunnoResponse> findApprovedRequestsByTeamLdaps(@Param("ldaps") List<String> ldaps);

        @Query("""
                            SELECT new com.vbs.capsAllocation.dto.VunnoRequestDto(
                                vr.id,
                                e.ldap,
                                vr.approver,
                                vr.applicationType,
                                vr.leaveType,
                                vr.duration,
                                vr.fromDate,
                                vr.toDate,
                                vr.backup,
                                vr.orgScreenshot,
                                vr.timesheetScreenshot,
                                vr.status,
                                CONCAT(e.firstName, ' ', e.lastName),
                                vr.reason,
                                vr.documentPath,
                                vr.timestamp,
                                vr.leaveCategory,
                                (
                                  SELECT va.actionType
                                  FROM VunnoAuditLog va
                                  WHERE va.vunnoResponseId = vr.id
                                  AND va.changedAt = (
                                        SELECT MAX(va2.changedAt)
                                        FROM VunnoAuditLog va2
                                        WHERE va2.vunnoResponseId = vr.id
                                  )
                                ),
                                (
                                  SELECT va.changedAt
                                  FROM VunnoAuditLog va
                                  WHERE va.vunnoResponseId = vr.id
                                  AND va.changedAt = (
                                        SELECT MAX(va2.changedAt)
                                        FROM VunnoAuditLog va2
                                        WHERE va2.vunnoResponseId = vr.id
                                  )
                                ),
                                (
                                  SELECT va.changedBy
                                  FROM VunnoAuditLog va
                                  WHERE va.vunnoResponseId = vr.id
                                  AND va.changedAt = (
                                        SELECT MAX(va2.changedAt)
                                        FROM VunnoAuditLog va2
                                        WHERE va2.vunnoResponseId = vr.id
                                  )
                                ),
                                vr.dayConfigurations
                            )
                            FROM VunnoResponse vr
                            JOIN vr.employee e
                            WHERE e.ldap IN :employeeLdaps
                              AND vr.status IN :statuses
                              AND vr.fromDate <= :endDate
                              AND vr.toDate >= :startDate
                            ORDER BY vr.timestamp DESC
                        """)
        List<VunnoRequestDto> findProcessedDtos(
                        @Param("employeeLdaps") List<String> employeeLdaps,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("statuses") List<String> statuses);

        // Pending requests
        @Query("""
                            SELECT new com.vbs.capsAllocation.dto.VunnoRequestDto(
                                vr.id,
                                e.ldap,
                                vr.approver,
                                vr.applicationType,
                                vr.leaveType,
                                vr.duration,
                                vr.fromDate,
                                vr.toDate,
                                vr.backup,
                                vr.orgScreenshot,
                                vr.timesheetScreenshot,
                                vr.status,
                                CONCAT(e.firstName, ' ', e.lastName),
                                vr.reason,
                                vr.documentPath,
                                vr.timestamp,
                                vr.leaveCategory,
                                vr.dayConfigurations
                            )
                            FROM VunnoResponse vr
                            JOIN vr.employee e
                            WHERE e.ldap IN :employeeLdaps
                              AND vr.status = :status
                              AND vr.fromDate <= :endDate
                              AND vr.toDate >= :startDate
                            ORDER BY vr.timestamp DESC
                        """)
        List<VunnoRequestDto> findPendingDtos(
                        @Param("employeeLdaps") List<String> employeeLdaps,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("status") String status);

        @Query("""
                        SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
                          FROM VunnoResponse v
                         WHERE v.employee.id = :employeeId
                           AND LOWER(v.applicationType) = 'leave'
                           AND v.status IN :statuses
                           AND v.fromDate <= :toDate
                           AND v.toDate   >= :fromDate
                           AND (:currentId IS NULL OR v.id <> :currentId)
                        """)
        boolean existsOverlappingLeave(@Param("employeeId") Long employeeId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("statuses") List<String> statuses,
                        @Param("currentId") Long currentId);

        // Defaulter logic specific queries (UNPLANNED leaves)

        // Count (Lead)
        @Query("SELECT COUNT(v) FROM VunnoResponse v " +
                        "WHERE v.fromDate <= :endDate AND v.toDate >= :startDate " +
                        "AND v.leaveCategory = 'UNPLANNED' " +
                        "AND v.status = 'APPROVED' " +
                        "AND v.employee.ldap IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.lead = :leadUsername AND e.status = 'Active'" +
                        ")")
        Long countUnplannedLeavesForLead(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("leadUsername") String leadUsername);

        // Count (Manager)
        @Query("SELECT COUNT(v) FROM VunnoResponse v " +
                        "WHERE v.fromDate <= :endDate AND v.toDate >= :startDate " +
                        "AND v.leaveCategory = 'UNPLANNED' " +
                        "AND v.status = 'APPROVED' " +
                        "AND v.employee.ldap IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.programManager = :managerUsername AND e.status = 'Active'"
                        +
                        ")")
        Long countUnplannedLeavesForManager(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("managerUsername") String managerUsername);

        // Count (Admin)
        @Query("SELECT COUNT(v) FROM VunnoResponse v " +
                        "WHERE v.fromDate <= :endDate AND v.toDate >= :startDate " +
                        "AND v.leaveCategory = 'UNPLANNED' " +
                        "AND v.status = 'APPROVED'")
        Long countUnplannedLeavesForAdmin(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // List (Lead)
        @Query("SELECT v FROM VunnoResponse v " +
                        "WHERE v.fromDate <= :endDate AND v.toDate >= :startDate " +
                        "AND v.leaveCategory = 'UNPLANNED' " +
                        "AND v.status = 'APPROVED' " +
                        "AND v.employee.ldap IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.lead = :leadUsername AND e.status = 'Active'" +
                        ")")
        List<VunnoResponse> findUnplannedLeavesForLead(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("leadUsername") String leadUsername);

        // List (Manager)
        @Query("SELECT v FROM VunnoResponse v " +
                        "WHERE v.fromDate <= :endDate AND v.toDate >= :startDate " +
                        "AND v.leaveCategory = 'UNPLANNED' " +
                        "AND v.status = 'APPROVED' " +
                        "AND v.employee.ldap IN (" +
                        "SELECT e.ldap FROM Employee e WHERE e.programManager = :managerUsername AND e.status = 'Active'"
                        +
                        ")")
        List<VunnoResponse> findUnplannedLeavesForManager(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("managerUsername") String managerUsername);

        // List (Admin)
        @Query("SELECT v FROM VunnoResponse v " +
                        "WHERE v.fromDate <= :endDate AND v.toDate >= :startDate " +
                        "AND v.leaveCategory = 'UNPLANNED' " +
                        "AND v.status = 'APPROVED'")
        List<VunnoResponse> findUnplannedLeavesForAdmin(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Find unplanned leave issues by LDAP and date range for issue details modal
        @Query("SELECT v FROM VunnoResponse v " +
                        "WHERE v.employee.ldap = :ldap " +
                        "AND v.fromDate <= :endDate AND v.toDate >= :startDate " +
                        "AND v.leaveCategory = 'UNPLANNED' " +
                        "AND v.status = 'APPROVED' " +
                        "ORDER BY v.fromDate DESC")
        List<VunnoResponse> findUnplannedLeaveIssuesByLdap(@Param("ldap") String ldap,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT v FROM VunnoResponse v " +
                        "WHERE v.employee.ldap = :ldap " +
                        "AND v.status = 'APPROVED' " +
                        "AND v.applicationType = 'Leave' " +
                        "AND v.fromDate <= :endDate AND v.toDate >= :startDate")
        List<VunnoResponse> findApprovedLeavesByLdap(@Param("ldap") String ldap,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}