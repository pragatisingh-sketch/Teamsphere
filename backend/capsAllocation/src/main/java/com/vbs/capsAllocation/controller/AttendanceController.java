package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.*;
import com.vbs.capsAllocation.model.Attendance;
import com.vbs.capsAllocation.model.ShiftDetails;
import com.vbs.capsAllocation.repository.AttendanceRepository;
import com.vbs.capsAllocation.repository.ShiftDetailsRepository;
import com.vbs.capsAllocation.service.AttendanceAuditService;
import com.vbs.capsAllocation.service.AttendanceService;
import com.vbs.capsAllocation.service.EmployeeService;
import com.vbs.capsAllocation.util.LoggerUtil;
import com.vbs.capsAllocation.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/atom")
public class AttendanceController {

    @Autowired
    private ShiftDetailsRepository shiftDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceAuditService attendanceAuditService;

    private Executor checkoutExecutor;

    public AttendanceController(@Qualifier("checkoutExecutor") Executor checkoutExecutor) {
        this.checkoutExecutor = checkoutExecutor;
    }

    private static final Logger logger = LoggerFactory.getLogger(AttendanceController.class);

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

    @PostMapping("/mark")
    public ResponseEntity<Attendance> markAttendance(@RequestBody AttendanceRequest request) {
        try {
            Attendance result = attendanceService.markAttendance(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<BaseResponse<CheckoutResponseDto>> checkout(@RequestBody AttendanceRequest request) {
        logger.info("Checkout API called for employee: {}", request.getLdap());
        logger.debug("Checkout request payload: [Ldap: {}, Lat: {}, Lon: {}, Reason: {}, Comment: {}]",
                request.getLdap(),
                request.getLatitude(),
                request.getLongitude(),
                request.getReason(),
                request.getComment());

        try {
            // Run checkout synchronously
            CheckoutResponseDto responseDto = attendanceService.checkoutAttendence(request);

            logger.info("✅ Checkout completed successfully for employee: {}", request.getLdap());
            return ResponseEntity.ok(BaseResponse.success("Checkout recorded successfully", responseDto));

        } catch (Exception ex) {
            logger.error("❌ Checkout failed for {}: {}", request.getLdap(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.error("Checkout failed: " + ex.getMessage(), 400));
        }
    }

    @GetMapping("/checkingIn")
    public CheckInStatusResponse getMyCheckInStatus(@AuthenticationPrincipal UserDetails user) {
        String ldap = user.getUsername();
        System.out.println("USER " + user);
        logger.debug("Getting check-in status for authenticated user: {}", ldap);
        return attendanceService.getCheckInStatus(ldap);
    }

    @GetMapping("/records/{ldap}")
    @PreAuthorize("#ldap == authentication.name")
    public ResponseEntity<List<Attendance>> getAttendanceByLdap(@PathVariable String ldap) {
        List<Attendance> records = attendanceRepository.findByEmployeeLdapOrderByEntryDateDesc(ldap);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/records")
    public ResponseEntity<List<Attendance>> getMyAttendanceRecords(@AuthenticationPrincipal UserDetails user) {
        String ldap = user.getUsername();
        logger.debug("Getting attendance records for authenticated user: {}", ldap);
        List<Attendance> records = attendanceRepository.findByEmployeeLdapOrderByEntryDateDesc(ldap);
        return ResponseEntity.ok(records);
    }

    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ACCOUNT_MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping
    public ResponseEntity<BaseResponse<List<AttendanceResponseDto>>> getMyAttendance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "true") boolean directOnly,
            @RequestParam(defaultValue = "false") boolean self) {
        try {
            String ldap = userDetails.getUsername();
            LoggerUtil.logDebug("Fetching attendance for user: {} from {} to {} (isFilter={}, directOnly={})",
                    ldap, startDate, endDate, directOnly);

            List<AttendanceResponseDto> records = attendanceService.getAttendanceRecords(ldap, startDate, endDate,
                    directOnly, self);

            return ResponseEntity.ok(
                    BaseResponse.success("Attendance records retrieved successfully", records));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching attendance records: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve attendance records: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Update attendance status
     */
    @PutMapping("/update-status/{attendanceId}")
    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<Attendance>> updateAttendanceStatus(
            @PathVariable Long attendanceId,
            @RequestBody AttendanceUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Attendance updatedAttendance = attendanceService.updateAttendanceStatus(
                    attendanceId,
                    request.getNewStatus(),
                    request.getReason(),
                    userDetails);

            return ResponseEntity.ok(
                    BaseResponse.success("Attendance status updated successfully", updatedAttendance));
        } catch (Exception e) {
            LoggerUtil.logError("Error updating attendance status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to update attendance status: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Update compliance status
     */
    @PutMapping("/update-compliance/{attendanceId}")
    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<Attendance>> updateComplianceStatus(
            @PathVariable Long attendanceId,
            @RequestBody AttendanceUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Attendance updatedAttendance = attendanceService.updateComplianceStatus(
                    attendanceId,
                    request.getIsDefaulter(),
                    request.getReason(),
                    userDetails);

            return ResponseEntity.ok(
                    BaseResponse.success("Compliance status updated successfully", updatedAttendance));
        } catch (Exception e) {
            LoggerUtil.logError("Error updating compliance status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to update compliance status: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get audit history for an attendance record
     */
    @GetMapping("/audit-history/{attendanceId}")
    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<List<AttendanceAuditDto>>> getAuditHistory(
            @PathVariable Long attendanceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<AttendanceAuditDto> auditHistory = attendanceAuditService.getAuditHistory(attendanceId);

            return ResponseEntity.ok(
                    BaseResponse.success("Audit history retrieved successfully", auditHistory));
        } catch (Exception e) {
            LoggerUtil.logError("Error fetching audit history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve audit history: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/shift-details/{code}")
    public ResponseEntity<ShiftDetails> getShiftDetailsByCode(@PathVariable String code) {
        ShiftDetails shift = shiftDetailsRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));
        return ResponseEntity.ok(shift);
    }

}
