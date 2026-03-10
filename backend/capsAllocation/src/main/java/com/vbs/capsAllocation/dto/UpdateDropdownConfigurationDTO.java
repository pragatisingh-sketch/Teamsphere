package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

/**
 * DTO for updating existing dropdown configuration options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDropdownConfigurationDTO {

    @NotBlank(message = "Option value is required")
    @Size(max = 100, message = "Option value must not exceed 100 characters")
    private String optionValue;

    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    @Min(value = 0, message = "Sort order must be 0 or greater")
    private Integer sortOrder;
}
