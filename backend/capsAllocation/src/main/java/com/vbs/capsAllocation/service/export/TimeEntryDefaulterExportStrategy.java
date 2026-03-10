package com.vbs.capsAllocation.service.export;

import com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO;
import com.vbs.capsAllocation.service.ReportsService;
import com.vbs.capsAllocation.util.ExcelStyleUtil;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Export strategy for Time Entry Defaulters (Weekly Missing Entries)
 */
@Component
public class TimeEntryDefaulterExportStrategy implements ExportStrategy {

    @Autowired
    private ReportsService reportsService;

    private static final String EXPORT_TYPE = "TIME_ENTRY_DEFAULTER";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public byte[] generateExcel(LocalDate startDate, LocalDate endDate, String userName, Map<String, Object> filters) {
        LoggerUtil.logDebug("Generating TIME_ENTRY_DEFAULTER export for user: {}, date range: {} to {}",
                userName, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Fetch data
            List<WeeklyTimeEntryDefaulterDTO> defaulters = reportsService.getWeeklyTimeEntryDefaulters(
                    startDate, endDate, userName, filters);

            // Create Summary Sheet
            createSummarySheet(workbook, defaulters);

            // Create Detailed Breakdown Sheet
            createDetailedBreakdownSheet(workbook, defaulters);

            // Write to byte array
            workbook.write(out);
            LoggerUtil.logDebug("Successfully generated Excel with {} defaulters", defaulters.size());
            return out.toByteArray();

        } catch (Exception e) {
            LoggerUtil.logError("Error generating TIME_ENTRY_DEFAULTER export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate export", e);
        }
    }

    @Override
    public String getExportType() {
        return EXPORT_TYPE;
    }

    /**
     * Create summary sheet with high-level defaulter information
     */
    private void createSummarySheet(Workbook workbook, List<WeeklyTimeEntryDefaulterDTO> defaulters) {
        Sheet sheet = workbook.createSheet("Summary");

        // Create styles
        CellStyle headerStyle = ExcelStyleUtil.createHeaderStyle(workbook);
        CellStyle cellStyle = ExcelStyleUtil.createCellStyle(workbook);
        CellStyle numberStyle = ExcelStyleUtil.createNumberStyle(workbook);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = { "Employee Name", "LDAP", "Email", "Team", "Manager", "Missing Weeks Count" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (WeeklyTimeEntryDefaulterDTO defaulter : defaulters) {
            Row row = sheet.createRow(rowNum++);

            Cell cell0 = row.createCell(0);
            cell0.setCellValue(defaulter.getEmployeeName());
            cell0.setCellStyle(cellStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(defaulter.getLdap());
            cell1.setCellStyle(cellStyle);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(defaulter.getEmail());
            cell2.setCellStyle(cellStyle);

            Cell cell3 = row.createCell(3);
            cell3.setCellValue(defaulter.getDepartment());
            cell3.setCellStyle(cellStyle);

            Cell cell4 = row.createCell(4);
            cell4.setCellValue(defaulter.getManager());
            cell4.setCellStyle(cellStyle);

            Cell cell5 = row.createCell(5);
            cell5.setCellValue(defaulter.getMissingWeeksCount());
            cell5.setCellStyle(numberStyle);
        }

        // Auto-size columns
        ExcelStyleUtil.autoSizeColumns(sheet, headers.length);
    }

    /**
     * Create detailed breakdown sheet showing each missing week and day
     */
    private void createDetailedBreakdownSheet(Workbook workbook, List<WeeklyTimeEntryDefaulterDTO> defaulters) {
        Sheet sheet = workbook.createSheet("Detailed Breakdown");

        // Create styles
        CellStyle headerStyle = ExcelStyleUtil.createHeaderStyle(workbook);
        CellStyle cellStyle = ExcelStyleUtil.createCellStyle(workbook);
        CellStyle dateStyle = ExcelStyleUtil.createDateStyle(workbook);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = { "Employee Name", "LDAP", "Email", "Team", "Manager", "Week Label", "Week Start",
                "Week End", "Whole Week Missing", "Missing Dates" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (WeeklyTimeEntryDefaulterDTO defaulter : defaulters) {
            for (WeeklyTimeEntryDefaulterDTO.WeeklyBreakdown week : defaulter.getWeeklyBreakdowns()) {
                Row row = sheet.createRow(rowNum++);

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(defaulter.getEmployeeName());
                cell0.setCellStyle(cellStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(defaulter.getLdap());
                cell1.setCellStyle(cellStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(defaulter.getEmail());
                cell2.setCellStyle(cellStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(defaulter.getDepartment());
                cell3.setCellStyle(cellStyle);

                Cell cell4 = row.createCell(4);
                cell4.setCellValue(defaulter.getManager());
                cell4.setCellStyle(cellStyle);

                Cell cell5 = row.createCell(5);
                cell5.setCellValue(week.getWeekLabel());
                cell5.setCellStyle(cellStyle);

                Cell cell6 = row.createCell(6);
                cell6.setCellValue(week.getWeekStartDate().format(DATE_FORMATTER));
                cell6.setCellStyle(dateStyle);

                Cell cell7 = row.createCell(7);
                cell7.setCellValue(week.getWeekEndDate().format(DATE_FORMATTER));
                cell7.setCellStyle(dateStyle);

                Cell cell8 = row.createCell(8);
                cell8.setCellValue(week.isWholeWeekMissing() ? "Yes" : "No");
                cell8.setCellStyle(cellStyle);

                // Format missing dates as comma-separated list
                String missingDatesStr = week.getMissingDays().stream()
                        .map(date -> date.format(DATE_FORMATTER))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");

                Cell cell9 = row.createCell(9);
                cell9.setCellValue(missingDatesStr);
                cell9.setCellStyle(cellStyle);
            }
        }

        // Auto-size columns
        ExcelStyleUtil.autoSizeColumns(sheet, headers.length);
    }
}
