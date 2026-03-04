package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceUploadResultDto {

    private int successCount;
    private int skippedCount;
    private List<SkippedEmployeeDto> skippedEmployees;
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedEmployeeDto {
        private String ldap;
        private String reason;
    }
}
