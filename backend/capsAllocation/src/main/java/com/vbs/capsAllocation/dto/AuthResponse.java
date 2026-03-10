package com.vbs.capsAllocation.dto;

import lombok.Data;
import lombok.Setter;

@Data
public class AuthResponse {
    private String token;
    private String role;
    private String username;
    @Setter
    private boolean passwordChangeRequired;

    public AuthResponse(String token, String role,String username) {
        this.token = token;
        this.role=role;
        this.username=username;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

}
