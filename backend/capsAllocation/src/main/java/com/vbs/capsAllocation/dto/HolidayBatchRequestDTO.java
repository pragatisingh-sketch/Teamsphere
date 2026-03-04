package com.vbs.capsAllocation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for batch creation of holiday time entries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayBatchRequestDTO {
    @NotEmpty(message = "Holiday entries are required")
    @Valid
    private List<CreateTimeEntryDTO> entries;
}