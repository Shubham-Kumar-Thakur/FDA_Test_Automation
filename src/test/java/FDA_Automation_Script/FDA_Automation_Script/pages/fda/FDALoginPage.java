package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
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
        enterEmail(email);
        enterPassword(password);
        clickLoginButton();
        verifyLoginSuccess(email);
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
