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

package org.opencastproject.workflow.handler.composer;

import org.opencastproject.caption.api.CaptionConverterException;
import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.caption.api.UnsupportedCaptionFormatException;
import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation handler for embedding captions in QuickTime movies.
 *
 */
public class CaptionEmbedderWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  /** Reference to caption service */
  private CaptionService captionService;

  /** Reference to composer service */
  private ComposerService composerService;

  /** Reference to workspace */
  private Workspace workspace;

  /** Setter for caption service via declarative activation */
  public void setCaptionService(CaptionService service) {
    captionService = service;
  }

  /** Setter for composer service via declarative activation */
  public void setComposerService(ComposerService service) {
    composerService = service;
  }

  /** Setter for workspace via declarative service */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-media-flavor", "The \"flavor\" of the track to use as embedding input");
    CONFIG_OPTIONS.put("source-captions-flavor", "The \"flavor\" of the captions to use as embedding input");
    CONFIG_OPTIONS.put("target-media-flavor", "The \"flavor\" of apply to embedded file");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {

    try {
      return embed(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Media package is searched for suitable QuickTime files and caption Catalogs based on configuration. Each caption is
   * converted to SRT format and embedding is executed.
   *
   * @param src
   *          source media package
   * @param operation
   *          current operation
   * @return media package with converted captions and embedded tracks
   * @throws Exception
   *           if conversion or embedding fails
   */
  private WorkflowOperationResult embed(MediaPackage src, WorkflowOperationInstance operation) throws Exception {

    MediaPackage mp = (MediaPackage) src.clone();

    // read configuration properties
    String sourceMediaFlavor = operation.getConfiguration("source-media-flavor");
    String sourceCaptionsFlavor = operation.getConfiguration("source-captions-flavor");
    String targetMediaFlavor = operation.getConfiguration("target-media-flavor");

    if (sourceMediaFlavor == null) {
      throw new IllegalStateException("Source media flavor must be specified.");
    }
    if (sourceCaptionsFlavor == null) {
      throw new IllegalStateException("Source captions flavor must be specified");
    }

    // get all qt files
    Track[] qtTracks = getQuickTimeTracks(mp, MediaPackageElementFlavor.parseFlavor(sourceMediaFlavor));
    if (qtTracks.length == 0) {
      logger.info("Skipping embedding: No suitable QuickTime files were found.");
      return createResult(mp, Action.CONTINUE, 0);
    }

    // get and convert all matching caption files
    Catalog[] convertedCaptions = convertCaptions(mp, MediaPackageElementFlavor.parseFlavor(sourceCaptionsFlavor),
            "subrip");
    if (convertedCaptions.length == 0) {
      logger.info("Skipping embedding: No SRT captions were produced after conversion.");
      return createResult(mp, Action.CONTINUE, 0);
    }

    // perform embedding, start all jobs at once
    long totalTimeInQueue = 0;
    Map<Track, Job> jobs = new HashMap<Track, Job>();
    for (Track t : qtTracks) {
      Job job = composerService.captions(t, convertedCaptions);
      jobs.put(t, job);
    }

    // Wait until all embedding jobs have returned
    JobBarrier.Result embeddingResult = waitForStatus(jobs.values().toArray(new Job[jobs.size()]));
    if (!embeddingResult.isSuccess()) {
      throw new WorkflowOperationException("Encoding failed");
    }

    // Process the results
    for (Map.Entry<Track, Job> entry : jobs.entrySet()) {
      Track t = entry.getKey();
      Job job = entry.getValue();
      Track processedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
      if (targetMediaFlavor != null) {
        processedTrack.setFlavor(MediaPackageElementFlavor.parseFlavor(targetMediaFlavor));
      }

      // add this receipt's queue time to the total
      totalTimeInQueue += job.getQueueTime();

      // add to media package
      mp.addDerived(processedTrack, t);
      String fileName = getFileNameFromElements(t, processedTrack);
      processedTrack.setURI(workspace.moveTo(processedTrack.getURI(), mp.getIdentifier().toString(),
              processedTrack.getIdentifier(), fileName));
    }

    return createResult(mp, Action.CONTINUE, totalTimeInQueue);
  }

  /**
   * Searches for QuickTime files with specified flavor.
   *
   * @param mediaPackage
   *          media package to be searched
   * @param flavor
   *          track flavor to be searched for
   * @return array of suitable tracks
   */
  private Track[] getQuickTimeTracks(MediaPackage mediaPackage, MediaPackageElementFlavor flavor) {
    Track[] tracks = mediaPackage.getTracks(flavor);
    List<Track> qtTrackList = new LinkedList<Track>();
    for (Track t : tracks) {
      if (t.getMimeType().isEquivalentTo("video", "quicktime") && t.hasVideo()) {
        qtTrackList.add(t);
      }
    }
    return qtTrackList.toArray(new Track[qtTrackList.size()]);
  }

  /**
   * Searches for specified caption catalogs and converts them to output format. Given media package is also updated
   * with converted captions.
   *
   * @param mediaPackage
   *          media package to be searched
   * @param flavor
   *          captions flavor
   * @param outputFormat
   *          captions output format
   * @return array of converted captions
   * @throws UnsupportedCaptionFormatException
   *           if input or output type is not supported
   * @throws CaptionConverterException
   *           if conversion fails
   * @throws WorkflowOperationException
   *           if conversion fails
   * @throws NotFoundException
   *           if captions cannot be found
   * @throws IOException
   *           if exception occured while reading captions
   */
  private Catalog[] convertCaptions(MediaPackage mediaPackage, MediaPackageElementFlavor flavor, String outputFormat)
          throws UnsupportedCaptionFormatException, CaptionConverterException, WorkflowOperationException,
          NotFoundException, MediaPackageException, IOException {

    // get all matching catalogs
    Catalog[] captions = mediaPackage.getCatalogs(flavor);
    if (captions.length == 0) {
      logger.info("No suitable captions found for conversion.");
      return new Catalog[0];
    }

    List<Catalog> convertedCaptions = new LinkedList<Catalog>();
    Set<String> captionLanguages = new HashSet<String>();

    for (Catalog caption : captions) {
      String[] languages = captionService.getLanguageList(caption, flavor.getSubtype());
      if (languages.length == 0) {
        // TODO look for already present language tags
        logger.warn("No language information stored for catalog {}", caption);
      }
      for (String language : languages) {
        if (!captionLanguages.contains(language)) {
          Job job = captionService.convert(caption, flavor.getSubtype(), outputFormat, language);
          if (!waitForStatus(job).isSuccess()) {
            throw new WorkflowOperationException("Caption converting failed.");
          }
          Catalog convertedCaption = (Catalog) MediaPackageElementParser.getFromXml(job.getPayload());
          // add to media package
          mediaPackage.addDerived(convertedCaption, caption);
          String fileName = getFileNameFromElements(caption, convertedCaption);
          convertedCaption.setURI(workspace.moveTo(convertedCaption.getURI(), mediaPackage.getIdentifier().toString(),
                  convertedCaption.getIdentifier(), fileName));
          convertedCaptions.add(convertedCaption);
        } else {
          logger.warn("Language {} already processed.");
        }
      }
    }

    return convertedCaptions.toArray(new Catalog[convertedCaptions.size()]);
  }
}
