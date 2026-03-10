package com.vbs.capsAllocation.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryApprovalIdsListDTO {

    @NotNull(message = "Time entry ID is required")
    private List<Long> timeEntryId;

}
