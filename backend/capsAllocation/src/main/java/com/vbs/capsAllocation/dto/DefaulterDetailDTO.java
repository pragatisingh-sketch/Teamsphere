package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefaulterDetailDTO {
    private Long employeeId;
    private String employeeName;
    private String email;
    private String department;
    private String manager;
    private String project;
    private String program;
    private long issueCount;
    private String status; // Critical, Warning, Good
    private LocalDate lastIncidentDate;
}
