package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.ScreenshotUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class MiraklLoginPage extends BasePage {

    // TODO: Verify locators against actual Mirakl login DOM
	 private static final By EMAIL_FIELD    = By.xpath("//input[@id='username']");
	    private static final By NEXT_BUTTON    = By.xpath("//span[@id='submitLabel']");
	    private static final By PASSWORD_FIELD = By.xpath("//input[@id='password']");
	    private static final By SIGN_IN_BUTTON = By.xpath("//button[normalize-space()='Sign in']");
	    // TODO: Verify against actual Mirakl MFA/OTP screen DOM — best-effort, same convention as the
	    // rest of this file's locators. Scoped under the "Verify Your Identity" screen so it never
	    // matches an unrelated input/button elsewhere on the page.
	    private static final By MFA_CODE_INPUT = By.xpath(
	        "//*[contains(normalize-space(.),'Verify Your Identity')]"
	        + "//input[@type='text' or @type='tel' or @type='number' or not(@type)]");
	    private static final By MFA_CONTINUE_BUTTON = By.xpath(
	        "//*[contains(normalize-space(.),'Verify Your Identity')]"
	        + "//button[contains(translate(normalize-space(.),'CONTINUE','continue'),'continue')]");
    public MiraklLoginPage(WebDriver driver) {
        super(driver);
    }

    public void enterEmail(String email) {
        LoggerUtility.info("Entering Mirakl email: " + email);
        type(EMAIL_FIELD, email);
    }

    public void clickNext() {
        LoggerUtility.info("Clicking Siguiente button on Mirakl login");
        click(NEXT_BUTTON);
    }

    public void enterPassword(String password) {
        LoggerUtility.info("Entering Mirakl password");
        // ROOT-CAUSED live (TC_E2E_004, 2026-09-17): a live run threw NoSuchElementException here after
        // paying the full 2-minute implicit wait — the password field genuinely wasn't rendered yet
        // right after clickNext(), a one-off render delay in the email->password page transition that
        // every prior successful login in this session happened not to hit. clickNext() itself has no
        // wait after it. Bounded wait for the field to become clickable before typing, matching the
        // "wait before interacting" convention used everywhere else in this codebase.
        WaitUtility.fluentWaitForClickable(driver, PASSWORD_FIELD);
        type(PASSWORD_FIELD, password);
    }

    public void clickSignIn() {
        LoggerUtility.info("Clicking Sign in button on Mirakl");
        click(SIGN_IN_BUTTON);
    }

    public void login(String email, String password) {
        waitForLoginFormOrRecover();
        enterEmail(email);
        clickNext();
        enterPassword(password);
        clickSignIn();
        handleMfaIfRequired(email);
        LoggerUtility.info("Switching To Mirakl");
        LoggerUtility.info("Mirakl Login Successful");
    }

    // CONFIRMED live (2026-09-10, real test run): after navigating back to the login URL to switch
    // Seller<->Operator in the same browser session, the #username field can fail to appear at all —
    // burning the full 2-minute implicit wait on a single blind attempt before failing. Root cause
    // unconfirmed (a still-logged-in redirect vs. a slow SPA render both fit the evidence), so instead
    // of a single theory-specific workaround, retry with a forced logout + refresh between attempts —
    // this recovers either way. Uses an instant JS DOM check (not findElement()) to avoid re-triggering
    // the same implicit-wait stall on each retry.
    private void waitForLoginFormOrRecover() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            if (isUsernameFieldPresent()) {
                if (attempt > 1) LoggerUtility.info("Mirakl login form appeared after " + (attempt - 1) + " recovery attempt(s)");
                return;
            }
            LoggerUtility.warn("Mirakl login form (#username) not present (attempt " + attempt
                + "/4) — forcing logout and refreshing");
            forceLogout();
            driver.navigate().refresh();
            sleep(2000);
        }
        LoggerUtility.warn("Mirakl login form still not confirmed present after recovery attempts — "
            + "proceeding anyway (final attempt will rely on the standard 2-minute implicit wait)");
    }

    private boolean isUsernameFieldPresent() {
        Object found = ((JavascriptExecutor) driver).executeScript(
            "return document.evaluate(\"//input[@id='username']\", document, null, "
            + "XPathResult.BOOLEAN_TYPE, null).booleanValue;");
        return Boolean.TRUE.equals(found);
    }

    // CONFIRMED live (2026-09-10, HTML dumps across every Mirakl page probed): a hidden
    // <form action="/logout" method="POST"> with only a _csrf token is present in the DOM on most
    // (not all) pages — its visible trigger is a "Logout" item inside the account-menu dropdown
    // (button id "accountingMenu"), rendered via portal only once expanded, so its exact selector was
    // never captured. Submitting the form directly via JS achieves the same server-side effect
    // without clicking through the dropdown; when the form isn't present yet, clicking the account
    // menu trigger first mounts it.
    public void logout() {
        LoggerUtility.info("Logging out of Mirakl (submitting /logout form to fully invalidate session)");
        forceLogout();
    }

    private void forceLogout() {
        Object submitted = ((JavascriptExecutor) driver).executeScript(
            "var f = document.querySelector('form[action=\"/logout\"]');"
            + "if (f) { f.submit(); return true; } return false;");
        if (Boolean.TRUE.equals(submitted)) {
            LoggerUtility.info("Mirakl logout form submitted");
            sleep(2000);
            return;
        }
        LoggerUtility.warn("Mirakl /logout form not found on current page — trying account menu");
        ((JavascriptExecutor) driver).executeScript(
            "var m = document.getElementById('accountingMenu'); if (m) { m.click(); }");
        sleep(1000);
        Object submittedAfterMenu = ((JavascriptExecutor) driver).executeScript(
            "var f = document.querySelector('form[action=\"/logout\"]');"
            + "if (f) { f.submit(); return true; } return false;");
        if (Boolean.TRUE.equals(submittedAfterMenu)) {
            LoggerUtility.info("Mirakl logout form submitted after opening account menu");
            sleep(2000);
        } else {
            LoggerUtility.warn("Mirakl /logout form still not found — session may not be fully invalidated");
        }
    }

    // TODO: Not verified against the live DOM (the #accountingMenu trigger's exact text content was
    // never captured — see the class-doc note on forceLogout() above for why its own selector wasn't
    // fully confirmed either). Best-effort read of the account-menu trigger's own visible text, which
    // on most Mirakl shops/portals is the logged-in shop/operator name. Returns "" (never throws) if
    // not found, so callers should treat this as informational logging, not an assertion target.
    public String getLoggedInAccountName() {
        Object text = ((JavascriptExecutor) driver).executeScript(
            "var m = document.getElementById('accountingMenu'); return m ? m.textContent.trim() : '';");
        String value = text == null ? "" : text.toString().trim();
        LoggerUtility.info("Mirakl logged-in account/shop name (best-effort): " + value);
        return value;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleMfaIfRequired(String email) {
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
        boolean mfaShown;
        try {
            mfaShown = !driver.findElements(
                By.xpath("//*[contains(normalize-space(.),'Verify Your Identity')]")).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
        if (!mfaShown) return;

        LoggerUtility.warn("Mirakl MFA verification required — check email '"
            + email + "' for OTP code, enter it in the browser window, then click Continue. "
            + "Test will wait up to 10 minutes...");

        // Diagnostic only (2026-09-11): every run this session shows the Seller MFA (jnag+2002@...)
        // completing quickly while the Operator MFA (jnag@...) consistently times out at the full
        // 5 minutes — a 100% split across 7 independent runs, not consistent with random human
        // timing. Capturing a screenshot + full page text the moment each MFA screen appears so the
        // two can be compared directly (different verification method prompt? extra step? different
        // wording?) rather than continuing to guess.
        try {
            String safeEmail = email.replaceAll("[^a-zA-Z0-9]", "_");
            ScreenshotUtility.captureScreenshot(driver, "MFA_" + safeEmail, ScreenshotUtility.INFO);
            Object bodyText = ((JavascriptExecutor) driver).executeScript(
                "var el = document.body; return el ? el.innerText.substring(0, 3000) : '';");
            LoggerUtility.info("MFA screen text for " + email + ": " + bodyText);
        } catch (Exception diagEx) {
            LoggerUtility.warn("Could not capture MFA diagnostic for " + email + ": " + diagEx.getMessage());
        }

        driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        try {
            // Widened 5 -> 10 minutes (2026-09-11, per explicit instruction): the Operator MFA
            // (jnag@...) consistently timed out at 5 minutes across 7 runs while the Seller MFA
            // (jnag+2002@...) completed quickly every time on an identical screen — not a code/UI
            // difference, most likely the Operator prompt arriving several minutes into the run when
            // attention has shifted elsewhere. More buffer time, not a logic change.
            // Auto-click Continue, gated on REAL EVIDENCE the code was typed (per explicit user
            // instruction) — never on a timer/cooldown. Evidence = a non-blank `value` on the OTP
            // input. Tracks the last value it clicked for so a retry (user clears and retypes after
            // a rejected code) triggers a fresh click instead of only ever firing once.
            String[] lastClickedValue = {null};
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofMinutes(10))
                .pollingEvery(java.time.Duration.ofSeconds(2))
                .until(d -> {
                    boolean stillOnMfaScreen = !d.findElements(
                        By.xpath("//*[contains(normalize-space(.),'Verify Your Identity')]")).isEmpty();
                    if (!stillOnMfaScreen) {
                        return true;
                    }
                    java.util.List<org.openqa.selenium.WebElement> inputs = d.findElements(MFA_CODE_INPUT);
                    if (!inputs.isEmpty()) {
                        String value = inputs.get(0).getAttribute("value");
                        if (value != null && !value.isBlank() && !value.equals(lastClickedValue[0])) {
                            java.util.List<org.openqa.selenium.WebElement> buttons = d.findElements(MFA_CONTINUE_BUTTON);
                            if (!buttons.isEmpty()) {
                                LoggerUtility.info("OTP code entered (real evidence, not a timer) — clicking Continue.");
                                lastClickedValue[0] = value;
                                try {
                                    buttons.get(0).click();
                                } catch (Exception clickEx) {
                                    LoggerUtility.warn("Auto-click Continue failed: " + clickEx.getMessage());
                                }
                            }
                        }
                    }
                    return false;
                });
            LoggerUtility.info("Mirakl MFA verification completed — continuing.");
        } catch (org.openqa.selenium.TimeoutException timeoutEx) {
            // Diagnostic (2026-09-11): widening 5->10 minutes made no difference — Operator MFA still
            // timed out at the full window. Capturing the page state at the exact moment of timeout
            // (not just at detection) to see whether the OTP field shows an entered value, an error
            // message, or is still untouched — distinguishes "OTP never entered" from "entered but
            // rejected/stuck" rather than continuing to guess from the outside.
            try {
                String safeEmail = email.replaceAll("[^a-zA-Z0-9]", "_");
                ScreenshotUtility.captureScreenshot(driver, "MFA_TIMEOUT_" + safeEmail, ScreenshotUtility.FAIL);
                Object bodyText = ((JavascriptExecutor) driver).executeScript(
                    "var el = document.body; return el ? el.innerText.substring(0, 3000) : '';");
                LoggerUtility.warn("MFA TIMEOUT page state for " + email + ": " + bodyText);
            } catch (Exception diagEx) {
                LoggerUtility.warn("Could not capture MFA timeout diagnostic for " + email + ": " + diagEx.getMessage());
            }
            throw timeoutEx;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }
}
