package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.LeaveUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveUsageLogRepository extends JpaRepository<LeaveUsageLog, Long> {

        // Sum total WFH used by an employee
        @Query("SELECT COALESCE(SUM(l.daysTaken), 0) FROM LeaveUsageLog l WHERE l.employee.ldap = :ldap AND l.leaveType = :leaveType")
        double sumDaysTakenByLdapAndLeaveType(@Param("ldap") String ldap, @Param("leaveType") String leaveType);

        // Sum total WFH used by an employee in a particular quarter and year
        @Query("SELECT COALESCE(SUM(l.daysTaken), 0) FROM LeaveUsageLog l WHERE l.employee.ldap = :ldap AND l.leaveType = :leaveType AND l.quarter = :quarter AND l.year = :year")
        double sumDaysTakenByLdapAndLeaveTypeAndQuarter(@Param("ldap") String ldap,
                        @Param("leaveType") String leaveType,
                        @Param("quarter") String quarter,
                        @Param("year") String year);

        List<LeaveUsageLog> findByEmployeeAndLeaveDateBetweenAndYear(Employee employee, LocalDate fromDate,
                        LocalDate toDate, Integer year);

        List<LeaveUsageLog> findByEmployeeAndLeaveTypeAndLeaveDateBetweenAndYear(
                        Employee employee,
                        String leaveType,
                        LocalDate fromDate,
                        LocalDate toDate,
                        Integer year);

        @Query("SELECT SUM(l.daysTaken) FROM LeaveUsageLog l " +
                        "WHERE l.employee.ldap = :ldap " +
                        "AND l.leaveType = :leaveType " +
                        "AND l.leaveDate >= :fromDate")
        Double sumDaysTakenByLdapAndLeaveTypeSinceDate(@Param("ldap") String ldap,
                        @Param("leaveType") String leaveType,
                        @Param("fromDate") LocalDate fromDate);

        // Count total leaves (excluding WFH) within a date range for Admin/OPS Manager
        @Query("SELECT COALESCE(SUM(l.daysTaken), 0) FROM LeaveUsageLog l " +
                        "WHERE l.leaveType != 'WFH' " +
                        "AND l.leaveDate BETWEEN :startDate AND :endDate")
        Double countLeavesByDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Count total WFH within a date range for Admin/OPS Manager
        @Query("SELECT COALESCE(SUM(l.daysTaken), 0) FROM LeaveUsageLog l " +
                        "WHERE l.leaveType = 'WFH' " +
                        "AND l.leaveDate BETWEEN :startDate AND :endDate")
        Double countWfhByDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Count total leaves (excluding WFH) within a date range for Manager
        @Query("SELECT COALESCE(SUM(l.daysTaken), 0) FROM LeaveUsageLog l " +
                        "WHERE l.leaveType != 'WFH' " +
                        "AND l.leaveDate BETWEEN :startDate AND :endDate " +
                        "AND l.employee.programManager = :managerUsername")
        Double countLeavesByDateRangeForManager(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("managerUsername") String managerUsername);

        // Count total WFH within a date range for Manager
        @Query("SELECT COALESCE(SUM(l.daysTaken), 0) FROM LeaveUsageLog l " +
                        "WHERE l.leaveType = 'WFH' " +
                        "AND l.leaveDate BETWEEN :startDate AND :endDate " +
                        "AND l.employee.programManager = :managerUsername")
        Double countWfhByDateRangeForManager(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("managerUsername") String managerUsername);

        // Count total leaves (excluding WFH) within a date range for Lead
        @Query("SELECT COALESCE(SUM(l.daysTaken), 0) FROM LeaveUsageLog l " +
                        "WHERE l.leaveType != 'WFH' " +
                        "AND l.leaveDate BETWEEN :startDate AND :endDate " +
                        "AND l.employee.lead = :leadUsername")
        Double countLeavesByDateRangeForLead(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("leadUsername") String leadUsername);

        // Count total WFH within a date range for Lead
        @Query("SELECT COALESCE(SUM(l.daysTaken), 0) FROM LeaveUsageLog l " +
                        "WHERE l.leaveType = 'WFH' " +
                        "AND l.leaveDate BETWEEN :startDate AND :endDate " +
                        "AND l.employee.lead = :leadUsername")
        Double countWfhByDateRangeForLead(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("leadUsername") String leadUsername);

        List<LeaveUsageLog> findByEmployeeAndLeaveTypeAndLeaveDateAndYearAndDaysTaken(
                        Employee employee,
                        String leaveType,
                        LocalDate leaveDate,
                        int year,
                        double daysTaken);

        // Find ALL logs for a specific date and leave type (regardless of daysTaken)
        List<LeaveUsageLog> findByEmployeeAndLeaveTypeAndLeaveDateAndYear(
                        Employee employee,
                        String leaveType,
                        LocalDate leaveDate,
                        int year);

        /**
         * Find all leaves within a date range (for all employees)
         */
        @Query("SELECT l FROM LeaveUsageLog l WHERE l.leaveDate BETWEEN :startDate AND :endDate ORDER BY l.employee.id, l.leaveDate")
        List<LeaveUsageLog> findAllLeavesBetweenDates(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Find all leaves within a date range for a specific manager's employees
         */
        @Query("SELECT l FROM LeaveUsageLog l WHERE l.leaveDate BETWEEN :startDate AND :endDate " +
                        "AND l.employee.programManager = :managerUsername ORDER BY l.employee.id, l.leaveDate")
        List<LeaveUsageLog> findLeavesByDateRangeForManager(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("managerUsername") String managerUsername);

        /**
         * Find all leaves within a date range for a specific lead's employees
         */
        @Query("SELECT l FROM LeaveUsageLog l WHERE l.leaveDate BETWEEN :startDate AND :endDate " +
                        "AND l.employee.lead = :leadUsername ORDER BY l.employee.id, l.leaveDate")
        List<LeaveUsageLog> findLeavesByDateRangeForLead(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("leadUsername") String leadUsername);
}
