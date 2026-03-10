package com.vbs.capsAllocation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "EMPLOYEE_RELATION")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The employee who holds the relation (subordinate)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnore
    private Employee employee;

    // The related employee (lead/manager)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "related_employee_id", nullable = false)
    private Employee relatedEmployee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "relationType_id", nullable = false)
    private RelationType relationType;

    @Column(name = "relationValue", nullable = false)
    private String relationValue;

    @Column(name = "effectiveDate")
    private String effectiveDate;

    @Column(name = "endDate")
    private String endDate;

    @Column(name = "isActive")
    private Boolean isActive = true;
}
