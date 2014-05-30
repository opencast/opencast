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
package org.opencastproject.gstreamer.service.impl;

import org.opencastproject.gstreamer.service.api.GStreamerLaunchException;
import org.opencastproject.gstreamer.service.api.GStreamerService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.DocUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.gstreamer.Gst;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GStreamer service that runs gstreamer pipelines with the gstreamer launch command.
 */
public class GStreamerServiceImpl extends AbstractJobProducer implements GStreamerService {
  /** If no specific location is configured for the gstreamer command this location is used by default. **/
  public static final String DEFAULT_GSTREAMER_LOCATION = "gst-launch";
  /** The key defining a custom gstreamer location.  **/
  public static final String CONFIG_GSTREAMER_LOCATION_KEY = "org.opencastproject.export.gstreamer.path";
  /** The location of the gstreamer binary for running commands. **/
  private String gstreamerLocation = DEFAULT_GSTREAMER_LOCATION;

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerServiceImpl.class);

  /** The collection name */
  public static final String COLLECTION = "gstreamer";

  /** Reference to the workspace service */
  private Workspace workspace = null;

  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** List of available operations on jobs */
  private enum Operation {
    Launch
  };

  /**
   * Creates a new instance of the composer service.
   */
  public GStreamerServiceImpl() {
    super(JOB_TYPE);
    Gst.init();
  }

  /** Check for a custom gstreamer location in the osgi context. **/
  public void activate(ComponentContext cc) {
    if (cc != null) {
      String path = StringUtils.trimToNull((String) cc.getBundleContext().getProperty(CONFIG_GSTREAMER_LOCATION_KEY));
      if (path == null) {
        logger.debug("DEFAULT " + CONFIG_GSTREAMER_LOCATION_KEY + ": " + DEFAULT_GSTREAMER_LOCATION);
      } else {
        gstreamerLocation = path;
        logger.debug("We are going to be using gstreamer binary: {}", path);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  protected String process(Job job) throws Exception {
    logger.debug("Processing Job");
    List<String> arguments = job.getArguments();
    MediaPackage mediaPackage = MediaPackageParser.getFromXml(arguments.get(0));
    MediaPackageElement resultingElement = launchGStreamerLine(job, mediaPackage, arguments.get(1), arguments.get(2));
    return MediaPackageElementParser.getAsXml(resultingElement);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.gstreamer.api.GStreamerService#launch(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String)
   */
  @Override
  public Job launch(MediaPackage mediapackage, String launch, String outputFiles) throws GStreamerLaunchException, MediaPackageException {
    try {
      LinkedList<String> arguments = new LinkedList<String>();
      arguments.add(MediaPackageParser.getAsXml(mediapackage));
      arguments.add(launch);
      arguments.add(outputFiles);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Launch.toString(), arguments);
    } catch (ServiceRegistryException e) {
      throw new GStreamerLaunchException("Unable to create a job", e);
    }
  }

  /** Launches the actual gstreamer pipeline after replacing variables from the mediapackage and output files list.**/
  private Track launchGStreamerLine(Job job, MediaPackage mediapackage, String gstreamerInput, String outputFileInput) throws GStreamerLaunchException, MediaPackageException {
    Gst.init();
    String gstreamerLine = replaceTrackVariables(mediapackage, gstreamerInput);
    String outputFileString = replaceTrackVariables(mediapackage, outputFileInput);
    launchGStreamerLine(gstreamerLine);
    String[] outputFiles = outputFileString.split(";");
    IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();
    Track track = null;
    for (String file : outputFiles) {
      String path = StringUtils.trimToNull(file);
      if (path == null) {
        continue;
      }
      File outputFile = new File(path);
      URI workspaceFile = null;
      try {
        InputStream in = new FileInputStream(outputFile);
        workspaceFile = workspace.putInCollection("gstreamer", outputFile.getName(), in);
      } catch (IOException e) {
        throw new GStreamerLaunchException("Could not start gstreamer pipeline: " + gstreamerLine + "\n"
                + e.getMessage());
      } finally {
        FileUtils.deleteQuietly(outputFile);
      }
      track = (Track) MediaPackageElementBuilderFactory
              .newInstance()
              .newElementBuilder()
              .elementFromURI(workspaceFile, MediaPackageElement.Type.Track,
                      new MediaPackageElementFlavor("video", "generic"));
      track.setIdentifier(idBuilder.createNew().toString());
      track.addTag("export");
    }
    return track;
  }

  /**
   * Launch the gstreamer pipeline from the gstreamerLine.
   *
   * @param gstreamerLine
   *          The gstreamer line to execute.
   * @throws GStreamerLaunchException
   *           Thrown if the pipeline fails to execute.
   */
  private void launchGStreamerLine(String gstreamerLine) throws GStreamerLaunchException {
    Process process = null;
    try {
      ArrayList<String> args = new ArrayList<String>();
      args.add(getGStreamerLocation());
      args.addAll(Arrays.asList(gstreamerLine.split(" ")));
      ProcessBuilder pb = new ProcessBuilder(args);
      pb.redirectErrorStream(true); // Unfortunately merges but necessary for deadlock prevention
      process = pb.start();
      BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
      process.waitFor();
      StringBuilder sb = new StringBuilder();
      String line = inputStream.readLine();
      while (line != null) {
        sb.append(line);
        line = inputStream.readLine();
      }
      if (process.exitValue() != 0) {
        throw new GStreamerLaunchException("gstreamer failed with error code: " + process.exitValue() + ". "
                + sb.toString());
      }
    } catch (IOException e) {
      throw new GStreamerLaunchException("Could not start gstreamer pipeline: " + gstreamerLine + "\n"
              + e.getMessage());
    } catch (InterruptedException e) {
      throw new GStreamerLaunchException("Could not start gstreamer pipeline: " + gstreamerLine + "\n"
              + e.getMessage());
    }
    logger.info("Gstreamer process finished");
  }

  /**
   * Replace all tracks with the proper location from the mediapackage.
   *
   * @param mediaPackage
   *          The mediapackage to look for tracks.
   * @param stringWithSubstitions
   *          The string to make the substitutions on.
   * @return The string with the updated track locations.
   */
  private String replaceTrackVariables(MediaPackage mediaPackage, String stringWithSubstitions) {
    HashMap<String, String> tracks = new HashMap<String, String>();
    for (Track track : mediaPackage.getTracks()) {
      try {
        tracks.put(track.getIdentifier(), workspace.get(track.getURI()).getAbsolutePath());
      } catch (NotFoundException e) {
        logger.error("Unable to replace a track id with its location due to it not being found", e);
      } catch (IOException e) {
        logger.error("Unable to replace a track id with its location due to an IO exception", e);
      }
    }
    return replaceTemplateValues(stringWithSubstitions, tracks);
  }

  /**
   * Substitutes all of the substitutions in the gstreamerCommandLine and returns the new String.
   *
   * @param gstreamerCommandLine
   *          The string to make the substitutions on.
   * @param substitutions
   *          The collection of substitutions to make.
   * @return The String with the substitutions made.
   */
  public static String replaceTemplateValues(String gstreamerCommandLine, Map<String, String> substitutions) {
    Map<String, Object> castedMap = new HashMap<String, Object>();
    castedMap.putAll(substitutions);
    String id = UUID.randomUUID().toString();
    return DocUtil.processTextTemplate(id, gstreamerCommandLine, castedMap);
  }

  /**
   * @return The location that this bundle is using to run gstreamer commands.
   */
  public String getGStreamerLocation() {
    return gstreamerLocation;
  }

  /**
   * Deletes any valid file in the list.
   *
   * @param encodingOutput
   *          list of files to be deleted
   */
  protected void cleanup(List<File> encodingOutput) {
    for (File file : encodingOutput) {
      if (file != null && file.isFile()) {
        String path = file.getAbsolutePath();
        if (file.delete()) {
          logger.info("Deleted local copy of image file at {}", path);
        } else {
          logger.warn("Could not delete local copy of image file at {}", path);
        }
      }
    }
  }

  protected void cleanupWorkspace(List<URI> workspaceURIs) {
    for (URI url : workspaceURIs) {
      try {
        workspace.delete(url);
      } catch (Exception e) {
        logger.warn("Could not delete {} from workspace: {}", url, e.getMessage());
      }
    }
  }

  protected File getTrack(Track track) throws GStreamerLaunchException {
    try {
      return workspace.get(track.getURI());
    } catch (NotFoundException e) {
      throw new GStreamerLaunchException("Requested track " + track + " is not found");
    } catch (IOException e) {
      throw new GStreamerLaunchException("Unable to access track " + track);
    }
  }

  /**
   * Sets the workspace
   *
   * @param workspace
   *          an instance of the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the service registry
   *
   * @param serviceManager
   */
  protected void setServiceRegistry(ServiceRegistry serviceManager) {
    this.serviceRegistry = serviceManager;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

}
