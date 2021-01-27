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

package org.opencastproject.workflow.handler.inspection;

import static java.lang.String.format;

import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionOptions;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Workflow operation used to inspect all tracks of a media package.
 */
public class InspectWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(InspectWorkflowOperationHandler.class);

  /** Option for rewriting existing metadata */
  private static final String OPT_OVERWRITE = "overwrite";

  /** Option to adjust whether mediapackages without media should be accepted */
  private static final String OPT_ACCEPT_NO_MEDIA = "accept-no-media";

  /** Option to adjust whether the exact frame count should be determined
      Note that this is an expensive operation. Its use should be avoided if not depending on the exact framecount
      Default: false */
  private static final String OPT_ACCURATE_FRAME_COUNT = "accurate-frame-count";

  /** The inspection service */
  private MediaInspectionService inspectionService = null;

  /** The dublin core catalog service */
  private DublinCoreCatalogService dcService;

  /** The local workspace */
  private Workspace workspace;

  public void setDublincoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param inspectionService
   *          the inspection service
   */
  protected void setInspectionService(MediaInspectionService inspectionService) {
    this.inspectionService = inspectionService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   *
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(
   *          org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage) workflowInstance.getMediaPackage().clone();
    // Inspect the tracks
    long totalTimeInQueue = 0;

    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    boolean rewrite = "true".equalsIgnoreCase(operation.getConfiguration(OPT_OVERWRITE));
    boolean acceptNoMedia = "true".equalsIgnoreCase(operation.getConfiguration(OPT_ACCEPT_NO_MEDIA));

    final Map<String, String> options = new HashMap<String, String>();
    if ("true".equalsIgnoreCase(operation.getConfiguration(OPT_ACCURATE_FRAME_COUNT))) {
      logger.info("Using accurate frame count for inspection media package {}", mediaPackage);
      options.put(MediaInspectionOptions.OPTION_ACCURATE_FRAME_COUNT, Boolean.TRUE.toString());
    }

    // Test if there are tracks in the mediapackage
    if (mediaPackage.getTracks().length == 0) {
      logger.warn("Recording {} contains no media", mediaPackage);
      if (!acceptNoMedia)
        throw new WorkflowOperationException("Mediapackage " + mediaPackage + " contains no media");
    }

    for (Track track : mediaPackage.getTracks()) {

      logger.info("Inspecting track '{}' of {}", track.getIdentifier(), mediaPackage);

      Job inspectJob = null;
      Track inspectedTrack;
      if (track != null && track.getURI() != null && (track.getURI().toString().endsWith(".vtt")
              || track.getURI().toString().endsWith(".srt"))) {
        inspectedTrack = (Track)track.clone();
        inspectedTrack.setMimeType(MimeType.mimeType("text", "vtt"));
        logger.info("Track '{}' of {} contains captions", track.getIdentifier(), mediaPackage);
      } else {
        try {
          inspectJob = inspectionService.enrich(track, rewrite, options);
          if (!waitForStatus(inspectJob).isSuccess()) {
            throw new WorkflowOperationException("Track " + track + " could not be inspected");
          }
        } catch (MediaInspectionException e) {
          throw new WorkflowOperationException("Error inspecting media package", e);
        } catch (MediaPackageException e) {
          throw new WorkflowOperationException("Error parsing media package", e);
        }

        // add this receipt's queue and execution times to the total
        long timeInQueue = inspectJob.getQueueTime() == null ? 0 : inspectJob.getQueueTime();
        totalTimeInQueue += timeInQueue;


        try {
          inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectJob.getPayload());
        } catch (MediaPackageException e) {
          throw new WorkflowOperationException("Unable to parse track from job " + inspectJob.getId(), e);
        }

        if (inspectedTrack == null)
          throw new WorkflowOperationException("Track " + track + " could not be inspected");

        if (inspectedTrack.getStreams().length == 0)
          throw new WorkflowOperationException(format("Track %s does not contain any streams", track));
      }
      // Replace the original track with the inspected one
      try {
        mediaPackage.remove(track);
        mediaPackage.add(inspectedTrack);
      } catch (UnsupportedElementException e) {
        logger.error("Error adding {} to media package", inspectedTrack, e);
      }
    }

    // Update dublin core with metadata
    try {
      updateDublinCore(mediaPackage);
    } catch (Exception e) {
      logger.warn("Unable to update dublin core data: {}", e.getMessage(), e);
      throw new WorkflowOperationException(e.getMessage());
    }

    return createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
  }

  /**
   * Updates those dublin core fields that can be gathered from the technical metadata.
   *
   * @param mediaPackage
   *          the media package
   */
  protected void updateDublinCore(MediaPackage mediaPackage) throws Exception {
    // Complete episode dublin core catalog (if available)
    Catalog[] dcCatalogs = mediaPackage.getCatalogs(MediaPackageElements.EPISODE,
            MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if (dcCatalogs.length > 0) {
      DublinCoreCatalog dublinCore = loadDublinCoreCatalog(dcCatalogs[0]);

      // Extent
      if (mediaPackage.getDuration() != null && !dublinCore.hasValue(DublinCore.PROPERTY_EXTENT)) {
        DublinCoreValue extent = EncodingSchemeUtils.encodeDuration(mediaPackage.getDuration());
        dublinCore.set(DublinCore.PROPERTY_EXTENT, extent);
        logger.debug("Setting dc:extent to '{}'", extent.getValue());
      }

      // Date created
      if (mediaPackage.getDate() != null && !dublinCore.hasValue(DublinCore.PROPERTY_CREATED)) {
        DublinCoreValue date = EncodingSchemeUtils.encodeDate(mediaPackage.getDate(), Precision.Minute);
        dublinCore.set(DublinCore.PROPERTY_CREATED, date);
        logger.debug("Setting dc:date to '{}'", date.getValue());
      }

      // Serialize changed dublin core
      InputStream in = dcService.serialize(dublinCore);
      String mpId = mediaPackage.getIdentifier().toString();
      String elementId = dcCatalogs[0].getIdentifier();
      workspace.put(mpId, elementId, FilenameUtils.getName(dcCatalogs[0].getURI().getPath()), in);
      dcCatalogs[0].setURI(workspace.getURI(mpId, elementId));
    }
  }

  /**
   * Loads a dublin core catalog from a mediapackage's catalog reference
   *
   * @param catalog
   *          the mediapackage's reference to this catalog
   * @return the dublin core
   * @throws IOException
   *           if there is a problem loading or parsing the dublin core object
   */
  protected DublinCoreCatalog loadDublinCoreCatalog(Catalog catalog) throws IOException {
    InputStream in = null;
    try {
      File f = workspace.get(catalog.getURI());
      in = new FileInputStream(f);
      return dcService.load(in);
    } catch (NotFoundException e) {
      throw new IOException("Unable to open catalog " + catalog, e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
