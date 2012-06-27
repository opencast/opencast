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

import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.cli.RemoteControlLauncher;
import org.openqa.selenium.server.htmlrunner.HTMLLauncher;

import java.io.File;

/**
 * Overrides {@link SeleniumServer#runHtmlTestSuite} to avoid calls to System.exit().
 */
public class NonExitingSeleniumServer extends SeleniumServer {
  /** The selenium test suite */
  protected File in;

  /** The selenium report */
  protected File out;

  /** The browser to test */
  protected String browser;

  /**
   * Constructs a non exiting selenium server.
   * 
   * @param in
   *          the selenium test suite
   * @param the
   *          selenium report
   * @throws Exception
   *           if the server can not be created
   */
  public NonExitingSeleniumServer(File in, File out, String browser) throws Exception {
    super(getConfig());
    this.in = in;
    this.out = out;
    this.browser = browser;
  }

  static RemoteControlConfiguration getConfig() {
    RemoteControlConfiguration config = new RemoteControlConfiguration();
    config.setHTMLSuite(true);
    config.setDebugMode(true);
    config.setTimeoutInSeconds(10);
    config.setProfilesLocation(new File(System.getProperty("java.io.tmpdir")));
    return config;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.openqa.selenium.server.SeleniumServer#runHtmlSuite()
   */
  @Override
  protected void runHtmlSuite() {
    final String result;
    try {
      if (!in.exists()) {
        RemoteControlLauncher.usage("Can't find HTML Suite file:" + in.getAbsolutePath());
      }
      addNewStaticContent(in.getParentFile());
      String startURL = Main.BASE_URL;
      HTMLLauncher launcher = new HTMLLauncher(this);
      out.createNewFile();

      if (!out.canWrite()) {
        RemoteControlLauncher.usage("can't write to result file " + out.getAbsolutePath());
      }

      result = launcher.runHTMLSuite(browser, startURL, in, out, 100, false);

      if (!"PASSED".equals(result)) {
        System.err.println("Tests failed, see result file for details: " + out.getAbsolutePath());
      }
    } catch (Exception e) {
      System.err.println("HTML suite exception seen:");
      e.printStackTrace();
    }
  }
}
