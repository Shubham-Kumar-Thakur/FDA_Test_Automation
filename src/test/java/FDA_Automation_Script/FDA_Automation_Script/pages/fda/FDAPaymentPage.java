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
	// normalize-space() (not text()) to match CARD_NUMBER_FIELD/EXPIRY_FIELD above — text() requires
	// an exact match with no extra whitespace or child nodes (e.g. a required-field asterisk or
	// tooltip icon span), making it the one iframe locator of the three that could silently fail
	// to find the label and, transitively, the CVV iframe itself.
	private static final By CVV_FIELD = By
			.xpath("//span[normalize-space()='Código de seguridad']/../following-sibling::div[1]//iframe");
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

				// Wait for the resolved input to actually be clickable (visible + enabled) before
				// touching it. The presence/isDisplayed check above only confirms the element
				// exists in the DOM — Adyen's securedFields can render a field (e.g. the security
				// code field) before it's enabled, commonly while it's still validating the card
				// number just typed into a sibling field. Without this wait, a native sendKeys()
				// on a not-yet-interactable element doesn't fail fast: ChromeDriver internally
				// polls for interactability bounded by whatever implicit wait is active — which by
				// this point has already been restored to the global 2-minute default in the
				// finally block above — so a genuinely-not-yet-ready field silently burns a full 2
				// minutes per retry attempt before throwing ElementNotInteractableException,
				// confirmed live (2026-09-17, TC_FBS_013 CVV field: two consecutive attempts each
				// failed at exactly 120s). Bounding this explicitly to 20s makes a real "never
				// becomes interactable" case fail fast across retries instead, and still succeeds
				// promptly once the field is genuinely just delayed rather than wrongly targeted.
				driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
				try {
					new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20))
							.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(input));
				} catch (org.openqa.selenium.TimeoutException te) {
					// Diagnostic-only: if this still times out, the log should say why (disabled?
					// zero-size? hidden via CSS?) instead of leaving another opaque "element not
					// interactable" to re-diagnose blind on the next live run.
					String diag;
					try {
						diag = "tag=" + input.getTagName() + " type=" + input.getAttribute("type")
								+ " disabled=" + input.getAttribute("disabled")
								+ " class=" + input.getAttribute("class")
								+ " style=" + input.getAttribute("style")
								+ " displayed=" + input.isDisplayed() + " enabled=" + input.isEnabled()
								+ " size=" + input.getSize();
					} catch (Exception diagEx) {
						diag = "diagnostic read failed: " + diagEx.getClass().getSimpleName();
					}
					LoggerUtility.warn("Resolved input never became clickable within 20s (" + iframeLocator + "): " + diag);
					throw te;
				} finally {
					driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
				}

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

	// Reads back a payment field's current raw value without typing anything — used only by
	// verifyAndReenterIfNeeded() below to detect a field that got silently wiped after it was
	// already typed and verified.
	private String readFieldValue(By iframeLocator) {
		try {
			driver.switchTo().defaultContent();
			driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
			org.openqa.selenium.WebElement iframe;
			try {
				iframe = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
						.until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(iframeLocator));
			} finally {
				driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
			}
			driver.switchTo().frame(iframe);
			java.util.List<org.openqa.selenium.WebElement> tagged = driver
					.findElements(By.cssSelector("input[data-fieldtype]"));
			org.openqa.selenium.WebElement input = !tagged.isEmpty() ? tagged.get(0)
					: driver.findElements(By.tagName("input")).stream()
							.filter(org.openqa.selenium.WebElement::isDisplayed).findFirst().orElse(null);
			String value = input == null ? null : input.getAttribute("value");
			driver.switchTo().defaultContent();
			return value;
		} catch (Exception e) {
			try {
				driver.switchTo().defaultContent();
			} catch (Exception ignored) {
			}
			return null;
		}
	}

	/**
	 * Re-checks all three payment fields immediately before submitting and re-types any that no
	 * longer hold the expected value. Confirmed live (2026-09-17, TC_FBS_013): typing the card
	 * number, then immediately moving to the expiry field, threw "target frame detached" on the
	 * expiry iframe — evidence that Adyen's SecuredFields component asynchronously re-renders its
	 * iframes shortly after a field completes (e.g. card-brand detection after a full card
	 * number). That re-render can happen after typeInIframeWithRetry()'s own per-field "value
	 * stuck" check already passed, silently wiping a field that tested fine moments earlier —
	 * Completar pago then submits against a blank/stale card number with no visible error, and the
	 * page just never navigates past #payment (no 3DS challenge, no failure message, no success
	 * page). Call this right before clickCompletePayment().
	 */
	public void verifyAndReenterIfNeeded(String cardNumber, String expiry, String cvv) {
		String expectedDigits = cardNumber.replaceAll("[^0-9]", "");
		for (int round = 1; round <= 2; round++) {
			boolean anyRetyped = false;

			String cardVal = readFieldValue(CARD_NUMBER_FIELD);
			String cardDigits = cardVal == null ? "" : cardVal.replaceAll("[^0-9]", "");
			if (!cardDigits.equals(expectedDigits)) {
				LoggerUtility.warn("Card number field no longer holds the expected value before submit (round "
						+ round + ", found " + cardDigits.length() + " digits) — re-entering");
				enterCardNumber(cardNumber);
				anyRetyped = true;
			}

			String expiryVal = readFieldValue(EXPIRY_FIELD);
			if (expiryVal == null || expiryVal.trim().isEmpty()) {
				LoggerUtility.warn("Expiry field no longer holds a value before submit (round " + round
						+ ") — re-entering");
				enterExpiry(expiry);
				anyRetyped = true;
			}

			String cvvVal = readFieldValue(CVV_FIELD);
			if (cvvVal == null || cvvVal.trim().isEmpty()) {
				LoggerUtility.warn("CVV field no longer holds a value before submit (round " + round
						+ ") — re-entering");
				enterCvv(cvv);
				anyRetyped = true;
			}

			if (!anyRetyped) {
				LoggerUtility.info("All payment fields confirmed intact before submit (round " + round + ")");
				return;
			}
		}
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
