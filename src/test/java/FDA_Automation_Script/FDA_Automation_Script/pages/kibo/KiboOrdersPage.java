package FDA_Automation_Script.FDA_Automation_Script.pages.kibo;

import FDA_Automation_Script.FDA_Automation_Script.pages.BasePage;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class KiboOrdersPage extends BasePage {

    // TODO: Verify locators against actual Kibo OMS navigation DOM
	private static final By HAMBURGER_ICON = By.xpath("//i[@class='fa fa-bars hamburgerMenu']");
    private static final By ORDERS_MENU    = By.xpath("//a[@class='menu-label'][normalize-space()='Orders']");
    private static final By ORDERS_LINK    = By.xpath("////a[@class='ng-star-inserted'][normalize-space()='Orders']");
    private static final By SEARCH_FIELD   = By.xpath("//input[@placeholder='Search' and @type='text']");
    private static final By ORDER_STATUS   = By.xpath("//span[@class='x-column-content-pill x-column-content-pill-semi-dark']");

    public KiboOrdersPage(WebDriver driver) {
        super(driver);
    }

    public void clickHamburger() {
        LoggerUtility.info("Clicking hamburger menu icon");
        click(HAMBURGER_ICON);
    }

    public void expandOrdersMenu() {
        LoggerUtility.info("Expanding Orders menu");
        click(ORDERS_MENU);
    }

    public void clickOrdersLink() {
        LoggerUtility.info("Clicking Orders link");
        click(ORDERS_LINK);
    }

    public void searchOrder(String searchTerm) {
        LoggerUtility.info("Searching Kibo for order: " + searchTerm);
        type(SEARCH_FIELD, searchTerm);
        pressEnter(SEARCH_FIELD);
    }

    public String getFirstOrderStatus() {
        String status = getText(ORDER_STATUS);
        LoggerUtility.info("Kibo order status: " + status);
        return status;
    }
}
