package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
        // year is essential — without it, Feb-2026 row conflicts with existing Feb-2025
        // row
        @UniqueConstraint(name = "leave_balances_employee_year_month_leavetype_key", columnNames = { "employee_id",
                "year", "month", "leave_type" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer year; // Numeric year: 2025
    private Integer month; // Numeric month: 7 for July

    private String leaveType; // SL, CL, EL, WFH

    private Double balance; // Remaining balance
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private String source; // e.g. "UPLOAD", "REVERSAL", "SYSTEM" (optional)

    private String uploadedBy; // LDAP or email of uploader (optional)

    private LocalDateTime uploadedAt; // Timestamp for auditing (optional)

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    public LeaveBalance(Employee employee, Integer year, Integer month, String leaveType, Double balance) {
        this.employee = employee;
        this.year = year;
        this.month = month;
        this.leaveType = leaveType;
        this.balance = balance;
    }

}
