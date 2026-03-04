package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceUploadDto {
    private String ldap;
    private Double slBalance;
    private Double clBalance;
    private Double elBalance;

    public LeaveBalanceUploadDto(String ldap, double slBalance, double clBalance, double elBalance) {

    }

}


