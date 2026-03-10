package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.*;
import com.vbs.capsAllocation.service.HolidayService;
import com.vbs.capsAllocation.util.LoggerUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for managing holidays in the Time Sheet System
 *
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    @Autowired
    private HolidayService holidayService;

    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping
    public ResponseEntity<BaseResponse<HolidayDTO>> createHoliday(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateHolidayDTO createHolidayDTO) {
        try {
            LoggerUtil.logDebug("Creating holiday: {} by user: {}", createHolidayDTO.getHolidayName(), userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success("Holiday created successfully", 
                            holidayService.createHoliday(createHolidayDTO, userDetails.getUsername()), 
                            HttpStatus.CREATED.value()));
        } catch (Exception e) {
            LoggerUtil.logError("Error creating holiday: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to create holiday: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping
    public ResponseEntity<BaseResponse<List<HolidayDTO>>> getAllActiveHolidays() {
        try {
            LoggerUtil.logDebug("Fetching all active holidays");
            return ResponseEntity.ok(BaseResponse.success("Holidays retrieved successfully", 
                    holidayService.getAllActiveHolidays()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching holidays: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve holidays: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/year/{year}")
    public ResponseEntity<BaseResponse<List<HolidayDTO>>> getHolidaysByYear(@PathVariable int year) {
        try {
            LoggerUtil.logDebug("Fetching holidays for year: {}", year);
            return ResponseEntity.ok(BaseResponse.success("Holidays retrieved successfully", 
                    holidayService.getHolidaysByYear(year)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching holidays for year {}: {}", year, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve holidays: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/check")
    public ResponseEntity<BaseResponse<Boolean>> isHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            LoggerUtil.logDebug("Checking if {} is a holiday", date);
            boolean isHoliday = holidayService.isHoliday(date);
            return ResponseEntity.ok(BaseResponse.success("Holiday check completed", isHoliday));
        } catch (Exception e) {
            LoggerUtil.logError("Error checking holiday for date {}: {}", date, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to check holiday: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/date/{date}")
    public ResponseEntity<BaseResponse<HolidayDTO>> getHolidayByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            LoggerUtil.logDebug("Fetching holiday for date: {}", date);
            return ResponseEntity.ok(BaseResponse.success("Holiday retrieved successfully", 
                    holidayService.getHolidayByDate(date)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching holiday for date {}: {}", date, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve holiday: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @PutMapping("/{holidayId}")
    public ResponseEntity<BaseResponse<HolidayDTO>> updateHoliday(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long holidayId,
            @Valid @RequestBody CreateHolidayDTO updateHolidayDTO) {
        try {
            LoggerUtil.logDebug("Updating holiday: {} by user: {}", holidayId, userDetails.getUsername());
            return ResponseEntity.ok()
                    .body(BaseResponse.success("Holiday updated successfully", 
                            holidayService.updateHoliday(holidayId, updateHolidayDTO, userDetails.getUsername())));
        } catch (Exception e) {
            LoggerUtil.logError("Error updating holiday: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to update holiday: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @DeleteMapping("/{holidayId}")
    public ResponseEntity<BaseResponse<String>> deleteHoliday(@PathVariable Long holidayId) {
        try {
            LoggerUtil.logDebug("Deleting holiday with ID: {}", holidayId);
            holidayService.deleteHoliday(holidayId);
            return ResponseEntity.ok(BaseResponse.success("Holiday deleted successfully"));
        } catch (Exception e) {
            LoggerUtil.logError("Error deleting holiday: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to delete holiday: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/upload-csv")
    public ResponseEntity<BaseResponse<List<HolidayDTO>>> uploadHolidaysFromCSV(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        try {
            LoggerUtil.logDebug("Uploading holidays from CSV by user: {}", userDetails.getUsername());
            List<HolidayDTO> holidays = holidayService.uploadHolidaysFromCSV(file, userDetails.getUsername());
            return ResponseEntity.ok(BaseResponse.success(
                    "Successfully uploaded " + holidays.size() + " holidays from CSV", holidays));
        } catch (Exception e) {
            LoggerUtil.logError("Error uploading holidays from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to upload holidays: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/type/{holidayType}")
    public ResponseEntity<BaseResponse<List<HolidayDTO>>> getHolidaysByType(@PathVariable String holidayType) {
        try {
            LoggerUtil.logDebug("Fetching holidays by type: {}", holidayType);
            return ResponseEntity.ok(BaseResponse.success("Holidays retrieved successfully", 
                    holidayService.getHolidaysByType(holidayType)));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching holidays by type {}: {}", holidayType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve holidays: " + e.getMessage(), 
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
