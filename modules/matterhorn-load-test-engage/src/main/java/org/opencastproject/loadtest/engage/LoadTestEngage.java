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

package org.opencastproject.loadtest.engage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Random;

/** Runs an individual web browser to test an engage server. **/
public class LoadTestEngage implements Runnable {
  /* The logger */
  private static final Logger logger = LoggerFactory.getLogger(LoadTestEngage.class);
  /* A random number generator used to pick episodes at random. */
  private Random generator = new Random();
  /* The URL location engage server that is to be load tested.*/
  private String engageServerUrl = "http://localhost:8080";
  /* The list of episode ids we can play on the engage server. */
  private LinkedList<String> episodeList = new LinkedList<String>();
  /* The WebDriver used to control the browser to open videos and authenticate. */
  private WebDriver driver = null;
  /* The amount of time to watch a video before moving onto the next one. */
  private int watchTime = 300;
  /* The number of milliseconds in a single second. */
  public static int MILLISECONDS_IN_SECONDS = 1000;
  /* The settings to use to interact with the gui such as user/pass and the names of those fields. */
  private GuiSettings guiSettings = new GuiSettings();
  /* The name of the current load test thread. */
  private String name = "";

  /**
   * Create a new load test for an engage server.
   *
   * @param name
   *          The name of this instance for logging purposes.
   * @param engageServerUrl
   *          The location of the engage server to test.
   * @param episodeList
   *          The list of ids of episodes that we can play in the engage server.
   * @param watchTime
   *          The amount of time to watch a video before moving onto the next one.
   * @param guiSettings
   *          The settings to interact with the gui in the browser such as user/pass and the ids of those fields.
   * @param browserToUse
   *          The browser to use in this test.
   */
  public LoadTestEngage(String name, String engageServerUrl, LinkedList<String> episodeList, int watchTime,
          GuiSettings guiSettings, Main.BrowserToUse browserToUse) {
    this.name = name;
    this.engageServerUrl = engageServerUrl;
    this.episodeList = episodeList;
    this.watchTime = watchTime;
    this.guiSettings = guiSettings;

    if (browserToUse == Main.BrowserToUse.Chrome) {
      driver = new ChromeDriver();
    }
    else if (browserToUse == Main.BrowserToUse.Safari){
      driver = new SafariDriver();
    }
    else if (browserToUse == Main.BrowserToUse.IE) {
      driver = new InternetExplorerDriver();
    }
    else {
      driver = new FirefoxDriver();
    }
  }

  /**
   * Runs the load test in its own separate thread.
   * @see java.lang.Runnable#run()
   **/
  @Override
  public void run() {
    while (true) {
      playNewStream();
      try {
        // Generate a random amount of time to watch between 1/2 the watch time and all of the watch time.
        int randomWatchTime = generator.nextInt(watchTime / 2) + watchTime / 2;
        logger.info(name + " is watching the current episode for " + randomWatchTime + " seconds.");
        Thread.sleep(randomWatchTime * MILLISECONDS_IN_SECONDS);
      } catch (InterruptedException e) {
        logger.error("There was an exception while sleeping before the next switch: " ,e);
      }
    }
  }

  /**
   * If opening a page results in a log in page this will authenticate against it.
   */
  private void authenticate() {
    logger.debug(name + "-Login title is: " + driver.getTitle());
    WebElement username = driver.findElement(By.name(guiSettings.getUsernameFieldName()));
    username.sendKeys(guiSettings.getUsername());
    WebElement password = driver.findElement(By.name(guiSettings.getPasswordFieldName()));
    password.sendKeys(guiSettings.getPassword());
    password.submit();
  }

  /**
   * Start playing a new episode.
   */
  public void playNewStream(){
    if(episodeList == null || episodeList.size() <= 0) {
      return;
    }
    String episodeUrl = engageServerUrl + "/engage/ui/watch.html?id=" + episodeList.get(generator.nextInt(episodeList.size()));
    driver.get(episodeUrl);
    if (!driver.getCurrentUrl().equalsIgnoreCase(episodeUrl)) {
      authenticate();
    }
    logger.info(name + " - Playing episode " + episodeUrl);
    logger.debug("Episode Page title is: " + driver.getTitle());

    // Play the episode.
    WebElement play = (new WebDriverWait(driver, 60)).until(new ExpectedCondition<WebElement>() {
      @Override
      public WebElement apply(WebDriver driver) {
        WebElement webElement = driver.findElement(By.id("oc_btn-play-pause"));
        if (webElement != null && webElement.isDisplayed()) {
          return webElement;
        }
        return null;
      }
    });

    play.click();

    // Advance the play using fast forward.
    WebElement fastForward = (new WebDriverWait(driver, 10000)).until(new ExpectedCondition<WebElement>() {
      @Override
      public WebElement apply(WebDriver driver) {
        WebElement webElement = driver.findElement(By.id("oc_btn-fast-forward"));
        if (webElement != null && webElement.isDisplayed()) {
          return webElement;
        }
        return null;
      }
    });


    for (int i = 0; i < 5; i++) {
      fastForward.click();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.error("There was an exception while fastforwarding:", e);
      }
    }
  }
}
