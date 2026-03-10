package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryDetailDTO {
    private String process;
    private String activity;
    private Integer timeInMins;
    private String comment;
    private String status;
} 