package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "release_items")
public class ReleaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReleaseItemType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id", nullable = false)
    @JsonIgnore
    private AppRelease release;

    @OneToMany(mappedBy = "releaseItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("stepOrder ASC")
    private List<ReleaseStep> steps = new ArrayList<>();

    // Constructors
    public ReleaseItem() {
    }

    public ReleaseItem(ReleaseItemType type, String title, String description) {
        this.type = type;
        this.title = title;
        this.description = description;
    }

    // Helper methods
    public void addStep(ReleaseStep step) {
        steps.add(step);
        step.setReleaseItem(this);
    }

    public void removeStep(ReleaseStep step) {
        steps.remove(step);
        step.setReleaseItem(null);
    }

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

    public AppRelease getRelease() {
        return release;
    }

    public void setRelease(AppRelease release) {
        this.release = release;
    }

    public List<ReleaseStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ReleaseStep> steps) {
        this.steps = steps;
    }
}
