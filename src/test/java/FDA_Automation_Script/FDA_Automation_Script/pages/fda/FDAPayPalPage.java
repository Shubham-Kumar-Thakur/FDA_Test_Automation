package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class FDAPayPalPage extends BasePage {

	// TODO: Verify locators against actual PayPal sandbox DOM
	// PayPal sandbox login page — email step
	private static final By EMAIL_FIELD = By.xpath("//input[@id='email' and @name='login_email']");
	private static final By NEXT_BTN = By.xpath("//button[text()='Siguiente' and @id='btnNext']");
	// PayPal sandbox login page — password step
	private static final By PASSWORD_FIELD = By
			.xpath("//input[@id='password' and @name='login_password']");
	private static final By LOGIN_BTN = By.xpath("//button[text()='Iniciar sesión' and @id='btnLogin']");
	// PayPal review/confirmation page — "Compra completa" / "Complete Purchase"
	private static final By COMPLETE_BTN = By
			.xpath("//button[text()='Compra completa' and @data-id='payment-submit-btn']");

	public FDAPayPalPage(WebDriver driver) {
		super(driver);
	}

	public void waitForPageLoad() {
		LoggerUtility.info("PayPal: Waiting for PayPal popup to fully load (up to 60s)");
		new WebDriverWait(driver, Duration.ofSeconds(60))
				.until(ExpectedConditions.or(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD),
						ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD),
						ExpectedConditions.titleContains("PayPal")));
		LoggerUtility.info("PayPal: Popup loaded — URL: " + driver.getCurrentUrl());
	}

	public void clickEmailField() {
		LoggerUtility.info("PayPal: Clicking 'Correo electrónico o número de celular' field");
		new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.elementToBeClickable(EMAIL_FIELD));
		click(EMAIL_FIELD);
	}

	public void enterEmail(String email) {
		LoggerUtility.info("PayPal: Entering PayPal email (value masked in logs)");
		type(EMAIL_FIELD, email);
	}

	public void clickNextButton() {
		LoggerUtility.info("PayPal: Clicking 'Siguiente' button");
		new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.elementToBeClickable(NEXT_BTN));
		jsClick(NEXT_BTN);
	}

	public void clickPasswordField() {
		LoggerUtility.info("PayPal: Clicking 'Contraseña' field");
		new WebDriverWait(driver, Duration.ofSeconds(30))
				.until(ExpectedConditions.elementToBeClickable(PASSWORD_FIELD));
		click(PASSWORD_FIELD);
	}

	public void enterPassword(String password) {
		LoggerUtility.info("PayPal: Entering PayPal password (value masked in logs)");
		type(PASSWORD_FIELD, password);
	}

	public void clickLoginButton() {
		LoggerUtility.info("PayPal: Clicking 'Iniciar sesión' button");
		new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.elementToBeClickable(LOGIN_BTN));
		jsClick(LOGIN_BTN);
	}

	public void clickCompletePayment() {
		LoggerUtility.info("PayPal: Clicking 'Compra completa' button");
		new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.elementToBeClickable(COMPLETE_BTN));
		jsClick(COMPLETE_BTN);
		LoggerUtility.info("PayPal: 'Compra completa' clicked successfully");
	}
}
