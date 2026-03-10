package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leads_request")
public class LeadsRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String status;

    private String requestedBy;

    private String requestType;

    private String ldap;

    @CreationTimestamp
    private Timestamp requestedAt;

    // Employee data is now stored in external JSON file
    // This field stores the key to retrieve data from the JSON file
    @Column(name = "employee_data_key")
    private String employeeDataKey;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isSignUp = false;

}
