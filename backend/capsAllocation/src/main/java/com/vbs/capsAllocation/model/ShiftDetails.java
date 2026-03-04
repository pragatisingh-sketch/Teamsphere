package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shift_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private String name;
    private String startTime;
    private String endTime;
    private String maxLoginTime;
    private String breakTime;
    private String halfTime;

    private Integer gracePeriodMinutes;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
