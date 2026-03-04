package com.vbs.capsAllocation.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "attendance_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference
    private Employee employee;

    private LocalDate entryDate;

    private LocalDateTime entryTimestamp;

    private LocalDate exitDate;

    private LocalDateTime exitTimestamp;

    private String comment;

    private String lateLoginReason;

    private Boolean isOutsideOffice;

    private Boolean isCheckOutOutsideOffice;

    private String lateOrEarlyLogoutReason;

    private Boolean isDefaulter;

    private String  lateOrEarlyCheckout;

}
