package com.vbs.capsAllocation.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "vunno_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VunnoResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    private String team;

    private String requestorName;

    private String approver;

    private LocalDate fromDate;

    private LocalDate toDate;

    private String applicationType;

    private String leaveType;

    private String duration;

    private String program;

    private String status;

    private String backup;

    private String backupLdap;

    private String backupName;

    @Column(length = 2048)
    private String orgScreenshot;

    @Column(length = 2048)
    private String timesheetScreenshot;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(length = 2048)
    private String reason;

    @Column(name = "document_path", length = 1024)
    private String documentPath;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "shift_code_at_request_time")
    private String shiftCodeAtRequestTime;

    @Column(name = "leave_category")
    private String leaveCategory;

    // For mixed leave type requests: stores per-day configuration as JSON
    // Format: [{"date":"2026-01-29","leaveType":"Casual Leave","duration":"Full
    // Day","durationValue":1.0}, ...]
    @Column(name = "day_configurations", columnDefinition = "TEXT")
    private String dayConfigurations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @ToString.Exclude
    @JsonBackReference
    private Employee employee;
}
