package com.fda.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExcelUtils {
    private static final Logger log = LogManager.getLogger(ExcelUtils.class);

    private ExcelUtils() {}

    /**
     * Reads an Excel sheet and returns rows as list of header-to-value maps.
     * Row 0 is treated as header; subsequent rows are data.
     */
    public static List<Map<String, String>> readSheet(String filePath, String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new IllegalArgumentException("Sheet not found: " + sheetName);

            Row headerRow = sheet.getRow(0);
            int colCount = headerRow.getLastCellNum();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int c = 0; c < colCount; c++) {
                    String header = getCellValue(headerRow.getCell(c));
                    String value = getCellValue(row.getCell(c));
                    rowMap.put(header, value);
                }
                data.add(rowMap);
            }
        } catch (IOException e) {
            log.error("Failed to read Excel: {}", filePath, e);
            throw new RuntimeException(e);
        }
        return data;
    }

    /**
     * Returns data as Object[][] for use with TestNG @DataProvider.
     * Each row is a Map&lt;String, String&gt; accessible in the test method.
     */
    public static Object[][] toDataProvider(String filePath, String sheetName) {
        List<Map<String, String>> rows = readSheet(filePath, sheetName);
        Object[][] result = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            result[i][0] = rows.get(i);
        }
        return result;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
