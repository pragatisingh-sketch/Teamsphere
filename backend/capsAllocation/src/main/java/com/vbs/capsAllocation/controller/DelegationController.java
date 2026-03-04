package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.BaseResponse;
import com.vbs.capsAllocation.dto.DelegationRequestDTO;
import com.vbs.capsAllocation.model.Delegation;
import com.vbs.capsAllocation.service.DelegationService;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delegation")
public class DelegationController {

        @Autowired
        private DelegationService delegationService;

        @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/delegate")
        public ResponseEntity<BaseResponse<String>> delegateRole(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestBody DelegationRequestDTO request) {
                try {
                        // Ensure the logged-in user is the delegator (or admin)
                        if (!userDetails.getUsername().equals(request.getDelegatorLdap()) &&
                                        !userDetails.getAuthorities().stream()
                                                        .anyMatch(a -> a.getAuthority()
                                                                        .equals("ROLE_ADMIN_OPS_MANAGER"))) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(BaseResponse.error("You can only delegate your own role.",
                                                                HttpStatus.FORBIDDEN.value()));
                        }

                        LoggerUtil.logDebug("Delegating role: {} -> {}", request.getDelegatorLdap(),
                                        request.getDelegateeLdap());
                        String result = delegationService.delegateRole(request);
                        return ResponseEntity.ok(BaseResponse.success(result, result));
                } catch (Exception e) {
                        LoggerUtil.logError("Error delegating role: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to delegate role: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @PostMapping("/revert")
        public ResponseEntity<BaseResponse<String>> revertDelegation(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestBody Map<String, String> request) {
                try {
                        String userLdap = request.get("userLdap");

                        // Ensure the logged-in user is the one requesting the revert (or admin)
                        // User can be either delegator or delegatee
                        if (!userDetails.getUsername().equals(userLdap) &&
                                        !userDetails.getAuthorities().stream()
                                                        .anyMatch(a -> a.getAuthority()
                                                                        .equals("ROLE_ADMIN_OPS_MANAGER"))) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(BaseResponse.error("You can only revert your own delegation.",
                                                                HttpStatus.FORBIDDEN.value()));
                        }

                        LoggerUtil.logDebug("Reverting delegation for: {}", userLdap);
                        String result = delegationService.revertDelegation(userLdap);
                        return ResponseEntity.ok(BaseResponse.success(result, result));
                } catch (Exception e) {
                        LoggerUtil.logError("Error reverting delegation: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to revert delegation: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }

        @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
        @GetMapping("/history/{ldap}")
        public ResponseEntity<BaseResponse<List<Delegation>>> getDelegationHistory(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable String ldap) {
                try {
                        LoggerUtil.logDebug("Fetching delegation history for: {}", ldap);
                        return ResponseEntity
                                        .ok(BaseResponse.success("History retrieved",
                                                        delegationService.getDelegationHistory(ldap)));
                } catch (Exception e) {
                        LoggerUtil.logError("Error fetching history: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(BaseResponse.error("Failed to fetch history: " + e.getMessage(),
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
                }
        }
}
