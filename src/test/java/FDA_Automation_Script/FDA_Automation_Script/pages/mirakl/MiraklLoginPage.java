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
        handleMfaIfRequired();
        LoggerUtility.info("Switching To Mirakl");
        LoggerUtility.info("Mirakl Login Successful");
    }

    private void handleMfaIfRequired() {
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
            + "skthakur@kognivera.com' for OTP code, enter it in the browser window, then click Continue. "
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
