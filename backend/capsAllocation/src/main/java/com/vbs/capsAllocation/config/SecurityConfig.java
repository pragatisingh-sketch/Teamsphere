package com.vbs.capsAllocation.config;

import com.vbs.capsAllocation.util.JwtFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Value("${app.environment:dev}")
    private String environment;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, PasswordEncoder encoder,
            UserDetailsService userDetailsService) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(encoder);
        return builder.build();
    }

    @SuppressWarnings({ "deprecation", "removal" })
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/auth/login",
                                "/auth/signup",
                                "/auth/logout",
                                "/auth/check-password-status",
                                "/auth/reset-password",
                                "/auth/forgot-password",
                                "/auth/verify-otp",
                                "/auth/reset-password-with-otp",
                                "/admin/reset-password-postman",
                                "/api/time-entries/**",
                                "/api/timesheets/**",
                                "/api/projects/**",
                                "/api/employees/**",
                                "/api/database/**", "/api/vunno/**", "/api/atom/**", "/uploads/**",
                                "/api/holidays/**",
                                "/api/dropdown-configurations/**",
                                "/api/employee-relations/**",
                                "/api/reports/**",
                                "/api/delegation/**",
                                "/api/health",
                                "/api/ping",
                                "/api/issues/**",
                                "/api/releases/**",
                                "/api/faqs",
                                "/api/faqs/**",
                                "/api/exports/**")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeRequests()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/forgot-password").permitAll()
                .requestMatchers("/auth/verify-otp").permitAll()
                .requestMatchers("/auth/reset-password-with-otp").permitAll()
                .requestMatchers("/auth/signup").permitAll()
                .requestMatchers("/admin/register").hasAnyRole("ADMIN_OPS_MANAGER", "MANAGER")
                .requestMatchers("/auth/check-password-status").authenticated()
                .requestMatchers("/auth/reset-password").authenticated()
                .requestMatchers("/superadmin/only").hasRole("ACCOUNT_MANAGER")
                .requestMatchers("/admin/only").hasRole("MANAGER")
                .requestMatchers("/user/only").hasAnyRole("USER", "MANAGER", "ACCOUNT_MANAGER")
                .requestMatchers("/api/employees/**", "/api/timesheet/**")
                .hasAnyRole("USER", "LEAD", "MANAGER", "ACCOUNT_MANAGER", "ADMIN_OPS_MANAGER")
                .requestMatchers("/admin/reset-password-postman/**").hasAnyRole("ADMIN_OPS_MANAGER", "LEAD", "MANAGER")
                .requestMatchers("/api/employees/change-role").hasRole("ADMIN_OPS_MANAGER")
                .requestMatchers("/api/database/**").hasRole("ADMIN_OPS_MANAGER")
                .requestMatchers("/api/dropdown-configurations/active/**").authenticated()
                .requestMatchers("/api/dropdown-configurations/**").hasRole("ADMIN_OPS_MANAGER")
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/holidays/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/ping").permitAll()
                .requestMatchers("/api/issues/**").authenticated()
                .requestMatchers("/api/atom/**").hasAnyRole("USER", "LEAD", "MANAGER", "ADMIN_OPS_MANAGER")
                .requestMatchers("/api/releases/version").permitAll()
                .requestMatchers("/api/releases/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/faqs").permitAll()
                .requestMatchers("/api/faqs/**").authenticated()
                .requestMatchers("/api/exports/**").authenticated()
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOrigins = new ArrayList<>();

        if ("prod".equalsIgnoreCase(environment)) {
            // Production strict origins
            allowedOrigins.add("https://teamsphere.in");
            allowedOrigins.add("https://www.teamsphere.in");
            allowedOrigins.add("https://api.teamsphere.in");
            allowedOrigins.add("https://teamsphere.34-47-188-211.sslip.io"); // ✅ ADDED
        } else {
            // Dev/Stage/Test permissive origins
            allowedOrigins.add("http://localhost");
            allowedOrigins.add("http://localhost:4200");
            allowedOrigins.add("http://localhost:8080");
            allowedOrigins.add("http://localhost:8081");
            allowedOrigins.add("http://localhost:8082");
            allowedOrigins.add("http://localhost:3000");
            allowedOrigins.add("http://localhost:80");
            allowedOrigins.add("http://127.0.0.1");
            allowedOrigins.add("http://127.0.0.1:4200");
            allowedOrigins.add("http://127.0.0.1:8080");
            allowedOrigins.add("http://127.0.0.1:8081");
            allowedOrigins.add("http://127.0.0.1:8082");
            allowedOrigins.add("https://teamsphere.in");
            allowedOrigins.add("https://teamsphere.34-47-188-211.sslip.io"); // ✅ ADDED
        }

        if (!"prod".equalsIgnoreCase(environment)) {
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress address = addresses.nextElement();
                            if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                                String ip = address.getHostAddress();
                                allowedOrigins.add("http://" + ip);
                                allowedOrigins.add("http://" + ip + ":4200");
                                allowedOrigins.add("http://" + ip + ":8080");
                                allowedOrigins.add("https://" + ip);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not detect network interfaces for CORS: " + e.getMessage());
            }
        }

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-XSRF-TOKEN"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}