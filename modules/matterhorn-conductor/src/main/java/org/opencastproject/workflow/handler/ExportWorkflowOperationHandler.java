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

import org.opencastproject.gstreamer.service.api.GStreamerLaunchException;
import org.opencastproject.gstreamer.service.api.GStreamerService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.util.NotFoundException;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

public class ExportWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {
  /** The key to retrieve the acl to control the exported media. **/
  public static final String EXPORT_ACL_KEY = "org.opencastproject.workflow.config.gstreamer.acl";
  /** The key to retrieve the output files created by the gstreamer pipeline. **/
  private static final String OUTPUT_FILES_KEY = "org.opencastproject.workflow.config.gstreamer.outputfiles";
  /** The key to retrieve the gstreamer launch command. **/
  private static final String GSTREAMER_COMMAND_KEY = "org.opencastproject.workflow.config.gstreamer";
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
  private static final Logger logger = LoggerFactory.getLogger(ExportWorkflowOperationHandler.class);

  /** The gstreamer service */
  private GStreamerService gstreamerService = null;
  
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
  
  /**
   * Set the gstreamer service.
   * 
   * @param workspace
   *          The new workspace.
   */
  public void setGStreamerService(GStreamerService gstreamerService) {
    this.gstreamerService = gstreamerService;
  }
  
  public void activate(ComponentContext cc) {
    if (cc != null) {
      super.activate(cc);
      setHoldActionTitle("Configure Export");
      registerHoldStateUserInterface(HOLD_UI_PATH);
      logger.info("Registering export template settings hold state ui from classpath {}", HOLD_UI_PATH);
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
    return launch(workflowInstance, properties);
  }

  private WorkflowOperationResult launch(WorkflowInstance workflowInstance, Map<String, String> properties)
          throws WorkflowOperationException {
    logger.debug("Processing gstreamer commandline workflow {} using {}", workflowInstance.getId(), properties);
    // MediaPackage from previous workflow operations
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    Properties totalProperties = getTotalProperties(mediaPackage, properties);
    
    String gstreamerCommand = totalProperties.getProperty(GSTREAMER_COMMAND_KEY);
    
    // Fail if we don't have a gstreamer command to execute.
    if (StringUtils.trimToNull(gstreamerCommand) == null) {
      throw new WorkflowOperationException("There was no gstreamer command to execute. ");
    } else if (mediaPackage.getTracks() == null || mediaPackage.getTracks().length <= 0) {
      throw new WorkflowOperationException("There are no tracks to run an export on. ");
    }
    
    String outputFiles = totalProperties.getProperty(OUTPUT_FILES_KEY);
    
    String acl = totalProperties.getProperty(EXPORT_ACL_KEY, "");
    
    long jobTime = 0;
    Job job = null;
    try {
      logger.debug("Creating gstreamer job for workflow {} using {}",workflowInstance.getId(), totalProperties);
      job = gstreamerService.launch(mediaPackage, gstreamerCommand, outputFiles);
      if (!waitForStatus(job).isSuccess()) {
        throw new WorkflowOperationException("Encoding failed");
      }
      jobTime = job.getQueueTime();
    } catch (GStreamerLaunchException e) {
      throw new WorkflowOperationException("Encoding failed ", e);
    } catch (MediaPackageException e) {
      throw new WorkflowOperationException("Encoding failed ", e);
    }
    
    logger.debug("Gstreamer launch operation completed");
    addTrackToMediaPackage(mediaPackage, job.getPayload());
    addAclStringToMediaPackageAsXml(mediaPackage, acl);
    WorkflowOperationResult result = createResult(mediaPackage, Action.CONTINUE, jobTime);
    return result;
  }

  private void addTrackToMediaPackage(MediaPackage mediaPackage, String trackString) {
    Track track;
    try {
      track = (Track) MediaPackageElementParser.getFromXml(trackString);
      mediaPackage.add(track);
    } catch (MediaPackageException e) {
     logger.warn("Couldn't add track to media package ", e);
    }
  }

  /**
   * Consolidates all of the properties into a single properties file.
   * 
   * @param mediaPackage
   *          The mediapackage to get all of the possible properties files from.
   * @param properties
   *          The runtime properties to combine the file properties with.
   * @return The total list of properties.
   */
  private Properties getTotalProperties(MediaPackage mediaPackage, Map<String, String> properties) {
    Properties totalProperties = new Properties();
    if (mediaPackage != null) {
      Attachment[] attachments = mediaPackage.getAttachments();
      if (attachments != null) {

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
      }
    }
    return totalProperties;
  }

  /**
   * Creates an XML document with the access control list that will allow users to view exported videos.
   * 
   * @param mediaPackage
   *          The media package to add the acl xml to.
   * @param acl
   *          The acl in proper xml that will be added to the media package.
   */
  private void addAclStringToMediaPackageAsXml(MediaPackage mediaPackage, String acl) {
    if (mediaPackage == null) {
      logger.warn("While trying to add acl XML to a media package, that media package was null.");
    }

    if (StringUtils.trimToNull(acl) == null) {
      logger.warn("There was no acl to add to the media package so it will be skipped.");
    }
    
    if (mediaPackage != null && StringUtils.trimToNull(acl) != null) {
      String identifier = mediaPackage.getIdentifier().toString();
      String extension = ".acl";
      InputStream inputStream;
      try {
        inputStream = new ByteArrayInputStream(acl.getBytes("UTF-8"));
        URI aclDestination = workspace.put(mediaPackage.getIdentifier().compact(),
                "attachment-" + (mediaPackage.getAttachments().length + 1), identifier + extension, inputStream);
        Attachment attachment = AttachmentImpl.fromURI(aclDestination);
        MediaPackageElementFlavor flavor = new MediaPackageElementFlavor("text", "acl",
                "The access control list for a given export file.");
        attachment.setFlavor(flavor);
        mediaPackage.add(attachment);
      } catch (UnsupportedEncodingException e) {
        logger.warn("Unable to create acl \"" + acl + "\" because the encoding was not supported. ", e);
      } catch (IOException e) {
        logger.warn("Unable to create acl \"" + acl + "\" due to an IOException. ", e);
      }
    }
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
}
