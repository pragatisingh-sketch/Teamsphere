package com.vbs.capsAllocation.dto;

import com.vbs.capsAllocation.model.ReleaseItemType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReleaseDTO {

    private Long id;
    private String version;
    private String title;
    private LocalDate releaseDate;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean notificationSent;
    private LocalDateTime notificationSentAt;
    private List<ReleaseItemDTO> releaseItems;

    // Nested DTO for release items
    public static class ReleaseItemDTO {
        private Long id;
        private ReleaseItemType type;
        private String title;
        private String description;
        private List<ReleaseStepDTO> steps;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public ReleaseItemType getType() {
            return type;
        }

        public void setType(ReleaseItemType type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<ReleaseStepDTO> getSteps() {
            return steps;
        }

        public void setSteps(List<ReleaseStepDTO> steps) {
            this.steps = steps;
        }
    }

    // Nested DTO for release steps
    public static class ReleaseStepDTO {
        private Long id;
        private Integer stepOrder;
        private String explanation;
        private String screenshotUrl;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getStepOrder() {
            return stepOrder;
        }

        public void setStepOrder(Integer stepOrder) {
            this.stepOrder = stepOrder;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public String getScreenshotUrl() {
            return screenshotUrl;
        }

        public void setScreenshotUrl(String screenshotUrl) {
            this.screenshotUrl = screenshotUrl;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public LocalDateTime getNotificationSentAt() {
        return notificationSentAt;
    }

    public void setNotificationSentAt(LocalDateTime notificationSentAt) {
        this.notificationSentAt = notificationSentAt;
    }

    public List<ReleaseItemDTO> getReleaseItems() {
        return releaseItems;
    }

    public void setReleaseItems(List<ReleaseItemDTO> releaseItems) {
        this.releaseItems = releaseItems;
    }
}
