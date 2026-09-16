package FDA_Automation_Script.FDA_Automation_Script.pages.adobe;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Login page for the Adobe Commerce (Magento) Admin panel — new package, no prior Adobe Admin
 * automation existed (distinct from the FDA customer-facing storefront, which already has its
 * own login under pages/fda/). Used by TC_E2E_009 to trigger the Mirakl->Magento product sync.
 */
public class AdobeAdminLoginPage extends BasePage {

    // TODO: Verify locators against actual Adobe Commerce Admin login DOM
    private static final By USERNAME_FIELD = By.xpath("//input[@id='username' or @name='login[username]']");
    private static final By PASSWORD_FIELD = By.xpath("//input[@id='login' or @name='login[password]']");
    private static final By SIGN_IN_BUTTON = By.xpath("//button[@type='submit']");

    public AdobeAdminLoginPage(WebDriver driver) {
        super(driver);
    }

    public void navigateTo(String url) {
        driver.get(url);
        LoggerUtility.info("Navigated to Adobe Admin URL: " + url);
    }

    public void login(String username, String password) {
        LoggerUtility.info("Logging into Adobe Admin as: " + username);
        type(USERNAME_FIELD, username);
        type(PASSWORD_FIELD, password);
        click(SIGN_IN_BUTTON);
        LoggerUtility.info("Adobe Admin login submitted");
    }
}
