package FDA_Automation_Script.FDA_Automation_Script.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Reads/updates the "Data" sheet of a Mirakl Offer import Excel — same confirmed real structure as
// ExcelImportValidator (row 0 = display labels, row 1 = technical field keys e.g. "sku"/"product-id"/
// "price", row 2+ = actual data). Scoped to TC_OU_009's use case: a single-offer Excel where only the
// price needs to be read back and updated; unlike ExcelImportValidator this never fixes/regenerates
// any other field.
public class ExcelUtility {

    private static final String SHEET_NAME = "Data";
    private static final int HEADER_KEY_ROW = 1;
    private static final int FIRST_DATA_ROW = 2;

    private ExcelUtility() {}

    public static class OfferRow {
        public final int rowIndex;
        public final String offerSku;
        public final String productId;
        public final double price;

        OfferRow(int rowIndex, String offerSku, String productId, double price) {
            this.rowIndex = rowIndex;
            this.offerSku = offerSku;
            this.productId = productId;
            this.price = price;
        }
    }

    /** Reads the first non-empty data row (row 2+) of the offer Excel's "Data" sheet. */
    public static OfferRow readFirstOfferRow(String excelPath) {
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IllegalStateException("Sheet '" + SHEET_NAME + "' not found in " + excelPath);
            }
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            requireColumns(col, "sku", "product-id", "price");

            int lastRow = sheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                String offerSku = getCellString(row, col.get("sku"));
                String productId = getCellString(row, col.get("product-id"));
                double price = getCellNumeric(row, col.get("price"));
                LoggerUtility.info("ExcelUtility: read offer row " + r + " — offerSku=" + offerSku
                    + ", productId=" + productId + ", price=" + price);
                return new OfferRow(r, offerSku, productId, price);
            }
            throw new IllegalStateException("No data row found in '" + SHEET_NAME + "' sheet of " + excelPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read offer Excel: " + excelPath, e);
        }
    }

    /** Updates the price column of the first non-empty data row and saves the workbook in place. */
    public static void updatePrice(String excelPath, double newPrice) {
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IllegalStateException("Sheet '" + SHEET_NAME + "' not found in " + excelPath);
            }
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            requireColumns(col, "price");

            int lastRow = sheet.getLastRowNum();
            boolean updated = false;
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                setCellNumeric(row, col.get("price"), newPrice);
                LoggerUtility.info("ExcelUtility: updated row " + r + " price -> " + newPrice);
                updated = true;
                break;
            }
            if (!updated) {
                throw new IllegalStateException("No data row found to update price in " + excelPath);
            }

            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
                workbook.write(fos);
            }
            LoggerUtility.info("ExcelUtility: saved updated price to " + excelPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update offer Excel: " + excelPath, e);
        }
    }

    private static Map<String, Integer> buildColumnIndex(Row keyRow) {
        Map<String, Integer> col = new HashMap<>();
        if (keyRow == null) return col;
        for (int c = 0; c < keyRow.getLastCellNum(); c++) {
            String key = getCellString(keyRow, c);
            if (!key.isBlank()) col.put(key.trim(), c);
        }
        return col;
    }

    private static void requireColumns(Map<String, Integer> col, String... names) {
        for (String name : names) {
            if (!col.containsKey(name)) {
                throw new IllegalStateException("ExcelUtility: required column '" + name + "' not found in header row");
            }
        }
    }

    private static boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellString(row, cell.getColumnIndex()).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String getCellString(Row row, Integer colIndex) {
        if (colIndex == null) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private static double getCellNumeric(Row row, Integer colIndex) {
        if (colIndex == null) return 0;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        try {
            return Double.parseDouble(getCellString(row, colIndex));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void setCellNumeric(Row row, Integer colIndex, double value) {
        if (colIndex == null) return;
        Cell cell = row.getCell(colIndex);
        if (cell == null) cell = row.createCell(colIndex);
        cell.setCellValue(value);
    }
}
