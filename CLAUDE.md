# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

End-to-end automation framework for the Farmacias del Ahorro (FDA) e-commerce ecosystem.
Covers the full order lifecycle: FDA storefront → Kibo OMS (REST API) → Mirakl marketplace → Shipment webhook APIs.

## Technology Stack

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 17 | Language |
| Selenium WebDriver | 4.25.0 | Browser automation |
| TestNG | 7.10.2 | Test execution framework |
| WebDriverManager | 5.9.2 | Auto driver management |
| REST Assured | 5.5.0 | API testing |
| Apache POI | 5.3.0 | Excel data utility |
| Log4j2 | 2.24.3 | Logging |
| Jackson | 2.18.2 | JSON serialization for REST Assured |
| ExtentReports | 5.1.2 | HTML execution report (`ExecutionReport.html`) |
| Maven | 3.x | Build + dependency management |

## Build

```bash
mvn clean compile
```

## Run Tests

```bash
mvn clean test
```

Run with specific suite (default is `testng.xml`; override with any custom XML):

```bash
mvn test -DsuiteXmlFile=src/test/resources/testng.xml
```

Run a single test class:

```bash
mvn test -Dtest=TC_FBO_001_Test
```

## Test Case Catalog

| Class | Description |
|-------|-------------|
| `TC_FBO_001_Test` | 1 SKU qty=1 → full fulfillment (Envioclick or Skydropx → Received) |
| `TC_FBO_002_Test` | 2 SKUs → 2 Mirakl shipments (WEB-A + WEB-B) → both Received |
| `TC_FBO_003_Test` | 1 SKU qty=2 → 1 Mirakl shipment (WEB-A) → Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_004_Test` | 2 SKUs qty=2 each → 2 Mirakl shipments (WEB-A + WEB-B) → both Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_005_Test` | 2 SKUs qty=1 each from **2 different 3P sellers** → 2 Mirakl shipments (WEB-A + WEB-B) → both Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_006_Test` | 2 SKUs qty=2 each from **2 different 3P sellers** → 2 Mirakl shipments (WEB-A + WEB-B) → both Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_007_Test` | 1 SKU qty=1 → **PayPal** payment → Mirakl accept WEB-A → Kibo API → Envioclick/Skydropx → Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions; **PayPal password is blank** — update `TC_PAYPAL_PASS` constant before running |
| `TC_FBO_008_Test` | 2 SKUs qty=1 each → **PayPal** → Mirakl accept WEB-A + WEB-B → both Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions; **PayPal password is blank** — update `TC_PAYPAL_PASS` before running; re-blank before committing |
| `TC_FBO_009_Test` | 1 SKU qty=2 → **PayPal** → Mirakl accept WEB-A → Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions; **PayPal password is blank** — update `TC_PAYPAL_PASS` before running; re-blank before committing |
| `TC_FBO_010_Test` | 2 SKUs qty=2 each → **PayPal** → Mirakl accept WEB-A + WEB-B → both Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions; **PayPal password is blank** — update `TC_PAYPAL_PASS` before running; re-blank before committing |
| `TC_FBO_011_Test` | 2 SKUs qty=1 each from **2 different 3P sellers** → **PayPal** → Mirakl accept WEB-A + WEB-B → both Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions; **PayPal password is blank** — update `TC_PAYPAL_PASS` before running; re-blank before committing |
| `TC_FBO_012_Test` | 2 SKUs qty=2 each from **2 different 3P sellers** → **PayPal** → Mirakl accept WEB-A + WEB-B → both Received; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions; **PayPal password is blank** — update `TC_PAYPAL_PASS` before running; re-blank before committing |
| `TC_FBO_SPLIT_01_Test` | 8 SKUs → 6 Mirakl shipments (WEB-A…WEB-F) → all Received |
| `TC_FBO_020_Test` | 1 SKU qty=1 → full fulfillment → return → compliance → full refund → Closed; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_021_Test` | 2 SKUs → full fulfillment → return → compliance → full refund → Closed |
| `TC_FBO_022_Test` | 1 SKU qty=2 → 1 Mirakl shipment (WEB-A) → full fulfillment → return → compliance → full refund → Closed; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_023_Test` | 2 SKUs qty=2 each from **1 3P seller** → 2 Mirakl shipments (WEB-A + WEB-B) → full fulfillment → return qty=2 → compliance → full refund → Closed; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_026_Test` | 1 SKU qty=2 from **1 3P seller** → 1 Mirakl shipment (WEB-A) → full fulfillment → **partial** return qty=1 → compliance → partial refund ("Refund part of the order") → Kibo return status = closed; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_027_Test` | 1 SKU → Mirakl accept → full cancel via API → Canceled |
| `TC_FBO_028_Test` | 2 SKUs → Mirakl accept WEB-A + WEB-B → cancel WEB-A only → WEB-A=Canceled, WEB-B=Awaiting |
| `TC_FBO_029_Test` | 1 SKU qty=2 from **1 3P seller** → Mirakl accept WEB-A → 1-min wait → full cancel via API → Canceled; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_030_Test` | 2 SKUs qty=2 each from **1 3P seller** → Mirakl accept WEB-A + WEB-B → 1-min wait → cancel WEB-A only → WEB-A=Canceled, WEB-B=Awaiting; resolves `offer_sku` via inline Mirakl API call; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_031_Test` | 2 SKUs qty=1 each from **2 different 3P sellers** → Mirakl accept WEB-A + WEB-B → 1-min wait → cancel WEB-A only using `order_line_id` format (`shipmentRefA + "-1"`) → WEB-A=Canceled, WEB-B=Awaiting; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_FBO_032_Test` | 2 SKUs qty=2 each from **2 different 3P sellers** → Mirakl accept WEB-A + WEB-B → 1-min wait → cancel WEB-A only using `order_line_id` format (`shipmentRefA + "-1"`) → WEB-A=Canceled, WEB-B=Awaiting; uses separate FDA account (`mgowda@kognivera.com`), `@BeforeClass`/`@AfterClass` swap sessions |
| `TC_E2E_009_Test` | **Current implementation (as of the code in this repo) differs from the description below it in this table's earlier revisions — no Adobe Admin phase, no FDA login exists in the code, and the Mirakl Operator verification phase has been intentionally removed (per explicit instruction) — only Seller and FDA storefront phases remain.** Seller updates an offer's price via Excel import, verified end-to-end: Mirakl Seller → FDA storefront PDP. Prompts interactively on stdin (`promptForPrice()`) for the new price before anything else runs (Enter with no input keeps the existing Excel price); writes it into the Excel file via `ExcelUtility.updatePrice()`. Reads the Mirakl Seller login credentials from environment variables (`MIRAKL_SELLER_USERNAME/PASSWORD`) via `requireEnv()` — never `config.properties`, never hardcoded. **`ADOBE_ADMIN_USERNAME/PASSWORD`, `MIRAKL_OPERATOR_USERNAME/PASSWORD`, and the `pages/adobe/` classes are not referenced anywhere in `TC_E2E_009_Test.java`** despite existing in the codebase and being documented elsewhere in this file — treat those as unused/legacy until wired back in. **Overrides `BaseClass.setupSuite()`** so Mirakl login goes straight to the Seller account, never the suite-default `mirakl.username`; only safe in its own isolated suite XML (`testng_e2e_009.xml`) — see [Known Issues](#known-issues). `MiraklLoginPage.login()` asserts a dashboard element (`Price and stock` nav) is reached before returning for this TC's Seller login. Flow: Seller notes current offer price (searches by **Product ID** — numeric, not Offer SKU — via `MiraklOffersPage.searchByProductId()`; waits for the grid via `waitForSingleResult()` — a genuine `WebDriverWait` poll, not `Thread.sleep` — and verifies the settled row's own Offer SKU matches before trusting it) → imports the Excel file via File Import → clicks Import, then polls the **"Track offer imports" report** (`MiraklFileImportPage.waitForImportCompletion()`, every 8s up to 2 min) for a terminal status and asserts **0 failed rows** (`getLatestImportFailedCount()`) — the transient "File imported" banner is checked only best-effort/non-blocking, since a real run (2026-09-10) showed it does not reliably appear before the form resets → re-searches by Product ID (same explicit-wait pattern) and asserts the Seller price changed and matches the Excel price → opens a **second, independent, incognito `ChromeDriver`** for the FDA storefront (never reuses Seller cookies) and **actively polls** (every 30s, up to a 10-minute SLA) searching by SKU and checking the PDP price, instead of a blind wait — throws a distinctly-worded `TIMEOUT:` error (not a normal assertion failure) if the SLA is exceeded — then also searches by Product Name, verifying PDP Name/SKU/Price against the Excel data each time (`PriceUtility.pricesEqual()`) |

## Project Structure

Note the package path repeats the project name (`FDA_Automation_Script.FDA_Automation_Script`) — this is intentional/existing convention, not a typo; new classes must follow it or they won't resolve in `testng.xml` `<class name="...">` entries.

```
src/
  test/
    java/FDA_Automation_Script/FDA_Automation_Script/
      base/
        BaseClass.java              ← @BeforeSuite / @AfterSuite / @AfterMethod, tab helpers, refreshAndWait()
      appenders/
        ExtentReportLog4j2Appender.java ← Custom Log4j2 `@Plugin(name="ExtentReport")`; forwards log events to active ExtentTest; re-entrancy guard prevents recursion
      listeners/
        TestNGListener.java         ← Auto-screenshot on pass/fail
        ExtentReportListener.java   ← Drives ExtentReports lifecycle (creates/closes test nodes, embeds screenshots); registered in testng.xml alongside TestNGListener — never replaces it
      pages/
        BasePage.java               ← Core Selenium helpers (click, type, getText, etc.)
        fda/
          FDAHomePage.java          ← Home, search, cart icon, profile icon, logout()
          FDALoginPage.java         ← Email/password login
          FDAPDPPage.java           ← Product Details Page; increaseQuantity() clicks Aumentar plus button; getProductSku() added for TC_E2E_009
          FDACartPage.java          ← Cart validation, removeAllItems()
          FDAPaymentPage.java       ← Credit card checkout, handle3dsChallenge()
          FDAPayPalPage.java        ← PayPal sandbox login: waitForPageLoad → email → clickNextButton → password → clickLoginButton → clickCompletePayment; locators are Spanish-language (Siguiente, Iniciar sesión, Compra completa)
          FDASuccessPage.java       ← Order ID extraction
          FDAOrderHistoryPage.java  ← Mis pedidos
          FDASearchResultsPage.java ← Search-results-grid presence check + click-through to PDP (TC_E2E_009 — name search can return a grid, not just a direct PDP redirect)
        kibo/
          KiboLoginPage.java        ← Kibo OMS login (not used in current suite — Kibo accessed via API)
          KiboOrdersPage.java       ← Navigation + search
          KiboOrderDetailPage.java  ← Status, Payments tab, Custom Data tab
        mirakl/
          MiraklLoginPage.java      ← Mirakl login (called in @BeforeSuite); `login()` asserts `isDashboardDisplayed()` (shared Seller/Operator "Price and stock" nav marker) before returning — login failures throw instead of being silently assumed successful; logout() added for TC_E2E_009 (not currently called — TC_E2E_009's `setupSuite()` override logs straight in as Seller instead of swapping; available for reuse by future multi-account Mirakl tests)
          MiraklOrdersPage.java     ← Navigation + search, hasSearchResults()
          MiraklOrderDetailPage.java ← Accept, getOrderStatus(), clickOrderInList()
          MiraklReturnPage.java     ← Return flow (used by TC_FBO_021)
          MiraklOffersPage.java     ← Price and stock → Offers: search by SKU, Shop filter (Operator), price/product-name/shop-name getters, `waitForSingleResult()` explicit-wait grid poll (TC_E2E_009)
          MiraklFileImportPage.java ← Price and stock → File import: upload Excel, select File Content = Offers, import status/ID, `waitForImportCompletion()`/`getLatestImportFailedCount()` polling the "Track offer imports" report (TC_E2E_009)
          MiraklCatalogPage.java    ← Seller-side Product Imports + Catalog Management: SKU search, Valid/Invalid data detection, Fragile/MSI edit-and-save loop (TC_E2E_009)
          MiraklOperatorCatalogPage.java ← Operator-side Product Imports: filter by Seller, bulk-select → More Actions → Edit Catalogs → FDA Catalog checkbox → Confirm, then Accept products popup (TC_E2E_009)
        adobe/
          AdobeAdminLoginPage.java  ← Adobe Commerce Admin login (3rd separate `ChromeDriver`, TC_E2E_009 only)
          AdobeMiraklSyncPage.java  ← Admin Synchronization screen: verify Products import row, trigger "MCM Products Asynchronous Import (CM52-CM53-CM54)" (TC_E2E_009)
      utils/
        ConfigReader.java           ← Reads config.properties (singleton)
        DriverFactory.java          ← ThreadLocal WebDriver, openNewTab(), switchToTab()
        WaitUtility.java            ← Fluent wait (post-refresh only)
        ScreenshotUtility.java      ← Captures PNG to test-output/screenshots/
        LoggerUtility.java          ← Log4j2 wrapper
        ApiUtility.java             ← REST Assured — Kibo auth/find/shipments, Envioclick, Skydropx, Cancel
        ReturnApiUtility.java       ← Mirakl GET order-line-id + POST return (used by TC_FBO_020, 021, 022, 023, 026)
        ShipmentDetails.java        ← Immutable value object: shipmentNumber, deliveryPartner, tplShipmentId, carrierName, trackingNumber, miraklSuffix
        ExcelUtility.java           ← POI reader for Mirakl offer file-import `.xlsx` (Data sheet, header-name column lookup); first real usage of the poi-ooxml dependency (TC_E2E_009)
        PriceUtility.java           ← Normalizes currency-formatted price strings ("$100.00", "100,00") to BigDecimal for numeric equality comparisons (TC_E2E_009)
        ExtentManager.java          ← Singleton ExtentReports + per-thread ExtentTest; report at `target/surefire-reports/ExecutionReport.html`; do NOT use LoggerUtility inside this class (circular dependency)
        StepLogger.java             ← Optional structured step logger; step()/pass()/fail()/info() route through LoggerUtility only; screenshot() is the sole direct ExtentReports caller (images can't travel through log messages)
      tests/
        TC_FBO_001_Test.java
        TC_FBO_002_Test.java
        TC_FBO_003_Test.java
        TC_FBO_004_Test.java
        TC_FBO_005_Test.java
        TC_FBO_006_Test.java
        TC_FBO_007_Test.java
        TC_FBO_008_Test.java
        TC_FBO_009_Test.java
        TC_FBO_010_Test.java
        TC_FBO_011_Test.java
        TC_FBO_012_Test.java
        TC_FBO_020_Test.java
        TC_FBO_021_Test.java
        TC_FBO_022_Test.java
        TC_FBO_023_Test.java
        TC_FBO_026_Test.java
        TC_FBO_027_Test.java
        TC_FBO_028_Test.java
        TC_FBO_029_Test.java
        TC_FBO_030_Test.java
        TC_FBO_031_Test.java
        TC_FBO_032_Test.java
        TC_FBO_SPLIT_01_Test.java
        TC_E2E_009_Test.java
    resources/
      config.properties             ← All URLs, credentials, API tokens
      log4j2.xml                    ← Log configuration
      testng.xml                    ← Suite definition (all 25 TCs, sequential)
```

## Output Artifacts

| Location | Contents |
|----------|---------|
| `test-output/screenshots/` | PNG screenshots: `{TC_NAME}_PASS.png`, `{TC_NAME}_FAIL.png`, `{TC_NAME}_INFO.png` |
| `test-output/logs/automation.log` | Full execution log |
| `target/surefire-reports/ExecutionReport.html` | ExtentReports HTML report (dark theme); generated by `ExtentReportListener` + `ExtentManager` |
| `target/screenshots/passed/` | Pass screenshots saved by `ExtentReportListener` |
| `target/screenshots/failed/` | Fail screenshots saved by `ExtentReportListener` |

## Key Framework Rules

**NEVER change these:**
- `BaseClass` — suite lifecycle owner
- `DriverFactory` — ThreadLocal driver, tab management
- `ConfigReader` — singleton config loader
- `TestNGListener` — automatic screenshots
- `ExtentReportListener` — ExtentReports lifecycle (wired in testng.xml alongside TestNGListener)
- `ExtentManager` — singleton report + per-thread test node (no LoggerUtility calls — circular dep)
- `ExtentReportLog4j2Appender` — Log4j2 plugin; removing breaks report logging for all tests
- `StepLogger` — structured step API; only `screenshot()` touches ExtentManager directly
- Any utility class

**Wait Strategy:**
- Global implicit wait: 2 minutes (set once in `DriverFactory.createDriver()`)
- `WaitUtility.fluentWait()` — ONLY after `driver.navigate().refresh()` or API calls
- `refreshAndWait(By locator)` in `BaseClass` wraps both in one call — prefer it over calling them separately
- NO `Thread.sleep()` except where explicitly present in existing tests (Mirakl propagation polling, Kibo shipment polling)

**Polling timings (do not change without discussion):**
- Mirakl order sync: `Thread.sleep(60_000)` × 5 attempts = 5 min max (TC_FBO_001–006, SPLIT_01, 021, 027, 028); TC_FBO_020 and TC_FBO_023 use 10 attempts = 10 min max
- Kibo 3PL data ready: `Thread.sleep(30_000)` × 20 attempts = 10 min max
- Mirakl post-webhook status: `refreshAndWait()` × 6 retries (no sleep — implicit wait handles timing)

**Browser Lifecycle:**
- `driver.quit()` only in `@AfterSuite`
- `@AfterMethod` navigates FDA and Mirakl tabs back to home — never closes browser

**Checkout window handling:**
After `fdaCartPage.clickProceedToPayment()`, Kibo checkout may open in a **new browser window** (not a new tab within the existing set). Each test class defines a private `switchToCheckoutWindow()` that detects the new window handle and updates `fdaTabHandle` accordingly. Always call it after `clickProceedToPayment()`.

**PayPal popup window handling (TC_FBO_007–012):**
PayPal checkout opens in a **second new window** on top of the Kibo checkout window. Switch to it by finding the handle that is neither `fdaTabHandle` nor `miraklTabHandle`. After `FDAPayPalPage.clickCompletePayment()`, switch back to the Kibo checkout window. `FDAPayPalPage.waitForPageLoad()` must be the first call after switching — it waits up to 60 s for the email field or title to appear. PayPal TCs wait 2 minutes after `clickAcceptButton()` before polling Kibo for 3PL data.

**Tab Layout (actual — 2 tabs only):**
- Tab 0: FDA (`fdaTabHandle`) — opened in `@BeforeSuite`
- Tab 1: Mirakl (`miraklTabHandle`) — opened in `@BeforeSuite`
- `kiboTabHandle` field exists but is **not** set by `@BeforeSuite`; Kibo is accessed entirely via `ApiUtility` REST calls, not via a browser tab

**Mirakl Sub-Order ID Convention:**
- Single-product orders: `orderId + "WEB"` maps to one Mirakl row; also referenced as `orderId + "WEB-A"` in multi-shipment tests
- Multi-product/split orders: suffixes `WEB-A`, `WEB-B`, `WEB-C` … `WEB-F` (up to 6 for SPLIT_01)

**Multi-account session swap (TC_FBO_003/004/005/006/020):**
These tests use a different FDA account. The mechanism:
- Credentials are **hardcoded `private static final` constants** in the test class (not in `config.properties`)
- `@BeforeClass`: calls `fdaHomePage.logout()` on the suite session, then logs in as the TC-specific user
- `@AfterClass(alwaysRun = true)`: calls `logout()` again and restores the suite default user (`config.getFdaUsername()`) so subsequent tests are unaffected
- TC_FBO_020 adds a conditional guard `if (!TC_FDA_USER.equals(config.getFdaUsername()))` in both `@BeforeClass` and `@AfterClass` — copy this pattern for new TCs that may follow a same-user TC
- Never put TC-specific credentials into `config.properties`; keep them as constants in the test class
- Kibo search term: `orderId + "WEB"` (e.g., `4000267985WEB`)
- Mirakl API sub-order ID: `orderId + "WEB-A"` (append `-A` for first sub-order in `ReturnApiUtility`)

**Dynamic Data:**
All values below are captured once per test and reused — never hardcoded:
`orderId`, `orderTotal`, `tplShipmentId`, `carrierName`, `trackingNumber`, `deliveryPartner`
Multi-shipment tests use `List<ShipmentDetails>` or a local `ShipmentInfo` inner class.

**Delivery Partner Routing:**
Tests branch on `deliveryPartner` string from Kibo custom field:
- Contains `"envioclick"` → `ApiUtility.postEnvioclickEnTransito()` then `postEnvioclickEntregado()`
- Contains `"skydropx"` → `ApiUtility.postSkydropxPickedUp()` then `postSkydropxDelivered()`

**Skydropx payload:** only needs `tplShipmentId` — the body carries `data.id`, `data.attributes.status`, and a `relationships.shipment` link built from that single ID. `carrierName` and `trackingNumber` are not sent to Skydropx.

**Envioclick payload mapping:**
| Field | Source |
|-------|--------|
| `carrier` | `carrierName` (from Kibo) |
| `idOrder` | `tplShipmentId` (3pl_shipmentId from Kibo) |
| `trackingCode` | `trackingNumber` (from Kibo) |
| `myShipmentReference` | `orderId + "WEB"` |

**Kibo shipment field extraction:**
Each test class defines a **private** `extractCustomField(Map<String, Object> item, String fieldKey)` that searches `item.data` then `item.packages[*].data`. This is duplicated across test files — do not move it to BaseClass without careful refactoring. For `Response`-level extraction, use `ApiUtility.extractKiboCustomField(response, fieldKey)` which also falls back to `extendedProperties[]`.

**Kibo shipment list response format:**
`kiboGetShipmentsByOrderId()` may return `items[]` (standard Kibo format) or `_embedded.shipments` (HAL format). Tests check both — copy this pattern when adding new Kibo list parsers.

**Kibo shipment index mapping for multi-SKU orders (TC_FBO_005/006/023):**
Kibo does not guarantee `items[0]` → WEB-A. For different-seller orders (TC_FBO_005/006), Seller B ships first: `items[0]` → WEB-B, `items[1]` → WEB-A. For same-seller orders (TC_FBO_023), index order depends on which SKU appears first in cart: the cart-first SKU maps to WEB-B (`items[0]`). Always assign `shipmentRef`/`miraklDetailUrl` by inspecting which SKU Kibo processed first — do not assume index 0 → WEB-A.

**Envioclick intermediate status:**
After `postEnvioclickEnTransito()`, Mirakl may show either `"Shipped"` or `"3PL delivery"` (both are valid intermediate states). Use `waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, retries)` — not `waitForMiraklStatus("Shipped", retries)` — for this step. After `postEnvioclickEntregado()`, the status must be exactly `"Received"`.

**`kiboSkipValidateItemsTask`:**
Call `ApiUtility.kiboSkipValidateItemsTask(token, shipmentNumber)` when staging SKUs have zero warehouse stock. This PUTs to `.../tasks/Validate%20Items%20In%20Stock/skipped` so the 3PL connector can proceed to generate a carrier label. Use it before polling for `deliveryPartner` / `3pl_shipmentId` if shipment data never populates.

**testng.xml execution order:**
Suite runs 24 TCs sequentially. Actual order:
`001 → 002 → 003 → 004 → 005 → 006 → 007 → 008 → 009 → 010 → 011 → 012 → SPLIT_01 → 020 → 021 → 022 → 023 → 026 → 027 → 028 → 029 → 030 → 031 → 032`
TC_FBO_SPLIT_01 runs **before** TC_FBO_020 (XML labels it "TEST 13").
PayPal TCs (007–012): update `TC_PAYPAL_PASS` constant in each test class before running — password is intentionally left blank in committed code. Always re-blank (`TC_PAYPAL_PASS = ""`) before committing; TC_FBO_008–012 are especially at risk because they carry filled-in passwords locally.

**Cancel API auth (TC_FBO_027/028):**
`ApiUtility.cancelFullOrder()` and `cancelShipment()` use **Cookie-only** auth — no Bearer token. The cookie values come from `config.getCancelOrderCookie()` and `config.getCancelShipmentCookie()` respectively. Do not add a Bearer header to these calls.

**Cancel post-acceptance delay (TC_FBO_029/030):**
Both TCs require a business-mandatory `Thread.sleep(60_000)` immediately after the last `clickAcceptButton()` call and before the cancel API call. Do not remove this delay.

**TC_FBO_030 `cancelShipment()` — offer_sku resolution:**
`ApiUtility.cancelShipment(cancelledShipmentId, cancelProductIds, apiOrderReference)` requires a `List<String>` of Mirakl `offer_sku` values (not EANs). TC_FBO_030 resolves these by calling the Mirakl REST API inline (`GET /api/orders?order_ids={shipmentRefA}`) and extracting `orders[0].order_lines.offer_sku` before accepting the shipment. If the API call fails or returns empty, it falls back to the known TC_SKU_2 constant. Always call the API before accepting WEB-A so the URL is still accessible.

**TC_FBO_030 `waitForMiraklStatus` variant:**
TC_FBO_030's private `waitForMiraklStatus` adds `Thread.sleep(10_000)` between retries (not present in TC_FBO_029 or other TCs). Copy TC_FBO_030's version for cancel-after-acceptance flows where the status transition may lag.

**Kibo API endpoints in `ApiUtility`:**
- `kiboGetShipmentsByOrderId(token, kiboOrderId)` — GET `/api/commerce/shipments?filter=orderId==<id>` — used by all current tests for 3PL data polling
- `kiboGetOrderShipments(token, kiboOrderId)` — GET `/api/commerce/orders/<id>/shipments` — exists but not used by current tests; prefer the former
- `kiboGetShipment(token, shipmentNumber)` — GET `/api/commerce/shipments/<number>` — single shipment lookup; does not throw on non-200
- `kiboGetOrder(token, kiboOrderId)` — GET `/api/commerce/orders/<id>` — full order response; throws on non-200
- `kiboGetReturnsForOrder(token, kiboOrderId)` — GET `/api/commerce/returns?filter=originalOrderId eq <id>` — used by TC_FBO_026 to poll for `return.status == "closed"`; does not throw on non-200

**`ReturnApiUtility.postReturn()` overloads:**
- `postReturn(orderCommercialId, orderLineId)` — always posts `quantity: 1`; used for full-quantity returns (TC_FBO_020/021/022)
- `postReturn(orderCommercialId, orderLineId, quantity)` — posts the specified quantity; used for partial returns (TC_FBO_026 passes `1` of `2`)
Both use Cookie-only auth via `config.get("return.service.cookie")`. The Mirakl order-line-id is fetched first via `getMiraklOrderLineId(commercialId)` which appends `-A` and calls `config.get("mirakl.api.token")` (Bearer).

**Raw `config.get()` keys (no typed getter):** `ReturnApiUtility` reads `mirakl.api.token`, `return.service.url`, and `return.service.cookie` directly via `config.get("key")`. If those properties are missing from `config.properties`, `ConfigReader.get()` throws `RuntimeException` immediately.

**`waitForMiraklStatus` / `waitForMiraklStatusOneOf`:**
These are **private** methods duplicated in every test class — not in `BaseClass`. Each new TC must copy both helpers verbatim from an existing test. `waitForMiraklStatus(expected, maxRetries)` polls via `refreshAndWait()` until the exact status matches. `waitForMiraklStatusOneOf(String[] expected, maxRetries)` stops on any match from the set.

**Envioclick payload timestamps:**
The `buildEnvioclickBody` helper hardcodes `timestamp`, `realPickupDate`, `arrivalDate`, and `realDeliveryDate` to old 2022 values. The Envioclick API does not validate these dates; only `status`, `idOrder`, `trackingCode`, `carrier`, and `myShipmentReference` are meaningful.

## Known Issues

**`TC_E2E_009` is listed in the main `testng.xml` (TEST 19) despite its documented isolation requirement.**
`TC_E2E_009_Test` declares its own `@BeforeSuite setupSuite()` that logs Mirakl straight in as Seller (see catalog entry above). TestNG runs **every** `@BeforeSuite` method found across all classes in a suite once each, in undefined relative order — it does not replace `BaseClass.setupSuite()`. With `TC_E2E_009_Test` present in `testng.xml` alongside the other 24 TCs, both `BaseClass.setupSuite()` and `TC_E2E_009_Test.setupSuite()` fire at suite start, and whichever runs last determines the actual Mirakl session for the rest of the run — risking a broken Mirakl login for TC_FBO_001–032. Only `testng_e2e_009.xml` (which contains just this one class) is safe. Before running the full suite, verify `TC_E2E_009_Test` has been removed from `testng.xml`, or run it exclusively via `-DsuiteXmlFile=src/test/resources/testng_e2e_009.xml`.

## Adding New Test Cases

1. Create new page objects in `pages/fda|kibo|mirakl/` extending `BasePage`
2. Create test class in `tests/` extending `BaseClass`
3. Add `@BeforeClass` to instantiate page objects
4. Add class to `testng.xml`
5. Reuse existing utilities — never duplicate

## Deprecated Stub

`src/test/java/FDA_Automation_Script/FDA_Automation_Script/pages/FDAHomePage.java` is a `@Deprecated` empty stub. Import `pages.fda.FDAHomePage` instead.

## Locator Notes

All locators are marked with `// TODO: Verify locators against actual DOM` comments.
Update them by inspecting the live application in browser DevTools.

## Credentials

Stored in `src/test/resources/config.properties`.
Do NOT commit this file to shared/public repositories.

## config.properties Key Reference

| Key | Used by | Notes |
|-----|---------|-------|
| `browser` | `DriverFactory` | `chrome` (default) or `firefox` |
| `fda.url` | `BaseClass`, tests | FDA storefront base URL |
| `fda.username` | `BaseClass`, tests | Suite-level FDA login |
| `fda.password` | `BaseClass`, tests | Suite-level FDA password |
| `fda.sku` | Tests | Primary SKU for single-product tests |
| `fda.split.skus` | `TC_FBO_SPLIT_01_Test` | Comma-separated list of 8 SKUs, split via `.split(",")` |
| `fda.card.number` | `FDAPaymentPage` | Credit card number |
| `fda.card.expiry` | `FDAPaymentPage` | Card expiry MM/YY |
| `fda.card.cvv` | `FDAPaymentPage` | Card CVV |
| `kibo.url` | `ConfigReader` | Kibo OMS UI URL (not used in tests) |
| `kibo.username` | `ConfigReader` | Kibo UI credentials (not used in tests) |
| `kibo.password` | `ConfigReader` | Kibo UI credentials (not used in tests) |
| `kibo.api.base.url` | `ApiUtility` | Kibo REST API base URL |
| `kibo.api.client.id` | `ApiUtility` | Kibo OAuth client ID |
| `kibo.api.client.secret` | `ApiUtility` | Kibo OAuth client secret |
| `mirakl.url` | `BaseClass`, tests | Mirakl portal URL |
| `mirakl.username` | `BaseClass` | Mirakl login |
| `mirakl.password` | `BaseClass` | Mirakl password |
| `mirakl.api.token` | `ReturnApiUtility`, TC_FBO_030 | Bearer token for Mirakl REST API |
| `envioclick.url` | `ApiUtility` | Envioclick webhook endpoint |
| `api.auth.token` | `ApiUtility` | Bearer token for Envioclick |
| `api.cookie` | `ApiUtility` | Cookie for Envioclick + Kibo calls |
| `skydropx.url` | `ApiUtility` | Skydropx webhook endpoint |
| `skydropx.api.token` | `ApiUtility` | Bearer token for Skydropx |
| `cancel.order.url` | `ApiUtility` | Cancel full order endpoint (Cookie-only auth) |
| `cancel.order.cookie` | `ApiUtility` | Cookie for cancel order calls |
| `cancel.shipment.url` | `ApiUtility` | Cancel shipment endpoint (Cookie-only auth) |
| `cancel.shipment.cookie` | `ApiUtility` | Cookie for cancel shipment calls |
| `return.service.url` | `ReturnApiUtility` | Return creation endpoint (Cookie-only auth) |
| `return.service.cookie` | `ReturnApiUtility` | Cookie for return service calls |
| `push.offers.to.empathy.url` | `ApiUtility` | Push Offers to Empathy endpoint (TC_E2E_009) — GET, Cookie-only auth, no body; propagates a Seller's updated offer price to the FDA storefront's catalog index |
| `push.offers.to.empathy.cookie` | `ApiUtility` | Cookie for Push Offers to Empathy calls |
| `excel.offer.file.path` | `TC_E2E_009_Test` (raw `config.get()`), `ExcelUtility` | Absolute path to the Mirakl offer file-import `.xlsx` used by TC_E2E_009 |
| `adobe.admin.url` | `TC_E2E_009_Test` (raw `config.get()`) | Adobe Commerce (Magento) Admin URL — URL only, not a credential; if blank, falls back to `fda.url` + `admin` (standard Magento admin path) |

**TC_E2E_009 login credentials are NOT in `config.properties`** — by explicit design, they are read from environment variables at test-run time via a private `requireEnv(String)` helper in `TC_E2E_009_Test`, and are never written to any file: `MIRAKL_SELLER_USERNAME`, `MIRAKL_SELLER_PASSWORD`. (`MIRAKL_OPERATOR_USERNAME/PASSWORD` and `ADOBE_ADMIN_USERNAME/PASSWORD` are no longer read by this TC — the Operator phase was removed and Adobe was never wired in — but are left here as a reminder they exist elsewhere/historically.) Set them in the shell immediately before invoking Maven, e.g. (PowerShell) `$env:MIRAKL_SELLER_USERNAME='...'; mvn test -DsuiteXmlFile=...`.
