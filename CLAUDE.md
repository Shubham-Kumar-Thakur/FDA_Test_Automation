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

Run all `TC_FBS_*` tests only (no `TC_FBO_*` tests) via the shared FBS-only suite:

```bash
mvn test -DsuiteXmlFile=src/test/resources/testng_fbs.xml
```

To run just one `TC_FBS_*` class in isolation, use the same `-Dtest` single-class approach as any other test (bypasses suite XML entirely — see "Run a single test class" above), or comment out the other `<test>` blocks in `testng_fbs.xml`.

## Test Case Catalog

Every `TC_FBO_*` test is tagged `@Test(groups = {"FBO"})`; every `TC_FBS_*` test is tagged `@Test(groups = {"FBS"})`. See FBS/FBO Group-Level Login for what that controls.

> `TC_FBO_SPLIT_01_Test.java` (referenced by `testng.xml`'s "TEST 13" block) exists in the working tree — an earlier note here claiming it was deleted was stale/incorrect.

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
| `TC_FBS_001_Test` | FBS (Fulfilled By Store) order: 1 SKU qty=1 → Credit Card → Mirakl accept → Kibo shipment delivery type verified = `FBS` → Mirakl Invoice document upload → DHL carrier + tracking number → Mark as Shipped → Custom field "Entregado" → Received; FDA login is the `FBS` group-level login (`fbs.username`/`fbs.password`, see FBS/FBO Group-Level Login) — no per-class login/logout; Mirakl reuses the suite-default session |
| `TC_FBS_002_Test` | FBS order, 2 SKUs qty=1 each from **1 3P seller** (no WEB-A/WEB-B split) → same Documents/Tracking/Mark as Shipped/Entregado → Received flow as TC_FBS_001; uses `fbs002.sku1`/`fbs002.sku2`/`fbs002.card.*`/`fbs002.invoice.file.path` |
| `TC_FBS_003_Test` | FBS order, 1 SKU qty=2 from **1 3P seller** → same Documents/Tracking/Mark as Shipped/Entregado → Received flow as TC_FBS_001; uses `fbs003.sku`/`fbs003.card.*`/`fbs003.invoice.file.path` |
| `TC_FBS_004_Test` | FBS order, 2 SKUs qty=2 each from **1 3P seller** (no WEB-A/WEB-B split — confirmed via live run) → same Documents/Tracking/Mark as Shipped/Entregado → Received flow as TC_FBS_001; combines TC_FBS_002's 2-product flow with TC_FBS_003's PDP quantity-increase flow; uses `fbs004.sku1`/`fbs004.sku2`/`fbs004.card.*`/`fbs004.invoice.file.path` |
| `TC_FBS_005_Test` | FBS order, 2 SKUs qty=1 each from **2 different 3P sellers** → **2 Mirakl shipments (WEB-A + WEB-B)** (different-seller splitting is structural in Mirakl, same as `TC_FBO_005`); full Accept → Kibo deliveryType check → Documents/Tracking/Mark as Shipped/Entregado → Received sequence runs independently for both shipments (own tracking number each, same invoice PDF for both); WEB-B reopened by re-searching the order list (not URL substitution — seller-split shipment IDs aren't guaranteed to differ only in the WEB-A/WEB-B suffix); uses `fbs005.sku1`/`fbs005.sku2`/`fbs005.card.*`/`fbs005.invoice.file.path` |
| `TC_FBS_006_Test` | FBS order, 2 SKUs qty=2 each from **2 different 3P sellers** → **2 Mirakl shipments (WEB-A + WEB-B)**, same split behavior as `TC_FBS_005`/`TC_FBO_006`; combines TC_FBS_004's PDP quantity-increase flow with TC_FBS_005's dual-shipment handling; uses `fbs006.sku1`/`fbs006.sku2`/`fbs006.card.*`/`fbs006.invoice.file.path` |
| `TC_FBS_007_Test` | FBS order, 1 SKU qty=1 from **1 3P seller**, same flow as `TC_FBS_001` except payment method is **PayPal** instead of Credit Card (mirrors `TC_FBO_007`'s PayPal window handling: `FDAPayPalPage`, PayPal popup window switching via `switchToPayPalWindow`/`waitForPayPalWindowClose`, plus two extra PayPal steps not present in `TC_FBO_007` — `selectVisaOption()` and `clickFullPurchaseButton()`, both best-effort/no-op if the sandbox account doesn't render that funding-source picker); like `TC_FBO_007`, the actual finalizing click is `fdaPayPalPage.clickCompletePayment()` ("Compra completa") **inside the PayPal popup**, right after the Visa/Full-purchase steps — an earlier revision called the FDA/Adyen `fdaPaymentPage.clickCompletePayment()` ("Completar pago") on the FDA page *after* the popup closed instead, which threw `NoSuchElementException` in a live run because that Adyen-specific button doesn't exist once PayPal is the selected payment method (confirmed 2026-09-09); FDA order status can land on `Procesando` instead of `Creada`/`Pendiente` for PayPal orders (tolerated in the assertion); `waitForMiraklStatus()` here adds a 10s sleep between retries (not just TC_FBO_030) because a live run showed the Entregado→Received transition can outlast ~80s of back-to-back refreshes; uses `fbs007.sku`/`fbs007.invoice.file.path`; PayPal credentials are hardcoded `TC_PAYPAL_EMAIL`/`TC_PAYPAL_PASS` constants in the test class (same sandbox account as `TC_FBO_007`), not config keys, per the PayPal-credential convention below |
| `TC_FBS_008_Test` | FBS order, 2 SKUs qty=1 each from **1 3P seller** (no WEB-A/WEB-B split, same seller as `TC_FBS_002`), **PayPal** payment — combines `TC_FBS_002`'s 2-product cart flow with `TC_FBS_007`'s (fixed) PayPal payment flow; the payment+order-history phase is 18 steps in both the Credit Card (`TC_FBS_002`) and PayPal (`TC_FBS_008`) manual test cases, so the Mirakl/Kibo/Documents/Tracking/Shipped/Entregado phase keeps identical step numbers between the two files; uses `fbs008.sku1`/`fbs008.sku2`/`fbs008.invoice.file.path`; PayPal credentials are the same hardcoded `TC_PAYPAL_EMAIL`/`TC_PAYPAL_PASS` constants as `TC_FBS_007` |
| `TC_FBS_009_Test` | FBS order, 1 SKU qty=2 from **1 3P seller**, **PayPal** payment — combines `TC_FBS_003`'s PDP quantity-increase flow (qty 1→2) with `TC_FBS_007`/`TC_FBS_008`'s PayPal payment flow; the PayPal payment section is exactly one step longer than TC_FBS_003's Credit Card section (extra "PayPal Pagar" click), so everything from "Proceed to payment" onward is shifted +1 vs `TC_FBS_003`'s step numbers; the manual case's "verify order total on the Completar pago button (e.g. Completar pago (MXN$480.00))" step has no PayPal-side equivalent in the live-confirmed working flow, so it's logged (cart total already captured) rather than asserted against a button that was never observed to exist for PayPal orders; uses `fbs009.sku`/`fbs009.invoice.file.path`; PayPal credentials are the same hardcoded `TC_PAYPAL_EMAIL`/`TC_PAYPAL_PASS` constants as `TC_FBS_007`/`TC_FBS_008` |
| `TC_FBS_010_Test` | FBS order, 2 SKUs qty=2 each from **1 3P seller** (no WEB-A/WEB-B split, same seller as `TC_FBS_004`), **PayPal** payment — combines `TC_FBS_004`'s 2-product/qty=2-each cart flow with `TC_FBS_007`/`008`/`009`'s PayPal payment flow; PayPal payment section is one step longer than TC_FBS_004's Credit Card section (extra "PayPal Pagar" click), so everything from "Proceed to payment" onward is shifted +1 vs `TC_FBS_004`'s step numbers; same "no PayPal-side Completar-pago-button-text equivalent" log-only handling as `TC_FBS_009` for that manual-case step; uses `fbs010.sku1`/`fbs010.sku2`/`fbs010.invoice.file.path`; PayPal credentials are the same hardcoded `TC_PAYPAL_EMAIL`/`TC_PAYPAL_PASS` constants as `TC_FBS_007`–`009` |
| `TC_FBS_011_Test` | FBS order, 2 SKUs qty=1 each from **2 different 3P sellers** → **2 Mirakl shipments (WEB-A + WEB-B)**, **PayPal** payment — combines `TC_FBS_005`'s dual-shipment flow with `TC_FBS_007`–`010`'s PayPal payment flow; **the manual test case's supplied SKU pair (32982398137/32982398136) is the confirmed same-seller pairing already used by `TC_FBS_002`/`004`/`008`/`010`** — using it here would produce a single shipment, contradicting the case's "two 3P Seller" title, so (per explicit user confirmation) this TC uses `TC_FBS_005`'s different-seller pairing instead (`fbs011.sku1`=32982398137, `fbs011.sku2`=4892828006) so it actually exercises the dual-shipment path; PayPal payment section is one step longer than TC_FBS_005's Credit Card section, so everything from "Proceed to payment" onward is shifted +1 vs `TC_FBS_005`'s step numbers; uses `fbs011.sku1`/`fbs011.sku2`/`fbs011.invoice.file.path`; PayPal credentials are the same hardcoded `TC_PAYPAL_EMAIL`/`TC_PAYPAL_PASS` constants as `TC_FBS_007`–`010` |
| `TC_FBS_012_Test` | FBS order, 2 SKUs qty=2 each from **2 different 3P sellers** → **2 Mirakl shipments (WEB-A + WEB-B)**, **PayPal** payment — combines `TC_FBS_006`'s dual-shipment/qty=2-each flow with `TC_FBS_007`–`011`'s PayPal payment flow; same SKU-pairing discrepancy and resolution as `TC_FBS_011` (uses `TC_FBS_006`'s different-seller pairing — `fbs012.sku1`=32982398137, `fbs012.sku2`=4892828010 — instead of the same-seller pair supplied in the manual case); PayPal payment section is one step longer than TC_FBS_006's Credit Card section, so everything from "Proceed to payment" onward is shifted +1 vs `TC_FBS_006`'s step numbers; uses `fbs012.sku1`/`fbs012.sku2`/`fbs012.invoice.file.path`; PayPal credentials are the same hardcoded `TC_PAYPAL_EMAIL`/`TC_PAYPAL_PASS` constants as `TC_FBS_007`–`011` |

## Project Structure

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
          FDAPDPPage.java           ← Product Details Page; increaseQuantity() clicks Aumentar plus button
          FDACartPage.java          ← Cart validation, removeAllItems()
          FDAPaymentPage.java       ← Credit card checkout, handle3dsChallenge()
          FDAPayPalPage.java        ← PayPal sandbox login: waitForPageLoad → email → clickNextButton → password → clickLoginButton → clickCompletePayment; locators are Spanish-language (Siguiente, Iniciar sesión, Compra completa)
          FDASuccessPage.java       ← Order ID extraction
          FDAOrderHistoryPage.java  ← Mis pedidos
        kibo/
          KiboLoginPage.java        ← Kibo OMS login (not used in current suite — Kibo accessed via API)
          KiboOrdersPage.java       ← Navigation + search
          KiboOrderDetailPage.java  ← Status, Payments tab, Custom Data tab
        mirakl/
          MiraklLoginPage.java      ← Mirakl login (called in @BeforeSuite)
          MiraklOrdersPage.java     ← Navigation + search, hasSearchResults()
          MiraklOrderDetailPage.java ← Accept, getOrderStatus(), clickOrderInList(); FBS flow: documents upload, carrier/tracking, Mark as Shipped, custom field (TC_FBS_001)
          MiraklReturnPage.java     ← Return flow (used by TC_FBO_021)
      utils/
        ConfigReader.java           ← Reads config.properties (singleton)
        DriverFactory.java          ← ThreadLocal WebDriver, openNewTab(), switchToTab()
        WaitUtility.java            ← Fluent wait (post-refresh only)
        ScreenshotUtility.java      ← Captures PNG to test-output/screenshots/
        LoggerUtility.java          ← Log4j2 wrapper
        ApiUtility.java             ← REST Assured — Kibo auth/find/shipments, Envioclick, Skydropx, Cancel
        ReturnApiUtility.java       ← Mirakl GET order-line-id + POST return (used by TC_FBO_020, 021, 022, 023, 026)
        ShipmentDetails.java        ← Immutable value object: shipmentNumber, deliveryPartner, tplShipmentId, carrierName, trackingNumber, miraklSuffix
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
        TC_FBO_SPLIT_01_Test.java   ← DELETED from working tree (uncommitted) but still referenced by testng.xml — see Test Case Catalog note
        TC_FBS_001_Test.java
        TC_FBS_002_Test.java
        TC_FBS_003_Test.java
        TC_FBS_004_Test.java
        TC_FBS_005_Test.java
        TC_FBS_006_Test.java
        TC_FBS_007_Test.java
        TC_FBS_008_Test.java
        TC_FBS_009_Test.java
        TC_FBS_010_Test.java
        TC_FBS_011_Test.java
        TC_FBS_012_Test.java
    resources/
      config.properties             ← All URLs, credentials, API tokens
      log4j2.xml                    ← Log configuration
      testng.xml                    ← Suite definition (36 <test> blocks, sequential)
      testng_fbs.xml                 ← FBS-only suite: all TC_FBS_00X tests (currently 001-012), no TC_FBO_* tests
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
- `BaseClass` — suite lifecycle owner; also owns FBO/FBS group-level FDA login via `@BeforeGroups`/`@AfterGroups` (see FBS/FBO Group-Level Login)
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
- Tab 0: FDA (`fdaTabHandle`) — opened in `@BeforeSuite`; FDA **login** itself happens later, in `@BeforeGroups("FBO")`/`@BeforeGroups("FBS")` (see FBS/FBO Group-Level Login)
- Tab 1: Mirakl (`miraklTabHandle`) — opened **and logged in** in `@BeforeSuite` (Mirakl login is not group-scoped)
- `kiboTabHandle` field exists but is **not** set by `@BeforeSuite`; Kibo is accessed entirely via `ApiUtility` REST calls, not via a browser tab

**FBS/FBO Group-Level Login:**
Every `@Test` is tagged `groups = {"FBO"}` or `groups = {"FBS"}`. `BaseClass` logs in to FDA **once per group, not once per test**:
- `@BeforeGroups("FBO")` → `fdaHomePage.logout()` is *not* called here (nothing is logged in yet after `@BeforeSuite`); it navigates to FDA and logs in with `config.getFboUsername()/getFboPassword()` (`fbo.username`/`fbo.password`) before the first `TC_FBO_*` test runs
- `@AfterGroups("FBO")` → logs out once, after the last `TC_FBO_*` test in the suite completes
- `@BeforeGroups("FBS")` → logs in with `config.getFbsUsername()/getFbsPassword()` (`fbs.username`/`fbs.password`) before the first `TC_FBS_*` test runs
- `@AfterGroups("FBS")` → logs out once, after the last `TC_FBS_*` test completes
- TestNG guarantees `@BeforeGroups` for a group fires before any `@BeforeClass` of a class containing a test in that group, so an individual test class's own `@BeforeClass` login (see Multi-account session swap below) always runs *after* the group-level login and can still override it
- Mirakl is unaffected by this — it keeps its single suite-wide login from `@BeforeSuite`
- Do not add a per-test-class FDA login/logout unless that specific test genuinely needs an account different from its group's default; the whole point of this mechanism is one login per group, not one per test (see the `TC_FBS_001` update: it dropped its own login once its account became identical to `fbs.username`; `TC_FBS_002`–`TC_FBS_006` were written from the start with no per-class login, relying solely on `@BeforeGroups("FBS")`)
- **Confirmed via a live full-suite run (2026-09-09, `testng_fbs.xml`) that this was NOT actually once-per-suite in practice:** when every `TC_FBS_00X` lived in its own separate `<test>` XML element (same structure `testng.xml` used for `TC_FBO_*`), TestNG re-invoked `@AfterGroups("FBS")` then `@BeforeGroups("FBS")` at every single `<test>` boundary, not once for the whole suite — the log showed a full logout+login cycle between every one of `TC_FBS_001` through `006`. This was harmless for the Credit Card tests (each cycle completed in ~15s) but was the trigger for the PayPal session-recognition issue below (a PayPal checkout became the 2nd+ one in that same browser far sooner than "once per suite" would predict).
- **Fixed 2026-09-10:** confirmed via a live 12-test run (`testng_fbs.xml`) that every one of `TC_FBS_001`–`012` still triggered its own logout+login cycle, and a 2-of-12 failure rate plus a repeated PayPal-iframe wait warning (see below) prompted a structural fix. Both `testng_fbs.xml` and the FBS section of `testng.xml` now list all 12 `TC_FBS_00X` classes inside a **single** `<test>` block instead of one `<test>` tag per class — TestNG scopes `@BeforeGroups`/`@AfterGroups` to `<test>` boundaries, so one shared `<test>` block makes `loginForFBS()`/`logoutAfterFBS()` fire exactly once for the whole FBS run (login before `TC_FBS_001`, logout after `TC_FBS_012`), matching the "once per group" behavior this doc originally described. If a new `<test>` tag is ever added for a class in the `FBS` (or `FBO`) group, this per-`<test>` re-firing quirk will resurface — keep new FBS classes inside the existing shared `<test>` block's `<classes>` list rather than giving them their own `<test>` tag.

**PayPal remembers a logged-in session within the same browser (TC_FBS_007–012):**
Confirmed via a live run (2026-09-09) with a screenshot at the failure point: the **first** PayPal checkout in a browser session shows the full email/password login form, but every PayPal checkout **after** that in the *same browser* (same PayPal cookies) skips straight to the "Pagar con" funding-source-picker + "Compra completa" screen — no email or password field appears at all. `TC_FBS_008`'s live run hung for ~2 minutes on `clickEmailField()`'s wait, then failed with a clean "element never became clickable" error once its own 30s wait expired; the screenshot captured at that failure showed the funding-picker screen already fully loaded with Visa pre-selected, proving there was never an email field to click. Every `TC_FBS_007`–`012` test now calls `fdaPayPalPage.isLoginScreenDisplayed()` (checks for `EMAIL_FIELD`/`PASSWORD_FIELD` presence with a short 10s implicit wait) and only runs the email/password/`clickLoginButton()` block if it returns true; otherwise it logs a note and proceeds directly to `selectVisaOption()`/`clickFullPurchaseButton()`/`clickCompletePayment()`. If a brand new PayPal FBS TC is added later, copy this conditional-login pattern from the start rather than assuming email/password entry always happens.

**`MiraklOrderDetailPage.clickAddTrackingInformationLink()` — diagnostic dump was unconditional, not failure-gated:**
Every FBS run logged two giant `WARN "Visible leaf-text elements after opening More actions"` dumps (the full order-detail page's leaf text, then the tracking-modal's leaf text) even when the click succeeded cleanly. Root cause: unlike every other method in this file (`clickDocumentsOption()`, `clickAddTrackingButton()`, `clickEntregadoOption()`), which only call the diagnostic `logVisibleMenuItemTexts()`/`dumpElementsContainingIgnoreCase()` helpers inside a `catch (NoSuchElementException)` block, `clickAddTrackingInformationLink()` called `logVisibleMenuItemTexts()` unconditionally — once right before clicking `ADD_TRACKING_LINK` and again right after — left over from the original debugging session that first identified the tracking-form locators. Confirmed via a live 12-test `testng_fbs.xml` run (2026-09-10): both dumps fired on every single FBS test, all 12. **Fixed:** both unconditional calls are removed; the diagnostic dump (plus the `dumpElementsContainingIgnoreCase` calls) now only fires inside the `catch (NoSuchElementException)` block around the click, matching the pattern used everywhere else in this file. The screenshot capture and its short settling `Thread.sleep(1000)` are unchanged.

**`FDAPaymentPage.clickPayPalButton()` — search iframes first, not main DOM first:**
Every PayPal FBS/FBO checkout logged a `WARN "PayPal button not in main DOM — searching iframes"` after burning a fixed 60s `WebDriverWait` on the main-DOM `PAYPAL_PAY_BTN` locator, because PayPal's Smart Button always renders inside an iframe on this page — the main-DOM branch has never once succeeded across any live run to date. Confirmed via a live 12-test `testng_fbs.xml` run (2026-09-10): the warning fired on all 6 PayPal tests (`TC_FBS_007`–`012`), each paying a guaranteed 60s tax before falling through to the (correct) iframe search. **Fixed:** `clickPayPalButton()` now polls all iframes first (`WebDriverWait` up to 30s, 500ms interval, via the private `clickPayPalButtonInAnyIframe()` helper) and only falls back to the main-DOM locator (15s wait) if no iframe match is found — main DOM is now the fallback, not the first attempt. If a future page variant genuinely renders the button outside an iframe, the fallback still handles it; it just no longer costs 60s on every single PayPal run.

**PayPal popup `waitForPageLoad()` silently taking ~4 minutes instead of its own 60s bound — `pageLoadStrategy` fix (`DriverFactory`):**
An earlier note here blamed this on `waitForPageLoad()`'s `ExpectedConditions.or(...)` missing a condition — that diagnosis was wrong. Confirmed across four separate live-run occurrences (2026-09-09, `TC_FBS_008` through `011`, every single PayPal checkout after the first one in a suite run): `waitForPageLoad()`'s `WebDriverWait(60s)` consistently took ~4 minutes to resolve, self-recovering every time with no code change in between. Root cause: Chrome's default `pageLoadStrategy` (`NORMAL`) makes *every* WebDriver command against a page block until the browser considers that page fully loaded — including slow/long-hanging third-party resources (PayPal's popup loads analytics/fraud-detection scripts that don't reliably finish quickly). `WebDriverWait`'s nominal timeout only bounds how long it *keeps polling*; it can't preempt a single in-flight command that's stuck waiting on the browser's own page-load signal, so the real ceiling ends up being the driver's `pageLoad` capability timeout (300s), not our 60s `Duration`. Fixed in `DriverFactory.createDriver()` by setting `ChromeOptions`/`FirefoxOptions.setPageLoadStrategy(PageLoadStrategy.EAGER)` — waits only for DOM-ready instead of full page load, which is all our explicit element-based waits (`WaitUtility`, `WebDriverWait`) actually need. This is an exception to the "NEVER change `DriverFactory`" rule below — made only after this exact symptom recurred identically four times in one suite run and the standard `pageLoadStrategy=EAGER` fix was confirmed to target the actual mechanism (not a guess). **This only takes effect for browsers launched *after* the fix** — an already-running suite (browser created before this change) will keep showing the ~4-minute pauses for its remaining PayPal checkouts; that isn't the fix failing, it just can't apply retroactively to an existing driver instance.

**Mirakl Sub-Order ID Convention:**
- Single-product orders: `orderId + "WEB"` maps to one Mirakl row; also referenced as `orderId + "WEB-A"` in multi-shipment tests
- Multi-product/split orders: suffixes `WEB-A`, `WEB-B`, `WEB-C` … `WEB-F` (up to 6 for SPLIT_01)
- **FBS single-shipment tests search with the specific suffix directly:** `TC_FBS_001`–`004` (1 3P seller → always exactly 1 shipment) search Mirakl with `orderId + "WEB-A"` directly — never the generic `orderId + "WEB"` — for the sync-wait loop, `hasSearchResults()`, and `clickOrderInList()`.
- **FBS dual-shipment tests (`TC_FBS_005`/`006`, 2 different 3P sellers → always exactly 2 shipments) search the sync-wait loop with the generic `orderId + "WEB"` term ONCE per attempt, then check that single result table for both `orderId + "WEB-A"` and `orderId + "WEB-B"` via `hasSearchResults()`.** An earlier revision searched directly with each specific suffix back-to-back in the same attempt (`searchOrder(shipmentRefA)` immediately followed by `searchOrder(shipmentRefB)`, no generic search or `clickAllOrders()` in between) — confirmed via live run on `TC_FBS_006` to leave the Mirakl search field holding both terms concatenated together (e.g. the field ends up containing `"...WEB-A...WEB-B"`), which never matches any row and makes the sync-wait loop spin through all 10 attempts reporting both shipments absent even after they've actually synced. Fixed in both `TC_FBS_005` and `TC_FBS_006` by searching the generic term exactly once per attempt. The later per-shipment steps (opening + accepting WEB-B specifically) still do a single fresh `searchOrder(shipmentRefB)` call after navigating back via `clickAllOrders()` — that's a lone search, not a back-to-back pair, so it isn't at risk of the same bug and doesn't need this fix.
- Kibo's `externalId` lookup in every FBS test still uses the plain `orderId + "WEB"` form regardless — Kibo's `externalOrderId` field never carries the Mirakl `-A`/`-B` shipment suffix (confirmed from live Kibo shipment JSON: `"externalOrderId":"...WEB"` even on a 2-shipment order).

**Multi-account session swap (TC_FBO_003/004/005/006/007–012/020/022/023/026/029/030/031/032):**
These tests carry their own `@BeforeClass`/`@AfterClass` FDA login on top of the FBO group-level login described in FBS/FBO Group-Level Login above:
- Credentials are **hardcoded `private static final` constants** in the test class (not in `config.properties`) — `TC_FDA_USER`/`TC_FDA_PASS`
- `@BeforeClass`: calls `fdaHomePage.logout()`, then logs in as `TC_FDA_USER`/`TC_FDA_PASS` — this runs *after* `@BeforeGroups("FBO")` has already logged in with `fbo.username`/`fbo.password`
- `@AfterClass(alwaysRun = true)`: calls `logout()` again and restores `config.getFdaUsername()/getFdaPassword()` (`fda.username`/`fda.password`) so subsequent tests are unaffected
- TC_FBO_020 adds a conditional guard `if (!TC_FDA_USER.equals(config.getFdaUsername()))` in both `@BeforeClass` and `@AfterClass` — copy this pattern for new TCs that may follow a same-user TC
- Never put TC-specific credentials into `config.properties`; keep them as constants in the test class
- Kibo search term: `orderId + "WEB"` (e.g., `4000267985WEB`)
- Mirakl API sub-order ID: `orderId + "WEB-A"` (append `-A` for first sub-order in `ReturnApiUtility`)
- **Note:** in every one of these TCs today, `TC_FDA_USER`/`TC_FDA_PASS` and `fda.username`/`fda.password` are literally the same value as `fbo.username`/`fbo.password` (`mgowda@kognivera.com`/`Mithun@12345`), so the per-class logout+relogin is currently a no-op layered on top of the group login. It's kept intentionally as the override mechanism for the day one of these TCs needs a genuinely different account — don't "simplify" it away.

TC_FBS_001 does **not** use this per-class swap pattern — it relies solely on `@BeforeGroups("FBS")`/`@AfterGroups("FBS")` for FDA login (see FBS/FBO Group-Level Login). It also does not swap Mirakl — the suite-default `mirakl.username`/`mirakl.password` already is the account this TC needs, so it reuses the Mirakl session from `@BeforeSuite` as-is.

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
`TC_FBS_001`–`TC_FBS_006`'s copies all add a third fallback level, `item.items[*].data` (line-item data), because the `FDA_Custom_STH_WF_V1.0` workflow puts `deliveryType` on the line item rather than the shipment or its packages — the per-file copies are no longer identical, so check which fields a new TC needs before copying an existing file's version verbatim.

**Deferred (soft) assertions for non-terminal checks mid-flow:**
Every `TC_FBS_*` test uses a `SoftAssert` for its Kibo `deliveryType` check(s) (mid-flow step — 60 in TC_FBS_001, 51 in TC_FBS_002, 44 in TC_FBS_003, 55 in TC_FBS_004; TC_FBS_005/006 assert once per Kibo shipment entry, matching their 2-shipment split) because that check happens mid-flow and must not abort the test before the remaining Mirakl steps (documents, tracking, Mark as Shipped, Received) run — a hard `Assert` there would stop the run before those later steps get exercised. The soft assertion is recorded and only surfaced via `softAssert.assertAll()` at the very end, after all steps complete. Use this pattern for any future TC where a validation midway through the flow should be recorded but not block later steps from running.

**Final FDA "Mis pedidos" re-check after Received (all `TC_FBS_*` tests):**
After every Mirakl shipment reaches `Received` (both shipments, for the dual-shipment `TC_FBS_005`/`006`), each FBS test switches back to the FDA tab, navigates to Mis pedidos, and re-asserts `isOrderHistoryDisplayed()` + `isOrderPresent(orderId)`, logging (not hard-asserting) the order's status text at that point — no test in this codebase asserts a specific FDA status string post-delivery, since that value has never been confirmed via a live run. This is a closing sanity check, not a new numbered manual-case step; it runs after the last Mirakl `PASS` screenshot and before the "execution completed successfully" log line. `driver.quit()` is still never called here — the shared browser/session lifecycle (one browser across all `TC_FBS_*` tests, closed only in `@AfterSuite`) is unchanged; this step only switches tabs.

**TC_FBS_004 — single Mirakl shipment, no WEB-A/WEB-B split (learned the hard way):**
An earlier revision of this test assumed TC_FBS_004 (2 SKUs qty=2 each, 1 3P seller) would split into WEB-A + WEB-B by analogy with its FBO counterpart, `TC_FBO_023`, which does split for the equivalent product setup. A live run proved that assumption wrong: order `4000304735` showed only a `WEB-A` row in Mirakl — `WEB-B` never existed — and the test failed on `Mirakl shipment WEB-B not found`. FBS orders consolidate into a single shipment regardless of quantity when both SKUs share **the same seller**, same as `TC_FBS_002` (2 SKUs qty=1 each, same seller); the FBO carrier/quantity-driven splitting behavior does not carry over to the FBS flow. **Do not assume same-seller multi-SKU FBS orders split without a live run confirming it for the specific SKUs in use.**

**TC_FBS_005/006 — 2 different 3P sellers DOES split, unlike TC_FBS_004:**
Unlike TC_FBS_004 (2 SKUs, same seller, wrongly assumed to split), TC_FBS_005 (qty=1 each) and TC_FBS_006 (qty=2 each) use 2 SKUs from **2 different 3P sellers**, which is a different and more reliable predictor of splitting: different-seller orders split into separate Mirakl shipments as a structural property of Mirakl's data model (each seller/shop gets its own order line), confirmed consistently across every 2-different-seller FBO test (`TC_FBO_005`, `006`, `011`, `012`). So both TC_FBS_005 and TC_FBS_006 implement WEB-A + WEB-B dual-shipment handling, reusing the same per-shipment fulfillment helper pattern originally built (then reverted) for TC_FBS_004. If this assumption ever turns out wrong for a specific SKU pair, the fix is the same as TC_FBS_004's: strip the dual-shipment handling back to a single-shipment flow — don't leave partially-dual code in place.

**Entregado → Received needs a pre-poll sleep, not just quick refreshes (all `TC_FBS_*` tests):**
Live runs of `TC_FBS_006` and later `TC_FBS_004` both failed with `Mirakl order status should change from 'Shipped' to 'Received'. Actual: Shipped` even though the whole Entregado click sequence (More actions → Custom field → Entregado → Yes → Confirm) completed with zero errors. The original 6 rapid `refreshAndWait()`-based status checks (~30s total, no sleep beforehand — or ~40s in `TC_FBS_001`, which already had a 10s wait) simply weren't enough time for the backend to process the change — unlike the carrier-webhook-driven transitions in the FBO tests (e.g. `TC_FBO_005`'s Envioclick/Skydropx flows), which always sleep 30s before polling and allow far more than 6 retries, this step had never gotten the same treatment in any FBS test. **Fixed in all six FBS tests** (`TC_FBS_001`–`006`): `Thread.sleep(30_000)` right after `clickCustomFieldConfirmButton()` (`TC_FBS_001` extended its existing 10s manual-case wait to 30s instead of adding a second sleep), and `waitForMiraklStatus("Received", ...)` raised from 6 to 20 retries. If a brand new FBS TC is added later, copy this pattern from the start rather than the bare 6-retry version.

**`switchToCheckoutWindow()` must poll, not sample once:**
A live run of TC_FBS_004 got stuck after "Proceed to payment": `driver.getWindowHandles()` was read exactly once, immediately after the click, so it caught neither a not-yet-opened checkout window nor a not-yet-closed original tab — both of the method's branches silently no-opped, leaving the driver focused on the original FDA tab. Every subsequent locator search (Siguiente button, credit-card radio, card-number iframe) then searched that stale tab and found nothing, each logging a misleading "not present — assuming already on payment step" instead of failing loudly, until ~3 minutes later the tab was found actually closed (`target window already closed`). Fixed by polling `getWindowHandles()` for up to 20s until the handle set settles into either "a third handle appeared" or "the original handle is gone" before deciding which window to switch to; TC_FBS_005 was written with this poll-based version from the start. If a future TC's checkout hangs the same way, check this same race first — every test class's `switchToCheckoutWindow()` is a private, per-file copy (see "Checkout window handling" below), so the fix must be applied per file, not in one shared place.

**Kibo shipment list response format:**
`kiboGetShipmentsByOrderId()` may return `items[]` (standard Kibo format) or `_embedded.shipments` (HAL format). Tests check both — copy this pattern when adding new Kibo list parsers.

**Kibo shipment index mapping for multi-SKU orders (TC_FBO_005/006/023):**
Kibo does not guarantee `items[0]` → WEB-A. For different-seller orders (TC_FBO_005/006), Seller B ships first: `items[0]` → WEB-B, `items[1]` → WEB-A. For same-seller orders (TC_FBO_023), index order depends on which SKU appears first in cart: the cart-first SKU maps to WEB-B (`items[0]`). Always assign `shipmentRef`/`miraklDetailUrl` by inspecting which SKU Kibo processed first — do not assume index 0 → WEB-A.

**Envioclick intermediate status:**
After `postEnvioclickEnTransito()`, Mirakl may show either `"Shipped"` or `"3PL delivery"` (both are valid intermediate states). Use `waitForMiraklStatusOneOf(new String[]{"Shipped", "3PL delivery"}, retries)` — not `waitForMiraklStatus("Shipped", retries)` — for this step. After `postEnvioclickEntregado()`, the status must be exactly `"Received"`.

**`kiboSkipValidateItemsTask`:**
Call `ApiUtility.kiboSkipValidateItemsTask(token, shipmentNumber)` when staging SKUs have zero warehouse stock. This PUTs to `.../tasks/Validate%20Items%20In%20Stock/skipped` so the 3PL connector can proceed to generate a carrier label. Use it before polling for `deliveryPartner` / `3pl_shipmentId` if shipment data never populates.

**testng.xml execution order:**
Suite runs 25 `<test>` blocks sequentially (TC_FBS_001–012 share a single `<test>` block — see FBS/FBO Group-Level Login for why). Actual order:
`001 → 002 → 003 → 004 → 005 → 006 → 007 → 008 → 009 → 010 → 011 → 012 → SPLIT_01 → 020 → 021 → 022 → 023 → 026 → 027 → 028 → 029 → 030 → 031 → 032 → (FBS_001…FBS_012 in one shared <test> block)`
TC_FBO_SPLIT_01 runs **before** TC_FBO_020 (XML labels it "TEST 13"). TC_FBS_001–012 run last; all twelve also live together in the shared `testng_fbs.xml` for FBS-only runs (see Run Tests above). Neither `TC_E2E_004_Test` nor the `OfferAndProductModule` package (`TC_FBS_001_Test`, `TC_OU_009_Test`) is referenced by `testng.xml` — they are separate, independently-run test classes (see "Offer & Product Automation (second module)" below).
PayPal TCs (TC_FBO_007–012, TC_FBS_007–012): update `TC_PAYPAL_PASS` constant in each test class before running — password is intentionally left blank in committed code. Always re-blank (`TC_PAYPAL_PASS = ""`) before committing; TC_FBO_008–012 are especially at risk because they carry filled-in passwords locally.

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
| `fda.username` | Multi-account-swap tests (`@AfterClass` restore) | Legacy suite-default FDA login; no longer used for the initial login (see `fbo.username`) |
| `fda.password` | Multi-account-swap tests (`@AfterClass` restore) | Password for `fda.username` |
| `fbo.username` | `BaseClass` `@BeforeGroups("FBO")` | FDA login shared by every `TC_FBO_*` test for the whole `FBO` group |
| `fbo.password` | `BaseClass` `@BeforeGroups("FBO")` | Password for `fbo.username` |
| `fbs.username` | `BaseClass` `@BeforeGroups("FBS")` | FDA login shared by every `TC_FBS_*` test for the whole `FBS` group |
| `fbs.password` | `BaseClass` `@BeforeGroups("FBS")` | Password for `fbs.username` |
| `fda.sku` | Tests | Primary SKU for single-product tests |
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
| `fbs001.sku` | `TC_FBS_001_Test` | SKU fulfilled by the FBS store |
| `fbs001.card.number` | `TC_FBS_001_Test` | Credit card number for the FBS checkout |
| `fbs001.card.expiry` | `TC_FBS_001_Test` | Card expiry MM/YY for the FBS checkout |
| `fbs001.card.cvv` | `TC_FBS_001_Test` | Card CVV for the FBS checkout |
| `fbs001.invoice.file.path` | `TC_FBS_001_Test` | Absolute path to the Invoice PDF uploaded to Mirakl; file must exist on the machine running the test |
| `fbs002.sku1` / `fbs002.sku2` | `TC_FBS_002_Test` | The two SKUs (same 3P seller) added to cart |
| `fbs002.card.number` / `fbs002.card.expiry` / `fbs002.card.cvv` | `TC_FBS_002_Test` | Credit card for the FBS checkout |
| `fbs002.invoice.file.path` | `TC_FBS_002_Test` | Absolute path to the Invoice PDF uploaded to Mirakl |
| `fbs003.sku` | `TC_FBS_003_Test` | SKU purchased at qty=2 |
| `fbs003.card.number` / `fbs003.card.expiry` / `fbs003.card.cvv` | `TC_FBS_003_Test` | Credit card for the FBS checkout |
| `fbs003.invoice.file.path` | `TC_FBS_003_Test` | Absolute path to the Invoice PDF uploaded to Mirakl |
| `fbs004.sku1` / `fbs004.sku2` | `TC_FBS_004_Test` | The two SKUs (same 3P seller) added to cart, each raised to qty=2 |
| `fbs004.card.number` / `fbs004.card.expiry` / `fbs004.card.cvv` | `TC_FBS_004_Test` | Credit card for the FBS checkout |
| `fbs004.invoice.file.path` | `TC_FBS_004_Test` | Absolute path to the Invoice PDF uploaded to Mirakl |
| `fbs005.sku1` / `fbs005.sku2` | `TC_FBS_005_Test` | The two SKUs (2 different 3P sellers) added to cart, qty=1 each |
| `fbs005.card.number` / `fbs005.card.expiry` / `fbs005.card.cvv` | `TC_FBS_005_Test` | Credit card for the FBS checkout |
| `fbs005.invoice.file.path` | `TC_FBS_005_Test` | Absolute path to the Invoice PDF uploaded to Mirakl (same file uploaded to both WEB-A and WEB-B) |
| `fbs006.sku1` / `fbs006.sku2` | `TC_FBS_006_Test` | The two SKUs (2 different 3P sellers) added to cart, each raised to qty=2 |
| `fbs006.card.number` / `fbs006.card.expiry` / `fbs006.card.cvv` | `TC_FBS_006_Test` | Credit card for the FBS checkout |
| `fbs006.invoice.file.path` | `TC_FBS_006_Test` | Absolute path to the Invoice PDF uploaded to Mirakl (same file uploaded to both WEB-A and WEB-B) |
| `fbs007.sku` | `TC_FBS_007_Test` | SKU for the single-product PayPal FBS checkout |
| `fbs007.invoice.file.path` | `TC_FBS_007_Test` | Absolute path to the Invoice PDF uploaded to Mirakl |
| `fbs008.sku1` / `fbs008.sku2` | `TC_FBS_008_Test` | The two SKUs (same 3P seller) added to cart, qty=1 each |
| `fbs008.invoice.file.path` | `TC_FBS_008_Test` | Absolute path to the Invoice PDF uploaded to Mirakl |
| `fbs009.sku` | `TC_FBS_009_Test` | SKU purchased at qty=2 |
| `fbs009.invoice.file.path` | `TC_FBS_009_Test` | Absolute path to the Invoice PDF uploaded to Mirakl |
| `fbs010.sku1` / `fbs010.sku2` | `TC_FBS_010_Test` | The two SKUs (same 3P seller) added to cart, each raised to qty=2 |
| `fbs010.invoice.file.path` | `TC_FBS_010_Test` | Absolute path to the Invoice PDF uploaded to Mirakl |
| `fbs011.sku1` / `fbs011.sku2` | `TC_FBS_011_Test` | The two SKUs (2 different 3P sellers — same pairing as `fbs005.sku1`/`fbs005.sku2`) added to cart, qty=1 each |
| `fbs011.invoice.file.path` | `TC_FBS_011_Test` | Absolute path to the Invoice PDF uploaded to Mirakl (same file uploaded to both WEB-A and WEB-B) |
| `fbs012.sku1` / `fbs012.sku2` | `TC_FBS_012_Test` | The two SKUs (2 different 3P sellers — same pairing as `fbs006.sku1`/`fbs006.sku2`) added to cart, each raised to qty=2 |
| `fbs012.invoice.file.path` | `TC_FBS_012_Test` | Absolute path to the Invoice PDF uploaded to Mirakl (same file uploaded to both WEB-A and WEB-B) |
| `fda.split.skus` | *(unused)* | No code reads this key today; not tied to any current test |
| `headless` | *(unused)* | `BaseClass` calls `DriverFactory.createDriver(false)` with a hardcoded `false` — this key is not currently wired to anything |
| `jnag.MiraklSeller.url` / `.email` / `.password` | `TC_E2E_004_Test` | Independent Mirakl Seller login, not shared with any other TC |
| `jnag.MiraklOperator.email` / `.password` | `TC_E2E_004_Test` | Independent Mirakl Operator login (catalog approval) |
| `jnag.Adobe.url` / `.username` / `.password` | `TC_E2E_004_Test` | Adobe Commerce (Magento) admin login for the MCM sync step |
| `jnag.FDA.username` / `.password` | `TC_E2E_004_Test` | FDA storefront login for the final PDP verification step |
| `tc.e2e004.product.excel.path` | `TC_E2E_004_Test` | Absolute path to the Product import Excel; hardcoded to a `C:\Users\JanviNag\Downloads\...` path — machine-specific, will not exist on another tester's machine |
| `tc.e2e004.offer.excel.path` | `TC_E2E_004_Test` | Absolute path to the Offer import Excel; same machine-specific caveat |
| `tc.e2e004.offer.sync.wait.seconds.initial` | `TC_E2E_004_Test` | Wait (60s) before the first Offer Excel upload attempt, post-Publish |
| `tc.e2e004.offer.sync.wait.seconds.retry` | `TC_E2E_004_Test` | Wait (180s) before a retry attempt if the first Offer import is rejected |
| `tc.e2e004.offer.import.max.attempts` | `TC_E2E_004_Test` | Max Offer import attempts (2) |
| `tc.ou009.mirakl.url` / `.username` / `.password` | `TC_OU_009_Test` | Mirakl Seller login for the offer-price-update flow |
| `tc.ou009.fda.url` / `.username` / `.password` | `TC_OU_009_Test` | FDA storefront login used in Phase 4 (a login-page URL, not the plain storefront root) |
| `tc.ou009.offer.excel.path` | `TC_OU_009_Test` | Absolute path to the single-offer-row Offer Excel read/updated by `ExcelUtility`; machine-specific |
| `tc.ou009.push.offers.url` / `.cookie` | `TC_OU_009_Test` (`ApiUtility.pushOffersToEmpathy()`) | GET endpoint that syncs Mirakl's offer/price data into FDA's search index; the cookie is a live session value that expires — refresh it from a real browser session against `tc.ou009.mirakl.url` if this call starts failing |

## Offer & Product Automation (second module, separate from Order Lifecycle)

A second, independent automation effort lives alongside the Order Lifecycle framework above. It targets Mirakl catalog/offer management (Seller + Operator roles) and Adobe Commerce (Magento) MCM sync, not the FDA order flow. None of these classes are wired into `testng.xml` or `testng_fbs.xml`, and **their source files still compile as part of every `mvn test` run** (Maven compiles all of `src/test/java` regardless of which suite XML runs) — see the blocking-build-issue callout under Test Case Catalog.

**`tests/OfferAndProductModule/TC_FBS_001_Test.java`** — a from-scratch re-implementation of the order-lifecycle `TC_FBS_001` flow (same package name collision avoided: this one lives in the `tests.OfferAndProductModule` sub-package, not `tests`), but with a deliberately different architecture:
- Does **not** extend `BaseClass` — no group-level login, no shared ThreadLocal driver.
- Uses `DualDriverManager.createFreshChromeDriver()` to launch two fully independent `ChromeDriver` instances (`fdaDriver`, `miraklDriver`) with their own cookies/sessions — true parallel-browser isolation, unlike `DriverFactory`'s single-driver/multi-tab model used everywhere else in this repo.
- Reads test data from its own `src/test/resources/testdata/TC_FBS_001.properties` via `TCFBS001TestDataReader`, isolated from `config.properties` — so this class and the order-lifecycle `TC_FBS_001_Test` never share configuration despite the same class/test name.
- Run via `mvn test -DsuiteXmlFile=src/test/resources/testngOfferAndProductModule.xml` (its own single-class suite) or `mvn test -Dtest=FDA_Automation_Script.FDA_Automation_Script.tests.OfferAndProductModule.TC_FBS_001_Test`.

**`tests/TC_E2E_004_Test.java`** — a standalone (not `testng.xml`-referenced; run only via `mvn test -Dtest=TC_E2E_004_Test`) full product+offer lifecycle: Seller uploads a Product Excel (`MiraklProductImportsPage`) → Operator approves via Catalog Management (`MiraklCatalogManagementPage`) → Adobe Admin MCM sync (`AdobeLoginPage`, `AdobeSynchronizationPage.clickImportToMagento()`) → both roles verify Published status → Offer Excel import (`MiraklFileImportsPage`, with retry/regenerate-SKU logic) → offer verified Active (`MiraklOfferPage`) → final FDA storefront PDP check. Extends `BaseClass` but overrides `setupSuite()` to skip FDA/Mirakl login entirely (`BaseClass` itself is untouched). Uses `ExcelImportValidator` to validate/auto-correct/regenerate IDs in the combined product+offer import Excel. All its config keys (`jnag.*`, `tc.e2e004.*`) are present in `config.properties`, but the two Excel path values are hardcoded to a specific tester's `Downloads` folder — update them for your machine before running.

**`tests/OfferAndProductModule/TC_OU_009_Test.java`** ("Offer Update") — Mirakl Seller updates an offer's price via Excel import, pushes the change to FDA's search index, then verifies the new price on the FDA storefront (PLP + PDP). Same architecture as this package's `TC_FBS_001_Test`: does **not** extend `BaseClass`, uses a single `DualDriverManager`-created Chrome instance logged into Mirakl in its own `@BeforeSuite` (`setupMiraklSession()`); the FDA storefront is opened in a **new tab within that same browser** during Phase 4 (not a second browser, since Mirakl and FDA phases never run concurrently). Flow: Phase 0 reads the offer Excel and prompts for a new price (`-Dtc.ou009.price=<value>`, required under Maven Surefire's forked JVM — stdin fallback for IDE/standalone runs) and writes it back via `ExcelUtility.updatePrice()`; Phase 1 captures the shop name and pre-update price/name from Mirakl Offers (search by Product ID, asserts exactly one result); Phase 2 uploads the Excel via `MiraklFileImportsPage` (Offers content type) and verifies the import reaches a terminal non-failed state; Phase 3 re-verifies the price changed and matches Excel; Phase 3B calls `ApiUtility.pushOffersToEmpathy()` (GET, cookie auth, retries 3x on connection failure, asserts HTTP 200); Phase 4 logs into FDA, searches by product name, checks the PLP price (non-blocking, 5 retries) then the PDP name/SKU/price (authoritative, with a seller-specific-offer-row fallback via `FDAPDPPage.getSellerOfferPrice()` if the PDP buy-box belongs to a different winning seller). Registered in `testngOfferAndProductModule.xml` alongside `TC_FBS_001_Test` as a second `<test>` block — run the whole suite via `mvn test -DsuiteXmlFile=src/test/resources/testngOfferAndProductModule.xml -Dtc.ou009.price=<value>`, or single-class via `mvn test -Dtest=FDA_Automation_Script.FDA_Automation_Script.tests.OfferAndProductModule.TC_OU_009_Test -Dtc.ou009.price=<value>`. Note: since TestNG fires every class's own `@BeforeSuite` once each before any test in the suite runs, running the combined suite opens all three browser windows (2 for `TC_FBS_001`, 1 for `TC_OU_009`) up front rather than lazily per `<test>` block.

**New page objects supporting this module:**
| Class | Responsibility |
|-------|-----------------|
| `pages/adobe/AdobeLoginPage.java` | Logs into Adobe Commerce (Magento) admin (Spanish UI, "Ingresar") |
| `pages/adobe/AdobeSynchronizationPage.java` | Triggers the MCM "Import in Magento" async product-import job; `isImportTriggered()` is best-effort (no real success confirmation exists in the admin UI) |
| `pages/mirakl/MiraklCatalogManagementPage.java` | Operator/Seller Catalog Manager grid: search, status polling (New/Pending/Published), Fragile/MSI edit-and-save |
| `pages/mirakl/MiraklFileImportsPage.java` | Prices & Stock → File Imports: upload, content-type select, import history polling |
| `pages/mirakl/MiraklOfferPage.java` | Prices & Stock → Offers: search/filter, Add Offer, offer-row field getters |
| `pages/mirakl/MiraklProductImportsPage.java` | Catalog → Product Imports: provider filter, row selection, Edit Catalogs → Accept |

**New utils supporting this module:**
| Class | Responsibility |
|-------|-----------------|
| `utils/DualDriverManager.java` | Standalone factory for independent `ChromeDriver` instances (same Chrome options as `DriverFactory`, but no ThreadLocal, no shared session) — used by both `OfferAndProductModule/TC_FBS_001_Test` and `OfferAndProductModule/TC_OU_009_Test` |
| `utils/ExcelImportValidator.java` | Validates/auto-corrects a combined product+offer import Excel's "Data" sheet; regenerates product/offer IDs and SKUs — used only by `TC_E2E_004_Test` |
| `utils/ExcelUtility.java` | Reads/updates the single-offer-row "Data" sheet of an Offer-only import Excel (`readFirstOfferRow()`, `updatePrice()`) — used only by `OfferAndProductModule/TC_OU_009_Test`; deliberately narrower than `ExcelImportValidator` (no auto-fix/regeneration, price-focused) |
| `utils/PriceUtility.java` | Parses/formats/compares price text pulled from Mirakl and FDA UI (`"$45.00"`, `"MXN$45.00"`, etc.) into plain doubles — used only by `OfferAndProductModule/TC_OU_009_Test`'s PLP/PDP price-match checks |
| `utils/TCFBS001TestDataReader.java` | Reads `testdata/TC_FBS_001.properties`, isolated from `config.properties` — used only by `OfferAndProductModule/TC_FBS_001_Test` |

`TC_OU_009_Test` also adds a handful of methods to existing page objects rather than new files: `FDASearchResultsPage.isResultsGridDisplayed()`/`getResultItemPrice()`/`openMatchingResult()`, `FDAPDPPage.getProductSku()` (JS `textContent` read — visibility-independent, same fix rationale as `getProductPrice()`) and `getSellerOfferPrice()` (best-effort fallback for a non-winning seller's own offer row), `MiraklLoginPage.getLoggedInAccountName()` (best-effort shop/account name read), and `MiraklOfferPage.getVisibleOfferRowCount()`. `ApiUtility.pushOffersToEmpathy()` was added the same way (Cookie-only GET, same pattern as `cancelFullOrder()`/`cancelShipment()`).

`AppTest.java` at the package root is unused Maven-archetype boilerplate (no `@Test` methods) — not part of any suite.
