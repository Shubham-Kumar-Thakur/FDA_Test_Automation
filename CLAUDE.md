# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run all tests (default suite)
mvn test

# Run a specific suite file
mvn test -DsuiteXmlFile=src/test/resources/testng_smoke.xml

# Run only smoke tests (using TestNG groups via surefire)
mvn test -Dgroups=smoke

# Run headless in a different browser
mvn test -Dbrowser=firefox -Dheadless=true

# Compile without running tests
mvn compile test-compile

# Clean build artifacts
mvn clean
```

Config overrides are passed as `-D` system properties; `ConfigManager` reads `config.properties` first, but system properties take precedence when accessed via `System.getProperty`.

## Architecture

**Layer structure** (src/main → framework; src/test → tests + pages):

```
src/main/java/com/fda/automation/
  config/ConfigManager.java      — singleton, loads config.properties; getters for browser/url/wait/headless
  utils/DriverFactory.java       — creates WebDriver per browser; reads from ConfigManager
  utils/ScreenshotUtils.java     — captures PNG to target/screenshots/ on failure
  utils/ExcelUtils.java          — reads .xlsx sheets into List<Map<String,String>> or Object[][] for @DataProvider
  base/BasePage.java             — abstract; wraps WebDriverWait; provides click/type/getText/isDisplayed/select helpers
  base/BaseTest.java             — TestNG base; ThreadLocal<WebDriver> for parallel safety; @BeforeMethod/@AfterMethod
  listeners/TestListener.java    — ITestListener; logs pass/fail/skip; auto-captures screenshot on failure

src/test/java/com/fda/automation/
  pages/                         — Page Object classes extending BasePage
  tests/                         — TestNG test classes extending BaseTest
```

**Key design decisions:**
- `BaseTest` uses `ThreadLocal<WebDriver>` — tests run in parallel (`parallel="methods"` in testng.xml) safely
- Implicit waits are explicitly set to 0; all waits go through `WebDriverWait` in `BasePage`
- `BasePage.navigateTo(path)` prepends `base.url` from config — page objects use relative paths only
- `TestListener` is wired in `testng.xml`, not via annotation, so it applies to all tests automatically

**Adding a new page:**
1. Create `src/test/java/com/fda/automation/pages/FooPage.java` extending `BasePage`
2. Define `private static final By` locators as constants
3. Use `BasePage` helper methods (`click`, `type`, `getText`, etc.) — do not call `driver.findElement` directly

**Adding a new test:**
1. Create `src/test/java/com/fda/automation/tests/FooTest.java` extending `BaseTest`
2. Call `getDriver()` to obtain the thread-local driver and pass to page constructors
3. Tag with `groups = {"smoke"}` or `groups = {"regression"}`

**Data-driven tests:** Use `ExcelUtils.toDataProvider(filePath, sheetName)` as the `@DataProvider` source; each row arrives as `Map<String, String>`.

## Configuration

`src/test/resources/config.properties`:
| Key | Default | Notes |
|-----|---------|-------|
| `browser` | `chrome` | `chrome`, `firefox`, `edge` |
| `base.url` | *(required)* | No trailing slash |
| `explicit.wait` | `10` | Seconds for `WebDriverWait` |
| `headless` | `false` | `true` for CI |

Logs written to `target/logs/test-run.log` (overwritten each run). Screenshots saved to `target/screenshots/`.
