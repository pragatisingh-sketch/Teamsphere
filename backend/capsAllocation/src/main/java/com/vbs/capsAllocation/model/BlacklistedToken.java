package com.vbs.capsAllocation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "blacklisted_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlacklistedToken {
    @Id
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String token;

    private Date expiryDate;
}
