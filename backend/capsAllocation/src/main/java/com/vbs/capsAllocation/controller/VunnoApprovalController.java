package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.*;
import com.vbs.capsAllocation.service.VunnoMgmtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.vbs.capsAllocation.util.LoggerUtil;

@RestController
@RequestMapping("/api/vunno")
public class VunnoApprovalController {

    @Autowired
    private VunnoMgmtService vunnoMgmtService;

    @PostMapping("/approve")
    public ResponseEntity<BaseResponse<String>> approveRequest(@RequestBody VunnoRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        LoggerUtil.logDebug("Approving leave request for ID: {}", requestDto.getId());
        String resultMessage = vunnoMgmtService.approveRequest(requestDto, userDetails);
        return ResponseEntity.ok(BaseResponse.success(resultMessage));
    }

    @PostMapping("/reject")
    public ResponseEntity<BaseResponse<String>> rejectRequest(@RequestBody VunnoRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        LoggerUtil.logDebug("Rejecting leave request for ID: {}", requestDto.getId());
        String resultMessage = vunnoMgmtService.rejectRequest(requestDto, userDetails);
        return ResponseEntity.ok(BaseResponse.success(resultMessage));
    }

    @PostMapping("/revoke")
    public ResponseEntity<BaseResponse<String>> revokeRequest(@RequestBody VunnoRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        LoggerUtil.logDebug("Revoking leave request for ID: {}", requestDto.getId());
        String resultMessage = vunnoMgmtService.revokeRequest(requestDto, userDetails);
        return ResponseEntity.ok(BaseResponse.success(resultMessage));
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @PutMapping("/category-update")
    public ResponseEntity<BaseResponse<String>> leaveCategoryUpdate(
            @RequestBody CategoryUpdateDto categoryUpdateDto, @AuthenticationPrincipal UserDetails userDetails) {
        LoggerUtil.logDebug("Leaving category update in leaveCategoryUpdate endpoint");
        String response = vunnoMgmtService.leaveCategoryUpdate(categoryUpdateDto, userDetails);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get audit history for a specific leave request
     * Only accessible by LEAD, MANAGER, and ADMIN_OPS_MANAGER
     */
    @PreAuthorize("hasRole('USER') or hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/audit-history/{vunnoResponseId}")
    public ResponseEntity<BaseResponse<java.util.List<VunnoAuditDto>>> getAuditHistory(
            @PathVariable Long vunnoResponseId) {
        LoggerUtil.logDebug("Fetching audit history for leave request ID: {}", vunnoResponseId);
        java.util.List<VunnoAuditDto> auditHistory = vunnoMgmtService.getAuditHistory(vunnoResponseId);
        return ResponseEntity.ok(BaseResponse.success("Audit history retrieved successfully", auditHistory));
    }
}