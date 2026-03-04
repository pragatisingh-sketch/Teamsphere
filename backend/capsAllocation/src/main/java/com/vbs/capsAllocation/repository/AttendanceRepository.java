package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.dto.AttendanceResponseDto;
import com.vbs.capsAllocation.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Get distinct employee LDAPs who have marked attendance on a specific date
    @Query("SELECT DISTINCT e.ldap FROM Attendance a JOIN a.employee e WHERE a.entryDate = :entryDate")
    List<String> findEmployeeLdapsByEntryDate(@Param("entryDate") LocalDate entryDate);

    @Query("SELECT FUNCTION('TO_CHAR', a.entryTimestamp, 'HH24:MI') FROM Attendance a WHERE a.id = :attendanceId")
    String findCheckInTimeById(@Param("attendanceId") String attendanceId);

    Optional<Attendance> findTopByEmployeeLdapAndEntryDateOrderByEntryTimestampDesc(
            String ldap,
            LocalDate entryDate);

    Optional<Attendance> findTopByEmployee_LdapOrderByEntryTimestampDesc(String ldap);

    @Query("SELECT a FROM Attendance a JOIN a.employee e WHERE e.ldap = :ldap AND a.entryDate = :entryDate")
    List<Attendance> findByEmployeeLdapAndEntryDate(
            @Param("ldap") String ldap,
            @Param("entryDate") LocalDate entryDate);

    @Query("SELECT a FROM Attendance a JOIN a.employee e WHERE e.ldap = :ldap AND a.exitDate = :exitDate")
    List<Attendance> findByEmployeeLdapAndExitDate(@Param("ldap") String ldap,
            @Param("exitDate") LocalDate today);

    @Query("""
                SELECT new com.vbs.capsAllocation.dto.AttendanceResponseDto(
                    a.id,
                    e.ldap,
                    e.team,
                    CONCAT(e.firstName, ' ', e.lastName),
                    a.entryDate,
                    a.entryTimestamp,
                    a.lateLoginReason,
                    a.isOutsideOffice,
                    a.isDefaulter,
                    a.comment,
                    a.exitDate,
                    a.exitTimestamp,
                    a.isCheckOutOutsideOffice,
                    a.lateOrEarlyLogoutReason,
                    a.lateOrEarlyCheckout
                )
                FROM Attendance a
                JOIN a.employee e
                WHERE e.ldap = :ldap
                  AND a.entryDate BETWEEN :startDate AND :endDate
            """)
    List<AttendanceResponseDto> findByEmployeeLdapAndEntryDateBetween(
            @Param("ldap") String ldap,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
                SELECT new com.vbs.capsAllocation.dto.AttendanceResponseDto(
                    a.id,
                    e.ldap,
                    e.team,
                    CONCAT(e.firstName, ' ', e.lastName),
                    a.entryDate,
                    a.entryTimestamp,
                    a.lateLoginReason,
                    a.isOutsideOffice,
                    a.isDefaulter,
                    a.comment,
                    a.exitDate,
                    a.exitTimestamp,
                    a.isCheckOutOutsideOffice,
                    a.lateOrEarlyLogoutReason,
                    a.lateOrEarlyCheckout
                )
                FROM Attendance a
                JOIN a.employee e
                WHERE e.ldap IN :ldaps
                  AND a.entryDate BETWEEN :startDate AND :endDate
            """)
    List<AttendanceResponseDto> findByLdapsAndDateRange(
            @Param("ldaps") List<String> ldaps,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Attendance> findByEmployeeLdapOrderByEntryDateDesc(String ldap);

    Optional<Attendance> findTopByEmployee_LdapOrderByEntryDateDesc(String ldap);

    List<Attendance> findByEmployee_LdapAndExitDateIsNull(String ldap);

    // List<Attendance> findByEmployee_LdapAndExitDateIsNull(String ldap);
    Optional<Attendance> findTopByEmployeeLdap(String ldap);

    // Attendance defaulter count methods similar to TimeEntryRepository
    @Query("SELECT COUNT(a) FROM Attendance a " +
            "WHERE a.entryDate BETWEEN :startDate AND :endDate " +
            "AND a.isDefaulter = TRUE " +
            "AND a.employee.ldap IN (" +
            "SELECT e.ldap FROM Employee e WHERE e.lead = :leadUsername AND e.status = 'Active'" +
            ")")
    Long countDefaultersForLead(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("leadUsername") String leadUsername);

    // optimised query

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE Attendance a
                SET a.exitDate = :exitDate,
                    a.exitTimestamp = :exitTimestamp,
                    a.isCheckOutOutsideOffice = :outside,
                    a.lateOrEarlyLogoutReason = :reason,
                    a.lateOrEarlyCheckout = :status
                WHERE a.id = :id
            """)
    int updateCheckout(
            @Param("id") Long id,
            @Param("exitDate") LocalDate exitDate,
            @Param("exitTimestamp") LocalDateTime exitTimestamp,
            @Param("outside") boolean outside,
            @Param("reason") String reason,
            @Param("status") String status);

    @Query("SELECT COUNT(a) FROM Attendance a " +
            "WHERE a.entryDate BETWEEN :startDate AND :endDate " +
            "AND a.isDefaulter = TRUE " +
            "AND a.employee.ldap IN (" +
            "SELECT e.ldap FROM Employee e WHERE e.programManager = :managerUsername AND e.status = 'Active'"
            +
            ")")
    Long countDefaultersForManager(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("managerUsername") String managerUsername);

    @Query("SELECT COUNT(a) FROM Attendance a " +
            "WHERE a.entryDate BETWEEN :startDate AND :endDate " +
            "AND a.isDefaulter = TRUE")
    Long countDefaultersForAdmin(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Defaulter List Queries

    @Query("SELECT a FROM Attendance a " +
            "WHERE a.entryDate BETWEEN :startDate AND :endDate " +
            "AND a.isDefaulter = TRUE " +
            "AND a.employee.ldap IN (" +
            "SELECT e.ldap FROM Employee e WHERE e.lead = :leadUsername AND e.status = 'Active'" +
            ")")
    List<Attendance> countDefaultersForLeadList(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("leadUsername") String leadUsername);

    @Query("SELECT a FROM Attendance a " +
            "WHERE a.entryDate BETWEEN :startDate AND :endDate " +
            "AND a.isDefaulter = TRUE " +
            "AND a.employee.ldap IN (" +
            "SELECT e.ldap FROM Employee e WHERE e.programManager = :managerUsername AND e.status = 'Active'"
            +
            ")")
    List<Attendance> countDefaultersForManagerList(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("managerUsername") String managerUsername);

    @Query("SELECT a FROM Attendance a " +
            "WHERE a.entryDate BETWEEN :startDate AND :endDate " +
            "AND a.isDefaulter = TRUE")
    List<Attendance> countDefaultersForAdminList(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Find defaulter issues by LDAP and date range for issue details modal
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.employee.ldap = :ldap " +
            "AND a.entryDate BETWEEN :startDate AND :endDate " +
            "AND a.isDefaulter = TRUE " +
            "ORDER BY a.entryDate DESC")
    List<Attendance> findDefaulterIssuesByLdap(@Param("ldap") String ldap,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
