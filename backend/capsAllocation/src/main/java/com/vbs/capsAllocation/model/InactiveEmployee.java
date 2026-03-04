package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "inactive_employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InactiveEmployee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String firstName;
    private String lastName;
    private String ldap;
    private String team;
    private String newLevel;
    private String lead;
    private String programManager;
    private String vendor;
    private String email;
    private String status;
    private String process;
    private String levelBeforeChange;
    private String levelAfterChange;
    private String backfillLdap;
    private String language;
    private String tenureTillDate;
    private String level;
    private Long parent;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "profile_pic", columnDefinition = "bytea")
    private byte[] profilePic;

    private String inactiveReason;
    private String pnseProgram;
    
    private String startDate;
    private String roleChangeEffectiveDate;
    private String lastBillingDate;
    private String billingStartDate;
    private String lwdMlStartDate;
    private String shift;
    private String location;
    private String resignationDate;
    @Column(name = "deleted_at")
    private LocalDate deletedAt;
    @Column(name = "inactive", nullable = true)
    private Boolean inactive = true;
} 