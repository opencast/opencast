/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.remotetest.ui;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.Parallelized;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Tests for the presence of a welcome page. */
@RunWith(Parallelized.class)
public class LoginTest {

  /** The selenium client */
  private Selenium selenium;

  /** The browser being used in this test execution. This is typically set by a parameterized junit runner. */
  private String browser;

  /** The Matterhorn login page URI */
  private String loginPage = "/login.html";

  /** The Matterhorn welcome page URI */
  private String welcomePage = "/welcome.html";

  /** The login form field for username */
  private String usernameField = "j_username";

  /** The login form field for password */
  private String passwordField = "j_password";

  /** The login form submit button */
  private String loginButton = "submit";

  /**
   * Constructs a new test instance. This is typically called by the junit parameterized runner.
   * 
   * @param browser
   *          the browser to use for testing
   */
  public LoginTest(String browser) {
    super();
    this.browser = browser;
  }

  @Parameters
  public static Collection<String[]> browsersStrings() throws Exception {
    List<String[]> list = new ArrayList<String[]>();
    for (String browser : Main.BROWSERS) {
      list.add(new String[] { browser });
    }
    return list;
  }

  @Before
  public void setUp() throws Exception {
    selenium = new DefaultSelenium("localhost", 4444, this.browser, Main.BASE_URL);
    selenium.start();
    selenium.setTimeout("10000");
    selenium.windowMaximize();
    login();
  }

  protected void login() {
    // try to open the welcome page
    selenium.open(welcomePage);
    selenium.waitForPageToLoad("30000");
    
    // if we were redirected to the login page, we need to log in
    if(selenium.getLocation().contains(loginPage)) {
      selenium.type(usernameField, Main.USERNAME);
      selenium.type(passwordField, Main.PASSWORD);
      selenium.click(loginButton);

      // now we should be on the welcome page, the original page we requested
      selenium.waitForPageToLoad("30000");
      Assert.assertTrue(selenium.getLocation().endsWith(welcomePage));
    }
  }

  @Test
  public void testButtonsExist() throws Exception {
    selenium.open(welcomePage);
    selenium.waitForPageToLoad("30000");
    // ensure we haven't been redirected
    Assert.assertTrue(selenium.getLocation().contains(welcomePage));
    
    // ensure the page has the elements we expect
    Assert.assertTrue(selenium.isElementPresent("adminlink"));
    Assert.assertTrue(selenium.isElementPresent("engagelink"));
  }

  @After
  public void tearDown() throws Exception {
    selenium.stop();
  }

}
