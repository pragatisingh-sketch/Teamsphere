package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.TimeEntryReminderRequestDTO;
import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for sending time-entry reminder emails to employees
 * who haven't filled their timesheets.
 */
@Service
public class TimeEntryReminderService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmployeeRepository employeeRepository;

    private static final String DEFAULT_SUBJECT = "[Action Required] Time-Entry Pending - Please Fill Your Timesheet";

    private static final String DEFAULT_MESSAGE_TEMPLATE = "Dear %s,<br><br>" +
            "This is a reminder that your time-entries are pending for the following periods:<br><br>" +
            "%s<br><br>" +
            "Please fill your timesheet at your earliest convenience by logging into the Highspring portal.<br><br>" +
            "Best regards,<br>" +
            "Highspring Team";

    /**
     * Send reminder to a single user
     * 
     * @param ldap           The LDAP of the user to send reminder to
     * @param customMessage  Optional custom message (null for default)
     * @param missingPeriods List of period strings to include in the email
     * @return true if email sent successfully
     */
    /**
     * Send reminder to a single user
     * 
     * @param ldap             The LDAP of the user to send reminder to
     * @param customMessage    Optional custom message (null for default)
     * @param missingPeriods   List of period strings to include in the email
     * @param missingDays      List of specific missing dates
     * @param wholeWeekMissing Boolean indicating if entire week is missing
     * @return true if email sent successfully
     */
    public boolean sendIndividualReminder(String ldap, String customMessage, List<String> missingPeriods,
            List<String> missingDays, boolean wholeWeekMissing) {
        try {
            Optional<Employee> employeeOpt = employeeRepository.findByLdap(ldap);
            if (employeeOpt.isEmpty()) {
                LoggerUtil.logError("Employee not found for LDAP: {}", ldap);
                return false;
            }

            Employee employee = employeeOpt.get();

            String email = employee.getEmail();
            if (email == null || email.isEmpty()) {
                LoggerUtil.logError("No email found for employee: {}", ldap);
                return false;
            }

            String name = employee.getFirstName() + " " + employee.getLastName();
            String periodsHtml = formatPeriodsAsHtml(missingPeriods);
            String missingDetailsHtml = formatMissingDetailsHtml(missingDays, wholeWeekMissing);

            // Combine periods and details
            String fullDetailsHtml = periodsHtml + missingDetailsHtml;

            String body;
            if (customMessage != null && !customMessage.isEmpty()) {
                // Replace placeholders in custom message
                body = customMessage
                        .replace("{name}", name)
                        .replace("{periods}", fullDetailsHtml);
            } else {
                body = String.format(DEFAULT_MESSAGE_TEMPLATE, name, fullDetailsHtml);
            }

            // Get manager email for CC if available
            String ccEmail = null;
            if (employee.getProgramManager() != null && !employee.getProgramManager().isEmpty()) {
                Optional<Employee> managerOpt = employeeRepository.findByLdap(employee.getProgramManager());
                if (managerOpt.isPresent() && managerOpt.get().getEmail() != null) {
                    ccEmail = managerOpt.get().getEmail();
                }
            }

            emailService.sendEmail(email, DEFAULT_SUBJECT, body, ccEmail);
            LoggerUtil.logInfo(TimeEntryReminderService.class, "Time-entry reminder sent to: {} ({})", name, email);
            return true;

        } catch (Exception e) {
            LoggerUtil.logError("Failed to send reminder to {}: {}", ldap, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send bulk reminders to multiple users
     * 
     * @param request The reminder request containing recipient LDAPs and message
     * @return Map with "success" count and "failed" count
     */
    public Map<String, Integer> sendBulkReminders(TimeEntryReminderRequestDTO request) {
        int successCount = 0;
        int failedCount = 0;

        for (String ldap : request.getRecipientLdaps()) {
            boolean success = sendIndividualReminder(
                    ldap,
                    request.getCustomMessage(),
                    request.getMissingPeriods(),
                    request.getMissingDays(),
                    request.isWholeWeekMissing());
            if (success) {
                successCount++;
            } else {
                failedCount++;
            }
        }

        LoggerUtil.logInfo(TimeEntryReminderService.class, "Bulk reminder completed. Success: {}, Failed: {}",
                successCount, failedCount);

        return Map.of(
                "success", successCount,
                "failed", failedCount);
    }

    /**
     * Format missing periods as an HTML unordered list
     */
    private String formatPeriodsAsHtml(List<String> periods) {
        if (periods == null || periods.isEmpty()) {
            return "<em>Multiple periods pending</em><br>";
        }

        return "<strong>Week(s):</strong><ul>" +
                periods.stream()
                        .map(p -> "<li>" + p + "</li>")
                        .collect(Collectors.joining())
                +
                "</ul>";
    }

    /**
     * Format missing details (specific days or whole week warning) as HTML
     */
    private String formatMissingDetailsHtml(List<String> missingDays, boolean wholeWeekMissing) {
        if (wholeWeekMissing) {
            return "<p style='color: red;'><strong> Note: Time entries are missing for the entire week.</strong></p>";
        }

        if (missingDays != null && !missingDays.isEmpty()) {
            // Format dates nicely, e.g., "2025-12-30" -> "Tue, Dec 30" logic would likely
            // happen in frontend
            // As this is backend, we'll list them as provided or simple list
            return "<p><strong>Missing Dates:</strong></p><ul>" +
                    missingDays.stream()
                            .map(d -> "<li>" + d + "</li>")
                            .collect(Collectors.joining())
                    + "</ul>";
        }

        return "";
    }
}
