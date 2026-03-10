package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String projectCode;

    @Column(nullable = false)
    private String projectName;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "is_overtime_eligible", nullable = false)
    private Boolean isOvertimeEligible = false;

    public Project(String projectCode, String projectName, User createdBy) {
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.createdBy = createdBy;
        this.isOvertimeEligible = false;
    }

    public Project(String projectCode, String projectName, User createdBy, Boolean isOvertimeEligible) {
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.createdBy = createdBy;
        this.isOvertimeEligible = isOvertimeEligible != null ? isOvertimeEligible : false;
    }

}
