package com.vbs.capsAllocation.service.export;

import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.TimeEntry;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.repository.TimeEntryRepository;
import com.vbs.capsAllocation.repository.TimeEntryRepository;
import com.vbs.capsAllocation.repository.UserRepository;
import com.vbs.capsAllocation.repository.HolidayRepository;
import com.vbs.capsAllocation.model.Holiday;
import com.vbs.capsAllocation.util.ExcelStyleUtil;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Export strategy for Attendance Tracker with Instructions and Structure tabs
 */
@Component
public class AttendanceTrackerExportStrategy implements ExportStrategy {

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    private static final String EXPORT_TYPE = "ATTENDANCE_TRACKER";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter HEADER_DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Override
    public byte[] generateExcel(LocalDate startDate, LocalDate endDate, String userName, Map<String, Object> filters) {
        LoggerUtil.logDebug("Generating ATTENDANCE_TRACKER export for user: {}, date range: {} to {}",
                userName, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Create Instructions Sheet (Tab 1)
            createInstructionsSheet(workbook);

            // Create Structure Sheet (Tab 2)
            createStructureSheet(workbook, startDate, endDate);

            // Write to byte array
            workbook.write(out);
            LoggerUtil.logDebug("Successfully generated Attendance Tracker Excel");
            return out.toByteArray();

        } catch (Exception e) {
            LoggerUtil.logError("Error generating ATTENDANCE_TRACKER export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate export", e);
        }
    }

    @Override
    public String getExportType() {
        return EXPORT_TYPE;
    }

    /**
     * Create Instructions sheet with color-coded attendance terms
     */
    private void createInstructionsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Instructions");

        // Create styles
        CellStyle headerStyle = ExcelStyleUtil.createHeaderStyle(workbook);
        CellStyle cellStyle = ExcelStyleUtil.createCellStyle(workbook);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = { "Terms", "Color", "Meaning", "Shift", "Timing" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows with color coding
        String[][] instructionsData = {
                { "Not Joined", "", "Not Joined", "S1", "7:30 AM - 4:30 PM" },
                { "Exit", "", "Employee Left", "S2", "4:30 PM - 1:30 AM" },
                { "Bench", "", "Bench", "S3", "10:30 PM - 7:30 AM" },
                { "S1/F", "", "S1 Shift / Full Day", "MS", "10:00 AM - 7:00 PM" },
                { "S2/F", "", "S2 Shift / Full Day", "MS2", "12:30 PM - 9:30 PM" },
                { "S3/F", "", "S3 Shift / Full Day", "", "" },
                { "MS/F", "", "MS Shift / Full Day", "", "" },
                { "MS2/F", "", "MS2 Shift / Full Day", "", "" },
                { "S1/H", "", "S1 Shift / Half Day", "", "" },
                { "S2/H", "", "S2 Shift / Half Day", "", "" },
                { "S3/H", "", "S3 Shift / Half Day", "", "" },
                { "MS/H", "", "MS Shift / Half Day", "", "" },
                { "MS2/H", "", "MS2 Shift / Half Day", "", "" },
                { "S1/A", "", "S1 Shift / Unplanned Leave", "", "" },
                { "S2/A", "", "S2 Shift / Unplanned Leave", "", "" },
                { "S3/A", "", "S3 Shift / Unplanned Leave", "", "" },
                { "MS/A", "", "MS Shift / Unplanned Leave", "", "" },
                { "MS2/A", "", "MS2 Shift / Unplanned Leave", "", "" },
                { "S1/C", "", "S1 Shift / Comp Off", "", "" },
                { "S2/C", "", "S2 Shift Comp Off", "", "" },
                { "S3/C", "", "S3 Shift / Comp Off", "", "" },
                { "MS/C", "", "MS Shift / Comp Off", "", "" },
                { "MS2/C", "", "MS2 Shift / Comp Off", "", "" },
                { "S1/Leave", "", "S1 Shift / Planned Leave", "", "" },
                { "S2/Leave", "", "S2 Shift / Planned Leave", "", "" },
                { "S3/Leave", "", "S3 Shift / Planned Leave", "", "" },
                { "MS/Leave", "", "MS Shift / Planned Leave", "", "" },
                { "MS2/Leave", "", "MS2 Shift / Planned Leave", "", "" },
                { "S1/BL", "", "S1 Shift / Breavement Leave", "", "" },
                { "S2/BL", "", "S2 Shift / Breavement Leave", "", "" },
                { "S3/BL", "", "S3 Shift / Breavement Leave", "", "" },
                { "MS/BL", "", "MS Shift / Breavement Leave", "", "" },
                { "MS2/BL", "", "MS2 Shift / Breavement Leave", "", "" },
                { "S1/WO", "", "S1 Shift / Week Off", "", "" },
                { "S2/WO", "", "S2 Shift / Week Off", "", "" },
                { "S3/WO", "", "S3 Shift / Week Off", "", "" },
                { "MS/WO", "", "MS Shift / Week Off", "", "" },
                { "MS2/WO", "", "MS2 Shift / Week Off", "", "" },
                { "S1/GO", "", "S1 Shift / Google Holiday", "", "" },
                { "S2/GO", "", "S2 Shift / Google Holiday", "", "" },
                { "S3/GO", "", "S3 Shift / Google Holiday", "", "" },
                { "MS/GO", "", "MS Shift / Google Holiday", "", "" },
                { "MS2/GO", "", "MS2 Shift / Google Holiday", "", "" },
                { "Maternity", "", "Maternity", "", "" },
                { "S1/PAT", "", "S1/Paternity Leave", "", "" },
                { "S2/PAT", "", "S2/Paternity Leave", "", "" },
                { "S3/PAT", "", "S3/Paternity Leave", "", "" },
                { "MS2/PAT", "", "MS2/Paternity Leave", "", "" },
                { "MS/PAT", "", "MS/Paternity Leave", "", "" },
                { "S1/SL", "", "S1 Shift / Sick Leave", "", "" },
                { "S2/SL", "", "S2 Shift / Sick Leave", "", "" },
                { "S3/SL", "", "S3 Shift / Sick Leave", "", "" },
                { "MS/SL", "", "MS Shift / Sick Leave", "", "" },
                { "MS2/SL", "", "MS2 Shift / Sick Leave", "", "" },
                { "S1/SLH", "", "S1 Shift / Sick Leave Half Day", "", "" },
                { "S2/SLH", "", "S2 Shift / Sick Leave Half Day", "", "" },
                { "S3/SLH", "", "S3 Shift / Sick Leave Half Day", "", "" },
                { "MS/SLH", "", "MS Shift / Sick Leave Half Day", "", "" },
                { "MS2/SLH", "", "MS2 Shift / Sick Leave Half Day", "", "" }
        };

        int rowNum = 1;
        for (String[] rowData : instructionsData) {
            Row row = sheet.createRow(rowNum);
            CellStyle coloredStyle = workbook.createCellStyle();
            coloredStyle.cloneStyleFrom(cellStyle);

            // Apply color based on term
            IndexedColors color = getColorForTerm(rowData[0]);
            if (color != null) {
                coloredStyle.setFillForegroundColor(color.getIndex());
                coloredStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }

            for (int i = 0; i < rowData.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(rowData[i]);
                if (i == 0 || i == 1) { // Apply color to Terms and Color columns
                    cell.setCellStyle(coloredStyle);
                } else {
                    cell.setCellStyle(cellStyle);
                }
            }
            rowNum++;
        }

        // Auto-size columns
        ExcelStyleUtil.autoSizeColumns(sheet, headers.length);
    }

    /**
     * Get color for attendance term based on reference screenshots
     */
    private IndexedColors getColorForTerm(String term) {
        if (term == null)
            return null;

        // Green - Bench
        if (term.equals("Bench"))
            return IndexedColors.LIGHT_GREEN;

        // Yellow - Half days
        if (term.matches("S\\d+/H|MS\\d*/H"))
            return IndexedColors.LIGHT_YELLOW;

        // Light Pink - Unplanned Leave (Absent)
        if (term.matches("S\\d+/A|MS\\d*/A|Maternity"))
            return IndexedColors.ROSE;

        // Cyan - Google Holiday
        if (term.matches("S\\d+/GO|MS\\d*/GO"))
            return IndexedColors.LIGHT_TURQUOISE;

        // Bright Pink - Paternity Leave
        if (term.matches("S\\d+/PAT|MS\\d*/PAT"))
            return IndexedColors.PINK;

        // Red - Planned Leave
        if (term.matches("S\\d+/Leave|MS\\d*/Leave"))
            return IndexedColors.RED;

        // Dark Gray - Bereavement Leave
        if (term.matches("S\\d+/BL|MS\\d*/BL"))
            return IndexedColors.GREY_50_PERCENT;

        // Light Gray - Week Off
        if (term.matches("S\\d+/WO|MS\\d*/WO"))
            return IndexedColors.GREY_25_PERCENT;

        // Orange - Sick Leave
        if (term.matches("S\\d+/SL.*|MS\\d*/SL.*"))
            return IndexedColors.LIGHT_ORANGE;

        return null;
    }

    /**
     * Create Structure sheet with employee attendance matrix
     */
    private void createStructureSheet(Workbook workbook, LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("Structure");

        // Create styles
        CellStyle headerStyle = ExcelStyleUtil.createHeaderStyle(workbook);
        CellStyle cellStyle = ExcelStyleUtil.createCellStyle(workbook);
        CellStyle dateStyle = ExcelStyleUtil.createDateStyle(workbook);
        CellStyle percentStyle = ExcelStyleUtil.createPercentStyle(workbook);

        // Create colored styles map
        Map<IndexedColors, CellStyle> coloredStyles = new HashMap<>();
        IndexedColors[] colors = {
                IndexedColors.LIGHT_GREEN, IndexedColors.LIGHT_YELLOW, IndexedColors.ROSE,
                IndexedColors.LIGHT_TURQUOISE, IndexedColors.PINK, IndexedColors.RED,
                IndexedColors.GREY_50_PERCENT, IndexedColors.GREY_25_PERCENT, IndexedColors.LIGHT_ORANGE
        };

        for (IndexedColors color : colors) {
            CellStyle style = workbook.createCellStyle();
            style.cloneStyleFrom(cellStyle);
            style.setFillForegroundColor(color.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            coloredStyles.put(color, style);
        }

        // Generate date columns
        List<LocalDate> dates = getDateRange(startDate, endDate);

        // Create header rows
        createStructureHeaders(sheet, headerStyle, dates);

        // Fetch all active employees (status can be Active, ON, or active)
        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(emp -> emp.getStatus() != null &&
                        (emp.getStatus().equalsIgnoreCase("ON") ||
                                emp.getStatus().equalsIgnoreCase("Active")))
                .sorted(Comparator.comparing(Employee::getLdap))
                .collect(Collectors.toList());

        LoggerUtil.logDebug("Found {} active employees with status Active/ON", employees.size());

        // Fetch all time entries in date range
        List<TimeEntry> timeEntries = timeEntryRepository.findByEntryDateBetween(startDate, endDate);
        LoggerUtil.logDebug("Found {} time entries between {} and {}",
                timeEntries.size(), startDate, endDate);

        Map<String, List<TimeEntry>> entriesByLdap = timeEntries.stream()
                .collect(Collectors.groupingBy(TimeEntry::getLdap));
        LoggerUtil.logDebug("Time entries grouped by {} unique LDAPs", entriesByLdap.size());

        // Fetch Holidays for the range
        List<Holiday> holidays = holidayRepository.findHolidaysBetweenDates(startDate, endDate, true);
        Set<LocalDate> holidayDates = holidays.stream()
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toSet());
        LoggerUtil.logDebug("Found {} active holidays in range", holidayDates.size());

        // Populate employee data rows
        int rowNum = 2;
        LoggerUtil.logDebug("Starting to populate {} employee rows", employees.size());
        for (Employee employee : employees) {
            List<TimeEntry> empEntries = entriesByLdap.getOrDefault(employee.getLdap(), Collections.emptyList());
            LoggerUtil.logDebug("Creating row for employee: {} with {} time entries",
                    employee.getLdap(), empEntries.size());
            createEmployeeRow(sheet, rowNum++, employee, empEntries, dates, cellStyle, dateStyle, percentStyle,
                    coloredStyles, holidayDates);
        }
        LoggerUtil.logDebug("Completed populating employee data, total rows: {}", rowNum - 2);

        // Freeze panes up to "Status" column (column D, index 3, so split at 4)
        sheet.createFreezePane(4, 2);

        // Auto-size fixed columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }

        // Set explicit width for date columns (make them wider)
        int dateColumnStartIndex = 4;
        int dateColumnEndIndex = dateColumnStartIndex + dates.size();
        for (int i = dateColumnStartIndex; i < dateColumnEndIndex; i++) {
            sheet.setColumnWidth(i, 3000); // ~12 characters wide (256 units per char)
        }
    }

    /**
     * Create header rows for Structure sheet
     */
    private void createStructureHeaders(Sheet sheet, CellStyle headerStyle, List<LocalDate> dates) {
        Workbook workbook = sheet.getWorkbook();

        // Create orange header style for fixed columns
        CellStyle orangeHeaderStyle = workbook.createCellStyle();
        orangeHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        orangeHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        orangeHeaderStyle.setBorderBottom(BorderStyle.THIN);
        orangeHeaderStyle.setBorderTop(BorderStyle.THIN);
        orangeHeaderStyle.setBorderLeft(BorderStyle.THIN);
        orangeHeaderStyle.setBorderRight(BorderStyle.THIN);
        Font orangeFont = workbook.createFont();
        orangeFont.setBold(true);
        orangeFont.setFontHeightInPoints((short) 11);
        orangeHeaderStyle.setFont(orangeFont);
        orangeHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        orangeHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Create purple/lavender header style for date columns
        CellStyle purpleHeaderStyle = workbook.createCellStyle();
        purpleHeaderStyle.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
        purpleHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        purpleHeaderStyle.setBorderBottom(BorderStyle.THIN);
        purpleHeaderStyle.setBorderTop(BorderStyle.THIN);
        purpleHeaderStyle.setBorderLeft(BorderStyle.THIN);
        purpleHeaderStyle.setBorderRight(BorderStyle.THIN);
        Font purpleFont = workbook.createFont();
        purpleFont.setBold(true);
        purpleFont.setFontHeightInPoints((short) 11);
        purpleHeaderStyle.setFont(purpleFont);
        purpleHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        purpleHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Row 0: Day of week
        Row dayRow = sheet.createRow(0);
        // Row 1: Date headers
        Row dateRow = sheet.createRow(1);

        // Fixed columns with orange headers
        String[] fixedHeaders = { "Ldap", "Name", "POC", "Status" };
        for (int i = 0; i < fixedHeaders.length; i++) {
            Cell cell = dateRow.createCell(i);
            cell.setCellValue(fixedHeaders[i]);
            cell.setCellStyle(orangeHeaderStyle);
        }

        // Date columns with purple headers
        int colNum = fixedHeaders.length;
        for (LocalDate date : dates) {
            // Day of week in row 0
            Cell dayCell = dayRow.createCell(colNum);
            dayCell.setCellValue(date.getDayOfWeek().toString().substring(0, 3));
            dayCell.setCellStyle(purpleHeaderStyle);

            // Date in row 1
            Cell dateCell = dateRow.createCell(colNum);
            dateCell.setCellValue(date.format(HEADER_DATE_FORMATTER));
            dateCell.setCellStyle(purpleHeaderStyle);

            colNum++;
        }

    }

    /**
     * Create employee attendance row
     */
    private void createEmployeeRow(Sheet sheet, int rowNum, Employee employee,
            List<TimeEntry> entries, List<LocalDate> dates,
            CellStyle cellStyle, CellStyle dateStyle, CellStyle percentStyle,
            Map<IndexedColors, CellStyle> coloredStyles, Set<LocalDate> holidayDates) {
        Row row = sheet.createRow(rowNum);
        int colNum = 0;

        // Ldap
        Cell ldapCell = row.createCell(colNum++);
        ldapCell.setCellValue(employee.getLdap() != null ? employee.getLdap() : "-");
        ldapCell.setCellStyle(cellStyle);

        // Name
        Cell nameCell = row.createCell(colNum++);
        String fullName = (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
                (employee.getLastName() != null ? employee.getLastName() : "");
        nameCell.setCellValue(fullName.trim().isEmpty() ? "-" : fullName.trim());
        nameCell.setCellStyle(cellStyle);

        // POC (Team Lead)
        Cell pocCell = row.createCell(colNum++);
        String pocEmail = "-";
        if (employee.getLead() != null) {
            Optional<User> leadUser = userRepository.findByUsername(employee.getLead());
            pocEmail = leadUser.map(User::getUsername).orElse(employee.getLead() + "@google.com");
        }
        pocCell.setCellValue(pocEmail);
        pocCell.setCellStyle(cellStyle);

        // Status
        Cell statusCell = row.createCell(colNum++);
        statusCell.setCellValue(employee.getStatus() != null ? employee.getStatus() : "-");
        statusCell.setCellStyle(cellStyle);

        // Map entries by date
        Map<LocalDate, TimeEntry> entryByDate = entries.stream()
                .collect(Collectors.toMap(TimeEntry::getEntryDate, e -> e, (e1, e2) -> e1));

        // Date columns with attendance codes
        for (LocalDate date : dates) {
            Cell cell = row.createCell(colNum++);
            String attendanceCode = getAttendanceCode(employee, date, entryByDate.get(date), holidayDates);
            cell.setCellValue(attendanceCode);

            IndexedColors color = getColorForTerm(attendanceCode);
            if (color != null && coloredStyles.containsKey(color)) {
                cell.setCellStyle(coloredStyles.get(color));
            } else {
                cell.setCellStyle(cellStyle);
            }

            // Count for summary logic removed
        }
    }

    /**
     * Determine attendance code based on time entry and date
     */
    private String getAttendanceCode(Employee employee, LocalDate date, TimeEntry entry, Set<LocalDate> holidayDates) {
        String shift = employee.getShift() != null ? employee.getShift() : "S1";

        // 1. Priority: Use DB value if present
        if (entry != null && entry.getAttendanceType() != null && !entry.getAttendanceType().trim().isEmpty()) {
            return entry.getAttendanceType();
        }

        // 2. Priority: Holiday Check (for missing data) - BEFORE Weekend check to show
        // GO on weekends if applicable?
        // Usually holidays override weekends (e.g. Christmas on Sunday).
        // User said: "if it is google holidayy ... instead of showing it S1/WO fill it
        // as S1/GO"
        if (holidayDates.contains(date)) {
            return shift + "/GO";
        }

        // 3. Priority: Weekend Check (for missing data)
        if (isWeekend(date)) {
            return shift + "/WO";
        }

        // 4. Priority: No Entry -> Default to Week Off (or Absent)
        if (entry == null) {
            return shift + "/WO"; // Default to week off if no entry
        }

        // 4. Fallback: Calculation based on time (for legacy data without
        // attendanceType)
        int timeInMins = entry.getTimeInMins();
        String attendanceType = entry.getAttendanceType(); // checking again for specific legacy mappings if needed

        // Legacy mapping if type was present but empty string? (Unlikely due to check
        // #1)
        // But let's keep the specific type checks just in case the DB has "LEAVE"
        // without shift prefix
        // (The user said "Whatever is the Attendance type just put it there", so maybe
        // we don't need this block
        // if the DB always has "S1/F". But if DB has "LEAVE", we might need to prefix
        // it.
        // Assuming DB has formatted string based on user input, but let's be safe).

        if (attendanceType != null) {
            if (attendanceType.equalsIgnoreCase("LEAVE")) {
                return shift + "/Leave";
            } else if (attendanceType.equalsIgnoreCase("SICK_LEAVE")) {
                return timeInMins < 480 ? shift + "/SLH" : shift + "/SL";
            } else if (attendanceType.equalsIgnoreCase("COMP_OFF")) {
                return shift + "/C";
            } else if (attendanceType.equalsIgnoreCase("HOLIDAY")) {
                return shift + "/GO";
            }
        }

        // Based on time worked
        if (timeInMins >= 480) { // Full day (8 hours)
            return shift + "/F";
        } else if (timeInMins > 0) { // Half day
            return shift + "/H";
        } else {
            return shift + "/A"; // Absent
        }
    }

    /**
     * Check if date is weekend
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Get list of dates in range
     */
    private List<LocalDate> getDateRange(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    /**
     * Create summary/aggregate rows at the bottom of the sheet
     */
}
