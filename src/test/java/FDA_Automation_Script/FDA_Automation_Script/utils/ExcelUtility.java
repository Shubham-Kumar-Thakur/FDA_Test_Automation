package FDA_Automation_Script.FDA_Automation_Script.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a Mirakl "Offers"/"Offers and Products" file-import template (.xlsx) using Apache POI.
 * First real usage of the poi-ooxml dependency in this framework — no Excel
 * utility existed previously (pom.xml declared the dependency but it was unused).
 */
public class ExcelUtility {

    private ExcelUtility() {
    }

    public static class OfferData {
        public final String sku;
        public final String price;
        public final String productName; // null when the template has no "Product Name" column
        public final String productId; // "Product ID" column (e.g. EAN/UPC) — distinct from Offer SKU

        public OfferData(String sku, String price, String productName, String productId) {
            this.sku = sku;
            this.price = price;
            this.productName = productName;
            this.productId = productId;
        }

        @Override
        public String toString() {
            return "OfferData{sku='" + sku + "', price='" + price + "', productName='" + productName
                + "', productId='" + productId + "'}";
        }
    }

    /**
     * Reads the first data row of the offer file's "Data" sheet.
     * Mirakl's standard offer template has 2 header rows: row 1 = human-readable
     * labels (e.g. "Offer SKU"), row 2 = machine field codes (e.g. "sku") — actual
     * offer rows start at row 3. Columns are located by row-1 header name rather
     * than a hardcoded index, so column re-ordering in the template is tolerated.
     */
    public static OfferData readFirstOffer(String filePath) {
        List<OfferData> offers = readAllOffers(filePath);
        if (offers.isEmpty()) {
            throw new IllegalStateException("No offer data row found in Excel file: " + filePath);
        }
        OfferData offer = offers.get(0);
        LoggerUtility.info("Excel offer read from " + filePath + ": " + offer);
        return offer;
    }

    /**
     * Reads every data row (SKU, price, product name/ID) from the offer file's "Data" sheet.
     * Used to iterate all SKUs for per-product catalog validation (Fragile/MSI) in TC_E2E_009.
     */
    public static List<OfferData> readAllOffers(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet("Data");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalStateException("Excel header row missing: " + filePath);
            }

            Map<String, Integer> columnIndex = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                columnIndex.put(getCellValueAsString(cell).trim(), cell.getColumnIndex());
            }

            Integer skuCol = columnIndex.get("Offer SKU");
            Integer priceCol = columnIndex.get("Offer Price");
            Integer nameCol = columnIndex.get("Product Name"); // not present in the standard offer template
            Integer productIdCol = columnIndex.get("Product ID");

            if (skuCol == null || priceCol == null) {
                throw new IllegalStateException(
                    "Excel file missing required 'Offer SKU'/'Offer Price' columns: " + filePath);
            }

            List<OfferData> offers = new ArrayList<>();
            for (int r = 2; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String sku = getCellValueAsString(row.getCell(skuCol)).trim();
                if (sku.isEmpty()) continue;

                String price = getCellValueAsString(row.getCell(priceCol)).trim();
                String name = (nameCol != null) ? getCellValueAsString(row.getCell(nameCol)).trim() : null;
                String productId = (productIdCol != null) ? getCellValueAsString(row.getCell(productIdCol)).trim() : null;

                offers.add(new OfferData(sku, price, (name == null || name.isEmpty()) ? null : name,
                    (productId == null || productId.isEmpty()) ? null : productId));
            }
            LoggerUtility.info("Excel: read " + offers.size() + " offer row(s) from " + filePath);
            return offers;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel offer file: " + filePath, e);
        }
    }

    /**
     * Overwrites the "Offer Price" cell for the given SKU's row in the Data sheet and saves the
     * file in place. Used by TC_E2E_009 to inject a tester-provided price before the file is
     * uploaded to Mirakl. The read (FileInputStream) and write (FileOutputStream) are done in
     * separate steps against the same path — opening both at once fails with a file-lock error
     * on Windows.
     */
    public static void updatePrice(String filePath, String sku, String newPrice) {
        Workbook workbook;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            workbook = new XSSFWorkbook(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open Excel offer file for price update: " + filePath, e);
        }

        try {
            Sheet sheet = workbook.getSheet("Data");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalStateException("Excel header row missing: " + filePath);
            }

            Map<String, Integer> columnIndex = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                columnIndex.put(getCellValueAsString(cell).trim(), cell.getColumnIndex());
            }

            Integer skuCol = columnIndex.get("Offer SKU");
            Integer priceCol = columnIndex.get("Offer Price");
            if (skuCol == null || priceCol == null) {
                throw new IllegalStateException(
                    "Excel file missing required 'Offer SKU'/'Offer Price' columns: " + filePath);
            }

            boolean updated = false;
            for (int r = 2; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String rowSku = getCellValueAsString(row.getCell(skuCol)).trim();
                if (rowSku.equals(sku)) {
                    Cell priceCell = row.getCell(priceCol);
                    if (priceCell == null) {
                        priceCell = row.createCell(priceCol);
                    }
                    priceCell.setCellValue(Double.parseDouble(newPrice));
                    updated = true;
                }
            }
            if (!updated) {
                throw new IllegalStateException("SKU '" + sku + "' not found in Excel file to update price: " + filePath);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
            LoggerUtility.info("Excel: updated Offer Price for SKU '" + sku + "' to '" + newPrice + "' in " + filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write updated Excel offer price: " + filePath, e);
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
                // best-effort close
            }
        }
    }

    /** Convenience wrapper — all Offer SKU values from the file, in row order. */
    public static List<String> getAllSKUs(String filePath) {
        List<String> skus = new ArrayList<>();
        for (OfferData offer : readAllOffers(filePath)) {
            skus.add(offer.sku);
        }
        return skus;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
