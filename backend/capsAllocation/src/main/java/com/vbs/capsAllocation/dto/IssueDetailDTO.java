package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for detailed issue information displayed in the issue details modal.
 * Used for TimeEntry, Attendance, and Leave issues.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueDetailDTO {

    private Long id;
    private String type; // "TimeEntry", "Attendance", "Leaves"
    private LocalDate date;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // TimeEntry specific fields
    private String project;
    private String activity;
    private Integer timeInMins;
    private String process;
    private String comment;

    // Attendance specific fields
    private LocalDateTime entryTimestamp;
    private LocalDateTime exitTimestamp;
    private String lateLoginReason;
    private String lateOrEarlyLogoutReason;
    private Boolean isOutsideOffice;

    // Leave specific fields
    private LocalDate fromDate;
    private LocalDate toDate;
    private String leaveType;
    private String leaveCategory;
    private String duration;
    private String applicationType;
    private LocalTime startTime;
    private LocalTime endTime;

    // Static factory methods for each type

    public static IssueDetailDTO fromTimeEntry(
            Long id, LocalDate entryDate, String project, String process,
            String activity, Integer timeInMins, String status, String comment,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        IssueDetailDTO dto = new IssueDetailDTO();
        dto.setId(id);
        dto.setType("TimeEntry");
        dto.setDate(entryDate);
        dto.setProject(project);
        dto.setProcess(process);
        dto.setActivity(activity);
        dto.setTimeInMins(timeInMins);
        dto.setStatus(status);
        dto.setComment(comment);
        dto.setDescription(activity + " - " + timeInMins + " mins");
        dto.setCreatedAt(createdAt);
        dto.setUpdatedAt(updatedAt);
        return dto;
    }

    public static IssueDetailDTO fromAttendance(
            Long id, LocalDate entryDate, LocalDateTime entryTimestamp,
            LocalDateTime exitTimestamp, String lateLoginReason,
            String lateOrEarlyLogoutReason, Boolean isOutsideOffice, String status) {
        IssueDetailDTO dto = new IssueDetailDTO();
        dto.setId(id);
        dto.setType("Attendance");
        dto.setDate(entryDate);
        dto.setEntryTimestamp(entryTimestamp);
        dto.setExitTimestamp(exitTimestamp);
        dto.setLateLoginReason(lateLoginReason);
        dto.setLateOrEarlyLogoutReason(lateOrEarlyLogoutReason);
        dto.setIsOutsideOffice(isOutsideOffice);
        dto.setStatus(status != null ? status
                : (isOutsideOffice != null && isOutsideOffice ? "Outside Office" : "Defaulter"));
        dto.setDescription(lateLoginReason != null ? lateLoginReason : "Attendance Issue");
        return dto;
    }

    public static IssueDetailDTO fromLeave(
            Long id, LocalDate fromDate, LocalDate toDate, String leaveType,
            String leaveCategory, String duration, String applicationType,
            String status, LocalTime startTime, LocalTime endTime, LocalDateTime timestamp) {
        IssueDetailDTO dto = new IssueDetailDTO();
        dto.setId(id);
        dto.setType("Leaves");
        dto.setDate(fromDate);
        dto.setFromDate(fromDate);
        dto.setToDate(toDate);
        dto.setLeaveType(leaveType);
        dto.setLeaveCategory(leaveCategory);
        dto.setDuration(duration);
        dto.setApplicationType(applicationType);
        dto.setStatus(status);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setCreatedAt(timestamp);
        dto.setDescription(leaveType + " (" + leaveCategory + ")");
        return dto;
    }
}
