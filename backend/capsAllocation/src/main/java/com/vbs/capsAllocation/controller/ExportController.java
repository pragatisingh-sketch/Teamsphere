package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.BaseResponse;
import com.vbs.capsAllocation.dto.ExportRequest;
import com.vbs.capsAllocation.dto.ExportResponse;
import com.vbs.capsAllocation.service.ExportService;
import com.vbs.capsAllocation.util.LoggerUtil;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling export requests (Excel/CSV)
 *
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/api/exports")
public class ExportController {

        @Autowired
        private ExportService exportService;

        /**
         * Download export as Excel file
         *
         * @param userDetails   Authenticated user details
         * @param exportRequest Export request containing type, date range, and filters
         * @return Excel file with appropriate headers
         */
        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/download")
        public ResponseEntity<?> downloadExport(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody ExportRequest exportRequest) {
                try {
                        LoggerUtil.logDebug("Export request received: type={}, user={}",
                                        exportRequest.getType(),
                                        userDetails.getUsername());

                        // Check if export type is ATTENDANCE_TRACKER and verify user role
                        if ("ATTENDANCE_TRACKER".equals(exportRequest.getType())) {
                                boolean hasPermission = userDetails.getAuthorities().stream()
                                                .map(GrantedAuthority::getAuthority)
                                                .anyMatch(role -> role.equals("ROLE_MANAGER")
                                                                || role.equals("ROLE_ADMIN_OPS_MANAGER"));

                                if (!hasPermission) {
                                        LoggerUtil.logWarn(ExportController.class,
                                                        "Unauthorized attendance tracker export attempt by user: {}",
                                                        userDetails.getUsername());
                                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                        .body(BaseResponse.error(
                                                                        "Access denied. Only Managers and Admin Ops Managers can export attendance tracker.",
                                                                        HttpStatus.FORBIDDEN.value()));
                                }
                        }

                        ExportResponse exportResponse = exportService.generateExport(
                                        exportRequest.getType(),
                                        exportRequest.getStartDate(),
                                        exportRequest.getEndDate(),
                                        userDetails.getUsername(),
                                        exportRequest.getFilters());

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.parseMediaType(exportResponse.getContentType()));
                        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\"" + exportResponse.getFilename() + "\"");
                        headers.setContentLength(exportResponse.getFileData().length);

                        return ResponseEntity.ok()
                                        .headers(headers)
                                        .body(exportResponse.getFileData());

                } catch (IllegalArgumentException e) {
                        LoggerUtil.logError("Invalid export type: {}", exportRequest.getType(), e);
                        return ResponseEntity.badRequest()
                                        .body(BaseResponse.error("Invalid export type: " + exportRequest.getType(),
                                                        HttpStatus.BAD_REQUEST.value()));

                } catch (Exception e) {
                        LoggerUtil.logError("Error generating export: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to generate export: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }
}
