/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.remotetest;

import org.opencastproject.remotetest.ui.NonExitingSeleniumServer;
import org.opencastproject.remotetest.ui.SeleniumTestSuite;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.WorkflowUtils;

import junit.framework.Assert;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.DefaultFilter;
import org.cyberneko.html.filters.Identity;
import org.cyberneko.html.filters.Writer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.openqa.selenium.server.SeleniumServer;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Runs JUnit and/or Selenium test suites against a Matterhorn server and/or capture agent.
 */
public class Main {

  public static String BASE_URL = "http://localhost:8080";
  public static String USERNAME = "matterhorn_system_account";
  public static String PASSWORD = "CHANGE_ME";
  public static Set<String> BROWSERS = new HashSet<String>();
  public static final String FIREFOX = "*firefox";
  public static final String SAFARI = "*safari";
  public static final String CHROME = "*googlechrome";

  /** Whether the selenium data has been loaded */
  private static boolean SELENESE_DATA_LOADED = false;

  public static final TrustedHttpClient getClient() {
    return new TrustedHttpClient(USERNAME, PASSWORD);
  }

  public static final void returnClient(TrustedHttpClient client) {
    if (client != null) {
      client.shutdown();
    }
  }

  public static final String getBaseUrl() {
    return BASE_URL;
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption(new Option("help", false, "print this message"));
    options.addOption(new Option("withperf", false, "run the performance tests"));
    options.addOption(new Option("withserver", false, "run the tests for the server side components"));
    options.addOption(new Option("withff", false, "run the selenium user interface tests with the firefox browser"));
    options.addOption(new Option("withchrome", false, "run the selenium user interface tests with the chrome browser"));
    options.addOption(new Option("withsafari", false, "run the selenium user interface tests with the safari browser"));
    options.addOption(new Option("url", true, "run tests against the Matterhorn installation at this URL"));
    options.addOption(new Option("username", true, "the username to use when accessing the Matterhorn installation"));
    options.addOption(new Option("password", true, "the password to use when accessing the Matterhorn installation"));

    // create the parser
    CommandLineParser parser = new PosixParser();
    List<Class<?>> testClasses = new ArrayList<Class<?>>();
    CommandLine line = null;
    try {
      line = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println("Parsing commandline failed: " + e.getMessage());
      System.exit(1);
    }

    if (line.hasOption("url")) {
      BASE_URL = line.getOptionValue("url");
    }
    if (line.hasOption("username")) {
      USERNAME = line.getOptionValue("username");
    }
    if (line.hasOption("password")) {
      PASSWORD = line.getOptionValue("password");
    }

    if (line.hasOption("help")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar matterhorn-test-harness-<version>-jar-with-dependencies.jar>", options);
      System.exit(0);
    }
    // should we run the server-side tests
    if (line.hasOption("withserver")) {
      System.out.println("Running with the 'server' test suite enabled");
      testClasses.add(ServerTests.class);
      if (line.hasOption("withperf")) {
        System.out.println("Running with the server performance test suite enabled");
        testClasses.add(ServerPerformanceTests.class);
      }
    }

    SeleniumServer seleniumServer = null;
    if (line.hasOption("withff") || line.hasOption("withchrome") || line.hasOption("withsafari")) {

      // Add the test suite
      testClasses.add(SeleniumTestSuite.class);

      // Assemble the browsers that the user wants to use in the tests
      if (line.hasOption("withff")) {
        runSeleneseTests(FIREFOX);
        BROWSERS.add(FIREFOX);
      }
      if (line.hasOption("withchrome")) {
        runSeleneseTests(CHROME);
        BROWSERS.add(CHROME);
      }
      if (line.hasOption("withsafari")) {
        runSeleneseTests(SAFARI);
        BROWSERS.add(SAFARI);
      }

      // Start the selenium server
      seleniumServer = new SeleniumServer();
      seleniumServer.start();
    }
    // if we don't have any test classes, add the server (not performance) tests... just so we have *something* to do
    if (testClasses.size() == 0) {
      System.out.println("No test suites specified... running server (not including performance) tests");
      testClasses.add(ServerTests.class);
    }

    // run the tests
    System.out.println("Beginning matterhorn test suite on " + BASE_URL);
    Result result = JUnitCore.runClasses(testClasses.toArray(new Class<?>[testClasses.size()]));

    if (seleniumServer != null) {
      seleniumServer.stop();
    }

    // print the results
    System.out.println(result.getRunCount() + " tests run, " + result.getIgnoreCount() + " tests ignored, "
            + result.getFailureCount() + " tests failed.  Total time = " + result.getRunTime() + "ms");
    if (result.getFailureCount() > 0) {
      for (Failure failure : result.getFailures()) {
        System.out.println(failure.getTrace());
      }
      System.exit(1);
    }
  }

  private static void loadSeleneseData() throws Exception {
    System.out.println("Loading sample data for selenium HTML tests");
    // get the zipped mediapackage from the classpath
    byte[] bytesToPost = IOUtils.toByteArray(Main.class.getResourceAsStream("/ingest.zip"));

    // post it to the ingest service
    HttpPost post = new HttpPost(BASE_URL + "/ingest/addZippedMediaPackage");
    post.setEntity(new ByteArrayEntity(bytesToPost));
    TrustedHttpClient client = getClient();
    HttpResponse response = client.execute(post);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    String workflowXml = EntityUtils.toString(response.getEntity());

    // Poll the workflow service to ensure this recording processes successfully
    String workflowId = WorkflowUtils.getWorkflowInstanceId(workflowXml);
    while(true) {
      if(WorkflowUtils.isWorkflowInState(workflowId, "SUCCEEDED")) {
        SELENESE_DATA_LOADED = true;
        break;
      }
      if(WorkflowUtils.isWorkflowInState(workflowId, "FAILED")) {
        Assert.fail("Unable to load sample data for selenese tests.");
      }
      Thread.sleep(5000);
      System.out.println("Waiting for sample data to process");
    }
    returnClient(client);
  }

  /**
   * Runs the selenese test suite in src/main/resources/selenium/suite.html
   *
   * @throws Exception
   *           if the selenese tests fail to run
   */
  private static void runSeleneseTests(String browser) throws Exception {
    if(!SELENESE_DATA_LOADED) {
      loadSeleneseData();
    }
    // Build the test suite and copy it, along with its associated files, to the temp directory
    String time = Long.toString(System.currentTimeMillis());
    File temp = new File(System.getProperty("java.io.tmpdir"), "selenium" + time);
    FileUtils.forceMkdir(temp);
    List<String> testNames = new ArrayList<String>();
    String[] relativePaths = getResourceListing("selenium");
    for (int i = 0; i < relativePaths.length; i++) {
      String relativePath = relativePaths[i];
      String filename = FilenameUtils.getName(relativePath.toString());
      File outFile = new File(temp, filename);
      // Copy the selenium test, prepending the login steps
      InputStream is = addLogin(Main.class.getResource("/" + relativePath).openStream());
      OutputStream os = new FileOutputStream(outFile);
      IOUtils.copy(is, os);
      IOUtils.closeQuietly(is);
      IOUtils.closeQuietly(os);
      testNames.add(filename);
    }

    // Construct the test suite from the files we've copied
    File testSuite = new File(temp, "suite.html");
    StringBuilder suiteHtml = new StringBuilder();
    suiteHtml.append("<html><head><title>Matterhorn Selenium Integration Test Suite</title>");
    suiteHtml.append("</head><body><table><tr><td><b>Suite Of Tests</b></td></tr>");
    for (String testName : testNames) {
      suiteHtml.append("<tr><td><a href=\"./");
      suiteHtml.append(testName);
      suiteHtml.append("\">");
      suiteHtml.append(testName);
      suiteHtml.append("</a></td></tr>");
    }
    suiteHtml.append("</table></body></html>");
    FileUtils.writeStringToFile(testSuite, suiteHtml.toString());

    File target = new File("target");
    if (!target.isDirectory()) {
      FileUtils.forceMkdir(target);
    }
    File report = new File(target, "selenium_results_" + browser.replaceAll("\\W", "") + "_" + time + ".html");

    // Run the selenese tests
    System.out.println("Beginning selenese tests for the '" + browser + "' browser on " + Main.BASE_URL);
    SeleniumServer seleniumServer = new NonExitingSeleniumServer(testSuite, report, browser);
    seleniumServer.boot();
    seleniumServer.stop();
    System.out.println("Finished selenese tests for '" + browser + "'.  Results are available at " + report);

    // Clean up
    // FileUtils.forceDelete(temp);
  }

  /** SAX parser for adding the Matterhorn login steps to a selenium test */
  static class LoginLogoutHtmlHandler extends DefaultFilter {

    /** The html parser configuration */
    HTMLConfiguration config = null;

    /** Whether the login element has been added */
    boolean loginAdded = false;

    /** Whether the logout element has been added */
    boolean logoutAdded = false;

    /**
     * Constructs the sax parser that injects login html to a selenium test
     *
     * @param config
     *          the html cofiguration
     */
    public LoginLogoutHtmlHandler(HTMLConfiguration config) {
      this.config = config;
    }

    /** The html to insert into the selenium test */
    protected static final String getLoginHtml() {
      return "<tr><td>open</td><td>/login.html</td><td></td></tr><tr><td>type</td><td>j_username</td><td>"
              + Main.USERNAME + "</td></tr>" + "<tr><td>type</td><td>j_password</td><td>" + Main.PASSWORD
              + "</td></tr>" + "<tr><td>clickAndWait</td><td>submit</td><td></td></tr>";
    }

    /**
     * {@inheritDoc}
     *
     * @see org.cyberneko.html.filters.DefaultFilter#startElement(org.apache.xerces.xni.QName,
     *      org.apache.xerces.xni.XMLAttributes, org.apache.xerces.xni.Augmentations)
     */
    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
      super.startElement(element, attributes, augs);
      if ("tbody".equalsIgnoreCase(element.rawname) && !loginAdded) {
        String loginHtml = getLoginHtml();
        XMLInputSource source = new XMLInputSource("loginHtml", "loginHtml", null, new StringReader(loginHtml), "UTF-8");
        config.pushInputSource(source);
        loginAdded = true;
      }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.cyberneko.html.filters.DefaultFilter#endElement(org.apache.xerces.xni.QName,
     *      org.apache.xerces.xni.Augmentations)
     */
    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
      if ("tbody".equalsIgnoreCase(element.rawname) && !logoutAdded) {
        String logoutHtml = "<tr><td>open</td><td>/j_spring_security_logout</td><td></td></tr></tbody>";
        XMLInputSource source = new XMLInputSource("logoutHtml", "logoutHtml", null, new StringReader(logoutHtml),
                "UTF-8");
        config.pushInputSource(source);
        logoutAdded = true;
      } else {
        super.endElement(element, augs);
      }
    }
  }

  /**
   * Adds the login steps to a selenium test
   *
   * @param the
   *          input stream containing the test
   * @return the input stream containing the test augmented with the login instructions
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  private static InputStream addLogin(InputStream is) throws Exception {
    HTMLConfiguration parser = new HTMLConfiguration();
    parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
    StringWriter stringWriter = new StringWriter();
    Writer writer = new Writer(stringWriter, "UTF-8");
    XMLDocumentFilter[] filters = { new LoginLogoutHtmlHandler(parser), new Identity(), writer };
    parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
    parser.parse(new XMLInputSource(null, null, null, is, null));
    is.close();
    return IOUtils.toInputStream(stringWriter.toString());
  }

  /**
   * List directory contents for a resource folder.
   *
   * @param path
   *          The classpath to scan for resources
   * @return URLs for each item
   * @throws Exception
   */
  private static String[] getResourceListing(final String path) throws Exception {
    Reflections r = new Reflections(path, new ResourcesScanner());
    return r.getResources(Pattern.compile(".*\\.html")).toArray(new String[0]);
  }
}
