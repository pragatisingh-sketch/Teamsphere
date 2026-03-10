package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.AttendanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceAuditRepository extends JpaRepository<AttendanceAuditLog, Long> {

    /**
     * Find all audit logs for a specific attendance record, ordered by most recent first
     * @param attendanceId The ID of the attendance record
     * @return List of audit logs
     */
    List<AttendanceAuditLog> findByAttendanceIdOrderByChangedAtDesc(Long attendanceId);

    /**
     * Find all modifications made by a specific user, ordered by most recent first
     * @param changedBy The LDAP of the user who made changes
     * @return List of audit logs
     */
    List<AttendanceAuditLog> findByChangedByOrderByChangedAtDesc(String changedBy);
}
