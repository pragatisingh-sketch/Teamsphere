package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopDefaulterDTO {
    private Long employeeId;
    private String employeeName;
    private String department;
    private long issueCount;
    private String avatarUrl; // Optional, for UI
}
