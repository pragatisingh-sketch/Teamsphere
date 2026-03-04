package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceUpdateRequest {

    private String newStatus; // New value for lateOrEarlyCheckout field (optional)

    private Boolean isDefaulter; // New value for compliance status (optional)

    private String reason; // Mandatory reason for the change
}
