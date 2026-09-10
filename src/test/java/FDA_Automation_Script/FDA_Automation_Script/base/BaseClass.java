package FDA_Automation_Script.FDA_Automation_Script.base;

import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDAHomePage;
import FDA_Automation_Script.FDA_Automation_Script.pages.fda.FDALoginPage;
import FDA_Automation_Script.FDA_Automation_Script.pages.mirakl.MiraklLoginPage;
import FDA_Automation_Script.FDA_Automation_Script.utils.ConfigReader;
import FDA_Automation_Script.FDA_Automation_Script.utils.DriverFactory;
import FDA_Automation_Script.FDA_Automation_Script.utils.LoggerUtility;
import FDA_Automation_Script.FDA_Automation_Script.utils.WaitUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeSuite;

public class BaseClass {

    public static WebDriver driver;
    public static String fdaTabHandle;
    public static String kiboTabHandle;
    public static String miraklTabHandle;

    protected static final ConfigReader config = ConfigReader.getInstance();

    @BeforeSuite
    public void setupSuite() {
        // 1. Launch browser — FDA tab is the initial window
        driver = DriverFactory.createDriver(false);
        fdaTabHandle = driver.getWindowHandle();
        LoggerUtility.info("Browser launched. FDA tab handle: " + fdaTabHandle);

        // 2. Load the FDA storefront in the FDA tab first, so it never sits blank
        // (login itself is still deferred to @BeforeGroups("FBO")/@BeforeGroups("FBS") — see below)
        driver.get(config.getFdaUrl());
        LoggerUtility.info("@BeforeSuite: FDA tab navigated to " + config.getFdaUrl());

        // 3. Open Mirakl in second tab and log in once for the entire suite
        LoggerUtility.info("@BeforeSuite: Opening Mirakl in second tab");
        miraklTabHandle = DriverFactory.openNewTab();
        driver.get(config.getMiraklUrl());
        MiraklLoginPage miraklLoginPage = new MiraklLoginPage(driver);
        miraklLoginPage.login(config.getMiraklUsername(), config.getMiraklPassword());
        LoggerUtility.info("@BeforeSuite: Mirakl login complete. Tab handle: " + miraklTabHandle);

        // 4. Leave focus on FDA tab — every test starts on the FDA tab
        switchToFDATab();
        LoggerUtility.info("@BeforeSuite: Setup complete. Browser has FDA (tab 1) and Mirakl (tab 2).");
    }

    // --- FBO group: one FDA login for the whole group, one logout after the last FBO test ---

    @BeforeGroups("FBO")
    public void loginForFBO() {
        LoggerUtility.info("@BeforeGroups(FBO): Logging in to FDA as " + config.getFboUsername());
        switchToFDATab();
        FDAHomePage fdaHomePage = new FDAHomePage(driver);
        FDALoginPage fdaLoginPage = new FDALoginPage(driver);
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickLoginLink();
        fdaLoginPage.login(config.getFboUsername(), config.getFboPassword());
        LoggerUtility.info("@BeforeGroups(FBO): FDA login complete — session shared by all FBO tests");
    }

    @AfterGroups("FBO")
    public void logoutAfterFBO() {
        LoggerUtility.info("@AfterGroups(FBO): Logging out of FDA after all FBO tests");
        try {
            switchToFDATab();
            FDAHomePage fdaHomePage = new FDAHomePage(driver);
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            LoggerUtility.info("@AfterGroups(FBO): FDA logout complete");
        } catch (Exception e) {
            LoggerUtility.warn("@AfterGroups(FBO): logout warning: " + e.getMessage());
        }
    }

    // --- FBS group: one FDA login for the whole group, one logout after the last FBS test ---

    @BeforeGroups("FBS")
    public void loginForFBS() {
        LoggerUtility.info("@BeforeGroups(FBS): Logging in to FDA as " + config.getFbsUsername());
        switchToFDATab();
        FDAHomePage fdaHomePage = new FDAHomePage(driver);
        FDALoginPage fdaLoginPage = new FDALoginPage(driver);
        fdaHomePage.navigateTo(config.getFdaUrl());
        fdaHomePage.clickProfileIcon();
        fdaHomePage.clickLoginLink();
        fdaLoginPage.login(config.getFbsUsername(), config.getFbsPassword());
        LoggerUtility.info("@BeforeGroups(FBS): FDA login complete — session shared by all FBS tests");
    }

    @AfterGroups("FBS")
    public void logoutAfterFBS() {
        LoggerUtility.info("@AfterGroups(FBS): Logging out of FDA after all FBS tests");
        try {
            switchToFDATab();
            FDAHomePage fdaHomePage = new FDAHomePage(driver);
            fdaHomePage.navigateTo(config.getFdaUrl());
            fdaHomePage.logout();
            LoggerUtility.info("@AfterGroups(FBS): FDA logout complete");
        } catch (Exception e) {
            LoggerUtility.warn("@AfterGroups(FBS): logout warning: " + e.getMessage());
        }
    }

    @AfterMethod
    public void navigateToHomePage() {
        try {
            if (fdaTabHandle != null) {
                switchToFDATab();
                driver.get(config.getFdaUrl());
                LoggerUtility.info("@AfterMethod: FDA navigated back to home page");
            }
            if (miraklTabHandle != null) {
                switchToMiraklTab();
                driver.get(config.getMiraklUrl());
                LoggerUtility.info("@AfterMethod: Mirakl navigated back to home dashboard");
            }
            // Leave focus on FDA tab for the next test
            switchToFDATab();
        } catch (Exception e) {
            LoggerUtility.warn("@AfterMethod navigation warning: " + e.getMessage());
        }
    }

    @AfterSuite
    public void tearDownSuite() {
        LoggerUtility.info("Suite complete. Closing browser.");
        DriverFactory.quitDriver();
    }

    // --- Tab switch helpers ---

    protected void switchToFDATab() {
        if (fdaTabHandle != null) {
            DriverFactory.switchToTab(fdaTabHandle);
            LoggerUtility.info("Switched to FDA tab");
        }
    }

    protected void switchToKiboTab() {
        if (kiboTabHandle != null) {
            DriverFactory.switchToTab(kiboTabHandle);
            LoggerUtility.info("Switched to Kibo OMS tab");
        }
    }

    protected void switchToMiraklTab() {
        if (miraklTabHandle != null) {
            DriverFactory.switchToTab(miraklTabHandle);
            LoggerUtility.info("Switched to Mirakl tab");
        }
    }

    // --- Refresh + fluent wait ---

    protected void refreshAndWait(By locator) {
        LoggerUtility.info("Refreshing page...");
        driver.navigate().refresh();
        WaitUtility.fluentWait(driver, locator);
        LoggerUtility.info("Page refresh complete. Element visible.");
    }
}
