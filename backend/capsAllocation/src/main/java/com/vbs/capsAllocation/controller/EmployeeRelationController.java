package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.BaseResponse;
import com.vbs.capsAllocation.model.EmployeeRelation;
import com.vbs.capsAllocation.service.EmployeeRelationService;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Controller for employee relation operations in the system
 */
@RestController
@RequestMapping("/api/employee-relations")
public class EmployeeRelationController {

    @Autowired
    private EmployeeRelationService employeeRelationService;

    /**
     * Get all employee relations
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<EmployeeRelation>>> getAllEmployeeRelations() {
        try {
            LoggerUtil.logDebug("Fetching all employee relations");
            return ResponseEntity.ok(BaseResponse.success("Employee relations retrieved successfully", employeeRelationService.getAllEmployeeRelations()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching employee relations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve employee relations: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get employee relation by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<EmployeeRelation>> getEmployeeRelationById(@PathVariable Long id) {
        try {
            LoggerUtil.logDebug("Fetching employee relation by ID: {}", id);
            Optional<EmployeeRelation> employeeRelation = employeeRelationService.getEmployeeRelationById(id);
            if (employeeRelation.isPresent()) {
                return ResponseEntity.ok(BaseResponse.success("Employee relation retrieved successfully", employeeRelation.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error("Employee relation not found", HttpStatus.NOT_FOUND.value()));
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching employee relation by ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve employee relation: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Create new employee relation
     */
    @PostMapping
    public ResponseEntity<BaseResponse<EmployeeRelation>> createEmployeeRelation(@RequestBody EmployeeRelation employeeRelation) {
        try {
            LoggerUtil.logDebug("Creating employee relation");
            EmployeeRelation result = employeeRelationService.createEmployeeRelation(employeeRelation);
            return ResponseEntity.ok(BaseResponse.success("Employee relation created successfully", result));
        } catch (Exception e) {
            LoggerUtil.logError("Error creating employee relation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to create employee relation: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Update employee relation
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<EmployeeRelation>> updateEmployeeRelation(@PathVariable Long id, @RequestBody EmployeeRelation employeeRelationDetails) {
        try {
            LoggerUtil.logDebug("Updating employee relation with ID: {}", id);
            EmployeeRelation result = employeeRelationService.updateEmployeeRelation(id, employeeRelationDetails);
            return ResponseEntity.ok(BaseResponse.success("Employee relation updated successfully", result));
        } catch (Exception e) {
            LoggerUtil.logError("Error updating employee relation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to update employee relation: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Delete employee relation by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<String>> deleteEmployeeRelation(@PathVariable Long id) {
        try {
            LoggerUtil.logDebug("Deleting employee relation with ID: {}", id);
            employeeRelationService.deleteEmployeeRelation(id);
            return ResponseEntity.ok(BaseResponse.success("Employee relation deleted successfully", "Deleted successfully"));
        } catch (Exception e) {
            LoggerUtil.logError("Error deleting employee relation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to delete employee relation: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Upload employee relations from CSV file
     */
    @PostMapping("/upload-csv")
    public ResponseEntity<BaseResponse<String>> uploadEmployeeRelationCsv(@RequestParam("file") MultipartFile file) {
        try {
            LoggerUtil.logDebug("Uploading employee relations CSV file: {}", file.getOriginalFilename());
            String result = employeeRelationService.uploadEmployeeRelationCsv(file);
            return ResponseEntity.ok(BaseResponse.success("CSV uploaded successfully", result));
        } catch (Exception e) {
            LoggerUtil.logError("Error uploading employee relations CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to upload CSV: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}