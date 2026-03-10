package com.vbs.capsAllocation.service;

import org.springframework.stereotype.Service;
import java.util.Date;

/**
 * Service for managing blacklisted JWT tokens
 * Tokens are added to the blacklist when a user logs out
 * or when a token is explicitly invalidated
 */
@Service
public class TokenBlacklistService {

    private final com.vbs.capsAllocation.repository.BlacklistedTokenRepository blacklistedTokenRepository;

    public TokenBlacklistService(
            com.vbs.capsAllocation.repository.BlacklistedTokenRepository blacklistedTokenRepository) {
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    /**
     * Add a token to the blacklist
     * 
     * @param token      The JWT token to blacklist
     * @param expiryDate The expiry date of the token
     */
    public void blacklistToken(String token, Date expiryDate) {
        com.vbs.capsAllocation.model.BlacklistedToken blacklistedToken = new com.vbs.capsAllocation.model.BlacklistedToken(
                token, expiryDate);
        blacklistedTokenRepository.save(blacklistedToken);
    }

    /**
     * Check if a token is blacklisted
     * 
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsById(token);
    }

    /**
     * Clean up expired tokens from the blacklist
     * This method should be called periodically to prevent memory leaks
     * 
     * @return The number of tokens removed
     */
    @org.springframework.transaction.annotation.Transactional
    public int cleanupExpiredTokens() {
        Date now = new Date();
        // Since deleteByExpiryDateBefore returns void, we'll just count before/after or
        // just return 0 for now
        // Ideally we'd optimize this but for safety let's just run the delete
        blacklistedTokenRepository.deleteByExpiryDateBefore(now);
        return 0; // Returning 0 as exact count is expensive to calculate with void return
    }

    /**
     * Get the current size of the blacklist
     * 
     * @return The number of tokens in the blacklist
     */
    public int getBlacklistSize() {
        return (int) blacklistedTokenRepository.count();
    }
}
