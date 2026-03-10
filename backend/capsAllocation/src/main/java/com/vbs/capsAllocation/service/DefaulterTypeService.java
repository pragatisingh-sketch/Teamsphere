package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/**
 * Generic interface for defaulter calculation across different entity types
 * 
 * @param <T> The entity type (TimeEntry, Attendance, etc.)
 */
public interface DefaulterTypeService<T> {

        /**
         * Get defaulter count for a specific date range and user level
         * 
         * @param start        Start date
         * @param end          End date
         * @param level        User role level (LEAD, MANAGER, ADMIN_OPS_MANAGER)
         * @param loggedInUser Currently logged in user
         * @return Count of defaulters
         */
        Long getDefaulterCount(LocalDate start, LocalDate end, String level, User loggedInUser);

        /**
         * Get repository for the entity type
         * 
         * @return JPA repository instance
         */
        JpaRepository<T, Long> getRepository();

        /**
         * Get detailed list of defaulters
         */
        java.util.List<com.vbs.capsAllocation.dto.DefaulterDetailDTO> getDefaultersList(
                        LocalDate start,
                        LocalDate end,
                        String level,
                        User loggedInUser,
                        java.util.Map<String, Object> filters);

        /**
         * Get top 3 defaulters
         */
        /**
         * Get top 3 defaulters
         */
        java.util.List<com.vbs.capsAllocation.dto.TopDefaulterDTO> getTopDefaulters(
                        LocalDate start,
                        LocalDate end,
                        String level,
                        User loggedInUser,
                        java.util.Map<String, Object> filters);

        /**
         * Get the entity type name for identification
         * 
         * @return Entity type name
         */
        String getEntityTypeName();
}