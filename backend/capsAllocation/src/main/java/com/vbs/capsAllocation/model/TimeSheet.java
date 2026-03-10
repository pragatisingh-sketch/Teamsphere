package com.vbs.capsAllocation.model;


import jakarta.persistence.Id;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "time_sheet")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSheet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
