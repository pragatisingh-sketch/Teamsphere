package com.vbs.capsAllocation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbs.capsAllocation.dto.BaseResponse;
import com.vbs.capsAllocation.dto.LeaveBalanceUploadResultDto;
import com.vbs.capsAllocation.dto.VunnoHistoryDto;
import com.vbs.capsAllocation.dto.VunnoMgmtDto;
import com.vbs.capsAllocation.dto.VunnoRequestDto;
import com.vbs.capsAllocation.service.ApprovalTokenService;
import com.vbs.capsAllocation.service.EarnedLeaveAccrualService;
import com.vbs.capsAllocation.service.VunnoMgmtService;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/vunno")
public class VunnoMgmtController {

    @Autowired
    private ApprovalTokenService approvalTokenService;

    @Autowired
    private VunnoMgmtService vunnoMgmtService;

    @Autowired
    private EarnedLeaveAccrualService earnedLeaveAccrualService;

    private static final Logger logger = LoggerFactory.getLogger(VunnoMgmtController.class);

    @GetMapping("/currentUserLdap")
    public ResponseEntity<String> getCurrentUserLdap() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal() instanceof String) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not properly authenticated");
        }

        // For JWT authentication, the name should be the LDAP
        String ldap = authentication.getName();
        return ResponseEntity.ok("\"" + ldap + "\""); // Return as JSON string

    }

    // Get All Details of Leads and Manager
    @GetMapping("/lead-manager/{ldap}")
    public List<VunnoMgmtDto> getLeadManagerDetails(@PathVariable String ldap) {
        return vunnoMgmtService.getLeadManagerDetails(ldap);
    }

    // Requesting Vunno into Sheets
    @PostMapping(value = "/requestedVunno", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> requestingLeave(
            @RequestPart("leaveRequest") String leaveRequestJson,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "document", required = false) MultipartFile document) throws Exception {

        logger.info("requestingLeave of controller class started");

        // Parse JSON payload to DTO
        ObjectMapper objectMapper = new ObjectMapper();
        VunnoRequestDto requestDto = objectMapper.readValue(leaveRequestJson, VunnoRequestDto.class);

        // If WFH and document is present, store the document path
        if ("Work From Home".equalsIgnoreCase(requestDto.getApplicationType()) && document != null) {
            String fileName = document.getOriginalFilename();
            String extension = fileName.substring(fileName.lastIndexOf('.'));
            String newFileName = UUID.randomUUID().toString() + extension;
            Path filePath = Paths.get("uploads/" + newFileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, document.getBytes());
            requestDto.setDocumentPath(filePath.toString()); // Assuming your DTO has this field
        } else {
            requestDto.setDocumentPath("NA");
        }
        String response = vunnoMgmtService.requestingLeave(requestDto);
        return ResponseEntity.ok(Collections.singletonMap("message", response));
    }

    @GetMapping("/getLeaveDetails")
    public List<Double> getAllCountOfLeaveWFH(@RequestParam String ldap, Authentication authentication)
            throws GeneralSecurityException, IOException {
        // Security check: Ensure the authenticated user can only check their own leave
        // details
        String authenticatedLdap = authentication.getName();
        if (!authenticatedLdap.equals(ldap)) {
            throw new RuntimeException("Access denied: You can only check your own leave details");
        }
        return vunnoMgmtService.getAllCountOfLeaveWFH(ldap);
    }

    @GetMapping("/getMyLeaveDetails")
    public List<Double> getMyLeaveDetails(Authentication authentication) throws GeneralSecurityException, IOException {
        String authenticatedLdap = authentication.getName();
        return vunnoMgmtService.getAllCountOfLeaveWFH(authenticatedLdap);
    }

    @GetMapping("/getHistoryForUser")
    public List<VunnoHistoryDto> readDataFromGoogleSheet(@RequestParam String ldap, Authentication authentication)
            throws GeneralSecurityException, IOException, InterruptedException {
        // Security check: Ensure the authenticated user can only check their own
        // history
        String authenticatedLdap = authentication.getName();
        if (!authenticatedLdap.equals(ldap)) {
            throw new RuntimeException("Access denied: You can only check your own history");
        }
        return vunnoMgmtService.getFilteredSheetData(ldap);
    }

    @GetMapping("/getMyHistory")
    public List<VunnoHistoryDto> getMyHistory(Authentication authentication)
            throws GeneralSecurityException, IOException, InterruptedException {
        String authenticatedLdap = authentication.getName();
        return vunnoMgmtService.getFilteredSheetData(authenticatedLdap);
    }

    @PostMapping("/upload-leave-balance")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER', 'ADMIN_OPS_MANAGER')")
    public ResponseEntity<LeaveBalanceUploadResultDto> uploadLeaveBalanceFromSheet(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
        try {
            LeaveBalanceUploadResultDto result = vunnoMgmtService.uploadLeaveBalances(file, userDetails, force);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException ise) {
            LeaveBalanceUploadResultDto conflictResult = new LeaveBalanceUploadResultDto(
                    0, 0, Collections.emptyList(), ise.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResult);
        } catch (Exception e) {
            LeaveBalanceUploadResultDto errorResult = new LeaveBalanceUploadResultDto(
                    0, 0, Collections.emptyList(), "Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Upload leave balance and return a downloadable CSV report of skipped
     * employees.
     * If all employees were processed successfully, a CSV with only headers is
     * returned.
     */
    @PostMapping("/upload-leave-balance/report")
    @PreAuthorize("hasAnyRole('LEAD', 'MANAGER', 'ADMIN_OPS_MANAGER')")
    public ResponseEntity<byte[]> uploadLeaveBalanceAndGetReport(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
        try {
            LeaveBalanceUploadResultDto result = vunnoMgmtService.uploadLeaveBalances(file, userDetails, force);
            byte[] csvBytes = vunnoMgmtService.generateSkippedReport(result.getSkippedEmployees());

            String filename = "leave_balance_skipped_report_"
                    + java.time.LocalDate.now().toString() + ".csv";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.add("X-Upload-Message", result.getMessage());
            headers.add("X-Success-Count", String.valueOf(result.getSuccessCount()));
            headers.add("X-Skipped-Count", String.valueOf(result.getSkippedCount()));

            return ResponseEntity.ok().headers(headers).body(csvBytes);
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('USER','LEAD', 'MANAGER', 'ADMIN_OPS_MANAGER')")
    @DeleteMapping("/deleteLeaveRequest/{id}")
    public ResponseEntity<BaseResponse<String>> deleteLeaveRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            LoggerUtil.logDebug("Deleting leave request with ID: {}", id);
            String result = vunnoMgmtService.deleteLeaveRequestWithPermission(id, userDetails);
            return ResponseEntity.ok(BaseResponse.success(result));
        } catch (Exception e) {
            LoggerUtil.logError("Error deleting leave request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to delete leave request: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/updateLeaveRequest/{id}")
    public ResponseEntity<BaseResponse<String>> updateLeaveRequest(
            @PathVariable Long id,
            @RequestPart("request") VunnoRequestDto updateRequest,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "document", required = false) MultipartFile document) {

        LoggerUtil.logDebug("Updating leave request with ID: {}", id);

        if ("Work From Home".equalsIgnoreCase(updateRequest.getApplicationType()) && document != null) {
            String fileName = document.getOriginalFilename();
            String extension = fileName.substring(fileName.lastIndexOf('.'));
            String newFileName = UUID.randomUUID().toString() + extension;
            Path filePath = Paths.get("uploads/" + newFileName);
            try {
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, document.getBytes());
                updateRequest.setDocumentPath(filePath.toString()); // Replace with new path
            } catch (IOException e) {
                LoggerUtil.logError("Error updating leave request: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(BaseResponse.error("Failed to update leave request: " + e.getMessage(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value()));
            }
        }
        String result = vunnoMgmtService.updateLeaveRequestWithPermission(id, updateRequest, userDetails);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/requests-for-approval")
    public ResponseEntity<BaseResponse<List<VunnoRequestDto>>> getApprovableRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam boolean directOnly,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching approvable leave requests for: {}", userDetails.getUsername());
            List<VunnoRequestDto> approvableRequests = vunnoMgmtService.getRequestsForApproval(userDetails, directOnly,
                    startDate, endDate);
            return ResponseEntity.ok(BaseResponse.success("Requests fetched successfully", approvableRequests));
        } catch (AccessDeniedException e) {
            LoggerUtil.logError("Access denied for approver {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN.value()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching approvable leave requests for {}: {}", userDetails.getUsername(),
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to fetch leave requests: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/processed-requests-for-approval")
    public ResponseEntity<BaseResponse<List<VunnoRequestDto>>> getProcessedRequestsForApproval(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "true") boolean directOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LoggerUtil.logDebug("Fetching processed leave requests for: {}", userDetails.getUsername());
            List<VunnoRequestDto> requests = vunnoMgmtService.getProcessedRequestsForApproval(userDetails, status,
                    directOnly, startDate, endDate);
            return ResponseEntity.ok(BaseResponse.success("Processed requests fetched successfully", requests));
        } catch (AccessDeniedException e) {
            LoggerUtil.logError("Access denied for approver {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN.value()));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching processed requests for {}: {}", userDetails.getUsername(),
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to fetch processed requests: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * One-time reconciliation endpoint for months when EL was first uploaded via
     * CSV and monthly accrual needs to be added once.
     */
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/accrue-monthly-el-reconcile-once")
    public ResponseEntity<BaseResponse<String>> reconcileMonthlyELOnce(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("One-time EL reconcile triggered by: {}", userDetails.getUsername());
            String result = earnedLeaveAccrualService.reconcileCurrentMonthELAfterUploadOneTime();
            return ResponseEntity.ok(BaseResponse.success(result));
        } catch (Exception e) {
            LoggerUtil.logError("Error running one-time EL reconcile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to run one-time EL reconcile: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

}
