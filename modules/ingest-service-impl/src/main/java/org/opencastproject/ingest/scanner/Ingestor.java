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

package org.opencastproject.ingest.scanner;

import static java.lang.String.format;
import static org.opencastproject.scheduler.api.RecordingState.UPLOAD_FINISHED;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.workflow.api.WorkflowInstance;

import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Used by the {@link InboxScannerService} to do the actual ingest. */
public class Ingestor implements Runnable {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(Ingestor.class);

  private final IngestService ingestService;

  private final SecurityContext secCtx;

  private final String workflowDefinition;

  private final Map<String, String> workflowConfig;

  private final MediaPackageElementFlavor mediaFlavor;

  private final File inbox;

  private final SeriesService seriesService;
  private final SchedulerService schedulerService;

  private final int maxTries;

  private final int secondsBetweenTries;

  private RateLimiter throttle = RateLimiter.create(1.0);

  private final Optional<Pattern> metadataPattern;
  private final DateTimeFormatter dateFormatter;

  private final boolean matchSchedule;

  /**
   * Thread pool to run the ingest worker.
   */
  private final ExecutorService executorService;

  /**
   * Completion service to manage internal completion queue of ingest jobs including retries
   */
  private final CompletionService<RetriableIngestJob> completionService;


  private class RetriableIngestJob implements Callable<RetriableIngestJob> {
    private final File artifact;
    private int retryCount;
    private boolean failed;
    private RateLimiter throttle;

    RetriableIngestJob(final File artifact, int secondsBetweenTries) {
      this.artifact = artifact;
      this.retryCount = 0;
      this.failed = false;
      throttle = RateLimiter.create(1.0 / secondsBetweenTries);
    }

    public boolean hasFailed() {
      return this.failed;
    }

    public int getRetryCount() {
      return this.retryCount;
    }

    public File getArtifact() {
      return this.artifact;
    }

    @Override
    public RetriableIngestJob call() {
      return secCtx.runInContext(() -> {
          if (hasFailed()) {
            logger.warn("This is retry number {} for file {}. We will wait for {} seconds before trying again",
                    retryCount, artifact.getName(), secondsBetweenTries);
            throttle.acquire();
          }
          try (InputStream in = new FileInputStream(artifact)) {
            failed = false;
            ++retryCount;
            if ("zip".equalsIgnoreCase(FilenameUtils.getExtension(artifact.getName()))) {
              logger.info("Start ingest inbox file {} as a zipped mediapackage", artifact.getName());
              WorkflowInstance workflowInstance = ingestService.addZippedMediaPackage(in, workflowDefinition, workflowConfig);
              logger.info("Ingested {} as a zipped mediapackage from inbox as {}. Started workflow {}.",
                      artifact.getName(), workflowInstance.getMediaPackage().getIdentifier().toString(),
                      workflowInstance.getId());
            } else {
              /* Create MediaPackage and add Track */
              logger.info("Start ingest track from file {}", artifact.getName());

              // Try extracting metadata from the file name and path
              String title = artifact.getName();
              String spatial = null;
              Date created = null;
              if (metadataPattern.isPresent()) {
                var matcher = metadataPattern.get().matcher(artifact.getName());
                if (matcher.find()) {
                  try {
                    title = matcher.group("title");
                  } catch (IllegalArgumentException e) {
                    logger.debug("{} matches no title in {}", metadataPattern.get(), artifact.getName(), e);
                  }
                  try {
                    spatial = matcher.group("spatial");
                  } catch (IllegalArgumentException e) {
                    logger.debug("{} matches no spatial in {}", metadataPattern.get(), artifact.getName(), e);
                  }
                  try {
                    var value = matcher.group("created");
                    logger.debug("Trying to parse matched date '{}' with formatter {}", value, dateFormatter);
                    created = Timestamp.valueOf(LocalDateTime.parse(value, dateFormatter));
                  } catch (DateTimeParseException e) {
                    logger.warn("Matched date does not match configured date-time format", e);
                  } catch (IllegalArgumentException e) {
                    logger.debug("{} matches no spatial in {}", metadataPattern, artifact.getName(), e);
                  }
                } else {
                  logger.debug("Regular expression {} does not match {}", metadataPattern.get(), artifact.getName());
                }
              }

              MediaPackage mediaPackage = null;
              var currentWorkflowDefinition = workflowDefinition;
              var currentWorkflowConfig = workflowConfig;

              // Check if we can match this to a scheduled event
              if (matchSchedule && spatial != null && created != null) {
                logger.debug("Try finding scheduled event for agent {} at time {}", spatial, created);
                var mediaPackages = schedulerService.findConflictingEvents(spatial, created, created);
                if (mediaPackages.size() > 1) {
                  logger.warn("Metadata match multiple events. Not using any!");
                } else if (mediaPackages.size() == 1) {
                  mediaPackage = mediaPackages.get(0);
                  var id = mediaPackage.getIdentifier().toString();
                  var eventConfiguration = schedulerService.getCaptureAgentConfiguration(id);
                  currentWorkflowDefinition = eventConfiguration.getOrDefault(
                          "org.opencastproject.workflow.definition",
                          workflowDefinition);
                  currentWorkflowConfig = eventConfiguration.entrySet().stream()
                          .filter(e -> e.getKey().startsWith("org.opencastproject.workflow.config."))
                          .collect(Collectors.toMap(e -> e.getKey().substring(36), Map.Entry::getValue));
                  schedulerService.updateRecordingState(id, UPLOAD_FINISHED);
                  logger.info("Found matching scheduled event {}", mediaPackage);
                } else {
                  logger.debug("No matching event found.");
                }
              }

              // create new media package and metadata catalog if we have none
              if (mediaPackage == null) {
                // create new media package
                mediaPackage = ingestService.createMediaPackage();

                DublinCoreCatalog dcc = DublinCores.mkOpencastEpisode().getCatalog();
                if (spatial != null) {
                  dcc.add(DublinCore.PROPERTY_SPATIAL, spatial);
                }
                if (created != null) {
                  dcc.add(DublinCore.PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(created, Precision.Second));
                }
                // fall back to filename for title if matcher did not catch any
                dcc.add(DublinCore.PROPERTY_TITLE, title);

                /* Check if we have a subdir and if its name matches an existing series */
                final File dir = artifact.getParentFile();
                if (FileUtils.directoryContains(inbox, dir)) {
                  /* cut away inbox path and trailing slash from artifact path */
                  var seriesID = dir.getName();
                  if (seriesService.getSeries(seriesID) != null) {
                    logger.info("Ingest from inbox into series with id {}", seriesID);
                    dcc.add(DublinCore.PROPERTY_IS_PART_OF, seriesID);
                  }
                }

                try (ByteArrayOutputStream dcout = new ByteArrayOutputStream()) {
                  dcc.toXml(dcout, true);
                  try (InputStream dcin = new ByteArrayInputStream(dcout.toByteArray())) {
                    mediaPackage = ingestService.addCatalog(dcin, "dublincore.xml", MediaPackageElements.EPISODE,
                            mediaPackage);
                    logger.info("Added DC catalog to media package for ingest from inbox");
                  }
                }
              }

              // Ingest media
              mediaPackage = ingestService.addTrack(in, artifact.getName(), mediaFlavor, mediaPackage);
              logger.info("Ingested track from file {} to media package {}",
                      artifact.getName(), mediaPackage.getIdentifier().toString());

              // Ingest media package
              WorkflowInstance workflowInstance = ingestService.ingest(mediaPackage, currentWorkflowDefinition,
                      currentWorkflowConfig);
              logger.info("Ingested {} from inbox, workflow {} started", artifact.getName(), workflowInstance.getId());
            }
          } catch (Exception e) {
            logger.error("Error ingesting inbox file {}", artifact.getName(), e);
            failed = true;
            return RetriableIngestJob.this;
          }
          try {
            FileUtils.forceDelete(artifact);
          } catch (IOException e) {
            logger.error("Unable to delete file {}", artifact.getAbsolutePath(), e);
          }
          return RetriableIngestJob.this;
      });
    }
  }

  @Override
  public void run() {
    while (true) {
      try {
        final Future<RetriableIngestJob> f = completionService.take();
        final RetriableIngestJob task = f.get();
        if (task.hasFailed()) {
          if (task.getRetryCount() < maxTries) {
            throttle.acquire();
            logger.warn("Retrying inbox ingest of {}", task.getArtifact().getAbsolutePath());
            completionService.submit(task);
          } else {
            logger.error("Inbox ingest failed after {} tries for {}", maxTries, task.getArtifact().getAbsolutePath());
          }
        }
      } catch (InterruptedException e) {
        logger.debug("Ingestor check interrupted", e);
        return;
      } catch (ExecutionException e) {
        logger.error("Ingestor check interrupted", e);
      }
    }
  }

  /**
   * Create new ingestor.
   *
   * @param ingestService         media packages are passed to the ingest service
   * @param secCtx                security context needed for ingesting with the IngestService or for putting files into the working file
   *                              repository
   * @param workflowDefinition    workflow to apply to ingested media packages
   * @param workflowConfig        the workflow definition configuration
   * @param mediaFlavor           media flavor to use by default
   * @param inbox                 inbox directory to watch
   * @param maxThreads            maximum worker threads doing the actual ingest
   * @param seriesService         reference to the active series service
   * @param maxTries              maximum tries for a ingest job
   * @param secondsBetweenTries   time between retires in seconds
   * @param metadataPattern       regular expression pattern for matching metadata in file names
   * @param dateFormatter         date formatter pattern for parsing temporal metadata
   */
  public Ingestor(IngestService ingestService, SecurityContext secCtx,
          String workflowDefinition, Map<String, String> workflowConfig, String mediaFlavor, File inbox, int maxThreads,
          SeriesService seriesService, int maxTries, int secondsBetweenTries, Optional<Pattern> metadataPattern,
          DateTimeFormatter dateFormatter, SchedulerService schedulerService, boolean matchSchedule) {
    this.ingestService = ingestService;
    this.secCtx = secCtx;
    this.workflowDefinition = workflowDefinition;
    this.workflowConfig = workflowConfig;
    this.mediaFlavor = MediaPackageElementFlavor.parseFlavor(mediaFlavor);
    this.inbox = inbox;
    this.executorService = Executors.newFixedThreadPool(maxThreads);
    this.completionService = new ExecutorCompletionService<>(executorService);
    this.seriesService = seriesService;
    this.maxTries = maxTries;
    this.secondsBetweenTries = secondsBetweenTries;
    this.metadataPattern = metadataPattern;
    this.dateFormatter = dateFormatter;
    this.schedulerService = schedulerService;
    this.matchSchedule = matchSchedule;
  }

  /**
   * Asynchronous ingest of an artifact.
   */
  public void ingest(final File artifact) {
    logger.info("Try ingest of file {}", artifact.getName());
    completionService.submit(new RetriableIngestJob(artifact, secondsBetweenTries));
  }

  /**
   * Return true if the passed artifact can be handled by this ingestor,
   * false if not (e.g. it lies outside of inbox or its name starts with a ".")
   */
  public boolean canHandle(final File artifact) {
    logger.trace("canHandle() {}, {}", myInfo(), artifact.getAbsolutePath());
    File dir = artifact.getParentFile();
    try {
      /* Stop if dir is empty, stop if artifact is dotfile, stop if artifact lives outside of inbox path */
      return dir != null && !artifact.getName().startsWith(".")
              && FileUtils.directoryContains(inbox, artifact)
              && artifact.canRead() && artifact.length() > 0;
    } catch (IOException e) {
      logger.warn("Unable to determine canonical path of {}", artifact.getAbsolutePath(), e);
      return false;
    }
  }

  public void cleanup(final File artifact) {
    try {
      File parentDir = artifact.getParentFile();
      if (FileUtils.directoryContains(inbox, parentDir)) {
        String[] filesList = parentDir.list();
        if (filesList == null || filesList.length == 0) {
          logger.info("Delete empty inbox for series {}",
                  StringUtils.substring(parentDir.getCanonicalPath(), inbox.getCanonicalPath().length() + 1));
          FileUtils.deleteDirectory(parentDir);
        }
      }
    } catch (Exception e) {
      logger.error("Unable to cleanup inbox for the artifact {}", artifact, e);
    }
  }

  public String myInfo() {
    return format("[%x thread=%x]", hashCode(), Thread.currentThread().getId());
  }
}
