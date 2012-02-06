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
    Firefox, Chrome
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

    if (line.hasOption("url")) {
      engageServerURL = line.getOptionValue("url");
    }
    if (line.hasOption("digestusername")) {
      digestUsername = line.getOptionValue("digestusername");
    }
    if (line.hasOption("digestpassword")) {
      digestPassword = line.getOptionValue("digestpassword");
    }

    // Get settings for interacting with the gui.
    if (line.hasOption("guiusername")) {
      guiSettings.setUsername(line.getOptionValue("guiusername"));
    }
    if (line.hasOption("guipassword")) {
      guiSettings.setPassword(line.getOptionValue("guipassword"));
    }
    if (line.hasOption("guiusernamefieldname")) {
      guiSettings.setUsernameFieldName(line.getOptionValue("guiusernamefieldname"));
    }
    if (line.hasOption("guipasswordfieldname")) {
      guiSettings.setPasswordFieldName(line.getOptionValue("guipasswordfieldname"));
    }

    if (line.hasOption("browsernumber")) {
      numberOfBrowsers = Integer.parseInt(line.getOptionValue("browsernumber"));
    }
    if (line.hasOption("watchtime")) {
      timeToWatchVideo = Integer.parseInt(line.getOptionValue("watchtime"));
    }

    if (line.hasOption("withchrome")) {
      browserToUse = BrowserToUse.Chrome;
    }
    if (line.hasOption("withff")) {
      browserToUse = BrowserToUse.Firefox;
    }

    if (line.hasOption("help")) {
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
    options.addOption(new Option("help", false, "print this message"));
    // Add browser options.
    options.addOption(new Option("withff", false, "run the load test with Firefox browser. Default:true"));
    options.addOption(new Option("withchrome", false, "run the load test with Chrome browser. Default:false"));
    // Server to hit
    options.addOption(new Option("url", true, "run load tests against the engage server at this url. \nDefault:"
            + engageServerURL));
    // Authentication to use to get episode list.
    options.addOption(new Option(
            "digestusername",
            true,
            "the username to use when accessing the Matterhorn services (org.opencastproject.security.digest.user in config.properties). \nDefault:"
                    + digestUsername));
    options.addOption(new Option(
            "digestpassword",
            true,
            "the password to use when accessing the Matterhorn services (org.opencastproject.security.digest.pass in config.properties). \nDefault:"
                    + digestPassword));
    // Authentication to handle gui.
    options.addOption(new Option(
            "guiusername",
            true,
            "the username to use when accessing the Matterhorn web pages (org.opencastproject.security.demo.admin.user in config.properties). \nDefault:"
                    + GuiSettings.DEFAULT_GUI_USERNAME));
    options.addOption(new Option(
            "guipassword",
            true,
            "the password to use when accessing the Matterhorn web pages (org.opencastproject.security.demo.admin.pass in config.properties). \nDefault:"
                    + GuiSettings.DEFAULT_GUI_PASSWORD));
    // In case of alternate login screen.
    options.addOption(new Option(
            "guiusernamefieldname",
            true,
            "the name of the username gui element to enter the gui username (e.g. may differ from default if using CAS). \nDefault:"
                    + GuiSettings.DEFAULT_GUI_USERNAME_FIELD_NAME));
    options.addOption(new Option(
            "guipasswordfieldname",
            true,
            "the name of the password gui element to enter the gui password (e.g. may differ from default if using CAS). \nDefault:"
                    + GuiSettings.DEFAULT_GUI_PASSWORD_FIELD_NAME));
    // load test parameters.
    options.addOption(new Option("browsernumber", true, "the number of browsers to spawn to access engage. \nDefault:"
            + numberOfBrowsers));
    options.addOption(new Option("watchtime", true,
            "the amount of time in seconds to wait before switching to another video. "
                    + "It will watch for 1/2 the time you specify up to the total amount you specify.\nDefault:"
                    + timeToWatchVideo));
    return options;
  }

  /** Retrieve the list of episodes from the engage server so that we can randomly switch between them. **/
  private static LinkedList<String> getListOfEpisodes() {
    TrustedHttpClient trustedClient = getClient();
    String request = engageServerURL + "/search/episode.xml?limit=10&offset=0";
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
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    LinkedList<String> episodeList = new LinkedList<String>();
    try {
      episodeList = parseEpisodeXml(new ByteArrayInputStream(xml.getBytes()));
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }

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
      pce.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (SAXException sae) {
      sae.printStackTrace();
    }

    return episodes;
  }
}
