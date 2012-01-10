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
package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.DocUtil;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowOperationResultImpl;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

public class GStreamerWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {
  public static final String DEFAULT_GSTREAMER_LOCATION = "/usr/bin/gst-launch";
  public static final String CONFIG_GSTREAMER_LOCATION_KEY  = "org.opencastproject.export.gstreamerpath";
  /** The location of the gstreamer binary for running commands. **/
  private String gstreamerLocation = DEFAULT_GSTREAMER_LOCATION;
  /** The id for the workflow operation which is recognized in the workflow xml file. */
  public static final String ID = "gstreamer";
  /** This is the description of the workflow operation. */
  public static final String DESCRIPTION = "Executes gstreamer command line workflow operations";
  /** Path to the hold ui resources */
  private static final String HOLD_UI_PATH = "/ui/operation/export/index.html";
  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;
  /** The workspace to pull files out of. */
  private Workspace workspace;
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerWorkflowOperationHandler.class);

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  public String getGStreamerLocation() {
    return gstreamerLocation;
  }
  
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * Set the workspace.
   * 
   * @param workspace
   *          The new workspace.
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
  
  public void activate(ComponentContext cc) {
    if (cc != null) {
      super.activate(cc);

      setHoldActionTitle("Configure Export");
      registerHoldStateUserInterface(HOLD_UI_PATH);
      logger.info("Registering export template settings hold state ui from classpath {}", HOLD_UI_PATH);

      String path = StringUtils.trimToNull((String) cc.getBundleContext().getProperty(CONFIG_GSTREAMER_LOCATION_KEY));
      if (path == null) {
        logger.debug("DEFAULT " + CONFIG_GSTREAMER_LOCATION_KEY + ": " + DEFAULT_GSTREAMER_LOCATION);
      } else {
        gstreamerLocation = path;
        logger.debug("GStreamerWorkflowOperationHandler is going to be using binary: {}", path);
      }
    }
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.info("Holding for export configuration...");
    return createResult(Action.PAUSE);
  }

  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context,
          Map<String, String> properties) throws WorkflowOperationException {
    logger.debug("Processing gstreamer commandline workflow {} using {}", workflowInstance.getId(), properties);
    // MediaPackage from previous workflow operations
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    Attachment[] attachments = mediaPackage.getAttachments();
    Properties totalProperties = new Properties();
    for (Attachment attachment : attachments) {
      try {
        File attachmentFile = workspace.get(attachment.getURI());
        FileReader fileReader = new FileReader(attachmentFile);
        Properties attachmentProperties = new Properties();
        attachmentProperties.load(fileReader);
        totalProperties.putAll(attachmentProperties);
      } catch (IllegalArgumentException e) {
        logger.info("Couldn't load properties from file " + attachment.getURI() + ", skipping it. ", e);
      } catch (NotFoundException e) {
        logger.error("The file " + attachment.getURI() + " did not exist.", e);
      } catch (IOException e) {
        logger.info("Couldn't load properties from file " + attachment.getURI() + ", skipping it. ", e);
      }
    }
    if (properties != null) {
      totalProperties.putAll(properties);
    }

    String gstreamerLine = totalProperties.getProperty("org.opencastproject.workflow.config.gstreamer");

    // Fail if we don't have a gstreamer command to execute.
    if (StringUtils.trimToNull(gstreamerLine) == null) {
      throw new WorkflowOperationException("There was no gstreamer command to execute. ");
    }

    // Test to make sure that we have at least one track in the media package.
    if (mediaPackage.getTracks() == null || mediaPackage.getTracks().length <= 0) {
      throw new WorkflowOperationException("There are no tracks in the media package to run operations on. ");
    }

    // Replace all tracks with the proper location.
    HashMap<String, String> tracks = new HashMap<String, String>();
    for (Track track : mediaPackage.getTracks()) {
      try {
        tracks.put(track.getIdentifier(), workspace.get(track.getURI()).getAbsolutePath());
      } catch (NotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    gstreamerLine = replaceTemplateValues(gstreamerLine, tracks);
    String outputFileString = replaceTemplateValues(totalProperties.getProperty("org.opencastproject.workflow.config.gstreamer.outputfiles"), tracks);
    logger.info("Creating gstreamer template process for workflow {} using {}",workflowInstance.getId(), properties);
    Process process = null;
    try {
      ArrayList<String> args = new ArrayList<String>();
      args.add(DEFAULT_GSTREAMER_LOCATION);
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
        throw new WorkflowOperationException("gstreamer failed with error code: " + process.exitValue() + ". "
                + sb.toString());
      }
    } catch (IOException e) {
      throw new WorkflowOperationException("Could not start gstreamer pipeline: " + gstreamerLine + "\n"
              + e.getMessage());
    } catch (InterruptedException e) {
      throw new WorkflowOperationException("Could not start gstreamer pipeline: " + gstreamerLine + "\n"
              + e.getMessage());
    }
    logger.info("Gstreamer process finished");

    String[] outputFiles = outputFileString.split(";");
    IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();
    //TODO:  Clone this?
    MediaPackage mediapackage = workflowInstance.getMediaPackage();
    for (String file : outputFiles) {
      String path = StringUtils.trimToNull(file);
      if (path == null) {
        continue;
      }
      File outputFile = new File(path);
      MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      URI workspaceFile = null;
      try {
        InputStream in = new FileInputStream(outputFile);
        workspaceFile = workspace.putInCollection("gstreamer", outputFile.getName(), in);
      } catch (IOException e) {
        throw new WorkflowOperationException("Could not start gstreamer pipeline: " + gstreamerLine + "\n"
                + e.getMessage());
      }
      Track t = (Track) builder.elementFromURI(workspaceFile, MediaPackageElement.Type.Track, new MediaPackageElementFlavor("video", "generic"));
      t.setIdentifier(idBuilder.createNew().toString());
      t.addTag("export");
      mediapackage.add(t);
    }

    //TODO:  Once we are actually calling a service please change the zero in the line below to the correct job.getQueueTime
    WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, 0);
    logger.debug("Compose operation completed");
    return result;
  }

  @Override
  public WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    return new WorkflowOperationResultImpl(workflowInstance.getMediaPackage(), null, Action.SKIP, 0);
  }

  @Override
  public void destroy(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
    // Nothing to do for cleaning.
  }

  public static String replaceTemplateValues(String gstreamerCommandLine, Map<String, String> substitutions) {
    Map<String, Object> castedMap = new HashMap<String, Object>();
    castedMap.putAll(substitutions);
    String id = UUID.randomUUID().toString();
    return DocUtil.processTextTemplate(id, gstreamerCommandLine, castedMap);
  }
}
