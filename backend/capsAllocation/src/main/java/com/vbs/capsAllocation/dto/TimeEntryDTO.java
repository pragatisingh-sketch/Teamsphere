package com.vbs.capsAllocation.dto;

import com.vbs.capsAllocation.model.Activity;
import com.vbs.capsAllocation.model.TimeEntryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryDTO {
    private Long id;
    private Long userId;
    private String username;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private LocalDate entryDate;
    private String ldap;
    private Long leadId;
    private String leadUsername;
    private String process;
    private Activity activity;
    private Integer timeInMins;
    private String attendanceType;
    private String comment;
    private TimeEntryStatus status;
    private String rejectionComment;
    private String shift; // Added shift field
    private Boolean isOvertime = false;
    private Boolean isDefaulter = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
