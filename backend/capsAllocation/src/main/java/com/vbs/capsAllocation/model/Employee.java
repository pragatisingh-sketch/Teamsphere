package com.vbs.capsAllocation.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "Employee" , uniqueConstraints = {@UniqueConstraint(columnNames = "ldap")} )
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    @Column( nullable = false,unique = true)
    private String ldap;
    private String startDate;
    private String team;
    private String newLevel;
    private String lead;
    private String programManager;
    private String vendor;
    private String email;
    private String status;

    private String lwdMlStartDate;
    private String process;
    private String resignationDate;
    private String roleChangeEffectiveDate;
    private String levelBeforeChange;
    private String levelAfterChange;
    private String lastBillingDate;
    private String backfillLdap;
    private String billingStartDate;
    private String language;
    private String tenureTillDate;
  //  private String addedInGoVfsWhoMain;
  //  private String addedInGoVfsWhoInactive;
    private String level;
    private String inactiveReason;
    
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "profile_pic")
    @JsonIgnore
    private byte[] profilePic;
    private Long parent;
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    private String pnseProgram="NA";
    private String location;
    private String shift;
    @Column(name = "inactive", nullable = true)
    private Boolean inactive = false;
@OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
@ToString.Exclude
@JsonIgnore
private List<LeaveBalance> leaveBalances = new ArrayList<>();

@ToString.Exclude
@OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore
private List<VunnoResponse> vunnoResponses = new ArrayList<>();

@OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
@ToString.Exclude
@JsonIgnore
private List<EmployeeRelation> employeeRelations = new ArrayList<>();

//TODO: add last created and modified at fields.


}

