package com.vbs.capsAllocation.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DataMigrationConfig {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataMigrationConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrateData() {
        // Check if the is_overtime column exists
        try {
            // Update existing records to set is_overtime to false
            jdbcTemplate.execute("UPDATE time_entries SET is_overtime = false WHERE is_overtime IS NULL");
            
            // After all records are updated, add the not null constraint if needed
            jdbcTemplate.execute("ALTER TABLE time_entries ALTER COLUMN is_overtime SET NOT NULL");
        } catch (Exception e) {
            // Log the exception but don't fail startup
            System.err.println("Error during data migration: " + e.getMessage());
        }
    }
}
