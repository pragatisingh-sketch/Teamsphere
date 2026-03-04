package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.DropdownConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DropdownConfiguration entity
 * Provides methods for querying dropdown configuration data
 */
@Repository
public interface DropdownConfigurationRepository extends JpaRepository<DropdownConfiguration, Long> {

    /**
     * Find all active dropdown options for a specific type, ordered by sort order
     * @param dropdownType The type of dropdown (e.g., PROJECT, PSE_PROGRAM, etc.)
     * @return List of active dropdown configurations sorted by sort order
     */
    List<DropdownConfiguration> findByDropdownTypeAndIsActiveTrueOrderBySortOrderAscOptionValueAsc(String dropdownType);

    /**
     * Find all dropdown options for a specific type (including inactive), ordered by sort order
     * @param dropdownType The type of dropdown
     * @return List of all dropdown configurations sorted by sort order
     */
    List<DropdownConfiguration> findByDropdownTypeOrderBySortOrderAscOptionValueAsc(String dropdownType);

    /**
     * Find a specific dropdown configuration by type and option value
     * @param dropdownType The type of dropdown
     * @param optionValue The option value
     * @return Optional containing the dropdown configuration if found
     */
    Optional<DropdownConfiguration> findByDropdownTypeAndOptionValue(String dropdownType, String optionValue);

    /**
     * Check if a dropdown option exists for a specific type and value
     * @param dropdownType The type of dropdown
     * @param optionValue The option value
     * @return true if exists, false otherwise
     */
    boolean existsByDropdownTypeAndOptionValue(String dropdownType, String optionValue);

    /**
     * Check if a dropdown option exists for a specific type and value, excluding a specific ID
     * Used for update operations to avoid self-conflict
     * @param dropdownType The type of dropdown
     * @param optionValue The option value
     * @param id The ID to exclude from the check
     * @return true if exists, false otherwise
     */
    boolean existsByDropdownTypeAndOptionValueAndIdNot(String dropdownType, String optionValue, Long id);

    /**
     * Get all distinct dropdown types
     * @return List of distinct dropdown types
     */
    @Query("SELECT DISTINCT d.dropdownType FROM DropdownConfiguration d ORDER BY d.dropdownType")
    List<String> findDistinctDropdownTypes();

    /**
     * Get the maximum sort order for a specific dropdown type
     * @param dropdownType The type of dropdown
     * @return Maximum sort order, or 0 if no records exist
     */
    @Query("SELECT COALESCE(MAX(d.sortOrder), 0) FROM DropdownConfiguration d WHERE d.dropdownType = :dropdownType")
    Integer findMaxSortOrderByDropdownType(@Param("dropdownType") String dropdownType);

    /**
     * Find dropdown configurations by IDs in a specific order
     * Used for reordering operations
     * @param ids List of IDs in the desired order
     * @return List of dropdown configurations
     */
    @Query("SELECT d FROM DropdownConfiguration d WHERE d.id IN :ids")
    List<DropdownConfiguration> findByIdIn(@Param("ids") List<Long> ids);

    /**
     * Count active dropdown options for a specific type
     * @param dropdownType The type of dropdown
     * @return Count of active options
     */
    long countByDropdownTypeAndIsActiveTrue(String dropdownType);

    /**
     * Count all dropdown options for a specific type
     * @param dropdownType The type of dropdown
     * @return Count of all options
     */
    long countByDropdownType(String dropdownType);
}
