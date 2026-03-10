package com.vbs.capsAllocation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vbs.capsAllocation.model.Activity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimeEntryDTO {
    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Entry date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;

    @NotNull(message = "Process is required")
    private String process;

    @NotNull(message = "Activity is required")
    private Activity activity;

    @NotNull(message = "Time in minutes is required")
    @Min(value = 1, message = "Time in minutes must be at least 1")
    private Integer timeInMins;

    @NotNull(message = "Attendance type is required")
    private String attendanceType;

    private Long leadId;

    private String comment;

    private String ldap;

    private Boolean isOvertime = false;
}
