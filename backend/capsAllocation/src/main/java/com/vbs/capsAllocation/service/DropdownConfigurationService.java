package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.CreateDropdownConfigurationDTO;
import com.vbs.capsAllocation.dto.DropdownConfigurationDTO;
import com.vbs.capsAllocation.dto.UpdateDropdownConfigurationDTO;

import java.util.List;

/**
 * Service interface for managing dropdown configurations
 */
public interface DropdownConfigurationService {

    /**
     * Create a new dropdown configuration option
     * @param createDTO The data for creating the dropdown option
     * @param createdBy The username of the user creating the option
     * @return The created dropdown configuration
     */
    DropdownConfigurationDTO createDropdownOption(CreateDropdownConfigurationDTO createDTO, String createdBy);

    /**
     * Get all active dropdown options for a specific type
     * @param dropdownType The type of dropdown
     * @return List of active dropdown configurations
     */
    List<DropdownConfigurationDTO> getActiveDropdownOptions(String dropdownType);

    /**
     * Get all dropdown options for a specific type (including inactive)
     * @param dropdownType The type of dropdown
     * @return List of all dropdown configurations
     */
    List<DropdownConfigurationDTO> getAllDropdownOptions(String dropdownType);

    /**
     * Update an existing dropdown configuration option
     * @param id The ID of the dropdown option to update
     * @param updateDTO The data for updating the dropdown option
     * @return The updated dropdown configuration
     */
    DropdownConfigurationDTO updateDropdownOption(Long id, UpdateDropdownConfigurationDTO updateDTO);

    /**
     * Delete a dropdown configuration option
     * @param id The ID of the dropdown option to delete
     */
    void deleteDropdownOption(Long id);

    /**
     * Get a specific dropdown option by ID
     * @param id The ID of the dropdown option
     * @return The dropdown configuration
     */
    DropdownConfigurationDTO getDropdownOptionById(Long id);

    /**
     * Get all dropdown types
     * @return List of distinct dropdown types
     */
    List<String> getAllDropdownTypes();

    /**
     * Reorder dropdown options for a specific type
     * @param dropdownType The type of dropdown
     * @param orderedIds List of IDs in the desired order
     * @return List of reordered dropdown configurations
     */
    List<DropdownConfigurationDTO> reorderDropdownOptions(String dropdownType, List<Long> orderedIds);

    /**
     * Check if user has permission to manage dropdown configurations
     * @param userRole The role of the current user
     * @return true if user can manage dropdowns, false otherwise
     */
    boolean canManageDropdowns(String userRole);
}
