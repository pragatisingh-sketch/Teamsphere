package com.vbs.capsAllocation.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
    // Role field removed for security - will be determined from the database
}
