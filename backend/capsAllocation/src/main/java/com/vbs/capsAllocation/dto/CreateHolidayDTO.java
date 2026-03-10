package com.vbs.capsAllocation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateHolidayDTO {
    
    @NotNull(message = "Holiday date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate holidayDate;
    
    @NotBlank(message = "Holiday name is required")
    private String holidayName;
    
    private String description;
    
    private String holidayType = "GOOGLE";
    
    private Boolean isActive = true;
}
