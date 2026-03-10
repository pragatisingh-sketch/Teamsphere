package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheetDTO {
    private String id;
    private String ldap;
    private String masked_orgid;
    private String subrole;
    private String role;
    private String date;
    private String process;
    private String billingCode;
    private String activity;
    private String status;
    private String lead_ldap;
    private String vendor;
    private String minutes;
    private String project;
    private String team;
    private String comment;
}
