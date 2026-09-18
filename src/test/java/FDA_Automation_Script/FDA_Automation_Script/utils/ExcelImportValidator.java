package FDA_Automation_Script.FDA_Automation_Script.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

// Validates and auto-corrects the "Data" sheet of a Mirakl combined product+offer import file
// (verified real column structure, 2026-09-07): row 0 = display labels, row 1 = technical field
// keys, row 2+ = actual data. Known-valid warehouse values (seller_warehouse, fda_warehouse_1..7)
// match those confirmed live for Tc_manualoffercreation_01's Logistics dropdown.
public class ExcelImportValidator {

    private static final String SHEET_NAME = "Data";
    private static final int HEADER_KEY_ROW = 1;
    private static final int FIRST_DATA_ROW = 2;
    private static final String DEFAULT_CATEGORY = "Food & Beverages > Rice";
    private static final String DEFAULT_PRICE = "200";
    private static final String DEFAULT_QUANTITY = "1000";
    private static final String DEFAULT_CONDITION = "New";
    private static final String DEFAULT_WAREHOUSE = "seller_warehouse";
    private static final String DEFAULT_LEADTIME_TO_SHIP = "2";
    private static final String DEFAULT_MIN_ORDER_QUANTITY = "1";
    private static final String DEFAULT_MAX_ORDER_QUANTITY = "1000";
    private static final String REFERENCE_SHEET_NAME = "ReferenceData";
    private static final List<String> VALID_WAREHOUSES = List.of(
        "seller_warehouse", "fda_warehouse_1", "fda_warehouse_2", "fda_warehouse_3",
        "fda_warehouse_4", "fda_warehouse_5", "fda_warehouse_6", "fda_warehouse_7");

    public static class ValidationResult {
        public final boolean wasValid;
        public final String fileToUpload;
        public final List<String> issues;

        ValidationResult(boolean wasValid, String fileToUpload, List<String> issues) {
            this.wasValid = wasValid;
            this.fileToUpload = fileToUpload;
            this.issues = issues;
        }
    }

    public static ValidationResult validateAndFix(String originalFilePath) throws IOException {
        List<String> issues = new ArrayList<>();
        boolean anyRowFixed = false;
        String fileToUpload;

        // Single workbook instance for the whole read-modify-write pass — never re-created mid-fix.
        try (FileInputStream fis = new FileInputStream(originalFilePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                issues.add("Sheet '" + SHEET_NAME + "' not found in workbook");
                LoggerUtility.warn("ExcelImportValidator: " + issues.get(issues.size() - 1));
                return new ValidationResult(false, originalFilePath, issues);
            }

            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            requireColumns(col, "name", "category", "brand", "shop_sku", "sku", "price", "quantity", "logistics");

            // Diagnostic only, logged once (not per-row): confirms what "New" actually represents in
            // this file's own reference data, rather than assuming it — the "state" reference column
            // only ever contains "New" in this file, matching DEFAULT_CONDITION.
            List<String> validStates = getDistinctReferenceValues(workbook, "state", 5);
            LoggerUtility.info("ExcelImportValidator: valid 'state' values from ReferenceData (first 5): " + validStates);

            int lastRow = sheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                LoggerUtility.info("ExcelImportValidator: Row " + r + " BEFORE validateAndFix — price="
                    + getCellString(row, col.get("price")) + ", quantity=" + getCellString(row, col.get("quantity"))
                    + ", state=" + getCellString(row, col.get("state")) + ", logistics=" + getCellString(row, col.get("logistics"))
                    + ", msi=" + (col.containsKey("msi") ? getCellString(row, col.get("msi")) : "")
                    + ", is_fragile=" + (col.containsKey("is_fragile") ? getCellString(row, col.get("is_fragile")) : "")
                    + ", shop_sku=" + getCellString(row, col.get("shop_sku"))
                    + ", product-id=" + (col.containsKey("product-id") ? getCellString(row, col.get("product-id")) : "")
                    + ", sku=" + getCellString(row, col.get("sku")));

                boolean rowFixed = false;
                rowFixed |= fixIfBlank(row, col.get("name"), "Product Title", "AutoProduct_" + System.currentTimeMillis(), issues, r);
                rowFixed |= fixIfBlank(row, col.get("category"), "Category", DEFAULT_CATEGORY, issues, r);
                rowFixed |= fixIfBlank(row, col.get("brand"), "Brand", "Generic", issues, r);

                String shopSku = getCellString(row, col.get("shop_sku"));
                if (isBlank(shopSku)) {
                    shopSku = "SKU" + System.currentTimeMillis();
                    setCellString(row, col.get("shop_sku"), shopSku);
                    issues.add("Row " + r + ": Shop SKU was blank, generated " + shopSku);
                    rowFixed = true;
                }
                // Offer SKU (Offer ID) is a DISTINCT identifier from the Product ID/Shop SKU — only
                // fill it in if genuinely blank, never force it to match shop_sku. Per business rule:
                // Product ID stays as generated elsewhere; Offer ID is its own alphanumeric value.
                String offerSku = getCellString(row, col.get("sku"));
                if (isBlank(offerSku)) {
                    offerSku = generateAlphanumericOfferSku(r);
                    setCellString(row, col.get("sku"), offerSku);
                    issues.add("Row " + r + ": Offer SKU was blank, generated " + offerSku);
                    rowFixed = true;
                }

                double price = getCellNumeric(row, col.get("price"));
                if (price <= 0) {
                    setCellNumeric(row, col.get("price"), Double.parseDouble(DEFAULT_PRICE));
                    issues.add("Row " + r + ": Price was " + price + " (must be > 0), set to " + DEFAULT_PRICE);
                    rowFixed = true;
                }

                double quantity = getCellNumeric(row, col.get("quantity"));
                if (quantity < 0) {
                    setCellNumeric(row, col.get("quantity"), Double.parseDouble(DEFAULT_QUANTITY));
                    issues.add("Row " + r + ": Quantity was " + quantity + " (must be >= 0), set to " + DEFAULT_QUANTITY);
                    rowFixed = true;
                } else if (getCellString(row, col.get("quantity")).isBlank()) {
                    setCellNumeric(row, col.get("quantity"), Double.parseDouble(DEFAULT_QUANTITY));
                    issues.add("Row " + r + ": Quantity was blank, set to " + DEFAULT_QUANTITY);
                    rowFixed = true;
                }

                String warehouse = getCellString(row, col.get("logistics"));
                if (isBlank(warehouse)) {
                    setCellString(row, col.get("logistics"), DEFAULT_WAREHOUSE);
                    issues.add("Row " + r + " logistics blank -> set to " + DEFAULT_WAREHOUSE);
                    rowFixed = true;
                } else if (!VALID_WAREHOUSES.contains(warehouse)) {
                    setCellString(row, col.get("logistics"), DEFAULT_WAREHOUSE);
                    issues.add("Row " + r + " logistics invalid ('" + warehouse + "') -> set to " + DEFAULT_WAREHOUSE);
                    rowFixed = true;
                }

                // Defensive fallback for the offer's Condition (state) — mirrors the price/quantity/
                // warehouse fix-if-blank pattern above.
                if (col.containsKey("state") && isBlank(getCellString(row, col.get("state")))) {
                    setCellString(row, col.get("state"), DEFAULT_CONDITION);
                    issues.add("Row " + r + ": Condition/State was blank, set to " + DEFAULT_CONDITION);
                    rowFixed = true;
                }

                // Offer mandatory fields that this file's rows always leave blank — Mirakl accepts
                // the import without them but they're required for a fully-formed offer.
                if (col.containsKey("leadtime-to-ship") && isBlank(getCellString(row, col.get("leadtime-to-ship")))) {
                    setCellString(row, col.get("leadtime-to-ship"), DEFAULT_LEADTIME_TO_SHIP);
                    issues.add("Row " + r + ": leadtime-to-ship was blank, set to " + DEFAULT_LEADTIME_TO_SHIP);
                    rowFixed = true;
                }
                if (col.containsKey("min-order-quantity") && isBlank(getCellString(row, col.get("min-order-quantity")))) {
                    setCellString(row, col.get("min-order-quantity"), DEFAULT_MIN_ORDER_QUANTITY);
                    issues.add("Row " + r + ": min-order-quantity was blank, set to " + DEFAULT_MIN_ORDER_QUANTITY);
                    rowFixed = true;
                }
                if (col.containsKey("max-order-quantity") && isBlank(getCellString(row, col.get("max-order-quantity")))) {
                    setCellString(row, col.get("max-order-quantity"), DEFAULT_MAX_ORDER_QUANTITY);
                    issues.add("Row " + r + ": max-order-quantity was blank, set to " + DEFAULT_MAX_ORDER_QUANTITY);
                    rowFixed = true;
                }

                // Fragile: Mirakl's Catalog Management dropdown only accepts Si/No — normalize
                // true/TRUE/1/false/FALSE/0 here too (in addition to regenerateProductIds()) so
                // validateAndFix() is a self-sufficient safety net regardless of call order.
                rowFixed |= normalizeSiNoField(row, col, "is_fragile", r, "No");
                // MSI: CONFIRMED live (2026-09-12, real Mirakl import-error attachment) — writing
                // "No"/"Yes" for msi into the UPLOADED file causes a genuine file-import-time
                // rejection: "The format of field 'msi' is not correct", with "0 offer(s) added" (the
                // whole offer silently fails to import, even though the product still gets created).
                // The SAME rejected row's is_fragile='No' had ZERO error — msi enforces a strict
                // boolean true/false FORMAT at import time, a completely different, stricter
                // validation than the LATER Catalog Management "Invalid data" UI check (which does
                // want Si/No/Yes). So msi must stay untouched as true/false in the uploaded file; the
                // true->Yes / false->No mapping only happens at live UI correction time in
                // Tc_fileimportproductoffer_01 (mapExcelMsiToMiraklOption), never written back here.

                // Offer linkage: product-id must equal shop_sku for the offer to attach to the
                // (re)generated product — enforce and log, don't just assume regenerateProductIds()
                // already got it right.
                if (col.containsKey("product-id")) {
                    String currentShopSku = getCellString(row, col.get("shop_sku"));
                    String currentProductId = getCellString(row, col.get("product-id"));
                    if (!currentShopSku.equals(currentProductId)) {
                        setCellString(row, col.get("product-id"), currentShopSku);
                        issues.add("Row " + r + ": product-id '" + currentProductId + "' did not match shop_sku '"
                            + currentShopSku + "' — corrected for offer linkage");
                        rowFixed = true;
                        LoggerUtility.info("ExcelImportValidator: Row " + r + " offer linkage corrected — product-id set to '"
                            + currentShopSku + "' to match shop_sku");
                    } else {
                        LoggerUtility.info("ExcelImportValidator: Row " + r + " offer linkage verified — product-id == shop_sku ('"
                            + currentShopSku + "')");
                    }
                }

                LoggerUtility.info("ExcelImportValidator: Row " + r + " AFTER validateAndFix — price="
                    + getCellString(row, col.get("price")) + ", quantity=" + getCellString(row, col.get("quantity"))
                    + ", state=" + getCellString(row, col.get("state")) + ", logistics=" + getCellString(row, col.get("logistics"))
                    + ", msi=" + (col.containsKey("msi") ? getCellString(row, col.get("msi")) : "")
                    + ", is_fragile=" + (col.containsKey("is_fragile") ? getCellString(row, col.get("is_fragile")) : "")
                    + ", shop_sku=" + getCellString(row, col.get("shop_sku"))
                    + ", product-id=" + (col.containsKey("product-id") ? getCellString(row, col.get("product-id")) : "")
                    + ", sku=" + getCellString(row, col.get("sku")));

                if (rowFixed) anyRowFixed = true;
            }

            File fixedDir = new File("test-output/fixed");
            if (!fixedDir.exists()) fixedDir.mkdirs();
            String fixedPath = new File(fixedDir, "fixed_combined_import.xlsx").getAbsolutePath();

            if (!anyRowFixed) {
                LoggerUtility.info("ExcelImportValidator: file is valid, no corrections needed — " + originalFilePath);
                fileToUpload = originalFilePath;
            } else {
                // Same in-memory workbook, written once — no second Workbook object created.
                try (FileOutputStream fos = new FileOutputStream(fixedPath)) {
                    workbook.write(fos);
                }
                LoggerUtility.warn("ExcelImportValidator: " + issues.size() + " issue(s) found and auto-corrected. Fixed file: " + fixedPath);
                for (String issue : issues) {
                    LoggerUtility.warn("ExcelImportValidator: " + issue);
                }
                fileToUpload = fixedPath;
            }
        }

        // Hard verification against whatever file will actually be uploaded — reopened fresh from
        // disk (not the in-memory object above) so it catches real serialization issues, not just
        // in-memory state. Throws loudly rather than silently uploading a file Mirakl will reject.
        try (FileInputStream verifyFis = new FileInputStream(fileToUpload);
             Workbook verifyWb = WorkbookFactory.create(verifyFis)) {
            Sheet verifySheet = verifyWb.getSheet(SHEET_NAME);
            Map<String, Integer> verifyCol = buildColumnIndex(verifySheet.getRow(HEADER_KEY_ROW));
            int lastRow = verifySheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = verifySheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                String priceVal = getCellString(row, verifyCol.get("price"));
                String qtyVal = getCellString(row, verifyCol.get("quantity"));
                String stateVal = getCellString(row, verifyCol.get("state"));
                String logisticsVal = getCellString(row, verifyCol.get("logistics"));
                String msiVal = verifyCol.containsKey("msi") ? getCellString(row, verifyCol.get("msi")) : "";
                String fragileVal = verifyCol.containsKey("is_fragile") ? getCellString(row, verifyCol.get("is_fragile")) : "";
                String shopSkuVal = getCellString(row, verifyCol.get("shop_sku"));
                String productIdVal = verifyCol.containsKey("product-id") ? getCellString(row, verifyCol.get("product-id")) : shopSkuVal;
                String skuVal = getCellString(row, verifyCol.get("sku"));

                // MSI must stay true/false in the uploaded file — CONFIRMED live (2026-09-12) that
                // "No"/"Yes" causes a real file-import-time rejection ("format of field 'msi' is not
                // correct", 0 offers added). Si/No/Yes are also tolerated here defensively in case a
                // future template variant already uses them, but true/false is the expected value.
                if (!isBlank(msiVal) && !"si".equalsIgnoreCase(msiVal) && !"no".equalsIgnoreCase(msiVal)
                        && !"yes".equalsIgnoreCase(msiVal) && !"true".equalsIgnoreCase(msiVal) && !"false".equalsIgnoreCase(msiVal)) {
                    throw new RuntimeException("MSI value unrecognized - found '" + msiVal + "' but expected true/false (unchanged)");
                }
                if (!isBlank(fragileVal) && !"si".equalsIgnoreCase(fragileVal) && !"no".equalsIgnoreCase(fragileVal) && !"yes".equalsIgnoreCase(fragileVal)) {
                    throw new RuntimeException("Fragile still invalid after fix - found '" + fragileVal + "' but expected Yes/No or Si/No, not true/false");
                }
                if (isBlank(priceVal) || Double.parseDouble(priceVal) <= 0) {
                    throw new RuntimeException("Row " + r + " price invalid after fix: price=" + priceVal);
                }
                if (isBlank(qtyVal)) {
                    throw new RuntimeException("Row " + r + " quantity invalid after fix: quantity=" + qtyVal);
                }
                if (isBlank(stateVal)) {
                    throw new RuntimeException("Row " + r + " state invalid after fix: state=" + stateVal);
                }
                if (isBlank(logisticsVal) || !VALID_WAREHOUSES.contains(logisticsVal)) {
                    throw new RuntimeException("Row " + r + " logistics invalid after fix: logistics=" + logisticsVal);
                }
                if (!shopSkuVal.equals(productIdVal)) {
                    throw new RuntimeException("Offer linkage broken: Row " + r + " product-id='" + productIdVal
                        + "' != shop_sku='" + shopSkuVal + "'");
                }
                if (isBlank(skuVal) || !skuVal.toUpperCase().startsWith("OFR")) {
                    throw new RuntimeException("Offer linkage broken: Row " + r + " sku='" + skuVal + "' does not start with OFR");
                }

                LoggerUtility.info("ExcelImportValidator: Row " + r + " POST-WRITE VERIFY (validateAndFix) — price='" + priceVal
                    + "', quantity='" + qtyVal + "', state='" + stateVal + "', logistics='" + logisticsVal
                    + "', msi='" + msiVal + "', is_fragile='" + fragileVal + "', shop_sku='" + shopSkuVal
                    + "', product-id='" + productIdVal + "', sku='" + skuVal + "'");
            }
        }

        LoggerUtility.info("ExcelImportValidator: final file to upload — " + fileToUpload);
        return new ValidationResult(!anyRowFixed, fileToUpload, issues);
    }

    // Reads distinct, non-blank values for the given technical field name from the ReferenceData
    // sheet (row 0 = header, row 1+ = values) — diagnostic only, used to log what this file's own
    // reference data considers valid rather than assuming it. Returns an empty list (with a warning
    // logged) if the sheet or column isn't found, rather than failing the whole validation pass.
    private static List<String> getDistinctReferenceValues(Workbook workbook, String fieldKey, int limit) {
        List<String> values = new ArrayList<>();
        Sheet refSheet = workbook.getSheet(REFERENCE_SHEET_NAME);
        if (refSheet == null) {
            LoggerUtility.warn("ExcelImportValidator: '" + REFERENCE_SHEET_NAME + "' sheet not found — skipping reference lookup for '" + fieldKey + "'");
            return values;
        }
        Row refHeader = refSheet.getRow(0);
        if (refHeader == null) return values;
        Map<String, Integer> refCol = buildColumnIndex(refHeader);
        if (!refCol.containsKey(fieldKey)) {
            LoggerUtility.warn("ExcelImportValidator: '" + REFERENCE_SHEET_NAME + "' has no '" + fieldKey + "' column — skipping reference lookup");
            return values;
        }
        int fieldColIndex = refCol.get(fieldKey);
        int lastRow = refSheet.getLastRowNum();
        for (int r = 1; r <= lastRow && values.size() < limit; r++) {
            Row row = refSheet.getRow(r);
            if (row == null) continue;
            String v = getCellString(row, fieldColIndex);
            if (!v.isBlank() && !values.contains(v)) values.add(v);
        }
        return values;
    }

    // Generates fresh, unique Product ID (shop_sku) and UPC/EAN values for every data row and writes
    // them back to the SAME file (in place) — run once at the start of every test execution so a
    // product created by a previous run (already past "New" status in Mirakl) never collides with
    // the current run's expectations. Per explicit instruction: Product ID/Shop SKU and UPC/EAN are
    // both pure 8-digit numeric (no letter prefix), and are DISTINCT values from each other. The
    // Offer ID (sku) is a separate identifier — generated as alphanumeric — never forced to match the
    // Product ID; all other columns (name, category, price, etc.) are untouched.
    public static List<String> regenerateProductIds(String filePath) throws IOException {
        List<String> newProductIds = new ArrayList<>();
        long baseMillis = System.currentTimeMillis();

        // Read fully into memory and close the input handle BEFORE opening an output handle on the
        // same path — Windows refuses to open a file for writing while another handle still has it
        // open for reading (sharing violation), even from the same process.
        Workbook workbook;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            workbook = WorkbookFactory.create(fis);
        }

        try {
            // CONFIRMED live (2026-09-09, real file structure): this workbook has 3 sheets — "Data"
            // (the actual product+offer rows this class edits), "ReferenceData" (a large lookup/
            // validation sheet, not per-row data), and "Columns" (documentation only). Logged here so
            // any future column-mapping issue is diagnosable from the log without re-opening the file.
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sh = workbook.getSheetAt(s);
                LoggerUtility.info("ExcelImportValidator: sheet '" + sh.getSheetName()
                    + "' has " + (sh.getLastRowNum() + 1) + " row(s)");
            }

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IllegalStateException("ExcelImportValidator: sheet '" + SHEET_NAME + "' not found in " + filePath);
            }
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            requireColumns(col, "shop_sku", "sku");

            // Full header dump + key-column index summary, so a future column-mapping question is
            // answerable directly from the log without re-opening the file in a separate probe.
            Row headerRow = sheet.getRow(HEADER_KEY_ROW);
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                LoggerUtility.info("ExcelImportValidator: Data sheet header col[" + c + "] = '"
                    + getCellString(headerRow, c) + "'");
            }
            LoggerUtility.info("ExcelImportValidator: Data sheet key columns — "
                + "Product ID(shop_sku)=" + col.get("shop_sku")
                + ", Offer ID(sku)=" + col.get("sku")
                + ", product-id(link)=" + col.get("product-id")
                + ", price=" + col.get("price")
                + ", quantity=" + col.get("quantity")
                + ", logistics=" + col.get("logistics")
                + ", state=" + col.get("state")
                + ", msi=" + col.get("msi"));

            int lastRow = sheet.getLastRowNum();
            int rowOffset = 0;
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    LoggerUtility.info("ExcelImportValidator: Data sheet row " + r + " is null/empty — skipped");
                    continue;
                }

                // BEFORE dump — proves the offer fields weren't already blank prior to regeneration,
                // so a "went blank" claim can be directly checked against this line in the log.
                LoggerUtility.info("ExcelImportValidator: Row " + r + " BEFORE regeneration — price="
                    + getCellString(row, col.get("price")) + ", quantity=" + getCellString(row, col.get("quantity"))
                    + ", state=" + getCellString(row, col.get("state")) + ", logistics="
                    + getCellString(row, col.get("logistics")));

                // Per explicit instruction: Product ID, Shop SKU, and UPC/EAN are all the SAME pure
                // 8-digit numeric value (no "GM" prefix) — only the Offer ID is a separate,
                // independently-generated alphanumeric identifier.
                String newProductId = String.format("%08d", (baseMillis + rowOffset) % 100_000_000L);
                String newOfferId = generateAlphanumericOfferSku(rowOffset);
                rowOffset++;
                setCellString(row, col.get("shop_sku"), newProductId);
                setCellString(row, col.get("sku"), newOfferId);
                if (col.containsKey("upc_ean")) {
                    setCellString(row, col.get("upc_ean"), newProductId);
                }

                // Fragile: write Mirakl's actual accepted dropdown value directly — see
                // normalizeSiNoField() for the full rationale and live evidence.
                normalizeSiNoField(row, col, "is_fragile", r, "No");
                // MSI: CONFIRMED live (2026-09-12, real Mirakl import-error attachment) — left
                // completely untouched here. Writing "No"/"Yes" causes a genuine file-import-time
                // rejection ("The format of field 'msi' is not correct", 0 offers added) — msi
                // requires a strict boolean true/false format at import, unlike is_fragile which
                // tolerates Si/No/Yes strings fine. See normalizeSiNoField()'s is_fragile comment
                // above and Tc_fileimportproductoffer_01.mapExcelMsiToMiraklOption() for where the
                // true->Yes / false->No mapping actually happens (live UI correction only).

                // CONFIRMED live (2026-09-09, real file structure): "product-id" is a SEPARATE
                // column from "shop_sku" — it's the offer's link back to its product, interpreted
                // per "product-id-type" (this file uses SHOP_SKU). Regenerating shop_sku without
                // also updating product-id leaves the offer pointing at the OLD, stale shop_sku
                // value, so the offer never links to the newly (re)created product — this was the
                // root cause of "product Valid but offer never appears".
                if (col.containsKey("product-id")) {
                    String idType = col.containsKey("product-id-type") ? getCellString(row, col.get("product-id-type")) : "";
                    if (idType.isBlank() || "SHOP_SKU".equalsIgnoreCase(idType)) {
                        setCellString(row, col.get("product-id"), newProductId);
                    }
                }

                newProductIds.add(newProductId);
                LoggerUtility.info("ExcelImportValidator: Row " + r + " — generated Product ID/Shop SKU/UPC-EAN '"
                    + newProductId + "' (all identical), Offer ID '" + newOfferId + "'");

                // Full-row dump AFTER regeneration — every offer field (price, quantity, state, msi,
                // logistics) is logged exactly as it will be written to disk, so it's directly
                // verifiable from the test log whether offer data survived alongside product data.
                StringBuilder rowDump = new StringBuilder("ExcelImportValidator: Row " + r + " full data — ");
                for (Map.Entry<String, Integer> e : col.entrySet()) {
                    rowDump.append(e.getKey()).append('=').append(getCellString(row, e.getValue())).append(" | ");
                }
                LoggerUtility.info(rowDump.toString());
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
            LoggerUtility.info("ExcelImportValidator: regenerated " + newProductIds.size()
                + " Product ID(s)/UPC-EAN(s)/Offer ID(s) in place — " + filePath);
            LoggerUtility.info("ExcelImportValidator: fixed file is the SAME in-memory workbook written back out "
                + "(workbook.write(fos)) — every sheet/column/row is preserved as-is, offer columns "
                + "(price/quantity/state/msi/logistics) included, never a partial/product-only copy.");
        } finally {
            workbook.close();
        }

        // Safety net: reopen the file FRESH FROM DISK (not the in-memory object we just wrote) and
        // verify Condition/Quantity/Price genuinely survived the write — fails loudly instead of
        // silently uploading a broken file if a future change ever does introduce data loss here.
        try (FileInputStream verifyFis = new FileInputStream(filePath);
             Workbook verifyWb = WorkbookFactory.create(verifyFis)) {
            Sheet verifySheet = verifyWb.getSheet(SHEET_NAME);
            Map<String, Integer> verifyCol = buildColumnIndex(verifySheet.getRow(HEADER_KEY_ROW));
            int lastRow = verifySheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = verifySheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                String priceVal = getCellString(row, verifyCol.get("price"));
                String qtyVal = getCellString(row, verifyCol.get("quantity"));
                String stateVal = getCellString(row, verifyCol.get("state"));
                String fragileVal = verifyCol.containsKey("is_fragile") ? getCellString(row, verifyCol.get("is_fragile")) : "";
                String msiVal = verifyCol.containsKey("msi") ? getCellString(row, verifyCol.get("msi")) : "";
                LoggerUtility.info("ExcelImportValidator: Row " + r + " POST-WRITE VERIFY — price='" + priceVal
                    + "', quantity='" + qtyVal + "', state='" + stateVal + "', is_fragile='" + fragileVal
                    + "', msi='" + msiVal + "'");
                if (isBlank(priceVal) || isBlank(qtyVal) || isBlank(stateVal)) {
                    throw new IllegalStateException("ExcelImportValidator: Row " + r
                        + " has blank offer data (price/quantity/state) after write — refusing to proceed with a broken file");
                }
                if (!isBlank(fragileVal) && !"si".equalsIgnoreCase(fragileVal) && !"no".equalsIgnoreCase(fragileVal) && !"yes".equalsIgnoreCase(fragileVal)) {
                    throw new IllegalStateException("Fragile still invalid after fix - found '" + fragileVal + "' but expected Yes/No or Si/No, not true/false");
                }
                // MSI must stay true/false in the uploaded file — CONFIRMED live (2026-09-12) that
                // "No"/"Yes" causes a real file-import-time rejection ("format of field 'msi' is not
                // correct", 0 offers added).
                if (!isBlank(msiVal) && !"si".equalsIgnoreCase(msiVal) && !"no".equalsIgnoreCase(msiVal)
                        && !"yes".equalsIgnoreCase(msiVal) && !"true".equalsIgnoreCase(msiVal) && !"false".equalsIgnoreCase(msiVal)) {
                    throw new IllegalStateException("MSI value unrecognized - found '" + msiVal + "' but expected true/false (unchanged)");
                }
            }
        }
        return newProductIds;
    }

    // CONFIRMED live (2026-09-10) run17/run18: every live run confirmed Mirakl's Catalog Management
    // Fragile dropdown only ever renders "Si"/"No" or "Yes"/"No" options — writing "true"/"false"
    // there always produces "Invalid data" ("The value true does not belong to the list.") and forced
    // a manual Phase-2 UI correction on every single run. "No" is the one option confirmed present in
    // BOTH observed dropdown variants (Si/No and Yes/No) — is_fragile always passes
    // trueLikeDefault="No", so every value (regardless of original true/false-like content) collapses
    // to "No", which is guaranteed valid whichever variant Mirakl renders for that row. (MSI does NOT
    // use this function — CONFIRMED live 2026-09-12, msi enforces a strict boolean true/false format
    // at file-import time, a completely different/stricter check than this Catalog Management dropdown
    // validation; see the msi handling comments in validateAndFix()/regenerateProductIds() above and
    // Tc_fileimportproductoffer_01.mapExcelMsiToMiraklOption() for msi's own, separate live-UI mapping.)
    // Cell written as a STRING.
    private static boolean normalizeSiNoField(Row row, Map<String, Integer> col, String fieldKey, int rowNum, String trueLikeDefault) {
        if (!col.containsKey(fieldKey)) return false;
        String original = getCellString(row, col.get(fieldKey));
        String originalLower = original.toLowerCase();
        String mapped;
        if (originalLower.equals("no") || originalLower.equals("false") || originalLower.equals("0")) {
            mapped = "No";
        } else {
            mapped = trueLikeDefault; // si/true/1/yes/unrecognized/blank — per explicit instruction
        }
        if (!mapped.equals(original)) {
            setCellString(row, col.get(fieldKey), mapped);
            LoggerUtility.info("ExcelImportValidator: Row " + rowNum + " " + fieldKey.toUpperCase()
                + " original='" + original + "' corrected to '" + mapped + "' for offer fix");
            return true;
        }
        return false;
    }

    // Alphanumeric, distinct from the numeric-only Product ID format — base-36 encoding (digits +
    // uppercase letters) of a time-based value keeps it unique across rows/runs without ever
    // colliding with the "GM"+digits Product ID pattern.
    private static String generateAlphanumericOfferSku(int rowOffset) {
        long value = System.nanoTime() + rowOffset;
        return "OFR" + Long.toString(value, 36).toUpperCase();
    }

    // Returns the "sku" (Offer ID — a distinct identifier from shop_sku since regenerateProductIds())
    // column value for every non-empty data row. Kept for Offer-specific lookups (e.g. Mirakl Offers
    // section); the Catalog Management/storefront flow uses getAllProductIds() instead.
    public static List<String> getAllSkus(String filePath) throws IOException {
        List<String> skus = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            int lastRow = sheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                String sku = getCellString(row, col.get("sku"));
                if (!sku.isBlank()) skus.add(sku);
            }
        }
        return skus;
    }

    // Returns the "shop_sku" (Product ID — the "GM"+digits identifier confirmed live to be what
    // Catalog Management search/detail, Operator status grids, and the FDA storefront PDP all key
    // on) column value for every non-empty data row — drives the Catalog Management per-SKU loop.
    public static List<String> getAllProductIds(String filePath) throws IOException {
        List<String> productIds = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            int lastRow = sheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                String productId = getCellString(row, col.get("shop_sku"));
                if (!productId.isBlank()) productIds.add(productId);
            }
        }
        return productIds;
    }

    // Returns every column value (keyed by technical field name from row 1) for the data row whose
    // "shop_sku" (Product ID) matches the given value — used to look up expected Name/Price for FDA
    // storefront checks.
    public static Map<String, String> getRowByProductId(String filePath, String productId) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            int lastRow = sheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                if (productId.equals(getCellString(row, col.get("shop_sku")))) {
                    Map<String, String> values = new HashMap<>();
                    for (Map.Entry<String, Integer> e : col.entrySet()) {
                        values.put(e.getKey(), getCellString(row, e.getValue()));
                    }
                    return values;
                }
            }
            throw new IllegalArgumentException("Product ID not found in Excel file: " + productId);
        }
    }

    // Added for TC_E2E_004 — its Offer Excel file has a different schema (technical key "sku" is the
    // Offer SKU, not the shop_sku/Product ID getRowByProductId() reads), same "Data" sheet / row 0
    // labels / row 1 keys / row 2+ data convention this whole class already relies on. Returns every
    // column value for the data row whose "sku" (Offer SKU) matches the given value.
    public static Map<String, String> getRowBySku(String filePath, String sku) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            int lastRow = sheet.getLastRowNum();
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                if (sku.equals(getCellString(row, col.get("sku")))) {
                    Map<String, String> values = new HashMap<>();
                    for (Map.Entry<String, Integer> e : col.entrySet()) {
                        values.put(e.getKey(), getCellString(row, e.getValue()));
                    }
                    return values;
                }
            }
            throw new IllegalArgumentException("Offer SKU not found in Excel file: " + sku);
        }
    }

    // Added for TC_E2E_004 Part 6->7: per explicit user instruction, once the product reaches
    // Published, Mirakl may display a Product ID different from the shop_sku originally submitted
    // (see MiraklCatalogManagementPage.getDisplayedProductId()) — the Offer Excel's "product-id"
    // column must be rewritten to that new value before Part 7 uploads the file, or the offer will
    // fail to link to the published product. Read-then-write-in-place, same pattern as
    // regenerateProductIds() above (Windows won't allow overlapping read/write handles on one path).
    public static void updateOfferProductId(String offerFilePath, String sku, String newProductId) throws IOException {
        Workbook workbook;
        try (FileInputStream fis = new FileInputStream(offerFilePath)) {
            workbook = WorkbookFactory.create(fis);
        }
        try {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            requireColumns(col, "sku", "product-id");
            int lastRow = sheet.getLastRowNum();
            boolean updated = false;
            for (int r = FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                if (sku.equals(getCellString(row, col.get("sku")))) {
                    String oldProductId = getCellString(row, col.get("product-id"));
                    setCellString(row, col.get("product-id"), newProductId);
                    LoggerUtility.info("ExcelImportValidator: Offer SKU " + sku + " product-id '"
                        + oldProductId + "' -> '" + newProductId + "'");
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                throw new IllegalArgumentException("Offer SKU not found in Excel file, product-id not updated: " + sku);
            }
            try (FileOutputStream fos = new FileOutputStream(offerFilePath)) {
                workbook.write(fos);
            }
        } finally {
            workbook.close();
        }
    }

    // Added for TC_E2E_004 to support unattended re-execution (e.g. run repeatedly from Eclipse
    // without a human regenerating IDs between runs): generates a fresh 8-digit numeric Product ID
    // and an 8-character alphanumeric Offer SKU, writing both into the Product/Offer Excel files in
    // place — Product Excel's shop_sku/upc_ean, and the Offer Excel's sku + product-id (kept in sync
    // with the new Product ID so Phase 0's own product-id/shop_sku equality assertion still holds).
    // Only row FIRST_DATA_ROW (the single data row both files use) is touched.
    public static void regenerateProductAndOfferIds(String productExcelPath, String offerExcelPath) throws IOException {
        String newProductId = String.format("%08d", System.currentTimeMillis() % 100_000_000L);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        char letter1 = (char) ('A' + rnd.nextInt(26));
        char letter2 = (char) ('A' + rnd.nextInt(26));
        long sixDigits = (System.currentTimeMillis() + 37) % 1_000_000L;
        String newOfferSku = "" + letter1 + letter2 + String.format("%06d", sixDigits);

        // ROOT-CAUSED live (2026-09-18): a prior run's Offer Excel write failed with
        // "being used by another process" (the file was open in Excel) AFTER the Product Excel write
        // had already succeeded — leaving the two files permanently out of sync (Product Excel showing
        // the new ID, Offer Excel still showing the old one), which then failed Phase 0's product-id
        // match assertion on every subsequent run with no way to self-heal (Phase 0 asserts before
        // Part 1's self-healing check ever runs). Verify BOTH files are actually writable before
        // touching either one, so a lock on either file aborts cleanly with NO changes made, instead
        // of leaving a partially-updated, inconsistent pair.
        verifyWritable(productExcelPath);
        verifyWritable(offerExcelPath);

        Workbook productWb;
        try (FileInputStream fis = new FileInputStream(productExcelPath)) {
            productWb = WorkbookFactory.create(fis);
        }
        try {
            Sheet sheet = productWb.getSheet(SHEET_NAME);
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            requireColumns(col, "shop_sku");
            Row row = sheet.getRow(FIRST_DATA_ROW);
            String oldProductId = getCellString(row, col.get("shop_sku"));
            setCellString(row, col.get("shop_sku"), newProductId);
            if (col.containsKey("upc_ean")) {
                setCellString(row, col.get("upc_ean"), newProductId);
            }
            LoggerUtility.info("ExcelImportValidator: regenerated Product ID '" + oldProductId + "' -> '" + newProductId + "'");
            writeWorkbookWithRetry(productWb, productExcelPath);
        } finally {
            productWb.close();
        }

        Workbook offerWb;
        try (FileInputStream fis = new FileInputStream(offerExcelPath)) {
            offerWb = WorkbookFactory.create(fis);
        }
        try {
            Sheet sheet = offerWb.getSheet(SHEET_NAME);
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            requireColumns(col, "sku", "product-id");
            Row row = sheet.getRow(FIRST_DATA_ROW);
            String oldOfferSku = getCellString(row, col.get("sku"));
            String oldProductId = getCellString(row, col.get("product-id"));
            setCellString(row, col.get("sku"), newOfferSku);
            setCellString(row, col.get("product-id"), newProductId);
            LoggerUtility.info("ExcelImportValidator: regenerated Offer SKU '" + oldOfferSku + "' -> '" + newOfferSku
                + "', product-id '" + oldProductId + "' -> '" + newProductId + "'");
            writeWorkbookWithRetry(offerWb, offerExcelPath);
        } finally {
            offerWb.close();
        }
    }

    // Added for TC_E2E_004's Offer Import retry flow: per explicit requirement, a retry after a
    // failed Offer Excel import must NOT regenerate the Product ID/Shop SKU (Phase 1 identifiers are
    // fixed for the whole execution) — only the Offer SKU may change, and only when the prior failed
    // attempt's own history row shows it actually created/updated something (never blindly reused
    // once real data exists under it). Verifies product-id is unchanged before and after, so this can
    // never accidentally desync the pair it's meant to preserve.
    public static String regenerateOfferSkuOnly(String offerExcelPath, String expectedProductId) throws IOException {
        verifyWritable(offerExcelPath);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        char letter1 = (char) ('A' + rnd.nextInt(26));
        char letter2 = (char) ('A' + rnd.nextInt(26));
        long sixDigits = (System.currentTimeMillis() + 41) % 1_000_000L;
        String newOfferSku = "" + letter1 + letter2 + String.format("%06d", sixDigits);

        Workbook offerWb;
        try (FileInputStream fis = new FileInputStream(offerExcelPath)) {
            offerWb = WorkbookFactory.create(fis);
        }
        try {
            Sheet sheet = offerWb.getSheet(SHEET_NAME);
            Map<String, Integer> col = buildColumnIndex(sheet.getRow(HEADER_KEY_ROW));
            requireColumns(col, "sku", "product-id");
            Row row = sheet.getRow(FIRST_DATA_ROW);
            String currentProductId = getCellString(row, col.get("product-id"));
            if (!expectedProductId.equals(currentProductId)) {
                throw new IllegalStateException("ExcelImportValidator: refusing to regenerate Offer SKU — "
                    + "Offer Excel product-id ('" + currentProductId + "') no longer matches the expected "
                    + "Product Shop SKU ('" + expectedProductId + "'). No changes were made.");
            }
            String oldOfferSku = getCellString(row, col.get("sku"));
            setCellString(row, col.get("sku"), newOfferSku);
            LoggerUtility.info("ExcelImportValidator: regenerated Offer SKU only (Product ID unchanged, "
                + expectedProductId + ") '" + oldOfferSku + "' -> '" + newOfferSku + "'");
            writeWorkbookWithRetry(offerWb, offerExcelPath);
        } finally {
            offerWb.close();
        }
        return newOfferSku;
    }

    // Windows-safe lock check that never truncates the file (unlike opening a FileOutputStream,
    // which would destroy content the moment it's opened even if we abort right after) — a rename
    // of a file to its own name only succeeds if no other process currently holds it open/locked.
    private static void verifyWritable(String path) {
        File f = new File(path);
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (f.renameTo(f)) {
                return;
            }
            LoggerUtility.warn("ExcelImportValidator: " + path + " appears locked by another process "
                + "(attempt " + attempt + "/5) — retrying in case it's transient");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("ExcelImportValidator: " + path + " is locked by another process "
            + "(e.g. open in Excel) — close it and retry. No changes were made to either Excel file.");
    }

    // ROOT-CAUSED live (2026-09-18): verifyWritable() above only checks for a lock at the START of
    // this method — it does not protect against the file being opened (e.g. in Excel) in the window
    // between that check and the actual write, which is exactly what happened live: the pre-check
    // passed, the workbook was read into memory, and only the final FileOutputStream open failed with
    // "being used by another process", leaving the regeneration half-applied (the product write could
    // succeed while the offer write fails, or vice versa). Retrying the actual write itself — not just
    // the pre-check — closes that gap for any lock that appears mid-operation.
    private static void writeWorkbookWithRetry(Workbook workbook, String path) throws IOException {
        IOException lastError = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            try (FileOutputStream fos = new FileOutputStream(path)) {
                workbook.write(fos);
                return;
            } catch (IOException e) {
                lastError = e;
                LoggerUtility.warn("ExcelImportValidator: write to " + path + " failed (attempt " + attempt
                    + "/5) — " + e.getMessage() + " — retrying");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IOException("ExcelImportValidator: could not write " + path
            + " after 5 attempts — it is locked by another process (e.g. open in Excel). Close it and retry.", lastError);
    }

    // ROOT-CAUSED live (2026-09-18): both real Excel files (products-en_US-...xlsx and
    // offers-en_US-...xlsx) carry a SECOND data row (row 3) left over from whenever the file was
    // first created — hardcoded values like "AUTO_PROD_20260826162734384_2" /
    // "AUTO_OFFER_20260827120000004_2" that regenerateProductAndOfferIds() (and every other method in
    // this class) never touches, since they all only operate on FIRST_DATA_ROW. Mirakl's importer
    // reads the WHOLE sheet, not just row 2 — confirmed live via a stale Offer import history row
    // showing "Lines read: 2, Lines with errors: 2", matching exactly one error per data row. This
    // strips every data row except FIRST_DATA_ROW so only our own controlled row is ever uploaded.
    // Idempotent — once removed, a row never reappears in that file again.
    public static void removeExtraDataRows(String filePath) throws IOException {
        Workbook workbook;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            workbook = WorkbookFactory.create(fis);
        }
        try {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            int lastRow = sheet.getLastRowNum();
            if (lastRow <= FIRST_DATA_ROW) {
                LoggerUtility.info("ExcelImportValidator: " + filePath + " already has exactly one data row — nothing to remove");
                return;
            }
            int removed = 0;
            for (int r = lastRow; r > FIRST_DATA_ROW; r--) {
                Row row = sheet.getRow(r);
                if (row != null) {
                    sheet.removeRow(row);
                    removed++;
                }
            }
            LoggerUtility.warn("ExcelImportValidator: removed " + removed + " stale extra data row(s) beyond row "
                + FIRST_DATA_ROW + " from " + filePath + " (leftover template data Mirakl would otherwise "
                + "upload alongside this execution's own row)");
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        } finally {
            workbook.close();
        }
    }

    private static Map<String, Integer> buildColumnIndex(Row keyRow) {
        Map<String, Integer> col = new HashMap<>();
        for (int c = 0; c < keyRow.getLastCellNum(); c++) {
            String key = getCellString(keyRow, c);
            if (!key.isBlank()) col.put(key.trim(), c);
        }
        return col;
    }

    private static void requireColumns(Map<String, Integer> col, String... names) {
        for (String name : names) {
            if (!col.containsKey(name)) {
                throw new IllegalStateException("ExcelImportValidator: required column '" + name + "' not found in header row");
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

    private static boolean fixIfBlank(Row row, int colIndex, String label, String defaultValue, List<String> issues, int rowNum) {
        if (isBlank(getCellString(row, colIndex))) {
            setCellString(row, colIndex, defaultValue);
            issues.add("Row " + rowNum + ": " + label + " was blank, set to '" + defaultValue + "'");
            return true;
        }
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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

    private static void setCellString(Row row, Integer colIndex, String value) {
        if (colIndex == null) return;
        Cell cell = row.getCell(colIndex);
        if (cell == null) cell = row.createCell(colIndex);
        cell.setCellValue(value);
    }

    private static void setCellNumeric(Row row, Integer colIndex, double value) {
        if (colIndex == null) return;
        Cell cell = row.getCell(colIndex);
        if (cell == null) cell = row.createCell(colIndex);
        cell.setCellValue(value);
    }
}
