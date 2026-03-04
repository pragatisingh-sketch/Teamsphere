package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.AttendanceReminderRequestDTO;
import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending attendance reminder emails
 * Similar to TimeEntryReminderService but for attendance tracking
 */
@Service
public class AttendanceReminderService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Send bulk attendance reminders
     * 
     * @param request DTO containing recipient LDAPs and custom message
     * @return Map with success and failed counts
     */
    public Map<String, Integer> sendBulkReminders(AttendanceReminderRequestDTO request) {
        Map<String, Integer> result = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;

        LoggerUtil.logInfo(AttendanceReminderService.class,
                "Sending attendance reminders to {} recipients", request.getRecipientLdaps().size());

        // Get employee details for recipients
        List<Employee> recipients = employeeRepository.findByLdapIn(request.getRecipientLdaps());

        for (Employee employee : recipients) {
            try {
                sendAttendanceReminderEmail(employee, request.getCustomMessage(), request.isBulk());
                successCount++;
                LoggerUtil.logDebug("Attendance reminder sent successfully to {}", employee.getEmail());
            } catch (Exception e) {
                failedCount++;
                LoggerUtil.logError("Failed to send attendance reminder to {}: {}",
                        employee.getEmail(), e.getMessage(), e);
            }
        }

        result.put("success", successCount);
        result.put("failed", failedCount);

        LoggerUtil.logInfo(AttendanceReminderService.class,
                "Attendance reminders sent - Success: {}, Failed: {}", successCount, failedCount);

        return result;
    }

    /**
     * Send individual attendance reminder email
     */
    private void sendAttendanceReminderEmail(Employee employee, String customMessage, boolean isBulk)
            throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(employee.getEmail());
        helper.setSubject("Reminder: Please Mark Your Attendance");

        String emailBody = buildEmailBody(employee, customMessage, isBulk);
        helper.setText(emailBody, true);

        mailSender.send(message);
    }

    /**
     * Build HTML email body for attendance reminder
     */
    private String buildEmailBody(Employee employee, String customMessage, boolean isBulk) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>");
        sb.append("<html><head>");
        sb.append("<style>");
        sb.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        sb.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        sb.append(
                ".header { background-color: #ff9800; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }");
        sb.append(".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }");
        sb.append(
                ".footer { background-color: #f1f1f1; padding: 15px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 5px 5px; }");
        sb.append(
                ".button { display: inline-block; padding: 10px 20px; background-color: #ff9800; color: white; text-decoration: none; border-radius: 5px; margin-top: 15px; }");
        sb.append(
                ".custom-message { background-color: #fff3cd; border-left: 4px solid #ff9800; padding: 15px; margin: 15px 0; }");
        sb.append("</style>");
        sb.append("</head><body>");

        sb.append("<div class='container'>");
        sb.append("<div class='header'>");
        sb.append("<h2>Attendance Reminder</h2>");
        sb.append("</div>");

        sb.append("<div class='content'>");
        sb.append("<p>Dear ").append(employee.getFirstName()).append(",</p>");
        sb.append("<p>This is a friendly reminder that you haven't marked your attendance for today.</p>");

        if (customMessage != null && !customMessage.isEmpty()) {
            sb.append("<div class='custom-message'>");
            sb.append("<strong>Additional Message:</strong><br/>");
            sb.append(customMessage);
            sb.append("</div>");
        }

        sb.append("<p>Please mark your attendance as soon as possible to ensure accurate tracking.</p>");
        sb.append(
                "<p>If you have already marked your attendance or if you are on leave, please disregard this message.</p>");

        sb.append("<p>Thank you for your cooperation!</p>");
        sb.append("<p>Best regards,<br/>HR Team</p>");
        sb.append("</div>");

        sb.append("<div class='footer'>");
        sb.append("<p>This is an automated email. Please do not reply to this message.</p>");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</body></html>");

        return sb.toString();
    }
}
