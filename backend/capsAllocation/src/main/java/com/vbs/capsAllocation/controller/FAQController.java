package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.*;
import com.vbs.capsAllocation.model.FAQ;
import com.vbs.capsAllocation.service.FAQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faqs")
public class FAQController {

    @Autowired
    private FAQService faqService;

    /**
     * Get all active FAQs (public endpoint for all users)
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<FAQ>>> getActiveFAQs() {
        List<FAQ> faqs = faqService.getActiveFAQs();
        return ResponseEntity.ok(BaseResponse.success("FAQs retrieved successfully", faqs));
    }

    /**
     * Get all FAQs including inactive (for managers)
     */
    @GetMapping("/all")
    public ResponseEntity<BaseResponse<List<FAQ>>> getAllFAQs(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        if (!faqService.canManageFAQs(username)) {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error("You are not authorized to view all FAQs", 403));
        }
        List<FAQ> faqs = faqService.getAllFAQs();
        return ResponseEntity.ok(BaseResponse.success("All FAQs retrieved successfully", faqs));
    }

    /**
     * Check if current user can manage FAQs
     */
    @GetMapping("/can-manage")
    public ResponseEntity<BaseResponse<Map<String, Boolean>>> canManageFAQs(
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        Map<String, Boolean> result = new HashMap<>();
        result.put("canManage", faqService.canManageFAQs(username));
        return ResponseEntity.ok(BaseResponse.success("Permission checked", result));
    }

    /**
     * Create a new FAQ
     */
    @PostMapping
    public ResponseEntity<BaseResponse<FAQ>> createFAQ(
            @RequestBody FAQ faq,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String username = userDetails.getUsername();
            FAQ created = faqService.createFAQ(faq, username);
            return ResponseEntity.ok(BaseResponse.success("FAQ created successfully", created));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(BaseResponse.error(e.getMessage(), 403));
        }
    }

    /**
     * Update an existing FAQ
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<FAQ>> updateFAQ(
            @PathVariable Long id,
            @RequestBody FAQ faq,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String username = userDetails.getUsername();
            FAQ updated = faqService.updateFAQ(id, faq, username);
            return ResponseEntity.ok(BaseResponse.success("FAQ updated successfully", updated));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(BaseResponse.error(e.getMessage(), 403));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(BaseResponse.error(e.getMessage(), 404));
        }
    }

    /**
     * Delete a FAQ
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteFAQ(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String username = userDetails.getUsername();
            faqService.deleteFAQ(id, username);
            return ResponseEntity.ok(BaseResponse.success("FAQ deleted successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(BaseResponse.error(e.getMessage(), 403));
        }
    }

    /**
     * Initialize default FAQs
     */
    @PostMapping("/initialize")
    public ResponseEntity<BaseResponse<Void>> initializeDefaultFAQs(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String username = userDetails.getUsername();
            if (!faqService.canManageFAQs(username)) {
                return ResponseEntity.status(403)
                        .body(BaseResponse.error("You are not authorized to initialize FAQs", 403));
            }
            faqService.initializeDefaultFAQs(username);
            return ResponseEntity.ok(BaseResponse.success("Default FAQs initialized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(BaseResponse.error(e.getMessage(), 500));
        }
    }
}
