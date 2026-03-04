package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.BaseResponse;
import com.vbs.capsAllocation.dto.ReleaseDTO;
import com.vbs.capsAllocation.dto.RecipientDTO;
import com.vbs.capsAllocation.dto.SendNotificationRequest;
import com.vbs.capsAllocation.service.ReleaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/releases")
public class ReleaseController {

    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    /**
     * Get current LDAP from security context
     */
    private String getCurrentLdap() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    /**
     * Check if current user is authorized
     */
    private boolean isAuthorized() {
        return releaseService.isAuthorized(getCurrentLdap());
    }

    /**
     * Get current application version (PUBLIC endpoint)
     */
    @GetMapping("/version")
    public ResponseEntity<BaseResponse<Map<String, String>>> getCurrentVersion() {
        String version = releaseService.getCurrentVersion();
        return ResponseEntity.ok(BaseResponse.success("Version retrieved", Map.of("version", version)));
    }

    /**
     * Get all releases
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<ReleaseDTO>>> getAllReleases() {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied. Only authorized users can access release management.",
                            HttpStatus.FORBIDDEN.value()));
        }
        List<ReleaseDTO> releases = releaseService.getAllReleases();
        return ResponseEntity.ok(BaseResponse.success("Releases retrieved", releases));
    }

    /**
     * Get specific release by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ReleaseDTO>> getReleaseById(@PathVariable Long id) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied.", HttpStatus.FORBIDDEN.value()));
        }
        return releaseService.getReleaseById(id)
                .map(release -> ResponseEntity.ok(BaseResponse.success("Release retrieved", release)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error("Release not found with id: " + id, HttpStatus.NOT_FOUND.value())));
    }

    /**
     * Create a new release
     */
    @PostMapping
    public ResponseEntity<BaseResponse<ReleaseDTO>> createRelease(@RequestBody ReleaseDTO dto) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied.", HttpStatus.FORBIDDEN.value()));
        }
        try {
            ReleaseDTO created = releaseService.createRelease(dto, getCurrentLdap());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success("Release created", created, HttpStatus.CREATED.value()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    /**
     * Update an existing release
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<ReleaseDTO>> updateRelease(
            @PathVariable Long id,
            @RequestBody ReleaseDTO dto) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied.", HttpStatus.FORBIDDEN.value()));
        }
        try {
            ReleaseDTO updated = releaseService.updateRelease(id, dto);
            return ResponseEntity.ok(BaseResponse.success("Release updated", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    /**
     * Delete a release
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteRelease(@PathVariable Long id) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied.", HttpStatus.FORBIDDEN.value()));
        }
        try {
            releaseService.deleteRelease(id);
            return ResponseEntity.ok(BaseResponse.success("Release deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    /**
     * Send release notification email
     */
    @PostMapping("/{id}/notify")
    public ResponseEntity<BaseResponse<Void>> sendNotification(
            @PathVariable Long id,
            @RequestBody SendNotificationRequest request) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied.", HttpStatus.FORBIDDEN.value()));
        }
        try {
            releaseService.sendNotification(id, request.getRecipientEmails());
            return ResponseEntity.ok(BaseResponse.success("Notification sent successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    // ===== Email Recipient Management =====

    /**
     * Get all saved recipients
     */
    @GetMapping("/recipients")
    public ResponseEntity<BaseResponse<List<RecipientDTO>>> getAllRecipients() {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied.", HttpStatus.FORBIDDEN.value()));
        }
        List<RecipientDTO> recipients = releaseService.getAllRecipients();
        return ResponseEntity.ok(BaseResponse.success("Recipients retrieved", recipients));
    }

    /**
     * Add a new recipient
     */
    @PostMapping("/recipients")
    public ResponseEntity<BaseResponse<RecipientDTO>> addRecipient(@RequestBody RecipientDTO dto) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied.", HttpStatus.FORBIDDEN.value()));
        }
        try {
            RecipientDTO created = releaseService.addRecipient(dto.getEmail(), dto.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success("Recipient added", created, HttpStatus.CREATED.value()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    /**
     * Delete a recipient
     */
    @DeleteMapping("/recipients/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteRecipient(@PathVariable Long id) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Access denied.", HttpStatus.FORBIDDEN.value()));
        }
        releaseService.deleteRecipient(id);
        return ResponseEntity.ok(BaseResponse.success("Recipient deleted successfully"));
    }
}
