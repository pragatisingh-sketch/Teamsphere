package com.vbs.capsAllocation.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProjectDTO {
    private Long id;
    private Long userId;
    private String username;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private LocalDate assignedDate;
    private String status;
    private Boolean isOvertimeEligible;
}
