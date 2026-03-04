package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Delegations")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Delegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String delegatorLdap;

    @Column(nullable = false)
    private String delegateeLdap;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role originalRole; // Delegator's original role

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role delegateeOriginalRole; // Delegatee's original role

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role delegatedRole; // Role being delegated (usually same as originalRole)

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DelegationStatus status;

    public enum DelegationStatus {
        ACTIVE,
        COMPLETED,
        CANCELLED,
        SCHEDULED
    }
}
