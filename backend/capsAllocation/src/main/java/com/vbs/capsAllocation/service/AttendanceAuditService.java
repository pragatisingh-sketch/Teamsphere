package com.vbs.capsAllocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vbs.capsAllocation.dto.AttendanceAuditDto;
import com.vbs.capsAllocation.model.Attendance;
import com.vbs.capsAllocation.model.AttendanceAuditLog;
import com.vbs.capsAllocation.repository.AttendanceAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceAuditService {

    @Autowired
    private AttendanceAuditRepository attendanceAuditRepository;

    private static final Logger logger = LoggerFactory.getLogger(AttendanceAuditService.class);

    private static final ZoneId INDIA_TIMEZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Log a status update to the audit trail
     */
    public void logStatusUpdate(
            Attendance attendance,
            String previousStatus,
            String newStatus,
            String reason,
            UserDetails userDetails) {
        Map<String, Object> previousValues = new HashMap<>();
        previousValues.put("lateOrEarlyCheckout", previousStatus);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("lateOrEarlyCheckout", newStatus);

        // Get current date and time in India timezone
        ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
        LocalDateTime auditTimeStamp = nowInIndia.toLocalDateTime();

        AttendanceAuditLog auditLog = new AttendanceAuditLog();
        auditLog.setAttendanceId(attendance.getId());
        auditLog.setActionType("STATUS_UPDATE");
        auditLog.setPreviousStatus(previousStatus);
        auditLog.setNewStatus(newStatus);
        auditLog.setChangedBy(userDetails.getUsername());
        auditLog.setChangedByRole(getRoleFromUserDetails(userDetails));
        auditLog.setChangedAt(auditTimeStamp);
        auditLog.setChangeReason(reason);
        auditLog.setChangeDescription("Attendance status updated from '" + previousStatus + "' to '" + newStatus + "'");
        auditLog.setPreviousValues(toJson(previousValues));
        auditLog.setNewValues(toJson(newValues));

        attendanceAuditRepository.save(auditLog);
        logger.info("Status update logged for attendance ID: {} by user: {}", attendance.getId(),
                userDetails.getUsername());
    }

    /**
     * Log a compliance status update to the audit trail
     */
    public void logComplianceUpdate(
            Attendance attendance,
            Boolean previousCompliance,
            Boolean newCompliance,
            String reason,
            UserDetails userDetails) {
        Map<String, Object> previousValues = new HashMap<>();
        previousValues.put("isDefaulter", previousCompliance);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("isDefaulter", newCompliance);

        // Get current date and time in India timezone
        ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
        LocalDateTime auditTimeStamp = nowInIndia.toLocalDateTime();

        AttendanceAuditLog auditLog = new AttendanceAuditLog();
        auditLog.setAttendanceId(attendance.getId());
        auditLog.setActionType("COMPLIANCE_UPDATE");
        auditLog.setPreviousCompliance(previousCompliance);
        auditLog.setNewCompliance(newCompliance);
        auditLog.setChangedBy(userDetails.getUsername());
        auditLog.setChangedByRole(getRoleFromUserDetails(userDetails));
        auditLog.setChangedAt(auditTimeStamp);
        auditLog.setChangeReason(reason);
        auditLog.setChangeDescription("Compliance status updated from '" +
                (previousCompliance ? "Non-Compliant" : "Compliant") + "' to '" +
                (newCompliance ? "Non-Compliant" : "Compliant") + "'");
        auditLog.setPreviousValues(toJson(previousValues));
        auditLog.setNewValues(toJson(newValues));

        attendanceAuditRepository.save(auditLog);
        logger.info("Compliance update logged for attendance ID: {} by user: {}", attendance.getId(),
                userDetails.getUsername());
    }

    /**
     * Get audit history for a specific attendance record
     */
    public List<AttendanceAuditDto> getAuditHistory(Long attendanceId) {
        List<AttendanceAuditLog> auditLogs = attendanceAuditRepository
                .findByAttendanceIdOrderByChangedAtDesc(attendanceId);

        return auditLogs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert audit log entity to DTO
     */
    private AttendanceAuditDto convertToDto(AttendanceAuditLog auditLog) {
        AttendanceAuditDto dto = new AttendanceAuditDto();
        dto.setId(auditLog.getId());
        dto.setAttendanceId(auditLog.getAttendanceId());
        dto.setActionType(auditLog.getActionType());
        dto.setPreviousStatus(auditLog.getPreviousStatus());
        dto.setNewStatus(auditLog.getNewStatus());
        dto.setPreviousCompliance(auditLog.getPreviousCompliance());
        dto.setNewCompliance(auditLog.getNewCompliance());
        dto.setChangedBy(auditLog.getChangedBy());
        dto.setChangedByRole(auditLog.getChangedByRole());
        dto.setChangedAt(auditLog.getChangedAt());
        dto.setChangeReason(auditLog.getChangeReason());
        dto.setChangeDescription(auditLog.getChangeDescription());
        return dto;
    }

    /**
     * Extract role from UserDetails
     */
    private String getRoleFromUserDetails(UserDetails userDetails) {
        return userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");
    }

    /**
     * Convert map to JSON string
     */
    private String toJson(Map<String, Object> map) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            logger.error("Error converting map to JSON", e);
            return "{}";
        }
    }
}
