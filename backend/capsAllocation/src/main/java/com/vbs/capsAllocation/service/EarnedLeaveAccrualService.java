package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.LeaveBalance;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.repository.LeaveBalanceRepository;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for automatically accruing Earned Leave (EL) for all
 * active employees. This runs on the 1st of every month via a scheduled task
 * called in {@link com.vbs.capsAllocation.config.SchedulingConfig}.
 * Each active employee receives {@value MONTHLY_EL_DAYS} days of EL per month
 * (source = "SYSTEM"). The job is <strong>idempotent</strong>: if a balance row
 * for the same employee / leave-type / month / year already exists (e.g.
 * created
 * by a manual HR upload), the accrual for that employee is skipped so no
 * duplicates are ever written.
 */
@Service
public class EarnedLeaveAccrualService {

        /** Days of Earned Leave credited to every active employee per month. */
        public static final double MONTHLY_EL_DAYS = 1.25;

        private static final String LEAVE_TYPE = "HR-EL";
        private static final String SOURCE = "SYSTEM";
        private static final String UPLOADED_BY = "Scheduler";
        private static final ZoneId INDIA_TZ = ZoneId.of("Asia/Kolkata");

        @Autowired
        private EmployeeRepository employeeRepository;

        @Autowired
        private LeaveBalanceRepository leaveBalanceRepository;

        /**
         * Accrues {@value MONTHLY_EL_DAYS} EL days for every active employee.
         *
         * <p>
         * Called by
         * {@link com.vbs.capsAllocation.config.SchedulingConfig#monthlyELAccrual()}
         * on the 1st of each month at midnight IST.
         * </p>
         */
        @Transactional
        public void accrueMonthlyEL() {

                LocalDate today = LocalDate.now(INDIA_TZ);
                int year = today.getYear();
                int month = today.getMonthValue();
                LocalDateTime now = LocalDateTime.now(INDIA_TZ);

                List<Employee> activeEmployees = employeeRepository.findByStatus("Active");

                LoggerUtil.logDebug(
                                "Monthly EL accrual started for {}/{} — {} active employee(s) found.",
                                month, year, activeEmployees.size());

                int credited = 0;
                int skipped = 0;

                for (Employee emp : activeEmployees) {
                        boolean alreadyExists = leaveBalanceRepository
                                        .findByEmployeeAndLeaveTypeAndMonthAndYear(emp, LEAVE_TYPE, month, year)
                                        .isPresent();

                        if (alreadyExists) {
                                LoggerUtil.logDebug(
                                                "EL accrual skipped for {} — record already exists for {}/{}.",
                                                emp.getLdap(), month, year);
                                skipped++;
                                continue;
                        }

                        // Fetch the employee's current latest EL balance
                        Optional<LeaveBalance> latestBalanceOpt = leaveBalanceRepository
                                        .findTopByEmployee_LdapAndLeaveTypeOrderByEffectiveFromDesc(emp.getLdap(),
                                                        LEAVE_TYPE);

                        double currentBalance = latestBalanceOpt.map(LeaveBalance::getBalance).orElse(0.0);
                        double newBalance = currentBalance + MONTHLY_EL_DAYS;

                        LeaveBalance lb = new LeaveBalance();
                        lb.setEmployee(emp);
                        lb.setLeaveType(LEAVE_TYPE);
                        lb.setBalance(newBalance);
                        lb.setYear(year);
                        lb.setMonth(month);
                        lb.setSource(SOURCE);
                        lb.setUploadedBy(UPLOADED_BY);
                        lb.setUploadedAt(now);
                        lb.setEffectiveFrom(LocalDate.of(year, month, 1));

                        leaveBalanceRepository.save(lb);
                        credited++;
                }

                LoggerUtil.logDebug(
                                "Monthly EL accrual complete for {}/{} — credited: {}, skipped: {}.",
                                month, year, credited, skipped);
        }

        /**
         * One-time reconciliation for months where HR uploaded EL balances before
         * monthly accrual. If an HR-EL row already exists for current month/year and is
         * not SYSTEM, add {@value MONTHLY_EL_DAYS} once. This avoids monthly cron
         * changes.
         */
        @Transactional
        public String reconcileCurrentMonthELAfterUploadOneTime() {

                LocalDate today = LocalDate.now(INDIA_TZ);
                int year = today.getYear();
                int month = today.getMonthValue();
                LocalDateTime now = LocalDateTime.now(INDIA_TZ);

                final String ONE_TIME_ADJUSTMENT_UPLOADER = "Manual-El-Topup-" + month + "-" + year;

                List<Employee> activeEmployees = employeeRepository.findByStatus("Active");

                int toppedUp = 0;
                int created = 0;
                int skippedAlreadySystem = 0;
                int skippedAlreadyAdjusted = 0;

                for (Employee emp : activeEmployees) {
                        Optional<LeaveBalance> existingOpt = leaveBalanceRepository
                                        .findByEmployeeAndLeaveTypeAndMonthAndYear(emp, LEAVE_TYPE, month, year);

                        if (existingOpt.isPresent()) {
                                LeaveBalance existing = existingOpt.get();

                                if (ONE_TIME_ADJUSTMENT_UPLOADER.equalsIgnoreCase(existing.getUploadedBy())) {
                                        skippedAlreadyAdjusted++;
                                        continue;
                                }

                                if (SOURCE.equalsIgnoreCase(existing.getSource())) {
                                        skippedAlreadySystem++;
                                        continue;
                                }

                                double current = existing.getBalance() != null ? existing.getBalance() : 0.0;
                                existing.setBalance(current + MONTHLY_EL_DAYS);
                                existing.setUploadedBy(ONE_TIME_ADJUSTMENT_UPLOADER);
                                existing.setUploadedAt(now);
                                leaveBalanceRepository.save(existing);
                                toppedUp++;
                                continue;
                        }

                        Optional<LeaveBalance> latestBalanceOpt = leaveBalanceRepository
                                        .findTopByEmployee_LdapAndLeaveTypeOrderByEffectiveFromDesc(emp.getLdap(),
                                                        LEAVE_TYPE);

                        double currentBalance = latestBalanceOpt.map(LeaveBalance::getBalance).orElse(0.0);
                        double newBalance = currentBalance + MONTHLY_EL_DAYS;

                        LeaveBalance lb = new LeaveBalance();
                        lb.setEmployee(emp);
                        lb.setLeaveType(LEAVE_TYPE);
                        lb.setBalance(newBalance);
                        lb.setYear(year);
                        lb.setMonth(month);
                        lb.setSource(SOURCE);
                        lb.setUploadedBy(UPLOADED_BY);
                        lb.setUploadedAt(now);
                        lb.setEffectiveFrom(LocalDate.of(year, month, 1));

                        leaveBalanceRepository.save(lb);
                        created++;
                }

                String result = String.format(
                                "EL one-time reconcile complete for %d/%d — toppedUp: %d, created: %d, skippedSystem: %d, skippedAdjusted: %d",
                                month, year, toppedUp, created, skippedAlreadySystem, skippedAlreadyAdjusted);
                LoggerUtil.logDebug(result);
                return result;
        }
}
