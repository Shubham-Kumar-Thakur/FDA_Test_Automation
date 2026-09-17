package FDA_Automation_Script.FDA_Automation_Script.pages.mirakl;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class MiraklLoginPage extends BasePage {

    // TODO: Verify locators against actual Mirakl login DOM
	 private static final By EMAIL_FIELD    = By.xpath("//input[@id='username']");
	    private static final By NEXT_BUTTON    = By.xpath("//span[@id='submitLabel']");
	    private static final By PASSWORD_FIELD = By.xpath("//input[@id='password']");
	    private static final By SIGN_IN_BUTTON = By.xpath("//button[normalize-space()='Sign in']");

    // TODO: Verify locators against actual Mirakl account-menu/logout DOM
    private static final By ACCOUNT_MENU = By.xpath(
        "//button[contains(@class,'account') or contains(@aria-label,'account') or contains(@aria-label,'Account')]");
    private static final By LOGOUT_LINK = By.xpath(
        "//a[contains(normalize-space(.),'Sign out')] | //button[contains(normalize-space(.),'Sign out')] | "
        + "//a[contains(normalize-space(.),'Log out')] | //button[contains(normalize-space(.),'Log out')]");

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
        type(PASSWORD_FIELD, password);
    }

    public void clickSignIn() {
        LoggerUtility.info("Clicking Sign in button on Mirakl");
        click(SIGN_IN_BUTTON);
    }

    public void login(String email, String password) {
        enterEmail(email);
        clickNext();
        enterPassword(password);
        clickSignIn();
        handleMfaIfRequired(email);
        LoggerUtility.info("Switching To Mirakl");
        if (!isDashboardDisplayed()) {
            throw new IllegalStateException("Mirakl login did not land on a recognizable dashboard "
                + "(neither Seller nor Operator 'Price and stock' nav item was found) — login may have failed.");
        }
        LoggerUtility.info("Mirakl Login Successful");
    }

    // Confirms login actually succeeded rather than assuming it did once clickSignIn() returns.
    // "Price and stock" nav item is present on both Seller (span text) and Operator (button#priceAndStock)
    // dashboards — same dual-role locator already relied on in MiraklOffersPage.navigateToOffers() —
    // so this single check covers both roles without branching.
    public boolean isDashboardDisplayed() {
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
        try {
            return !driver.findElements(By.xpath(
                "//span[normalize-space()='Price and stock'] | //button[@id='priceAndStock']")).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }

    // Mirrors FDAHomePage.logout() — used for the Seller -> Operator session
    // swap on the same Mirakl tab (TC_E2E_009), since MiraklLoginPage had no
    // logout counterpart to login() before this.
    public void logout() {
        LoggerUtility.info("Attempting Mirakl logout");
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
        try {
            jsClick(ACCOUNT_MENU);
            java.util.List<org.openqa.selenium.WebElement> logoutLinks = driver.findElements(LOGOUT_LINK);
            if (!logoutLinks.isEmpty()) {
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
                jsClick(LOGOUT_LINK);
                LoggerUtility.info("Mirakl logout successful");
            } else {
                LoggerUtility.info("Mirakl logout link not found — user may already be logged out");
            }
        } catch (Exception e) {
            LoggerUtility.warn("Mirakl logout issue: " + e.getMessage() + " — continuing");
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
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
            + "Test will wait up to 5 minutes...");

        driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofMinutes(5))
                .pollingEvery(java.time.Duration.ofSeconds(3))
                .until(d -> d.findElements(
                    By.xpath("//*[contains(normalize-space(.),'Verify Your Identity')]")).isEmpty());
            LoggerUtility.info("Mirakl MFA verification completed — continuing.");
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }
}
