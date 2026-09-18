package FDA_Automation_Script.FDA_Automation_Script.pages.adobe;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

// CONFIRMED live (2026-09-10): this Magento installation renders its admin login page in Spanish
// (document lang="es"), same pattern already seen in Mirakl/PayPal ("Siguiente"/"Iniciar sesión").
// The username/password field ids match Magento's defaults, but the submit button is a plain
// <button type="submit">Ingresar</button> — no id="send2", no English "Sign in" text.
public class AdobeLoginPage extends BasePage {

    private static final By USERNAME_FIELD = By.xpath("//input[@id='login_username' or @name='login[username]']");
    private static final By PASSWORD_FIELD = By.xpath("//input[@id='login_password' or @name='login[password]']");
    private static final By SIGN_IN_BUTTON = By.xpath(
        "//button[@id='send2' or @type='submit' or contains(normalize-space(),'Sign in') "
        + "or contains(normalize-space(),'Ingresar')]");

    public AdobeLoginPage(WebDriver driver) {
        super(driver);
    }

    public void login(String username, String password) {
        LoggerUtility.info("Logging into Adobe Admin as: " + username);
        type(USERNAME_FIELD, username);
        type(PASSWORD_FIELD, password);
        click(SIGN_IN_BUTTON);
        LoggerUtility.info("Adobe Admin login submitted");
    }
}
