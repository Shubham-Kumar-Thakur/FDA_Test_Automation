package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class FDAPaymentPage extends BasePage {

	private static final By NEXT_BUTTON = By.xpath("//button[@data-role='opc-continue']//span[text()='Siguiente']");
	private static final By CREDIT_CARD_RADIO = By
			.xpath("//input[@type='radio' and @name='payment[method]' and @id='adyen_cc']");
	private static final By CARD_NUMBER_FIELD = By.xpath(
			"//span[@class='adyen-checkout__label__text'][normalize-space()='Número de tarjeta']/../following-sibling::div[1]//iframe");
	private static final By EXPIRY_FIELD = By
			.xpath("//span[normalize-space()='Fecha de expiración']/../following-sibling::div[1]//iframe");
	private static final By CVV_FIELD = By
			.xpath("//span[text()='Código de seguridad']/../following-sibling::div[1]//iframe");
	private static final By COMPLETE_PAY_BTN = By.xpath("//button[contains(text(),'Completar pago (MXN$')]");
	// TODO: Verify PayPal locators against actual payment page DOM
	private static final By PAYPAL_RADIO = By.xpath("//input[@id='paypal_express']/..//label[@for='paypal_express'] ");
	private static final By PAYPAL_PAY_BTN = By
			.xpath("//div[@class='paypal-button-label-container']");

	public FDAPaymentPage(WebDriver driver) {
		super(driver);
	}

	public void clickNextButton() {
		LoggerUtility.info("Clicking Siguiente button on shipping page (if present)");
		// Temporarily reduce implicit wait — page may already be on payment step
		driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(15));
		try {
			java.util.List<org.openqa.selenium.WebElement> btns = driver.findElements(NEXT_BUTTON);
			if (!btns.isEmpty()) {
				scrollIntoView(NEXT_BUTTON);
				jsClick(NEXT_BUTTON);
				LoggerUtility.info("Siguiente button clicked");
			} else {
				LoggerUtility.info("Siguiente button not present — checkout already on payment step");
			}
		} finally {
			driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
		}
	}

	public void selectCreditCardOption() {
		LoggerUtility.info("Selecting Credit/Debit Card Payment option");
		// If only one payment method exists, the radio may not be rendered; skip
		// gracefully
		driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(30));
		try {
			java.util.List<org.openqa.selenium.WebElement> radios = driver.findElements(CREDIT_CARD_RADIO);
			if (!radios.isEmpty()) {
				scrollIntoView(CREDIT_CARD_RADIO);
				jsClick(CREDIT_CARD_RADIO);
				LoggerUtility.info("Credit/Debit Card radio selected");
			} else {
				LoggerUtility.info("Credit card radio not present — assuming it is the only/default payment option");
			}
		} finally {
			driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
		}
	}

	public void enterCardNumber(String cardNumber) {
		LoggerUtility.info("Entering card number");
		String digits = cardNumber.replaceAll("[^0-9]", "");
		LoggerUtility.info("Card number digits to type: " + digits.length());
		typeInIframeWithRetry(CARD_NUMBER_FIELD, digits, true);
	}

	public void enterExpiry(String expiry) {
		LoggerUtility.info("Entering expiration date: " + expiry);
		typeInIframeWithRetry(EXPIRY_FIELD, expiry, false);
	}

	public void enterCvv(String cvv) {
		LoggerUtility.info("Entering security code");
		typeInIframeWithRetry(CVV_FIELD, cvv, false);
	}

	private void typeInIframeWithRetry(By iframeLocator, String text, boolean clearFirst) {
		Exception lastEx = null;
		for (int attempt = 1; attempt <= 5; attempt++) {
			try {
				// Recover if current window handle became stale (checkout opened in popup/new
				// tab)
				try {
					driver.switchTo().defaultContent();
				} catch (Exception windowEx) {
					String msg = windowEx.getMessage() == null ? "" : windowEx.getMessage().split("\n")[0];
					LoggerUtility.warn("Window handle stale (" + msg + ") — switching to available window");
					driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
					try {
						java.util.Set<String> available = driver.getWindowHandles();
						if (!available.isEmpty()) {
							driver.switchTo().window(available.iterator().next());
							LoggerUtility.info("Recovered — now on: " + driver.getWindowHandle());
							driver.switchTo().defaultContent();
						}
					} finally {
						driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
					}
				}
				// Suppress implicit wait so WebDriverWait(30s) is the true timeout for iframe
				// presence
				driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
				org.openqa.selenium.WebElement iframe;
				try {
					iframe = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
							.until(org.openqa.selenium.support.ui.ExpectedConditions
									.presenceOfElementLocated(iframeLocator));
				} finally {
					driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
				}
				((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);",
						iframe);
				driver.switchTo().frame(iframe);

				// Select Adyen's real encrypted field by its data-fieldtype attribute
				// (encryptedCardNumber / encryptedExpiryDate / encryptedSecurityCode), not by
				// on-screen visibility alone. Adyen injects an invisible autofill-decoy <input>
				// into the same iframe to trap browser autofill; that decoy commonly has
				// opacity:0 with a real size/position, so isDisplayed() alone can still match it,
				// silently swallowing sendKeys while the field the user actually sees stays empty.
				// The decoy never carries data-fieldtype, so it's a reliable way to skip it.
				driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
				try {
					new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(60))
							.until(d -> {
								try {
									return !d.findElements(By.cssSelector("input[data-fieldtype]")).isEmpty()
											|| d.findElements(By.tagName("input")).stream()
													.anyMatch(org.openqa.selenium.WebElement::isDisplayed);
								} catch (org.openqa.selenium.StaleElementReferenceException e) {
									return false;
								}
							});
				} finally {
					driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
				}
				java.util.List<org.openqa.selenium.WebElement> tagged = driver
						.findElements(By.cssSelector("input[data-fieldtype]"));
				org.openqa.selenium.WebElement input = !tagged.isEmpty() ? tagged.get(0)
						: driver.findElements(By.tagName("input")).stream()
								.filter(org.openqa.selenium.WebElement::isDisplayed).findFirst()
								.orElseThrow(() -> new org.openqa.selenium.NoSuchElementException(
										"No input found in iframe: " + iframeLocator));

				// JS click bypasses overlay/focus restrictions in Adyen iframes
				((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", input);
				if (clearFirst) {
					new org.openqa.selenium.interactions.Actions(driver).keyDown(org.openqa.selenium.Keys.CONTROL)
							.sendKeys("a").keyUp(org.openqa.selenium.Keys.CONTROL).perform();
					input.sendKeys(org.openqa.selenium.Keys.DELETE);
				}
				input.sendKeys(text);

				// Verify the value actually stuck — a silently-swallowed keystroke (wrong input
				// targeted, field not yet interactive) throws no exception, so sendKeys() alone
				// is not a reliable success signal.
				String actualValue = input.getAttribute("value");
				String actualDigits = actualValue == null ? "" : actualValue.replaceAll("[^0-9]", "");
				if (clearFirst) {
					if (!actualDigits.equals(text)) {
						throw new RuntimeException("Card field value mismatch after typing: expected "
								+ text.length() + " digits, field now holds " + actualDigits.length());
					}
				} else if (actualValue == null || actualValue.trim().isEmpty()) {
					throw new RuntimeException("Field is still empty after typing");
				}

				input.sendKeys(org.openqa.selenium.Keys.TAB);
				driver.switchTo().defaultContent();
				LoggerUtility.info("Iframe field typed on attempt " + attempt);
				return;
			} catch (Exception e) {
				lastEx = e;
				String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().split("\n")[0];
				LoggerUtility.warn("Iframe attempt " + attempt + " failed: " + msg);
				try {
					driver.switchTo().defaultContent();
				} catch (Exception ignored) {
				}
				if (attempt < 5) {
					try {
						Thread.sleep(2_000);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
		throw new RuntimeException("Failed to type in iframe after 5 attempts", lastEx);
	}

	public String getCompletePaymentButtonText() {
		String text = getText(COMPLETE_PAY_BTN);
		LoggerUtility.info("Completar pago button text: " + text);
		return text;
	}

	public void clickCompletePayment() {
		LoggerUtility.info("Clicking Completar pago button");
		scrollIntoView(COMPLETE_PAY_BTN);
		new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
				.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(COMPLETE_PAY_BTN));
		jsClick(COMPLETE_PAY_BTN);
	}

	// ----------------------------------------------------------------
	// PayPal payment methods
	// ----------------------------------------------------------------

	public void selectPayPalOption() {
		LoggerUtility.info("Selecting PayPal payment option");
		driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(30));
		try {
			java.util.List<org.openqa.selenium.WebElement> radios = driver.findElements(PAYPAL_RADIO);
			if (!radios.isEmpty()) {
				scrollIntoView(PAYPAL_RADIO);
				jsClick(PAYPAL_RADIO);
				LoggerUtility.info("PayPal radio button selected");
			} else {
				LoggerUtility.info("PayPal radio not found — assuming PayPal is default/only payment option");
			}
		} finally {
			driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
		}
	}

	public String getPayPalButtonText() {
		String text = getText(PAYPAL_PAY_BTN);
		LoggerUtility.info("PayPal Pagar button text: " + text);
		return text;
	}

	public java.util.Set<String> getAllWindowHandles() {
		return driver.getWindowHandles();
	}

	public void clickPayPalButton() {
		LoggerUtility.info("Clicking PayPal Pagar button");
		// PayPal Smart Buttons always render inside an iframe on this page — confirmed across
		// every live run to date that the main-DOM locator never matches, so searching iframes
		// first (instead of burning a fixed 60s wait on main DOM before falling back) avoids a
		// guaranteed-to-fail wait on every PayPal checkout.
		driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
		boolean clicked = false;
		try {
			try {
				clicked = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
						.pollingEvery(java.time.Duration.ofMillis(500))
						.until(d -> clickPayPalButtonInAnyIframe(d));
			} catch (org.openqa.selenium.TimeoutException te) {
				clicked = false;
			}
			if (!clicked) {
				LoggerUtility.info("PayPal button not found in any iframe — trying main DOM");
				new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
						.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(PAYPAL_PAY_BTN));
				((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
						"arguments[0].scrollIntoView(true);", driver.findElement(PAYPAL_PAY_BTN));
				((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
						"arguments[0].click();", driver.findElement(PAYPAL_PAY_BTN));
				clicked = true;
				LoggerUtility.info("PayPal button found and clicked in main DOM");
			}
		} finally {
			driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
			try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
		}
		if (!clicked) {
			throw new RuntimeException(
					"PayPal Pagar button not found in any iframe or main DOM after 45s");
		}
		LoggerUtility.info("PayPal Pagar button clicked — waiting for PayPal popup window");
	}

	private boolean clickPayPalButtonInAnyIframe(WebDriver d) {
		d.switchTo().defaultContent();
		int iframeCount = d.findElements(By.tagName("iframe")).size();
		for (int i = 0; i < iframeCount; i++) {
			try {
				d.switchTo().frame(i);
				java.util.List<org.openqa.selenium.WebElement> btns = d.findElements(PAYPAL_PAY_BTN);
				if (!btns.isEmpty() && btns.get(0).isDisplayed()) {
					((org.openqa.selenium.JavascriptExecutor) d)
							.executeScript("arguments[0].scrollIntoView(true);", btns.get(0));
					((org.openqa.selenium.JavascriptExecutor) d)
							.executeScript("arguments[0].click();", btns.get(0));
					LoggerUtility.info("PayPal button clicked inside iframe index " + i);
					return true;
				}
			} catch (Exception iframeEx) {
				LoggerUtility.warn("iframe[" + i + "] search failed: " + iframeEx.getClass().getSimpleName());
			} finally {
				try { d.switchTo().defaultContent(); } catch (Exception ignored) {}
			}
		}
		return false;
	}

	public void handle3dsChallenge() {
		By challengeIframe = By.cssSelector("iframe.adyen-checkout__threeds2__challenge--full-screen, "
				+ "div.adyen-checkout__threeds2 iframe, " + "iframe[name='threeDSIframe']");
		By passwordField = By
				.cssSelector("input[placeholder*='password'], input[placeholder*='Password'], input[type='password']");
		By okBtn = By.xpath("//button[normalize-space(.)='OK']");

		driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
		boolean has3ds = false;
		try {
			new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
					.until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(challengeIframe));
			has3ds = true;
		} catch (Exception ignored) {
		} finally {
			driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
		}

		if (has3ds) {
			LoggerUtility.info("3DS challenge detected, entering test password");
			driver.switchTo().frame(driver.findElement(challengeIframe));
			driver.findElement(passwordField).sendKeys("password");
			jsClick(okBtn);
			driver.switchTo().defaultContent();
			LoggerUtility.info("3DS challenge completed");
		} else {
			LoggerUtility.info("No 3DS challenge detected");
		}
	}
}
