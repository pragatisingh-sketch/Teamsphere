package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceAuditDto {

    private Long id;

    private Long attendanceId;

    private String actionType;

    private String previousStatus;

    private String newStatus;

    private Boolean previousCompliance;

    private Boolean newCompliance;

    private String changedBy;

    private String changedByRole;

    private LocalDateTime changedAt;

    private String changeReason;

    private String changeDescription;
}
