package FDA_Automation_Script.FDA_Automation_Script.pages.fda;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class FDAHomePage extends BasePage {

    // Before login: single <button> with text "Mi cuenta"; after login: nested <button><button "Mi cuenta">
	private static final By PROFILE_ICON   = By.xpath("//button[@data-action='customer-menu-toggle'] | //li[@data-tooltip='Mi cuenta']/button[@type='button' and @aria-label='Mi cuenta']");
    private static final By SEARCH_FIELD   = By.xpath("//section[contains(@class,'empathy-search-input-wrapper')]/input[@placeholder='\u00BFQu\u00E9 est\u00E1s buscando?']");
    // Mis pedidos link appears in dropdown after login
    private static final By MY_ORDERS_LINK = By.xpath("//li/a[text()='Mis pedidos']");
    // Iniciar sesion appears in dropdown before login
    private static final By LOGIN_LINK     = By.xpath("//li//a[@class='customer-sign-in-link']//span[contains(text(),'Iniciar sesi\u00F3n')]");
    // Cerrar sesion appears in dropdown for logged-in users
    private static final By LOGOUT_LINK    = By.xpath(
        "//li/a[contains(@href,'customer/account/logout')] | " +
        "//li/a[normalize-space(.)='Cerrar sesi\u00F3n'] | " +
        "//li/a[contains(normalize-space(.),'Cerrar sesi\u00F3n')]");
    private static final By CART_ICON      = By.xpath("//div[@data-block='minicart']/a[@class='action showcart']");

    public FDAHomePage(WebDriver driver) {
        super(driver);
    }

    public void navigateTo(String url) {
        driver.get(url);
        LoggerUtility.info("Navigated to FDA URL: " + url);
    }

    public void clickProfileIcon() {
        LoggerUtility.info("Clicking Mi cuenta profile icon");
        // Diagnostic only (2026-09-16): PROFILE_ICON stopped matching on a real run — dump the
        // live header DOM on a genuine miss so the real current markup can be read off a live
        // run instead of guessing again, same pattern as FDAPDPPage.getProductSku().
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5));
        try {
            jsClick(PROFILE_ICON);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            try {
                java.nio.file.Files.writeString(
                    java.nio.file.Path.of("test-output/logs/fda_profile_icon_missing.html"),
                    driver.getPageSource());
                LoggerUtility.info("Diagnostic: dumped page source to "
                    + "test-output/logs/fda_profile_icon_missing.html");
            } catch (Exception dumpFailure) {
                LoggerUtility.error("Diagnostic page-source dump failed: " + dumpFailure.getMessage());
            }
            throw e;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }

    public void clickLoginLink() {
        LoggerUtility.info("Clicking Iniciar sesion link");
        jsClick(LOGIN_LINK);
    }

    public void enterSearchQuery(String query) {
        LoggerUtility.info("Entering search query: " + query);
        type(SEARCH_FIELD, query);
    }

    public void pressSearchEnter() {
        LoggerUtility.info("Pressing Enter to execute search");
        pressEnter(SEARCH_FIELD);
    }

    public void clickCartIcon() {
        LoggerUtility.info("Clicking cart icon");
        click(CART_ICON);
    }

    public void clickMyOrdersLink() {
        LoggerUtility.info("Clicking Mis pedidos link");
        jsClick(MY_ORDERS_LINK);
    }

    public boolean isLoggedIn() {
        // After successful login, URL leaves the login page
        return !driver.getCurrentUrl().contains("customer/account/login");
    }

    public void logout() {
        LoggerUtility.info("Attempting FDA logout");
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
        try {
            jsClick(PROFILE_ICON);
            java.util.List<org.openqa.selenium.WebElement> logoutLinks = driver.findElements(LOGOUT_LINK);
            if (!logoutLinks.isEmpty()) {
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
                jsClick(LOGOUT_LINK);
                LoggerUtility.info("FDA logout successful");
            } else {
                LoggerUtility.info("Logout link not found — user may already be logged out");
            }
        } catch (Exception e) {
            LoggerUtility.warn("FDA logout issue: " + e.getMessage() + " — continuing");
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMinutes(2));
        }
    }
}