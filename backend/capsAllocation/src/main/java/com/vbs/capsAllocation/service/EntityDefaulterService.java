package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.EntityDefaulterComparison;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.util.DateRangeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive service for handling defaulters across multiple entity types
 */
@Service
public class EntityDefaulterService {

        @Autowired
        private List<DefaulterTypeService<?>> defaulterTypeServices;

        /**
         * Get defaulter comparison for all entity types
         */
        public Map<String, EntityDefaulterComparison> getAllEntityDefaulterComparison(
                        LocalDate startDate,
                        LocalDate endDate,
                        String level,
                        User loggedInUser) {
                Map<String, EntityDefaulterComparison> comparisons = new HashMap<>();

                for (DefaulterTypeService<?> service : defaulterTypeServices) {
                        EntityDefaulterComparison comparison = getDefaulterComparison(
                                        service, startDate, endDate, level, loggedInUser);
                        comparisons.put(service.getEntityTypeName(), comparison);
                }

                return comparisons;
        }

        /**
         * Get defaulter comparison for a specific entity type
         */
        public EntityDefaulterComparison getDefaulterComparison(
                        DefaulterTypeService<?> defaulterService,
                        LocalDate startDate,
                        LocalDate endDate,
                        String level,
                        User loggedInUser) {
                Long current = defaulterService.getDefaulterCount(startDate, endDate, level, loggedInUser);
                var previous = DateRangeUtils.getPreviousRange(startDate, endDate);
                Long previousCount = defaulterService.getDefaulterCount(
                                previous.getStartDate(), previous.getEndDate(), level, loggedInUser);

                return new EntityDefaulterComparison(
                                defaulterService.getEntityTypeName(),
                                current,
                                previousCount,
                                previous);
        }

        /**
         * Get defaulter comparison for a specific entity type by name
         */
        public EntityDefaulterComparison getDefaulterComparisonByEntityType(
                        String entityTypeName,
                        LocalDate startDate,
                        LocalDate endDate,
                        String level,
                        User loggedInUser) {
                DefaulterTypeService<?> service = defaulterTypeServices.stream()
                                .filter(s -> s.getEntityTypeName().equalsIgnoreCase(entityTypeName))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException(
                                                "No defaulter service found for entity type: " + entityTypeName));

                return getDefaulterComparison(service, startDate, endDate, level, loggedInUser);
        }

        /**
         * Get all available entity types
         */
        public List<String> getAvailableEntityTypes() {
                return defaulterTypeServices.stream()
                                .map(DefaulterTypeService::getEntityTypeName)
                                .collect(Collectors.toList());
        }

        public List<com.vbs.capsAllocation.dto.DefaulterDetailDTO> getDefaultersList(
                        String entityTypeName,
                        LocalDate startDate,
                        LocalDate endDate,
                        String level,
                        User loggedInUser,
                        Map<String, Object> filters) {

                DefaulterTypeService<?> service = getServiceByType(entityTypeName);
                return service.getDefaultersList(startDate, endDate, level, loggedInUser, filters);
        }

        public List<com.vbs.capsAllocation.dto.TopDefaulterDTO> getTopDefaulters(
                        String entityTypeName,
                        LocalDate startDate,
                        LocalDate endDate,
                        String level,
                        User loggedInUser,
                        Map<String, Object> filters) {

                DefaulterTypeService<?> service = getServiceByType(entityTypeName);
                return service.getTopDefaulters(startDate, endDate, level, loggedInUser, filters);
        }

        private DefaulterTypeService<?> getServiceByType(String entityTypeName) {
                return defaulterTypeServices.stream()
                                .filter(s -> s.getEntityTypeName().equalsIgnoreCase(entityTypeName))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException(
                                                "No defaulter service found for entity type: " + entityTypeName));
        }
}