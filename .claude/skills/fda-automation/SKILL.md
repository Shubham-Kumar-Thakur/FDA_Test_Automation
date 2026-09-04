---
name: fda-automation
description: FDA / Kibo / Mirakl Selenium automation framework rules. Use when generating, reviewing, or extending automation scripts for this project. Captures mandatory wait strategy, browser lifecycle, logging, screenshot, assertion, API payload, and coding standards.
---

# FDA Automation Framework — Mandatory Rules

Read BEFORE writing any code for this project.

## Stack

Java 17 · Selenium 4.25.0 · TestNG 7.10.2 · REST Assured 5.5.0 · Apache POI 5.3.0 · Log4j2 2.24.3 · WebDriverManager 5.9.2

## Immutable Framework Files (NEVER modify)

- `base/BaseClass.java`
- `utils/DriverFactory.java`
- `utils/ConfigReader.java`
- `utils/WaitUtility.java`
- `utils/ScreenshotUtility.java`
- `utils/LoggerUtility.java`
- `utils/ApiUtility.java`
- `listeners/TestNGListener.java`

Only add: new Page Objects, new Test classes, new helper methods if absolutely necessary.

## Wait Strategy

```java
// Global (set once in DriverFactory — DO NOT set again):
driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));

// Fluent wait — ONLY after refresh/reload/API execution:
WaitUtility.fluentWait(driver, locator);           // timeout=2min, poll=2sec
WaitUtility.fluentWaitForText(driver, locator, "expected text");

// Fluent wait config:
// timeout = 2 minutes
// polling = 2 seconds
// ignore: NoSuchElementException, StaleElementReferenceException, ElementNotInteractableException
```

**NEVER** use `Thread.sleep()` — only exception: Step 76 in TC_FBO_001 (30 sec shipment wait).

## Browser Lifecycle

```java
// NEVER call these in @AfterMethod or @AfterClass:
// driver.quit();
// driver.close();

// @AfterMethod: navigate each tab back to home page
// @AfterSuite only: DriverFactory.quitDriver();
```

Browser has 3 tabs:
- Tab 0: FDA (`fdaTabHandle`)
- Tab 1: Kibo OMS (`kiboTabHandle`)
- Tab 2: Mirakl (`miraklTabHandle`)

Switch tabs via `BaseClass.switchToFDATab()`, `switchToKiboTab()`, `switchToMiraklTab()`.

## Logging (Log4j2 via LoggerUtility)

```java
LoggerUtility.info("FDA Login Successful");
LoggerUtility.info("Order ID = " + orderId);
LoggerUtility.info("Kibo Status = " + kiboStatus);
LoggerUtility.info("Payment Status = " + paymentStatus);
LoggerUtility.info("API Response = " + response.getStatusCode());
LoggerUtility.error("Test Failed: " + e.getMessage());
```

Log: every action, every validation, every extracted value, every API request/response, every tab/refresh/retry.

## Screenshots

```java
ScreenshotUtility.captureScreenshot(driver, "TC_FBO_001", ScreenshotUtility.PASS);
ScreenshotUtility.captureScreenshot(driver, "TC_FBO_001", ScreenshotUtility.FAIL);
ScreenshotUtility.captureScreenshot(driver, "TC_FBO_001", ScreenshotUtility.INFO);
```

Capture after: login, search, PDP, cart, payment, success, order history, every status verification, every API call.
Saved to: `test-output/screenshots/TC_FBO_001_PASS.png`

## Assertions (TestNG only)

```java
Assert.assertEquals(actual, expected, "meaningful failure message");
Assert.assertTrue(condition, "meaningful failure message");
Assert.assertFalse(condition, "meaningful failure message");
```

Every assertion must have a failure message.

## Page Object Model

- All locators: `private static final By` inside Page class
- All business logic: methods in Page class
- Test class: only test flow + assertions + method calls
- NEVER put locators in test classes

## Dynamic Data (TC_FBO_001)

Capture once, store in instance fields, reuse everywhere:

| Variable | Source |
|----------|--------|
| `orderId` | FDA success page |
| `orderTotal` | FDA cart page |
| `tplShipmentId` | Kibo custom data: `3pl_shipmentId` |
| `carrierName` | Kibo custom data: `carrierName` |
| `trackingNumber` | Kibo custom data: `tracking_number` |
| `labelUrl` | Kibo custom data: `labelUrl` |
| `trackingUrl` | Kibo custom data: `tracking_url` |
| `deliveryPartner` | Kibo custom data: `deliveryPartner` |

## API Payloads

### Envioclick

```java
ApiUtility.postEnvioclickEnTransito(tplShipmentId, trackingNumber, orderId, carrierName);
ApiUtility.postEnvioclickEntregado(tplShipmentId, trackingNumber, orderId, carrierName);
```

Field mapping:
- `carrier` → `carrierName`
- `idOrder` → `tplShipmentId` (3pl_shipmentId from Kibo)
- `trackingCode` → `trackingNumber`
- `myShipmentReference` → `orderId + "WEB"`

### Skydropx

```java
ApiUtility.postSkydropxPickedUp(tplShipmentId);
ApiUtility.postSkydropxDelivered(tplShipmentId);
```

Field mapping:
- `data.id` → `tplShipmentId`
- `relationships.shipment.data.id` → `tplShipmentId`

## Status Flow (TC_FBO_001)

```
FDA: Creada
Kibo: Accepted → Processing → Completed
Payment: Authorized → Captured
Mirakl: Pending acceptance → Awaiting shipment → Shipped → Received

Envioclick: En tránsito → Shipped | Entregado → Received
Skydropx:   Picked_up → Shipped  | Delivered → Received
```

## Kibo Search Term

Always search Kibo using: `orderId + "WEB"` (e.g., `4000267985WEB`)

## Adding a New Test Case

1. Create Page Objects in `pages/fda|kibo|mirakl/` extending `BasePage`
2. Create test class in `tests/` extending `BaseClass`
3. Add to `testng.xml`
4. Reuse existing utilities — never duplicate

## Config Keys

```
fda.url, fda.username, fda.password, fda.sku, fda.card.number, fda.card.expiry, fda.card.cvv
kibo.url, kibo.username, kibo.password
mirakl.url, mirakl.username, mirakl.password
envioclick.url, skydropx.url, api.auth.token, api.cookie
```

Read via: `ConfigReader.getInstance().getFdaUrl()` etc.
