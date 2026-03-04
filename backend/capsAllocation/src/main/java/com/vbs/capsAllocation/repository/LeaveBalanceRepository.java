package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.LeaveBalance;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

        // Fetch leave balances for a given employee's ldap
        @Query("SELECT lb FROM LeaveBalance lb WHERE lb.employee.ldap = :ldap")
        List<LeaveBalance> findByEmployeeLdap(@Param("ldap") String ldap);

        @Modifying
        @Transactional
        @Query(value = "INSERT INTO leave_balances (employee_id, month, leave_type, balance) " +
                        "VALUES (:employeeId, :month, :leaveType, :balance) " +
                        "ON CONFLICT (employee_id, month, leave_type) DO UPDATE SET balance = EXCLUDED.balance", nativeQuery = true)
        void upsertLeaveBalance(@Param("employeeId") Long employeeId,
                        @Param("month") String month,
                        @Param("leaveType") String leaveType,
                        @Param("balance") double balance);

        Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndMonthAndYear(
                        Employee employee,
                        String leaveType,
                        int month,
                        int year);

        Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndMonthAndYearAndSource(
                        Employee employee,
                        String leaveType,
                        int month,
                        int year,
                        String source);

        Optional<LeaveBalance> findTopByEmployeeLdapAndLeaveTypeOrderByUploadedAtDesc(String ldap, String leaveType);

        List<LeaveBalance> findByEmployeeAndMonthAndYear(Employee employee, int month, int year);

        @Query("SELECT COUNT(lb) > 0 FROM LeaveBalance lb WHERE lb.month = :month AND lb.year = :year")
        boolean existsByMonthAndYear(@Param("month") Integer month, @Param("year") Integer year);

        @Query(value = "SELECT uploaded_by FROM leave_balances " +
                        "WHERE EXTRACT(MONTH FROM uploaded_at) = EXTRACT(MONTH FROM CURRENT_DATE) " +
                        "AND EXTRACT(YEAR FROM uploaded_at) = EXTRACT(YEAR FROM CURRENT_DATE) " +
                        "ORDER BY uploaded_at DESC LIMIT 1", nativeQuery = true)
        Optional<String> findUploaderForThisMonth();

        Optional<LeaveBalance> findTopByEmployee_LdapAndLeaveTypeOrderByEffectiveFromDesc(String ldap,
                        String leaveType);

}
