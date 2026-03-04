package com.vbs.capsAllocation.dto;

import lombok.Data;

@Data
public class EmployeeRelationDTO {
    private Long id;
    private Long employeeId;
    private Long relationTypeId;
    private String relationValue;
    private Boolean isPrimary;
    private String effectiveDate;
    private String endDate;
    private Boolean isActive;
}