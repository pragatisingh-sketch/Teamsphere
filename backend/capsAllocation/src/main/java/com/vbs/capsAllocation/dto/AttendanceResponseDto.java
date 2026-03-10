package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceResponseDto {
    private long id;
    private String ldap;
    private String team;
    private String name;
    private LocalDate entryDate;
    private LocalDateTime entryTimestamp;
    private String lateLoginReason;
    private Boolean isOutsideOffice;
    private Boolean isDefaulter;
    private String comment;
    private LocalDate exitDate;
    private LocalDateTime exitTimestamp;
    private Boolean isCheckOutOutsideOffice;
    private String lateOrEarlyLogoutReason;
    private String  lateOrEarlyCheckout;
    
}
