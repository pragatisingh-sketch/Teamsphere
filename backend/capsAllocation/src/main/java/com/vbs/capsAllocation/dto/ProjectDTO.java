package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private Long id;
    private String projectCode;
    private String projectName;
    private Long createdById;
    private String createdByUsername;
    private Boolean isOvertimeEligible;
}
