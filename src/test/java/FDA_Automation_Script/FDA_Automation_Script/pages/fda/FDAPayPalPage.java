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
	// PayPal "Pay with" funding-source picker — not always rendered (sandbox-account dependent).
	// The fallback branch is deliberately narrow (immediate following-sibling text, or an
	// aria-label) rather than "any ancestor mentions Visa anywhere in the DOM" — that broader
	// form matches virtually any radio button on the page once the word "Visa" appears anywhere
	// (e.g. in a footer or terms block), which can silently select the wrong funding source.
	private static final By VISA_RADIO = By.xpath(
			"//label[contains(.,'Visa')]//input[@type='radio'] | "
			+ "//input[@type='radio'][following-sibling::*[1][contains(.,'Visa')]] | "
			+ "//input[@type='radio'][@aria-label[contains(.,'Visa')]]");
	private static final By FULL_PURCHASE_BTN = By.xpath(
			"//button[contains(normalize-space(.),'Full purchase') or contains(normalize-space(.),'Realizar compra completa')]");

	public FDAPayPalPage(WebDriver driver) {
		super(driver);
	}

	public void waitForPageLoad() {
		LoggerUtility.info("PayPal: Waiting for PayPal popup to fully load (up to 60s)");
		new WebDriverWait(driver, Duration.ofSeconds(60))
				.until(ExpectedConditions.or(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD),
						ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD),
						ExpectedConditions.visibilityOfElementLocated(COMPLETE_BTN),
						ExpectedConditions.titleContains("PayPal")));
		LoggerUtility.info("PayPal: Popup loaded — URL: " + driver.getCurrentUrl());
	}

	// PayPal remembers a logged-in session within the same browser: confirmed via a live run
	// (2026-09-09, TC_FBS_008) that the SECOND+ PayPal checkout in one browser session skips
	// email/password entirely and lands directly on the "Pagar con" funding-source + "Compra
	// completa" screen — a screenshot at the point of failure showed no email/password field on
	// the page at all, which is why clickEmailField()'s wait was hanging/timing out. Callers must
	// check this before attempting the email/password steps.
	public boolean isLoginScreenDisplayed() {
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		try {
			return !driver.findElements(EMAIL_FIELD).isEmpty() || !driver.findElements(PASSWORD_FIELD).isEmpty();
		} finally {
			driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
		}
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

	// Not every PayPal sandbox account shows a funding-source picker — skip gracefully if absent,
	// same defensive pattern already used for optional payment-method UI elsewhere in this codebase.
	public void selectVisaOption() {
		LoggerUtility.info("PayPal: Selecting 'Visa' radio button under 'Pay with' section (if present)");
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		try {
			java.util.List<org.openqa.selenium.WebElement> radios = driver.findElements(VISA_RADIO);
			if (!radios.isEmpty()) {
				scrollIntoView(VISA_RADIO);
				jsClick(VISA_RADIO);
				LoggerUtility.info("PayPal: Visa radio button selected");
			} else {
				LoggerUtility.info("PayPal: Visa radio button not present — assuming default funding source already applies");
			}
		} finally {
			driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
		}
	}

	public void clickFullPurchaseButton() {
		LoggerUtility.info("PayPal: Clicking 'Full purchase' button (if present)");
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		try {
			java.util.List<org.openqa.selenium.WebElement> btns = driver.findElements(FULL_PURCHASE_BTN);
			if (!btns.isEmpty()) {
				scrollIntoView(FULL_PURCHASE_BTN);
				jsClick(FULL_PURCHASE_BTN);
				LoggerUtility.info("PayPal: 'Full purchase' button clicked");
			} else {
				LoggerUtility.info("PayPal: 'Full purchase' button not present — proceeding directly to Compra completa");
			}
		} finally {
			driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(2));
		}
	}
}
