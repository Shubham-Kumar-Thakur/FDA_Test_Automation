package com.fda.automation.tests;

import com.fda.automation.base.BaseTest;
import com.fda.automation.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test(groups = {"smoke"}, description = "Valid credentials redirect to secure area")
    public void testSuccessfulLogin() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.open();
        loginPage.login("tomsmith", "SuperSecretPassword!");
        Assert.assertTrue(loginPage.isLoginSuccessful(),
                "Expected redirect to /secure after valid login");
    }

    @Test(groups = {"smoke"}, description = "Invalid password shows error flash message")
    public void testInvalidPasswordShowsError() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.open();
        loginPage.login("tomsmith", "wrongpassword");
        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Expected error message for invalid credentials");
    }

    @Test(groups = {"regression"}, description = "Empty credentials show error flash message")
    public void testEmptyCredentialsShowsError() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.open();
        loginPage.login("", "");
        String flash = loginPage.getFlashMessage();
        Assert.assertFalse(flash.isEmpty(), "Expected a flash message for empty credentials");
        sdfghjk
        
    }
}
