package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "release_steps")
public class ReleaseStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @Column(length = 500)
    private String screenshotUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_item_id", nullable = false)
    @JsonIgnore
    private ReleaseItem releaseItem;

    // Constructors
    public ReleaseStep() {
    }

    public ReleaseStep(Integer stepOrder, String explanation, String screenshotUrl) {
        this.stepOrder = stepOrder;
        this.explanation = explanation;
        this.screenshotUrl = screenshotUrl;
    }

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

    public ReleaseItem getReleaseItem() {
        return releaseItem;
    }

    public void setReleaseItem(ReleaseItem releaseItem) {
        this.releaseItem = releaseItem;
    }
}
