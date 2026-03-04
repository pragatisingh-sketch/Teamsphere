package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.ReleaseDTO;
import com.vbs.capsAllocation.dto.ReleaseDTO.ReleaseItemDTO;
import com.vbs.capsAllocation.dto.ReleaseDTO.ReleaseStepDTO;
import com.vbs.capsAllocation.dto.RecipientDTO;
import com.vbs.capsAllocation.model.*;
import com.vbs.capsAllocation.repository.AppReleaseRepository;
import com.vbs.capsAllocation.repository.ReleaseEmailRecipientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReleaseService {

    private final AppReleaseRepository releaseRepository;
    private final ReleaseEmailRecipientRepository recipientRepository;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    // Authorized LDAPs for release management
    private static final Set<String> AUTHORIZED_LDAPS = Set.of("piyushmi", "vrajoriya");

    public ReleaseService(AppReleaseRepository releaseRepository,
            ReleaseEmailRecipientRepository recipientRepository,
            EmailService emailService) {
        this.releaseRepository = releaseRepository;
        this.recipientRepository = recipientRepository;
        this.emailService = emailService;
    }

    /**
     * Check if the given LDAP is authorized for release management
     */
    public boolean isAuthorized(String ldap) {
        return ldap != null && AUTHORIZED_LDAPS.contains(ldap.toLowerCase());
    }

    /**
     * Get the current (latest) version string
     */
    public String getCurrentVersion() {
        return releaseRepository.findTopByOrderByReleaseDateDesc()
                .map(AppRelease::getVersion)
                .orElse("1.0.0");
    }

    /**
     * Get all releases
     */
    public List<ReleaseDTO> getAllReleases() {
        return releaseRepository.findAllByOrderByReleaseDateDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific release by ID
     */
    public Optional<ReleaseDTO> getReleaseById(Long id) {
        return releaseRepository.findById(id).map(this::toDTO);
    }

    /**
     * Create a new release
     */
    @Transactional
    public ReleaseDTO createRelease(ReleaseDTO dto, String createdBy) {
        if (releaseRepository.existsByVersion(dto.getVersion())) {
            throw new IllegalArgumentException("Version " + dto.getVersion() + " already exists");
        }

        AppRelease release = new AppRelease(
                dto.getVersion(),
                dto.getTitle(),
                dto.getReleaseDate(),
                createdBy);

        // Add release items
        if (dto.getReleaseItems() != null) {
            for (ReleaseItemDTO itemDTO : dto.getReleaseItems()) {
                ReleaseItem item = new ReleaseItem(
                        itemDTO.getType(),
                        itemDTO.getTitle(),
                        itemDTO.getDescription());

                // Add steps to item
                if (itemDTO.getSteps() != null) {
                    for (ReleaseStepDTO stepDTO : itemDTO.getSteps()) {
                        ReleaseStep step = new ReleaseStep(
                                stepDTO.getStepOrder(),
                                stepDTO.getExplanation(),
                                stepDTO.getScreenshotUrl());
                        item.addStep(step);
                    }
                }

                release.addReleaseItem(item);
            }
        }

        AppRelease saved = releaseRepository.save(release);
        return toDTO(saved);
    }

    /**
     * Update an existing release
     */
    @Transactional
    public ReleaseDTO updateRelease(Long id, ReleaseDTO dto) {
        AppRelease release = releaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Release not found with id: " + id));

        // Check version uniqueness if changed
        if (!release.getVersion().equals(dto.getVersion()) && releaseRepository.existsByVersion(dto.getVersion())) {
            throw new IllegalArgumentException("Version " + dto.getVersion() + " already exists");
        }

        release.setVersion(dto.getVersion());
        release.setTitle(dto.getTitle());
        release.setReleaseDate(dto.getReleaseDate());

        // Clear and rebuild items
        release.getReleaseItems().clear();

        if (dto.getReleaseItems() != null) {
            for (ReleaseItemDTO itemDTO : dto.getReleaseItems()) {
                ReleaseItem item = new ReleaseItem(
                        itemDTO.getType(),
                        itemDTO.getTitle(),
                        itemDTO.getDescription());

                if (itemDTO.getSteps() != null) {
                    for (ReleaseStepDTO stepDTO : itemDTO.getSteps()) {
                        ReleaseStep step = new ReleaseStep(
                                stepDTO.getStepOrder(),
                                stepDTO.getExplanation(),
                                stepDTO.getScreenshotUrl());
                        item.addStep(step);
                    }
                }

                release.addReleaseItem(item);
            }
        }

        AppRelease saved = releaseRepository.save(release);
        return toDTO(saved);
    }

    /**
     * Delete a release
     */
    @Transactional
    public void deleteRelease(Long id) {
        if (!releaseRepository.existsById(id)) {
            throw new IllegalArgumentException("Release not found with id: " + id);
        }
        releaseRepository.deleteById(id);
    }

    /**
     * Send release notification email
     */
    @Transactional
    public void sendNotification(Long releaseId, List<String> recipientEmails) {
        AppRelease release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release not found with id: " + releaseId));

        if (recipientEmails == null || recipientEmails.isEmpty()) {
            throw new IllegalArgumentException("No recipients specified");
        }

        // Check if this is an update (re-notification)
        boolean isUpdate = release.getNotificationSent() != null && release.getNotificationSent();
        String subject = (isUpdate ? "[UPDATE] " : "") + "TeamSphere v" + release.getVersion() + " - "
                + release.getTitle();
        String htmlContent = buildEmailContent(release, isUpdate);

        // Send email
        emailService.sendEmail(recipientEmails, null, subject, htmlContent);

        // Mark as sent
        release.setNotificationSent(true);
        release.setNotificationSentAt(LocalDateTime.now());
        releaseRepository.save(release);
    }

    /**
     * Build HTML email content for release notification
     */
    private String buildEmailContent(AppRelease release, boolean isUpdate) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append(
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }");
        html.append(
                ".container { max-width: 800px; margin: 20px auto; background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); overflow: hidden; }");
        html.append(
                ".header { background: linear-gradient(135deg, #005cbf 0%, #003d82 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; }");
        html.append(
                ".version-badge { display: inline-block; background: rgba(255,255,255,0.2); padding: 8px 16px; border-radius: 20px; margin-top: 10px; font-size: 16px; }");
        html.append(".content { padding: 30px; }");
        html.append(".section { margin-bottom: 30px; }");
        html.append(
                ".section-title { font-size: 20px; color: #333; border-bottom: 2px solid #e0e0e0; padding-bottom: 10px; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; }");
        html.append(".bug-fix { color: #e53e3e; }");
        html.append(".enhancement { color: #38a169; }");
        html.append(".feature { color: #3182ce; }");
        html.append(
                ".item { background: #f9f9f9; border-radius: 8px; padding: 20px; margin-bottom: 15px; border-left: 4px solid #005cbf; }");
        html.append(".item-title { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 10px; }");
        html.append(".item-description { color: #666; margin-bottom: 15px; }");
        html.append(
                ".step { background: white; border-radius: 6px; padding: 15px; margin: 10px 0; border: 1px solid #e0e0e0; }");
        html.append(
                ".step-number { display: inline-block; background: #005cbf; color: white; width: 24px; height: 24px; border-radius: 50%; text-align: center; line-height: 24px; font-size: 12px; margin-right: 10px; }");
        html.append(".step-explanation { color: #444; }");
        html.append(
                ".screenshot { max-width: 100%; border-radius: 8px; margin-top: 10px; border: 1px solid #e0e0e0; }");
        html.append(
                ".footer { background: #f5f5f5; padding: 20px; text-align: center; color: #666; font-size: 14px; }");
        html.append(
                ".btn { display: inline-block; background: #005cbf; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; margin-top: 15px; }");
        html.append("</style></head><body>");

        // Header
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>TeamSphere Release Notes</h1>");
        html.append("<div class='version-badge'>Version ").append(release.getVersion()).append("</div>");
        html.append("<p style='margin-top:15px;opacity:0.9;'>").append(release.getTitle()).append("</p>");
        html.append("<p style='margin:5px 0;opacity:0.7;font-size:14px;'>Released on ")
                .append(release.getReleaseDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                .append("</p>");
        html.append("</div>");

        html.append("<div class='content'>");

        // Group items by type
        Map<ReleaseItemType, List<ReleaseItem>> itemsByType = release.getReleaseItems().stream()
                .collect(Collectors.groupingBy(ReleaseItem::getType));

        // Bug Fixes
        if (itemsByType.containsKey(ReleaseItemType.BUG_FIX) || itemsByType.containsKey(ReleaseItemType.HOTFIX)) {
            html.append("<div class='section'>");
            html.append("<div class='section-title bug-fix'>🐛 Bug Fixes</div>");

            List<ReleaseItem> bugFixes = new ArrayList<>();
            if (itemsByType.containsKey(ReleaseItemType.BUG_FIX))
                bugFixes.addAll(itemsByType.get(ReleaseItemType.BUG_FIX));
            if (itemsByType.containsKey(ReleaseItemType.HOTFIX))
                bugFixes.addAll(itemsByType.get(ReleaseItemType.HOTFIX));

            for (ReleaseItem item : bugFixes) {
                appendItemHtml(html, item);
            }
            html.append("</div>");
        }

        // Enhancements
        if (itemsByType.containsKey(ReleaseItemType.ENHANCEMENT)) {
            html.append("<div class='section'>");
            html.append("<div class='section-title enhancement'>✨ Enhancements</div>");
            for (ReleaseItem item : itemsByType.get(ReleaseItemType.ENHANCEMENT)) {
                appendItemHtml(html, item);
            }
            html.append("</div>");
        }

        // New Features
        if (itemsByType.containsKey(ReleaseItemType.FEATURE)) {
            html.append("<div class='section'>");
            html.append("<div class='section-title feature'>🚀 New Features</div>");
            for (ReleaseItem item : itemsByType.get(ReleaseItemType.FEATURE)) {
                appendItemHtml(html, item);
            }
            html.append("</div>");
        }

        html.append("</div>"); // content

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Thank you for using TeamSphere!</p>");
        html.append("<a href='").append(appBaseUrl)
                .append("' class='btn' style='display:inline-block;background:#005cbf;color:#ffffff !important;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:600;'>Open TeamSphere</a>");
        html.append("</div>");

        html.append("</div>"); // container
        html.append("</body></html>");

        return html.toString();
    }

    private void appendItemHtml(StringBuilder html, ReleaseItem item) {
        html.append("<div class='item'>");
        html.append("<div class='item-title'>").append(escapeHtml(item.getTitle())).append("</div>");
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            html.append("<div class='item-description'>").append(escapeHtml(item.getDescription())).append("</div>");
        }

        if (item.getSteps() != null && !item.getSteps().isEmpty()) {
            for (ReleaseStep step : item.getSteps()) {
                html.append("<div class='step'>");
                html.append("<span class='step-number'>").append(step.getStepOrder()).append("</span>");
                html.append("<span class='step-explanation'>").append(escapeHtml(step.getExplanation()))
                        .append("</span>");
                if (step.getScreenshotUrl() != null && !step.getScreenshotUrl().isEmpty()) {
                    html.append("<br/><img class='screenshot' src='").append(step.getScreenshotUrl())
                            .append("' alt='Screenshot'/>");
                }
                html.append("</div>");
            }
        }

        html.append("</div>");
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ===== Email Recipient Management =====

    public List<RecipientDTO> getAllRecipients() {
        return recipientRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(r -> new RecipientDTO(r.getId(), r.getEmail(), r.getName(), r.getIsActive()))
                .collect(Collectors.toList());
    }

    @Transactional
    public RecipientDTO addRecipient(String email, String name) {
        if (recipientRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        ReleaseEmailRecipient recipient = new ReleaseEmailRecipient(email, name);
        recipient = recipientRepository.save(recipient);
        return new RecipientDTO(recipient.getId(), recipient.getEmail(), recipient.getName(), recipient.getIsActive());
    }

    @Transactional
    public void deleteRecipient(Long id) {
        recipientRepository.deleteById(id);
    }

    // ===== DTO Mapping =====

    private ReleaseDTO toDTO(AppRelease release) {
        ReleaseDTO dto = new ReleaseDTO();
        dto.setId(release.getId());
        dto.setVersion(release.getVersion());
        dto.setTitle(release.getTitle());
        dto.setReleaseDate(release.getReleaseDate());
        dto.setCreatedBy(release.getCreatedBy());
        dto.setCreatedAt(release.getCreatedAt());
        dto.setUpdatedAt(release.getUpdatedAt());
        dto.setNotificationSent(release.getNotificationSent());
        dto.setNotificationSentAt(release.getNotificationSentAt());

        if (release.getReleaseItems() != null) {
            List<ReleaseItemDTO> itemDTOs = new ArrayList<>();
            for (ReleaseItem item : release.getReleaseItems()) {
                ReleaseItemDTO itemDTO = new ReleaseItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setType(item.getType());
                itemDTO.setTitle(item.getTitle());
                itemDTO.setDescription(item.getDescription());

                if (item.getSteps() != null) {
                    List<ReleaseStepDTO> stepDTOs = new ArrayList<>();
                    for (ReleaseStep step : item.getSteps()) {
                        ReleaseStepDTO stepDTO = new ReleaseStepDTO();
                        stepDTO.setId(step.getId());
                        stepDTO.setStepOrder(step.getStepOrder());
                        stepDTO.setExplanation(step.getExplanation());
                        stepDTO.setScreenshotUrl(step.getScreenshotUrl());
                        stepDTOs.add(stepDTO);
                    }
                    itemDTO.setSteps(stepDTOs);
                }
                itemDTOs.add(itemDTO);
            }
            dto.setReleaseItems(itemDTOs);
        }

        return dto;
    }
}
