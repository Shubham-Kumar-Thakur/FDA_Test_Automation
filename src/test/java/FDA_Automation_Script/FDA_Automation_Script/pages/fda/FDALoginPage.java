package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class FDALoginPage extends BasePage {

    // login[username] / login[password] scopes to the registered-customers form only
	private static final By EMAIL_FIELD    = By.xpath("//input[@id='email' and @name='login[username]']");
    private static final By PASSWORD_FIELD = By.xpath("//input[@id='pass' and @name='login[password]' and @type='password']");
    private static final By LOGIN_BUTTON   = By.xpath("//button[@class='action login primary']//span[contains(text(),'Iniciar sesi\u00F3n')]");
    private static final By LOGIN_ERROR_MSG = By.xpath(
        "//div[contains(@class,'message-error')] | //div[contains(@class,'mage-error')] | " +
        "//li[contains(@class,'message-error')] | //div[@data-bind and contains(@class,'message-error')]");

    public FDALoginPage(WebDriver driver) {
        super(driver);
    }

    public void enterEmail(String email) {
        LoggerUtility.info("Entering FDA email: " + email);
        type(EMAIL_FIELD, email);
    }

    public void enterPassword(String password) {
        LoggerUtility.info("Entering FDA password");
        jsClick(PASSWORD_FIELD);
        type(PASSWORD_FIELD, password);
    }

    public void clickLoginButton() {
        LoggerUtility.info("Clicking Iniciar sesi\u00F3n button");
        scrollIntoView(LOGIN_BUTTON);
        jsClick(LOGIN_BUTTON);
    }

    public void login(String email, String password) {
        waitForLoginFormReady();
        enterEmail(email);
        enterPassword(password);
        clickLoginButton();
        verifyLoginSuccess(email);
    }

    // Clicking "Iniciar sesion" is usually a client-side route change: the URL flips to
    // customer/account/login before the form has actually finished rendering, so the browser shows
    // a blank/white page for a moment while the email/password fields are present-but-unpainted in
    // the DOM (same class of SPA race documented for search/PDP navigation). A hard refresh forces
    // a clean, fully rendered login form before we start typing into it.
    //
    // A live run of the full FBS suite (2026-09-09) showed this URL navigation does not always
    // happen: the first 6 login/logout cycles in that browser session each navigated to
    // customer/account/login and refreshed fine, but the 7th cycle (deep into the session) never
    // changed the URL at all, and hard-gating on fluentWaitForUrl's 2-minute timeout skipped every
    // subsequent test. Racing both signals — URL change OR the email field already present, e.g.
    // a quick-login variant that doesn't navigate — avoids blocking the whole login on a signal
    // that isn't guaranteed to fire, while still forcing the hard refresh when the URL genuinely
    // did change (preserving the original SPA-race fix for the normal case).
    private void waitForLoginFormReady() {
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        boolean urlChanged;
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
                    .until(d -> d.getCurrentUrl().contains("customer/account/login")
                            || !d.findElements(EMAIL_FIELD).isEmpty());
            urlChanged = driver.getCurrentUrl().contains("customer/account/login");
        } catch (Exception e) {
            LoggerUtility.warn("Neither login URL nor email field appeared within 30s after clicking "
                    + "Iniciar sesion — proceeding to the long fluentWait below as a last resort");
            urlChanged = false;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
        if (urlChanged) {
            driver.navigate().refresh();
        }
        WaitUtility.fluentWait(driver, EMAIL_FIELD);
    }

    // Magento re-renders the same login URL with an inline error on bad credentials instead of
    // throwing — clickLoginButton() alone cannot tell success from failure, so wait for either
    // the URL to leave the login page (success) or a *visible, non-empty* error message (failure).
    // mage-error elements exist in the DOM as empty/hidden validation placeholders even when no
    // error has occurred, so presence alone is not a reliable failure signal — only isDisplayed()
    // with real text counts.
    private void verifyLoginSuccess(String email) {
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20))
                .until(d -> !d.getCurrentUrl().contains("customer/account/login")
                        || !getVisibleLoginErrorText().isEmpty());
        } catch (Exception ignored) {
            // Timed out waiting for either signal — fall through to the URL check below,
            // which will report the failure clearly.
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }

        if (driver.getCurrentUrl().contains("customer/account/login")) {
            String reason = getVisibleLoginErrorText();
            if (reason.isEmpty()) {
                reason = "still on login page after 20s — no visible error message (page title: " + driver.getTitle() + ")";
            }
            throw new RuntimeException("FDA login failed for " + email + ": " + reason);
        }
        LoggerUtility.info("FDA Login Successful — verified logged-in state for " + email);
    }

    private String getVisibleLoginErrorText() {
        for (org.openqa.selenium.WebElement el : driver.findElements(LOGIN_ERROR_MSG)) {
            try {
                if (el.isDisplayed()) {
                    String text = el.getText().trim();
                    if (!text.isEmpty()) return text;
                }
            } catch (Exception ignored) {
                // stale element between findElements() and isDisplayed() — skip it
            }
        }
        return "";
    }
}
