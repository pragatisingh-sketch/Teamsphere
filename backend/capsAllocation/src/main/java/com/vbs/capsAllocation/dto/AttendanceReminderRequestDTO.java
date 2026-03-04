package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Attendance Reminder Email Requests
 * Used to send bulk or individual attendance reminders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReminderRequestDTO {
    private List<String> recipientLdaps;
    private String customMessage;
    private boolean isBulk;
}
