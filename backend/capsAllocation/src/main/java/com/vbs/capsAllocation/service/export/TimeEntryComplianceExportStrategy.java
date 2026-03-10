package com.vbs.capsAllocation.service.export;

import com.vbs.capsAllocation.dto.DefaulterDetailDTO;
import com.vbs.capsAllocation.dto.IssueDetailDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Export strategy for Time Entry Compliance Issues (Overall Non-Compliance
 * Report)
 */
@Component
public class TimeEntryComplianceExportStrategy implements ExportStrategy {

    @Autowired
    private ReportsService reportsService;

    private static final String EXPORT_TYPE = "TIME_ENTRY_COMPLIANCE";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] generateExcel(LocalDate startDate, LocalDate endDate, String userName, Map<String, Object> filters) {
        LoggerUtil.logDebug("Generating TIME_ENTRY_COMPLIANCE export for user: {}, date range: {} to {}",
                userName, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Fetch all defaulters
            List<DefaulterDetailDTO> defaulters = reportsService.getAllDefaulters(
                    "TimeEntry", startDate, endDate, userName, filters);

            // Create Summary Sheet
            createSummarySheet(workbook, defaulters);

            // Create Detailed Instances Sheet with all issues
            createDetailedInstancesSheet(workbook, defaulters, startDate, endDate);

            // Write to byte array
            workbook.write(out);
            LoggerUtil.logDebug("Successfully generated Excel with {} defaulters", defaulters.size());
            return out.toByteArray();

        } catch (Exception e) {
            LoggerUtil.logError("Error generating TIME_ENTRY_COMPLIANCE export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate export", e);
        }
    }

    @Override
    public String getExportType() {
        return EXPORT_TYPE;
    }

    /**
     * Create summary sheet with defaulter counts
     */
    private void createSummarySheet(Workbook workbook, List<DefaulterDetailDTO> defaulters) {
        Sheet sheet = workbook.createSheet("Summary");

        // Create styles
        CellStyle headerStyle = ExcelStyleUtil.createHeaderStyle(workbook);
        CellStyle cellStyle = ExcelStyleUtil.createCellStyle(workbook);
        CellStyle numberStyle = ExcelStyleUtil.createNumberStyle(workbook);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = { "Employee Name", "Email", "Team", "Manager", "Process", "Program", "Total Instances" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (DefaulterDetailDTO defaulter : defaulters) {
            Row row = sheet.createRow(rowNum++);

            Cell cell0 = row.createCell(0);
            cell0.setCellValue(defaulter.getEmployeeName());
            cell0.setCellStyle(cellStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(defaulter.getEmail());
            cell1.setCellStyle(cellStyle);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(defaulter.getDepartment());
            cell2.setCellStyle(cellStyle);

            Cell cell3 = row.createCell(3);
            cell3.setCellValue(defaulter.getManager());
            cell3.setCellStyle(cellStyle);

            Cell cell4 = row.createCell(4);
            cell4.setCellValue(defaulter.getProject());
            cell4.setCellStyle(cellStyle);

            Cell cell5 = row.createCell(5);
            cell5.setCellValue(defaulter.getProgram());
            cell5.setCellStyle(cellStyle);

            Cell cell6 = row.createCell(6);
            cell6.setCellValue(defaulter.getIssueCount());
            cell6.setCellStyle(numberStyle);
        }

        // Auto-size columns
        ExcelStyleUtil.autoSizeColumns(sheet, headers.length);
    }

    /**
     * Create detailed instances sheet showing each time-entry issue
     */
    private void createDetailedInstancesSheet(Workbook workbook, List<DefaulterDetailDTO> defaulters,
            LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("Detailed Instances");

        // Create styles
        CellStyle headerStyle = ExcelStyleUtil.createHeaderStyle(workbook);
        CellStyle cellStyle = ExcelStyleUtil.createCellStyle(workbook);
        CellStyle dateStyle = ExcelStyleUtil.createDateStyle(workbook);
        CellStyle numberStyle = ExcelStyleUtil.createNumberStyle(workbook);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = { "Employee Name", "Email", "Entry Date", "Created At", "Updated At", "Project", "Process",
                "Activity", "Minutes", "Status", "Comments" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows - fetch detailed issues for each defaulter
        int rowNum = 1;
        for (DefaulterDetailDTO defaulter : defaulters) {
            // Get LDAP from email (DefaulterDetailDTO doesn't have ldap field)
            String ldap = defaulter.getEmail().split("@")[0];

            try {
                List<IssueDetailDTO> issues = reportsService.getUserIssues(
                        "timeentry", ldap, startDate, endDate);

                for (IssueDetailDTO issue : issues) {
                    Row row = sheet.createRow(rowNum++);

                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(defaulter.getEmployeeName());
                    cell0.setCellStyle(cellStyle);

                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue(defaulter.getEmail());
                    cell1.setCellStyle(cellStyle);

                    Cell cell2 = row.createCell(2);
                    cell2.setCellValue(issue.getDate() != null ? issue.getDate().format(DATE_FORMATTER) : "");
                    cell2.setCellStyle(dateStyle);

                    Cell cell3 = row.createCell(3);
                    cell3.setCellValue(
                            issue.getCreatedAt() != null ? issue.getCreatedAt().format(DATETIME_FORMATTER) : "");
                    cell3.setCellStyle(cellStyle);

                    Cell cell4 = row.createCell(4);
                    cell4.setCellValue(
                            issue.getUpdatedAt() != null ? issue.getUpdatedAt().format(DATETIME_FORMATTER) : "");
                    cell4.setCellStyle(cellStyle);

                    Cell cell5 = row.createCell(5);
                    cell5.setCellValue(issue.getProject() != null ? issue.getProject() : "");
                    cell5.setCellStyle(cellStyle);

                    Cell cell6 = row.createCell(6);
                    cell6.setCellValue(issue.getProcess() != null ? issue.getProcess() : "");
                    cell6.setCellStyle(cellStyle);

                    Cell cell7 = row.createCell(7);
                    cell7.setCellValue(issue.getActivity() != null ? issue.getActivity() : "");
                    cell7.setCellStyle(cellStyle);

                    Cell cell8 = row.createCell(8);
                    int minutes = issue.getTimeInMins() != null ? issue.getTimeInMins() : 0;
                    cell8.setCellValue(minutes);
                    cell8.setCellStyle(numberStyle);

                    Cell cell9 = row.createCell(9);
                    cell9.setCellValue(issue.getStatus() != null ? issue.getStatus() : "");
                    cell9.setCellStyle(cellStyle);

                    Cell cell10 = row.createCell(10);
                    cell10.setCellValue(issue.getComment() != null ? issue.getComment() : "");
                    cell10.setCellStyle(cellStyle);
                }
            } catch (Exception e) {
                LoggerUtil.logWarn(TimeEntryComplianceExportStrategy.class,
                        "Could not fetch issues for employee {}: {}", ldap, e.getMessage());
            }
        }

        // Auto-size columns
        ExcelStyleUtil.autoSizeColumns(sheet, headers.length);
    }
}
