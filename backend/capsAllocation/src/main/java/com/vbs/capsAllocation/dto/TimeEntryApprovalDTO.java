package com.vbs.capsAllocation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryApprovalDTO {
    @NotNull(message = "Time entry ID is required")
    private Long timeEntryId;
}
