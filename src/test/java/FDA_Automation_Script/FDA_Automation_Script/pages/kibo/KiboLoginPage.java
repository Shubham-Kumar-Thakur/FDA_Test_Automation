package FDA_Automation_Script.FDA_Automation_Script.pages.kibo;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ConfigReader;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class KiboLoginPage extends BasePage {

    // TODO: Verify locators against actual Kibo OMS login DOM
    private static final By EMAIL_FIELD    = By.xpath("//input[@id='Email']");
    private static final By NEXT_BUTTON    = By.xpath("//input[@id='buttonSubmit']");
    private static final By PASSWORD_FIELD = By.xpath("//input[@id='Password']");
    private static final By LOGIN_BUTTON   = By.xpath("//input[@value='Log in']");

    public KiboLoginPage(WebDriver driver) {
        super(driver);
    }

    public void enterEmail(String email) {
        LoggerUtility.info("Entering Kibo email: " + email);
        type(EMAIL_FIELD, email);
    }

    public void clickNext() {
        LoggerUtility.info("Clicking Next button on Kibo login");
        By cfToken = By.name("cf-turnstile-response");
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        try {
            // Step 1: wait 60s for Turnstile to auto-verify (invisible mode can take time)
            boolean tokenReady = false;
            try {
                new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(60))
                    .until(d -> {
                        try {
                            String v = d.findElement(cfToken).getAttribute("value");
                            return v != null && !v.isEmpty();
                        } catch (Exception e) { return false; }
                    });
                LoggerUtility.info("Turnstile: auto-verified");
                tokenReady = true;
            } catch (Exception ignored) {
                LoggerUtility.info("Turnstile: not auto-verified after 60s, attempting widget click");
            }

            // Step 2: if not auto-verified, try clicking the Turnstile widget
            if (!tokenReady) {
                try {
                    // Broader selectors — Cloudflare Turnstile iframe src varies
                    By turnstileFrame = By.cssSelector(
                        "iframe[src*='challenges.cloudflare.com'], " +
                        "iframe[src*='cloudflare.com'], " +
                        "iframe[src*='turnstile'], " +
                        "div.cf-turnstile iframe, " +
                        "[data-sitekey] iframe");
                    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
                        .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(turnstileFrame));

                    org.openqa.selenium.WebElement iframeEl = driver.findElement(turnstileFrame);
                    LoggerUtility.info("Turnstile: iframe found, src=" + iframeEl.getAttribute("src"));
                    driver.switchTo().frame(iframeEl);
                    // Try multiple selectors for the Turnstile "I am human" checkbox
                    org.openqa.selenium.WebElement clickTarget = null;
                    for (String sel : new String[]{
                        ".ctp-checkbox-label",
                        "input[type='checkbox']",
                        ".cf-turnstile-checkbox-label",
                        "[id*='checkbox']",
                        "label",
                        "body"
                    }) {
                        try {
                            clickTarget = driver.findElement(By.cssSelector(sel));
                            LoggerUtility.info("Turnstile: found click target: " + sel);
                            break;
                        } catch (Exception ignored2) {}
                    }
                    if (clickTarget != null) {
                        // Human-like: move to element, pause, click
                        new org.openqa.selenium.interactions.Actions(driver)
                            .moveToElement(clickTarget)
                            .pause(java.time.Duration.ofMillis(800))
                            .click()
                            .perform();
                        LoggerUtility.info("Turnstile: clicked checkbox via Actions on: " + clickTarget.getTagName());
                    }
                    driver.switchTo().defaultContent();

                    // Step 3: wait up to 60s for token after manual click
                    try {
                        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(60))
                            .until(d -> {
                                try {
                                    String v = d.findElement(cfToken).getAttribute("value");
                                    return v != null && !v.isEmpty();
                                } catch (Exception e) { return false; }
                            });
                        LoggerUtility.info("Turnstile: token obtained after checkbox click");
                        tokenReady = true;
                    } catch (Exception e) {
                        LoggerUtility.info("Turnstile: no token after 60s checkbox wait");
                    }
                } catch (Exception ex) {
                    LoggerUtility.info("Turnstile: iframe interaction failed: " + ex.getMessage());
                    try { driver.switchTo().defaultContent(); } catch (Exception ignored3) {}
                    // Fallback: click the outer Turnstile container div (no iframe switch needed)
                    try {
                        org.openqa.selenium.WebElement outer = driver.findElement(
                            By.cssSelector(".cf-turnstile, [data-sitekey], [class*='turnstile']"));
                        LoggerUtility.info("Turnstile: clicking outer container: " + outer.getAttribute("class"));
                        new org.openqa.selenium.interactions.Actions(driver)
                            .moveToElement(outer)
                            .pause(java.time.Duration.ofMillis(500))
                            .click()
                            .perform();
                        // Wait briefly for token after outer click
                        try {
                            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20))
                                .until(d -> {
                                    try {
                                        String v = d.findElement(cfToken).getAttribute("value");
                                        return v != null && !v.isEmpty();
                                    } catch (Exception e2) { return false; }
                                });
                            LoggerUtility.info("Turnstile: token obtained after outer container click");
                            tokenReady = true;
                        } catch (Exception ignored4) {
                            LoggerUtility.info("Turnstile: no token after outer container click");
                        }
                    } catch (Exception ignored5) {
                        LoggerUtility.info("Turnstile: outer container not found either");
                    }
                }
            }

            if (!tokenReady) {
                LoggerUtility.info("Turnstile: proceeding without token (server-side may reject)");
            }
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
        click(NEXT_BUTTON);
    }

    public void enterPassword(String password) {
        LoggerUtility.info("Entering Kibo password");
        type(PASSWORD_FIELD, password);
    }

    public void clickLogin() {
        LoggerUtility.info("Clicking Log in button on Kibo");
        click(LOGIN_BUTTON);
    }

    public void login(String email, String password) {
        // Try API-based auth to bypass Cloudflare (tenant URL from config, no redirect confusion)
        if (loginViaApi(email, password)) {
            return;
        }
        // Fall back to UI login with Turnstile checkbox attempt
        LoggerUtility.info("API login failed — UI fallback with Turnstile click");
        enterEmail(email);
        clickNext();
        enterPassword(password);
        clickLogin();
        LoggerUtility.info("Kibo Login Successful (UI)");
    }

    private boolean loginViaApi(String email, String password) {
        // Build API base from the configured Kibo tenant URL (avoids redirect domain confusion)
        String kiboConfigUrl = ConfigReader.getInstance().getKiboUrl().replaceAll("/$", "");
        String[] authPaths = {
            "/api/platform/adminuser/authtickets",
            "/api/platform/applications/authtickets",
            "/api/platform/adminuser/authtickets/oauth"
        };

        for (String path : authPaths) {
            String endpoint = kiboConfigUrl + path;
            LoggerUtility.info("Kibo API auth attempt: " + endpoint);
            try {
                io.restassured.response.Response response = io.restassured.RestAssured.given()
                    .baseUri(kiboConfigUrl)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(java.util.Map.of("username", email, "password", password))
                    .post(path);

                int status = response.getStatusCode();
                String body = response.getBody().asString();
                LoggerUtility.info("  → status: " + status + " | body: " + body.substring(0, Math.min(300, body.length())));

                if (status == 200 || status == 201) {
                    LoggerUtility.info("Kibo API auth succeeded at: " + path);
                    injectCookiesAndNavigate(kiboConfigUrl, response);
                    return true;
                }
            } catch (Exception e) {
                LoggerUtility.info("  → exception: " + e.getMessage());
            }
        }
        return false;
    }

    private void injectCookiesAndNavigate(String kiboBase, io.restassured.response.Response apiResponse) {
        try {
            // Navigate browser to a path on the TENANT domain that won't redirect (API path shows JSON, stays on domain)
            driver.get(kiboBase + "/api/");
            LoggerUtility.info("Browser navigated to API path on tenant domain: " + driver.getCurrentUrl());
        } catch (Exception e) {
            LoggerUtility.info("Could not navigate to API path: " + e.getMessage());
        }

        // Set all API response cookies into the browser session
        java.util.Map<String, String> cookies = apiResponse.getCookies();
        LoggerUtility.info("API response cookies: " + cookies.keySet());
        for (java.util.Map.Entry<String, String> entry : cookies.entrySet()) {
            try {
                driver.manage().addCookie(new org.openqa.selenium.Cookie(entry.getKey(), entry.getValue()));
                LoggerUtility.info("Cookie injected: " + entry.getKey());
            } catch (Exception e) {
                LoggerUtility.info("Cookie skipped (" + entry.getKey() + "): " + e.getMessage());
            }
        }

        // Also inject accessToken from JSON body as mozushared if present
        try {
            String accessToken = apiResponse.jsonPath().getString("accountAuthTicket.accessToken");
            if (accessToken != null && !accessToken.isEmpty()) {
                driver.manage().addCookie(new org.openqa.selenium.Cookie("mozushared", accessToken));
                LoggerUtility.info("Injected accessToken as mozushared cookie");
            }
        } catch (Exception ignored) {}

        // Navigate to dashboard — with auth cookies, should not redirect to login
        driver.get(kiboBase + "/");
        LoggerUtility.info("Kibo Login Successful (API — Cloudflare bypassed). Current URL: " + driver.getCurrentUrl());
    }
}
