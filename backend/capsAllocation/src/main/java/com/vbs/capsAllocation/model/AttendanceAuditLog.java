package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attendance_id", nullable = false)
    private Long attendanceId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // STATUS_UPDATE, COMPLIANCE_UPDATE

    @Column(name = "previous_status", length = 255)
    private String previousStatus;

    @Column(name = "new_status", length = 255)
    private String newStatus;

    @Column(name = "previous_compliance")
    private Boolean previousCompliance;

    @Column(name = "new_compliance")
    private Boolean newCompliance;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy; // LDAP of the person making the change

    @Column(name = "changed_by_role", length = 100)
    private String changedByRole; // Role of the person making the change

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason; // Mandatory reason for the change

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    @Column(name = "previous_values", columnDefinition = "TEXT")
    private String previousValues; // JSON string of previous values

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues; // JSON string of new values
}
