package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.BaseResponse;
import com.vbs.capsAllocation.dto.CreateDropdownConfigurationDTO;
import com.vbs.capsAllocation.dto.DropdownConfigurationDTO;
import com.vbs.capsAllocation.dto.UpdateDropdownConfigurationDTO;
import com.vbs.capsAllocation.service.DropdownConfigurationService;
import com.vbs.capsAllocation.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for managing dropdown configurations
 * Provides endpoints for CRUD operations on dropdown options
 */
@RestController
@RequestMapping("/api/dropdown-configurations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DropdownConfigurationController {

    private final DropdownConfigurationService dropdownConfigurationService;

    /**
     * Create a new dropdown configuration option
     * Only accessible by ADMIN_OPS_MANAGER role
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<DropdownConfigurationDTO>> createDropdownOption(
            @Valid @RequestBody CreateDropdownConfigurationDTO createDTO) {
        
        LoggerUtil.logMethodEntry(DropdownConfigurationController.class, "createDropdownOption");

        String currentUser = getCurrentUsername();
        DropdownConfigurationDTO created = dropdownConfigurationService.createDropdownOption(createDTO, currentUser);

        BaseResponse<DropdownConfigurationDTO> response = new BaseResponse<>(
            "SUCCESS", 201, "Dropdown option created successfully", created);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all active dropdown options for a specific type
     * Accessible by all authenticated users
     */
    @GetMapping("/active/{dropdownType}")
    public ResponseEntity<BaseResponse<List<DropdownConfigurationDTO>>> getActiveDropdownOptions(
            @PathVariable String dropdownType) {
        
        LoggerUtil.logMethodEntry(DropdownConfigurationController.class, "getActiveDropdownOptions");

        List<DropdownConfigurationDTO> options = dropdownConfigurationService.getActiveDropdownOptions(dropdownType);

        BaseResponse<List<DropdownConfigurationDTO>> response = new BaseResponse<>(
            "SUCCESS", 200, "Active dropdown options retrieved successfully", options);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all dropdown options for a specific type (including inactive)
     * Only accessible by ADMIN_OPS_MANAGER role
     */
    @GetMapping("/all/{dropdownType}")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<List<DropdownConfigurationDTO>>> getAllDropdownOptions(
            @PathVariable String dropdownType) {
        
        LoggerUtil.logMethodEntry(DropdownConfigurationController.class, "getAllDropdownOptions");

        List<DropdownConfigurationDTO> options = dropdownConfigurationService.getAllDropdownOptions(dropdownType);

        BaseResponse<List<DropdownConfigurationDTO>> response = new BaseResponse<>(
            "SUCCESS", 200, "All dropdown options retrieved successfully", options);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing dropdown configuration option
     * Only accessible by ADMIN_OPS_MANAGER role
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<DropdownConfigurationDTO>> updateDropdownOption(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDropdownConfigurationDTO updateDTO) {
        
        LoggerUtil.logMethodEntry(DropdownConfigurationController.class, "updateDropdownOption");

        DropdownConfigurationDTO updated = dropdownConfigurationService.updateDropdownOption(id, updateDTO);

        BaseResponse<DropdownConfigurationDTO> response = new BaseResponse<>(
            "SUCCESS", 200, "Dropdown option updated successfully", updated);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a dropdown configuration option
     * Only accessible by ADMIN_OPS_MANAGER role
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<Void>> deleteDropdownOption(@PathVariable Long id) {
        
        LoggerUtil.logMethodEntry(DropdownConfigurationController.class, "deleteDropdownOption");

        dropdownConfigurationService.deleteDropdownOption(id);

        BaseResponse<Void> response = new BaseResponse<>(
            "SUCCESS", 200, "Dropdown option deleted successfully", null);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific dropdown option by ID
     * Only accessible by ADMIN_OPS_MANAGER role
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<DropdownConfigurationDTO>> getDropdownOptionById(@PathVariable Long id) {
        
        LoggerUtil.logMethodEntry(DropdownConfigurationController.class, "getDropdownOptionById");

        DropdownConfigurationDTO option = dropdownConfigurationService.getDropdownOptionById(id);

        BaseResponse<DropdownConfigurationDTO> response = new BaseResponse<>(
            "SUCCESS", 200, "Dropdown option retrieved successfully", option);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all dropdown types
     * Only accessible by ADMIN_OPS_MANAGER role
     */
    @GetMapping("/types")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<List<String>>> getAllDropdownTypes() {
        
        LoggerUtil.logMethodEntry(DropdownConfigurationController.class, "getAllDropdownTypes");

        List<String> types = dropdownConfigurationService.getAllDropdownTypes();

        BaseResponse<List<String>> response = new BaseResponse<>(
            "SUCCESS", 200, "Dropdown types retrieved successfully", types);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Reorder dropdown options for a specific type
     * Only accessible by ADMIN_OPS_MANAGER role
     */
    @PutMapping("/reorder/{dropdownType}")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<List<DropdownConfigurationDTO>>> reorderDropdownOptions(
            @PathVariable String dropdownType,
            @RequestBody List<Long> orderedIds) {
        
        LoggerUtil.logMethodEntry(DropdownConfigurationController.class, "reorderDropdownOptions");

        List<DropdownConfigurationDTO> reordered = dropdownConfigurationService.reorderDropdownOptions(dropdownType, orderedIds);

        BaseResponse<List<DropdownConfigurationDTO>> response = new BaseResponse<>(
            "SUCCESS", 200, "Dropdown options reordered successfully", reordered);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current authenticated username
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }
}
