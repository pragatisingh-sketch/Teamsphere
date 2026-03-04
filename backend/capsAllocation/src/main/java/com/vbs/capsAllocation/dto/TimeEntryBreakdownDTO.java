package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryBreakdownDTO {
    private LocalDate date;
    private Integer timeInMins;
    private Integer entries;
    private List<TimeEntryDetailDTO> details;
} 