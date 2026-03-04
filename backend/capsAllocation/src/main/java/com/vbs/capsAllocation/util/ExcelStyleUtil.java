package com.vbs.capsAllocation.util;

import org.apache.poi.ss.usermodel.*;

/**
 * Utility class for creating styled Excel cells and rows
 */
public class ExcelStyleUtil {

    /**
     * Create a header cell style with bold text and background color
     */
    public static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();

        // Background color (light blue)
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Borders
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Font - bold
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);

        // Alignment
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        return headerStyle;
    }

    /**
     * Create a standard cell style with borders
     */
    public static CellStyle createCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();

        // Borders
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);

        // Alignment
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        return cellStyle;
    }

    /**
     * Create a date cell style
     */
    public static CellStyle createDateStyle(Workbook workbook) {
        CellStyle dateStyle = createCellStyle(workbook);
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        return dateStyle;
    }

    /**
     * Create a number cell style
     */
    public static CellStyle createNumberStyle(Workbook workbook) {
        CellStyle numberStyle = createCellStyle(workbook);
        numberStyle.setAlignment(HorizontalAlignment.RIGHT);
        return numberStyle;
    }

    /**
     * Create a percentage cell style
     */
    public static CellStyle createPercentStyle(Workbook workbook) {
        CellStyle percentStyle = createCellStyle(workbook);
        CreationHelper createHelper = workbook.getCreationHelper();
        percentStyle.setDataFormat(createHelper.createDataFormat().getFormat("0.00%"));
        percentStyle.setAlignment(HorizontalAlignment.RIGHT);
        return percentStyle;
    }

    /**
     * Auto-size all columns in a sheet
     */
    public static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // Add a bit of padding
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 500);
        }
    }
}
