package com.vbs.capsAllocation.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbs.capsAllocation.dto.ErrorResponse;
import com.vbs.capsAllocation.service.CustomUserDetailsService;
import com.vbs.capsAllocation.service.TokenBlacklistService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService,
            TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip JWT check for login and other public endpoints
        if (path.startsWith("/approveRequest") ||
                path.equals("/auth/login") ||
                path.equals("/auth/forgot-password") ||
                path.equals("/auth/verify-otp") ||
                path.equals("/auth/reset-password-with-otp") ||
                path.equals("/auth/signup") ||
                path.equals("/admin/register")) {
            System.out.println("Skipping JWT validation for public endpoint: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        // Special handling for password reset endpoint
        if (path.equals("/admin/reset-password-postman")) {
            System.out.println("Special handling for password reset endpoint");
            // Continue with JWT validation but log more details
        }

        final String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String token = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);

            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Your session has been logged out. Please login again.");
                return;
            }

            try {
                username = jwtUtil.extractUsername(token);
            } catch (ExpiredJwtException e) {
                System.out.println("JWT token expired for user: " + e.getClaims().getSubject());
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Your session has expired. Please login again.");
                return;
            } catch (Exception e) {
                System.out.println("Error extracting username from token: " + e.getMessage());
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Your session is invalid. Please login again.");
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (path.equals("/admin/reset-password-postman")) {
                    System.out.println("User details for password reset: " + userDetails.getUsername());
                    System.out.println("Authorities: " + userDetails.getAuthorities());
                }

                if (jwtUtil.isTokenValid(token)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    if (path.equals("/admin/reset-password-postman")) {
                        System.out.println("Authentication set in SecurityContext for password reset");
                    }
                } else {
                    System.out.println("Token validation failed for user: " + username);
                }
            } catch (Exception e) {
                System.out.println("Error during authentication: " + e.getMessage());
                e.printStackTrace();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Send a standardized error response using ErrorResponse DTO
     * 
     * @param response   HTTP response object
     * @param statusCode HTTP status code
     * @param message    User-friendly error message
     * @throws IOException if writing to response fails
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        ErrorResponse errorResponse = ErrorResponse.error(message, statusCode);
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
