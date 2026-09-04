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
        LoggerUtility.info("FDA Login Successful");
    }
}
