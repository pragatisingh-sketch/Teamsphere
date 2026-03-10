package com.vbs.capsAllocation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EmployeeDTO {
    private Long id;
    @NotNull
    @Size(min = 1, max = 50)
    private String firstName;
    private String lastName;
    @NotNull
    @Size(min = 1, max = 50)
    private String ldap;
    private String startDate;
    private String team;
    private String newLevel;
    private String lead;
    private String programManager;
    private String vendor;
    @Size(min = 1, max = 50)
    @NotEmpty(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    @NotNull
    @Size(min = 1, max = 50)
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
    //private String addedInGoVfsWhoMain;
    //private String addedInGoVfsWhoInactive;
    private byte[] profilePic;
    private Long parent;
    private String level;
    private String inactiveReason;
    private String pnseProgram;
    private String location;
    private String shift;

    private List<EmployeeRelationDTO> employeeRelations = new ArrayList<>();

}
