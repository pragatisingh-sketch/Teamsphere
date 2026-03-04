package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.ExportResponse;
import com.vbs.capsAllocation.service.export.ExportStrategy;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing and executing export strategies
 */
@Service
public class ExportService {

    private final Map<String, ExportStrategy> strategies = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Auto-wire all ExportStrategy beans and register them
     */
    @Autowired
    public ExportService(List<ExportStrategy> exportStrategies) {
        for (ExportStrategy strategy : exportStrategies) {
            registerStrategy(strategy.getExportType(), strategy);
        }
        LoggerUtil.logInfo(ExportService.class, "Registered {} export strategies", strategies.size());
    }

    /**
     * Register an export strategy
     */
    public void registerStrategy(String type, ExportStrategy strategy) {
        strategies.put(type, strategy);
        LoggerUtil.logDebug("Registered export strategy: {}", type);
    }

    /**
     * Execute an export based on type and return wrapped response
     * 
     * @param type      Export type (e.g., TIME_ENTRY_DEFAULTER,
     *                  TIME_ENTRY_COMPLIANCE)
     * @param startDate Start date for data range
     * @param endDate   End date for data range
     * @param userName  Username of logged-in user
     * @param filters   Optional filters
     * @return ExportResponse containing file data and metadata
     * @throws IllegalArgumentException if export type is not registered
     */
    public ExportResponse generateExport(String type, LocalDate startDate, LocalDate endDate,
            String userName, Map<String, Object> filters) {
        ExportStrategy strategy = strategies.get(type);

        if (strategy == null) {
            throw new IllegalArgumentException("Unknown export type: " + type);
        }

        LoggerUtil.logInfo(ExportService.class, "Executing export: type={}, user={}, dateRange={} to {}",
                type, userName, startDate, endDate);

        byte[] excelData = strategy.generateExcel(startDate, endDate, userName, filters);
        String filename = generateFilename(type, startDate, endDate);
        String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        return new ExportResponse(excelData, filename, contentType);
    }

    /**
     * Generate filename for export
     */
    private String generateFilename(String type, LocalDate startDate, LocalDate endDate) {
        String dateRange = startDate.format(DATE_FORMATTER) + "_to_" + endDate.format(DATE_FORMATTER);
        String typeName = type.toLowerCase().replace("_", "-");
        return typeName + "_" + dateRange + ".xlsx";
    }
}
