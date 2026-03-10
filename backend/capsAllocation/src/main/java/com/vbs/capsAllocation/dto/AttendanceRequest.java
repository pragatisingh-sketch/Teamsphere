package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {
    private String ldap;
    private String reason;
    private String comment;
    private double latitude;
    private double longitude;
    private LocalDateTime entryTimestamp;
    private String deviceType; // "Mobile", "Tablet", "Desktop", or "Unknown"
    private Double accuracy; // GPS accuracy in meters
}
