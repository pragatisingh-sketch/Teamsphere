package com.vbs.capsAllocation.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ApprovalTokenService {

    @Autowired
    private CacheManager cacheManager;

    private static final Logger logger = LoggerFactory.getLogger(VunnoMgmtService.class);

    public void storeToken(int requestId, String token, String action) {
        Cache cache = cacheManager.getCache("approvalTokens");
        cache.put(token, new ApprovalToken(requestId, action,
                LocalDateTime.now().plusHours(48)));
    }

//    public boolean isValidToken(String token, int requestId, String action) {
//        System.out.println("⚠️ Bypassing token validation for testing");
//        return true; // FOR TESTING ONLY
//    }


    public boolean isValidToken(String token, int requestId, String action) {
        System.out.println("=== Token Validation Check ===");
        System.out.println("Input token: " + token);
        System.out.println("Input requestId: " + requestId);
        System.out.println("Input action: " + action);

        Cache cache = cacheManager.getCache("approvalTokens");
        ApprovalToken storedToken = cache.get(token, ApprovalToken.class);

        if (storedToken == null) {
            logger.error("❌ Token not found: {}", token);
            return false;
        }
        if (storedToken.getExpiry().isBefore(LocalDateTime.now())) {
            logger.error("⏰ Token expired: {}", storedToken.getExpiry());
            return false;
        }
        return storedToken != null
                && storedToken.getRequestId() == requestId
                && storedToken.getAction().equals(action)
                && storedToken.getExpiry().isAfter(LocalDateTime.now());
    }

    // Validate token
//    public boolean isValidToken(String token, int requestId, String action) {
//        Cache cache = cacheManager.getCache("approvalTokens");
//        ApprovalToken storedToken = cache.get(token, ApprovalToken.class);
//        return storedToken != null
//                && storedToken.getRequestId() == requestId
//                && storedToken.getAction().equals(action)
//                && storedToken.getExpiry().isAfter(LocalDateTime.now());
//    }

    // Invalidate used token
    public void invalidateToken(String token) {
        Cache cache = cacheManager.getCache("approvalTokens");
        cache.evict(token);
    }

}

@Data
@AllArgsConstructor
class ApprovalToken {
    private int requestId;
    private String action;
    private LocalDateTime expiry;
}
