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
package org.opencastproject.loadtest.impl;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/** Handles an individual ingest from copying the media package, changing the correct metadata and posting it to the server. **/
public class IngestJob implements Runnable {
  // The logger.
  private static final Logger logger = LoggerFactory.getLogger(IngestJob.class);
  // The delay in minutes before starting the ingest.
  private long startDelay = 0;
  // The media package id.
  private String id = "";
  // The LoadTesting environment we will be performing this ingest in.
  private LoadTest loadTest = null;
  // The client to use to connect to the core for the ingest.
  private TrustedHttpClient client = null;

  /**
   * Prepares and ingests a single media package to the core for load testing.
   * 
   * @param id
   *          The mediapackage id to ingest
   * @param startDelay
   *          The amount of time in minutes to wait until ingesting.
   * @param newLoadTesting
   *          The LoadTesting instance that is running this ingest
   * @param newClient
   *          The client to use to connect to the core.
   */
  public IngestJob(String id, long startDelay, LoadTest loadTest, TrustedHttpClient newClient) {
    this.id = id;
    this.startDelay = startDelay;
    this.loadTest = loadTest;
    client = newClient;
  }

  /** Waits for the start delay, changes the correct metadata for the mediapackage and ingests it. **/
  @Override
  public void run() {
    ThreadCounter.add();
    try {
      Thread.sleep(startDelay * LoadTest.MILLISECONDS_IN_SECONDS * LoadTest.SECONDS_IN_MINUTES);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    logger.info("Starting to run " + id);
    String workingDirectory = loadTest.getWorkspaceLocation() + IOUtils.DIR_SEPARATOR + id + "/";
    // Create working directory
    try {
      FileUtils.forceMkdir(new File(workingDirectory));
    } catch (IOException e) {
      logger.error("Had trouble creating working directory at " + workingDirectory + " because " + e.getMessage());
    }
    // Copy source media package
    logger.info("Beginning Copy of " + id);
    String copyCommand = "cp " + loadTest.getSourceMediaPackageLocation() + " " + workingDirectory + id + ".zip";
    Execute.launch(copyCommand);
    // Change id in manifest.xml
    updateManifestID(workingDirectory);
    // Change id in episode.xml
    updateEpisodeID(workingDirectory);
    // Update the new manifest.xml and episode.xml to the zip file.
    logger.info("Beginning update of zipfile " + id);
    updateZip(workingDirectory);
    ingestMediaPackageWithJava(workingDirectory, workingDirectory + id + ".zip", loadTest.getWorkflowID());
    logger.info("Finished ingesting " + id);
    ThreadCounter.subtract();
  }

  /**
   * Unzips the manifest.xml and changes the id from the mediapackage.
   * 
   * @param workingDirectory
   *          The location of the media package.
   */
  private void updateManifestID(String workingDirectory) {
    // Change id in manifest.xml
    String extractManifestCommand = "unzip " + workingDirectory + id + ".zip" + " manifest.xml " + "-d "
            + workingDirectory;
    Execute.launch(extractManifestCommand);
    changeManifestID(workingDirectory + "manifest.xml");
  }

  /**
   * Parse and change the manifest.xml's id to match the mediapackage id we will be ingesting.
   * 
   * @param filepath
   *          The path to the manifest.xml file.
   */
  private void changeManifestID(String filepath) {
    try {
      logger.info("Filepath for changing the manifest id is " + filepath);
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(filepath);

      // Get the mediapackage element by tag name directly
      Node mediapackage = doc.getElementsByTagName("mediapackage").item(0);

      // update mediapackage attribute
      NamedNodeMap attr = mediapackage.getAttributes();
      Node nodeAttr = attr.getNamedItem("id");
      nodeAttr.setTextContent(id);

      // write the content into xml file
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File(filepath));
      transformer.transform(source, result);
    } catch (ParserConfigurationException pce) {
      pce.printStackTrace();
    } catch (TransformerException tfe) {
      tfe.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (SAXException sae) {
      sae.printStackTrace();
    }
  }

  /**
   * Unzips and changes the mediapackage id in the episode.xml file.
   * 
   * @param workingDirectory
   *          The path to the mediapackage location.
   */
  private void updateEpisodeID(String workingDirectory) {
    // Change id in manifest.xml
    String extractManifestCommand = "unzip " + workingDirectory + id + ".zip" + " episode.xml " + "-d "
            + workingDirectory;
    Execute.launch(extractManifestCommand);
    changeEpisodeID(workingDirectory + "episode.xml");
  }

  /**
   * Parse the episode.xml file and change the mediapackage id.
   * 
   * @param filepath
   *          The location of the episode.xml.
   */
  private void changeEpisodeID(String filepath) {
    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(filepath);

      // Get the identifier element by tag name directly
      Node identifier = doc.getElementsByTagName("dcterms:identifier").item(0);
      identifier.setTextContent(id);

      // write the content into xml file
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File(filepath));
      transformer.transform(source, result);
    } catch (ParserConfigurationException pce) {
      pce.printStackTrace();
    } catch (TransformerException tfe) {
      tfe.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (SAXException sae) {
      sae.printStackTrace();
    }
  }

  /**
   * Update the changed manifest.xml and episode.xml with the changed mediapackage ids in the zip file.
   * 
   * @param workingDirectory
   *          The location of the media package.
   */
  private void updateZip(String workingDirectory) {
    String updateManifestCommand = "zip -fj " + workingDirectory + id + ".zip " + workingDirectory + "manifest.xml"
            + " " + workingDirectory + "episode.xml";
    Execute.launch(updateManifestCommand);
  }

  /**
   * Use the TrustedHttpClient from matterhorn to ingest the mediapackage.
   * 
   * @param workingDirectory
   *          The path to the working directory for the LoadTesting.
   * @param mediaPackageLocation
   *          The location of the mediapackage we want to ingest.
   */
  private void ingestMediaPackageWithJava(String workingDirectory, String mediaPackageLocation, String workflowID) {
    logger.info("Ingesting recording with HttpTrustedClient: {}", id);
    try {
      URL url = new URL(loadTest.getCoreAddress() + "/ingest/addZippedMediaPackage");
      File fileDesc = new File(mediaPackageLocation);
      // Set the file as the body of the request
      MultipartEntity entities = new MultipartEntity();
      // Check to see if the properties have an alternate workflow definition attached
      String workflowInstance = id;

      try {
        entities.addPart("workflowDefinitionId", new StringBody(workflowID, Charset.forName("UTF-8")));
        entities.addPart("workflowInstanceId", new StringBody(workflowInstance, Charset.forName("UTF-8")));
        entities.addPart(fileDesc.getName(), new InputStreamBody(new FileInputStream(fileDesc), fileDesc.getName()));
      } catch (FileNotFoundException ex) {
        logger.error("Could not find zipped mediapackage " + fileDesc.getAbsolutePath());
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("This system does not support UTF-8", e);
      }

      logger.debug("Ingest URL is " + url.toString());
      HttpPost postMethod = new HttpPost(url.toString());
      postMethod.setEntity(entities);

      // Send the file
      HttpResponse response = null;
      int retValue = -1;
      try {
        logger.debug("Sending the file " + fileDesc.getAbsolutePath() + " with a size of " + fileDesc.length());
        response = client.execute(postMethod);
      } catch (TrustedHttpClientException e) {
        logger.error("Unable to ingest recording {}, message reads: {}.", id, e.getMessage());
      } catch (NullPointerException e) {
        logger.error("Unable to ingest recording {}, null pointer exception!", id);
      } finally {
        if (response != null) {
          retValue = response.getStatusLine().getStatusCode();
          client.close(response);
        } else {
          retValue = -1;
        }
      }

      if (retValue == HttpURLConnection.HTTP_OK) {
        logger.info(id + " successfully ingested.");
      }
      
      else {
        logger.info(id + " ingest failed with return code " + retValue + " and status line " + response.getStatusLine().toString());
      }

    } catch (MalformedURLException e) {
      logger.error("Malformed URL for ingest target \"" + loadTest.getCoreAddress()
              + "/ingest/addZippedMediaPackage\"");
    }
  }

  /**
   * Get the mediapackage id for this ingest.
   * 
   * @return The mediapackage id.
   */
  public String getID() {
    return id;
  }

  @Override
  public String toString() {
    return "IngestJob with id\"" + id + "\" and delay \"" + startDelay + "\" minutes";
  }
}
