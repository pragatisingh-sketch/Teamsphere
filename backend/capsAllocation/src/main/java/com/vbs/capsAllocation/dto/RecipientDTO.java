package com.vbs.capsAllocation.dto;

public class RecipientDTO {
    private Long id;
    private String email;
    private String name;
    private Boolean isActive;

    public RecipientDTO() {
    }

    public RecipientDTO(Long id, String email, String name, Boolean isActive) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
