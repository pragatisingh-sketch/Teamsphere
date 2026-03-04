package com.vbs.capsAllocation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryUpdateDto {
    @JsonProperty("id")
    @NotNull(message = "id is required")
    private Long id;

    @JsonProperty("category")
    @NotBlank(message = "category is required")
    private String category;

    @JsonProperty("reason")
    private String reason;
}

