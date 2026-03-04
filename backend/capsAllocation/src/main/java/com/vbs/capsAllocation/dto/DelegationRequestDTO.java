package com.vbs.capsAllocation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DelegationRequestDTO {
    private String delegatorLdap;
    private String delegateeLdap;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
