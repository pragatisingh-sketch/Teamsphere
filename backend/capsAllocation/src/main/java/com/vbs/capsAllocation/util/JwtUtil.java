package com.vbs.capsAllocation.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Claims;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:43200000}") // Default: 12 hours
    private long expirationMs;

    @Value("${jwt.issuer:teamsphere-api}")
    private String issuer;

    @Value("${jwt.audience:teamsphere-client}")
    private String audience;

    private Key getSigningKey() {
        return new SecretKeySpec(secret.getBytes(), SignatureAlgorithm.HS256.getJcaName());
    }

    // Generate a JWT token with enhanced security
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(issuer)
                .setAudience(audience)
                .setId(UUID.randomUUID().toString()) // Add a unique JWT ID
                .signWith(getSigningKey())
                .compact();
    }

    // Extract username from JWT token
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Extract role from JWT token
    public String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    // Extract all claims from token
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Validate a JWT token
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);

            // Check if token is expired
            if (claims.getExpiration().before(new Date())) {
                return false;
            }

            // Verify issuer and audience
            if (!issuer.equals(claims.getIssuer()) || !audience.equals(claims.getAudience())) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Get expiration date from token
    public Date getExpirationDateFromToken(String token) {
        try {
            return extractAllClaims(token).getExpiration();
        } catch (ExpiredJwtException e) {
            // If token is expired, return the expiration date from the exception
            return e.getClaims().getExpiration();
        } catch (Exception e) {
            // If token is invalid, return a date in the past
            return new Date(0);
        }
    }
}
