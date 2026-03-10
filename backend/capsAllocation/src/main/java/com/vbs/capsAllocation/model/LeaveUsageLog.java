package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "leave_usage_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String leaveType; // SL, CL, EL, WFH

    private Double daysTaken;

    private LocalDate leaveDate;

    private Integer year;

    private String quarter; // Q1, Q2, Q3, Q4

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    public String getLeaveType() {
        return leaveType;
    }

    public Double getDaysTaken() {
        return daysTaken;
    }

}
