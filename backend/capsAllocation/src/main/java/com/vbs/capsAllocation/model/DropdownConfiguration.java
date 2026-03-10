package com.vbs.capsAllocation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entity representing dropdown configuration options for various form fields
 * Supports dynamic dropdown values that can be managed by admin users
 */
@Entity
@Table(name = "dropdown_configurations", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"dropdown_type", "option_value"})
       },
       indexes = {
           @Index(name = "idx_dropdown_type", columnList = "dropdown_type"),
           @Index(name = "idx_dropdown_type_active", columnList = "dropdown_type, is_active"),
           @Index(name = "idx_sort_order", columnList = "dropdown_type, sort_order")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropdownConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of dropdown (e.g., PROJECT, PSE_PROGRAM, PROCESS, LEVEL, LANGUAGE, LOCATION, VENDOR)
     */
    @Column(name = "dropdown_type", nullable = false, length = 50)
    @NotBlank(message = "Dropdown type is required")
    @Size(max = 50, message = "Dropdown type must not exceed 50 characters")
    private String dropdownType;

    /**
     * The actual value stored in the database and used in forms
     */
    @Column(name = "option_value", nullable = false, length = 100)
    @NotBlank(message = "Option value is required")
    @Size(max = 100, message = "Option value must not exceed 100 characters")
    private String optionValue;

    /**
     * Display name shown to users (can be different from option_value)
     */
    @Column(name = "display_name", nullable = false, length = 100)
    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    /**
     * Whether this option is active and should be shown in dropdowns
     */
    @Column(name = "is_active", nullable = false)
    @NotNull(message = "Active status is required")
    private Boolean isActive = true;

    /**
     * Sort order for displaying options (lower numbers appear first)
     */
    @Column(name = "sort_order", nullable = false)
    @NotNull(message = "Sort order is required")
    private Integer sortOrder = 0;

    /**
     * Username of the user who created this configuration
     */
    @Column(name = "created_by", nullable = false, length = 50)
    @NotBlank(message = "Created by is required")
    @Size(max = 50, message = "Created by must not exceed 50 characters")
    private String createdBy;

    /**
     * Timestamp when this configuration was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp when this configuration was last updated
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Constructor for creating new dropdown configurations
     */
    public DropdownConfiguration(String dropdownType, String optionValue, String displayName, 
                               Boolean isActive, Integer sortOrder, String createdBy) {
        this.dropdownType = dropdownType;
        this.optionValue = optionValue;
        this.displayName = displayName;
        this.isActive = isActive != null ? isActive : true;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.createdBy = createdBy;
    }
}
