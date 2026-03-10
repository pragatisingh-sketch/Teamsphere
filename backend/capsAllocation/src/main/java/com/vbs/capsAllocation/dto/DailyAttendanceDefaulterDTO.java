package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Daily Attendance Defaulters Report
 * Contains employee information for those who haven't marked attendance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyAttendanceDefaulterDTO {
    private Long employeeId;
    private String employeeName;
    private String ldap;
    private String email;
    private String department;
    private String manager;
    private LocalDate lastAttendanceDate;
}
