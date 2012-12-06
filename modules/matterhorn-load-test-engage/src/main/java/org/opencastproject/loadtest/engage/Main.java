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

import org.opencastproject.loadtest.engage.util.TrustedHttpClient;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Runs a load test against an engage or all-in-one server by watching random videos.
 */
public class Main {
  private static final String SEARCH_URL = "/search/episode.xml?limit=1000&offset=0";

  /** Keys **/
  private static final String BROWSER_NUMBER_KEY = "browsernumber";
  private static final String DIGEST_PASSWORD_KEY = "digestpassword";
  private static final String DIGEST_USERNAME_KEY = "digestusername";
  private static final String ENGAGE_URL_KEY = "url";
  private static final String GUI_PASSWORD_FIELDNAME_KEY = "guipasswordfieldname";
  private static final String GUI_USERNAME_FIELDNAME_KEY = "guiusernamefieldname";
  private static final String GUI_PASSWORD_KEY = "guipassword";
  private static final String GUI_USERNAME_KEY = "guiusername";
  private static final String HELP_KEY = "help";
  private static final String WATCH_TIME_KEY = "watchtime";
  private static final String WITH_CHROME_KEY = "withchrome";
  private static final String WITH_FIREFOX_KEY = "withff";
  private static final String WITH_IE_KEY = "withie";
  private static final String WITH_SAFARI_KEY = "withsafari";

  /** Variables **/
  /* The logger */
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  
  /* The number browsers that will be spawned to load test the server. */
  public static int numberOfBrowsers = 1;
  /* The amount of time to watch a given video before moving onto the next one. */
  public static int timeToWatchVideo = 300;
  /* The URL of the server that will be load tested. Should be an all-in-one core or the engage server. */
  public static String engageServerURL = "http://localhost:8080";
  /* The username to use to make rest endpoint requests. */
  public static String digestUsername = "matterhorn_system_account";
  /* The password to use to make rest endpoint requests. */
  public static String digestPassword = "CHANGE_ME";

  /* The collection of all browsers that the user can choose from. */
  public enum BrowserToUse {
    Chrome, Firefox, IE, Safari 
  };

  /** Create a new trusted client to make rest endpoint requests. **/
  public static final TrustedHttpClient getClient() {
    return new TrustedHttpClient(digestUsername, digestPassword);
  }

  /** Shut down the client after rest endpoint requests are finished. **/
  public static final void returnClient(TrustedHttpClient client) {
    if (client != null) {
      client.shutdown();
    }
  }

  /** Returns the URL of the engage server that will be load tested. **/
  public static final String getEngageServerUrl() {
    return engageServerURL;
  }

  /** Runs the load testing against an engage server. **/
  public static void main(String[] args) throws Exception {
    // Create the default settings for the GUI interaction such as user/pass and field names.
    GuiSettings guiSettings = new GuiSettings();
    // Set the default browser to use as Firefox.
    BrowserToUse browserToUse = BrowserToUse.Firefox;
    // Create command line options.
    Options options = createOptions();
    // Parse the command line options.
    CommandLineParser parser = new PosixParser();
    CommandLine line = null;
    try {
      line = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println("Parsing commandline failed: " + e.getMessage());
      System.exit(1);
    }

    if (line.hasOption(ENGAGE_URL_KEY)) {
      engageServerURL = line.getOptionValue(ENGAGE_URL_KEY);
    }
    if (line.hasOption(DIGEST_USERNAME_KEY)) {
      digestUsername = line.getOptionValue(DIGEST_USERNAME_KEY);
    }
    if (line.hasOption(DIGEST_PASSWORD_KEY)) {
      digestPassword = line.getOptionValue(DIGEST_PASSWORD_KEY);
    }

    // Get settings for interacting with the gui.
    if (line.hasOption(GUI_USERNAME_KEY)) {
      guiSettings.setUsername(line.getOptionValue(GUI_USERNAME_KEY));
    }
    if (line.hasOption(GUI_PASSWORD_KEY)) {
      guiSettings.setPassword(line.getOptionValue(GUI_PASSWORD_KEY));
    }
    if (line.hasOption(GUI_USERNAME_FIELDNAME_KEY)) {
      guiSettings.setUsernameFieldName(line.getOptionValue(GUI_USERNAME_FIELDNAME_KEY));
    }
    if (line.hasOption(GUI_PASSWORD_FIELDNAME_KEY)) {
      guiSettings.setPasswordFieldName(line.getOptionValue(GUI_PASSWORD_FIELDNAME_KEY));
    }

    if (line.hasOption(BROWSER_NUMBER_KEY)) {
      numberOfBrowsers = Integer.parseInt(line.getOptionValue(BROWSER_NUMBER_KEY));
    }
    if (line.hasOption(WATCH_TIME_KEY)) {
      timeToWatchVideo = Integer.parseInt(line.getOptionValue(WATCH_TIME_KEY));
    }

    if (line.hasOption(WITH_CHROME_KEY)) {
      browserToUse = BrowserToUse.Chrome;
    }
    if (line.hasOption(WITH_IE_KEY)) {
      browserToUse = BrowserToUse.IE;
    }
    if (line.hasOption(WITH_SAFARI_KEY)) {
      browserToUse = BrowserToUse.Safari;
    }
    if (line.hasOption(WITH_FIREFOX_KEY)) {
      browserToUse = BrowserToUse.Firefox;
    }

    if (line.hasOption(HELP_KEY)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar matterhorn-load-test-engage-<version>-jar-with-dependencies.jar>", options);
      System.exit(0);
    }

    LinkedList<String> episodeList = getListOfEpisodes();

    if (episodeList == null || episodeList.size() <= 0) {
      logger.error("You need at least one episode in the engage player to run this load test. ");
      System.exit(0);
    }

    // Create browsers to run test.
    LoadTestEngage loadTestEngage = null;
    Thread thread = null;
    for (int i = 0; i < numberOfBrowsers; i++) {
      loadTestEngage = new LoadTestEngage("Browser " + i, engageServerURL, episodeList, timeToWatchVideo, guiSettings,
              browserToUse);
      thread = new Thread(loadTestEngage);
      thread.start();
    }
  }

  /** Create the command line options and help feedback for running the load tests. **/
  private static Options createOptions() {
    Options options = new Options();
    options.addOption(new Option(HELP_KEY, false, "Print this message"));
    // Add browser options.
    options.addOption(new Option(WITH_FIREFOX_KEY, false, "Run the load test with Firefox browsers. Default:true"));
    options.addOption(new Option(WITH_CHROME_KEY, false, "Run the load test with Chrome browsers. Default:false"));
    options.addOption(new Option(WITH_IE_KEY, false, "Run the load test with the Internet Explorer browsers. Default:false"));
    options.addOption(new Option(WITH_SAFARI_KEY, false, "Run the load test with Safari browsers. Default:false"));
    // Server to hit
    options.addOption(new Option(ENGAGE_URL_KEY, true,
            "Run the load tests against the engage server at this url. \nDefault:" + engageServerURL));
    // Authentication to use to get episode list.
    options.addOption(new Option(DIGEST_USERNAME_KEY, true,
            "The username to use when accessing the Matterhorn services (org.opencastproject.security.digest.user"
                    + " in config.properties). \nDefault:" + digestUsername));
    options.addOption(new Option(DIGEST_PASSWORD_KEY, true,
            "The password to use when accessing the Matterhorn services (org.opencastproject.security.digest.pass"
                    + "in config.properties). \nDefault:" + digestPassword));
    // Authentication to handle gui.
    options.addOption(new Option(GUI_USERNAME_KEY, true,
            "The username to use when accessing the Matterhorn web pages (org.opencastproject.security.demo.admin.user"
                    + " in config.properties). \nDefault:" + GuiSettings.DEFAULT_GUI_USERNAME));
    options.addOption(new Option(GUI_PASSWORD_KEY, true,
            "The password to use when accessing the Matterhorn web pages (org.opencastproject.security.demo.admin.pass"
                    + " in config.properties). \nDefault:" + GuiSettings.DEFAULT_GUI_PASSWORD));
    // In case of alternate login screen.
    options.addOption(new Option(GUI_USERNAME_FIELDNAME_KEY, true,
            "The name of the username gui element to enter the gui username (e.g. may differ from default if using CAS)."
                    + " \nDefault:" + GuiSettings.DEFAULT_GUI_USERNAME_FIELD_NAME));
    options.addOption(new Option(GUI_PASSWORD_FIELDNAME_KEY, true,
            "The name of the password gui element to enter the gui password (e.g. may differ from default if using CAS)."
                    + " \nDefault:" + GuiSettings.DEFAULT_GUI_PASSWORD_FIELD_NAME));
    // load test parameters.
    options.addOption(new Option(BROWSER_NUMBER_KEY, true,
            "The number of browsers to spawn to access engage. \nDefault:" + numberOfBrowsers));
    options.addOption(new Option(WATCH_TIME_KEY, true,
            "The amount of time in seconds to wait before switching to another video. "
                    + "It will watch for 1/2 the time you specify up to the total amount you specify.\nDefault:"
                    + timeToWatchVideo));
    return options;
  }

  /** Retrieve the list of episodes from the engage server so that we can randomly switch between them. **/
  private static LinkedList<String> getListOfEpisodes() {
    TrustedHttpClient trustedClient = getClient();
    String request = engageServerURL + SEARCH_URL;
    HttpGet httpGet = new HttpGet(request);
    logger.debug("Using " + httpGet.getMethod() + " on url " + request + " to get the episode list.");
    HttpResponse response = trustedClient.execute(httpGet);
    if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
      logger.debug("The result from trying to get the episode list is " + response.getStatusLine());
    } else {
      logger.warn("The result from trying to get the episode list is " + response.getStatusLine());
    }

    String xml = "";
    try {
      xml = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
    } catch (IllegalStateException e) {
      logger.error("Could not get episode list from response because of: ", e);
    } catch (IOException e) {
      logger.error("Could not get episode list from response because of: ", e);
    }

    LinkedList<String> episodeList = new LinkedList<String>();
    episodeList = parseEpisodeXml(new ByteArrayInputStream(xml.getBytes()));
    return episodeList;
  }

  /**
   * Parse the episode xml to retrieve all of the episode ids.
   * 
   * @param is
   *          This is the input stream representation of the xml.
   * @return A linked list of ids for the episodes on the engage server as Strings.
   **/
  public static LinkedList<String> parseEpisodeXml(InputStream is) {
    LinkedList<String> episodes = new LinkedList<String>();
    try {
      // Parse document
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document document = docBuilder.parse(is);

      // Process results
      Node result = null;
      Node id = null;

      for (int i = 0; i < document.getElementsByTagName("result").getLength(); i++) {
        result = document.getElementsByTagName("result").item(i);
        id = result.getAttributes().getNamedItem("id");
        episodes.add(id.getNodeValue());
      }
    } catch (ParserConfigurationException pce) {
      logger.error("The list of episodes could not be parsed from the xml received from the engage server because of:",
              pce);
    } catch (IOException ioe) {
      logger.error("The list of episodes could not be parsed from the xml received from the engage server because of:",
              ioe);
    } catch (SAXException sae) {
      logger.error("The list of episodes could not be parsed from the xml received from the engage server because of:",
              sae);
    }

    return episodes;
  }
}
