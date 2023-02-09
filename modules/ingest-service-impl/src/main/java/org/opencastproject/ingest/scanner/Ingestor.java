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
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workspace.api.Workspace;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
  private final Workspace workspace;

  private final int maxTries;

  private final int secondsBetweenTries;

  private RateLimiter throttle = RateLimiter.create(1.0);

  private final Optional<Pattern> metadataPattern;
  private final DateTimeFormatter dateFormatter;
  private final String ffprobe;

  private final Gson gson = new Gson();

  private final boolean matchSchedule;
  private final float matchThreshold;

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
              Float duration = null;
              if (metadataPattern.isPresent()) {
                var matcher = metadataPattern.get().matcher(artifact.getName());
                if (matcher.find()) {
                  try {
                    title = matcher.group("title");
                  } catch (IllegalArgumentException e) {
                    logger.debug("{} matches no 'title' in {}", metadataPattern.get(), artifact.getName(), e);
                  }
                  try {
                    spatial = matcher.group("spatial");
                  } catch (IllegalArgumentException e) {
                    logger.debug("{} matches no 'spatial' in {}", metadataPattern.get(), artifact.getName(), e);
                  }
                  try {
                    var value = matcher.group("created");
                    logger.debug("Trying to parse matched date '{}' with formatter {}", value, dateFormatter);
                    created = Timestamp.valueOf(LocalDateTime.parse(value, dateFormatter));
                  } catch (DateTimeParseException e) {
                    logger.warn("Matched date does not match configured date-time format", e);
                  } catch (IllegalArgumentException e) {
                    logger.debug("{} matches no 'created' in {}", metadataPattern.get(), artifact.getName(), e);
                  }
                } else {
                  logger.debug("Regular expression {} does not match {}", metadataPattern.get(), artifact.getName());
                }
              }

              // Try extracting additional metadata via ffprobe
              if (ffprobe != null) {
                JsonFormat json = probeMedia(artifact.getAbsolutePath()).format;
                created = json.tags.getCreationTime() == null ? created : json.tags.getCreationTime();
                duration = json.getDuration();
                logger.debug("Extracted metadata from file: {}", json);
              }

              MediaPackage mediaPackage = null;
              var currentWorkflowDefinition = workflowDefinition;
              var currentWorkflowConfig = workflowConfig;

              // Check if we can match this to a scheduled event
              if (matchSchedule && spatial != null && created != null) {
                logger.debug("Try finding scheduled event for agent {} at time {}", spatial, created);
                var end = duration == null ? created : DateUtils.addSeconds(created, duration.intValue());
                var mediaPackages = schedulerService.findConflictingEvents(spatial, created, end);
                if (matchThreshold > 0F && mediaPackages.size() > 1) {
                  var filteredMediaPackages = new ArrayList<MediaPackage>();
                  for (var mp : mediaPackages) {
                    var schedule =  schedulerService.getTechnicalMetadata(mp.getIdentifier().toString());
                    if (overlap(schedule.getStartDate(), schedule.getEndDate(), created, end) > matchThreshold) {
                      filteredMediaPackages.add(mp);
                    }
                  }
                  mediaPackages = filteredMediaPackages;
                }
                if (mediaPackages.size() > 1) {
                  logger.warn("Metadata match multiple events. Not using any!");
                } else if (mediaPackages.size() == 1) {
                  mediaPackage = mediaPackages.get(0);
                  var id = mediaPackage.getIdentifier().toString();
                  var eventConfiguration = schedulerService.getCaptureAgentConfiguration(id);

                  // Check if the scheduled event already has a recording associated with it
                  // If so, ingest the file as a new event
                  try {
                    Recording recordingState = schedulerService.getRecordingState(id);
                    if (recordingState.getState().equals(UPLOAD_FINISHED)) {
                      var referenceId = mediaPackage.getIdentifier().toString();
                      mediaPackage = (MediaPackage) mediaPackage.clone();
                      mediaPackage.setIdentifier(IdImpl.fromUUID());

                      // Update dublincore title and set reference to originally scheduled event
                      try {
                        DublinCoreCatalog dc = DublinCoreUtil.loadEpisodeDublinCore(workspace, mediaPackage).get();
                        var newTitle = dc.get(DublinCore.PROPERTY_TITLE).get(0).getValue()
                                + " (" + Instant.now().getEpochSecond() + ")";
                        dc.set(DublinCore.PROPERTY_TITLE, newTitle);
                        dc.set(DublinCore.PROPERTY_REFERENCES, referenceId);
                        mediaPackage = updateDublincCoreCatalog(mediaPackage, dc);
                        mediaPackage.setTitle(newTitle);
                      } catch (Exception e) {
                        // Don't fail the ingest if we could not set metadata for some reason
                      }
                    }
                  } catch (NotFoundException e) {
                    // Occurs if a scheduled event has not started yet
                  }

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

    private JsonFFprobe probeMedia(final String file) throws IOException {

      final String[] command = new String[] {
              ffprobe,
              "-show_format",
              "-of",
              "json",
              file
      };

      // Execute ffprobe and obtain the result
      logger.debug("Running ffprobe: {}", (Object) command);

      String output;
      Process process = null;
      try {
        process = new ProcessBuilder(command)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();

        try (InputStream in = process.getInputStream()) {
          output = IOUtils.toString(in, StandardCharsets.UTF_8);
        }

        if (process.waitFor() != 0) {
          throw new IOException("FFprobe exited abnormally");
        }
      } catch (InterruptedException e) {
        throw new IOException(e);
      } finally {
        IoSupport.closeQuietly(process);
      }

      return gson.fromJson(output, JsonFFprobe.class);
    }

    /**
     * Calculate the overlap of two events `a` and `b`.
     * @param aStart Begin of event a
     * @param aEnd End of event a
     * @param bStart Begin of event b
     * @param bEnd End of event b
     * @return How much of `a` overlaps with `n`. Return a float in the range of <pre>[0.0, 1.0]</pre>.
     */
    private float overlap(Date aStart, Date aEnd, Date bStart, Date bEnd) {
      var min = Math.min(aStart.getTime(), bStart.getTime());
      var max = Math.max(aEnd.getTime(), bEnd.getTime());
      var aLen = aEnd.getTime() - aStart.getTime();
      var bLen = bEnd.getTime() - bStart.getTime();
      var overlap =  aLen + bLen - (max - min);
      logger.debug("Detected overlap of {} ({})", overlap, overlap / (float) aLen);
      if (aLen == 0F) {
        return 1F;
      }
      if (overlap > 0F) {
        return overlap / (float) aLen;
      }
      return 0.0F;
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
          DateTimeFormatter dateFormatter, SchedulerService schedulerService, String ffprobe, boolean matchSchedule,
          float matchThreshold, Workspace workspace) {
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
    this.ffprobe = ffprobe;
    this.matchSchedule = matchSchedule;
    this.matchThreshold = matchThreshold;
    this.workspace = workspace;
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

  /**
   *
   * @param mp
   *          the mediapackage to update
   * @param dc
   *          the dublincore metadata to use to update the mediapackage
   * @return the updated mediapackage
   * @throws IOException
   *           Thrown if an IO error occurred adding the dc catalog file
   * @throws MediaPackageException
   *           Thrown if an error occurred updating the mediapackage or the mediapackage does not contain a catalog
   */
  private MediaPackage updateDublincCoreCatalog(MediaPackage mp, DublinCoreCatalog dc)
          throws IOException, MediaPackageException {
    try (InputStream inputStream = IOUtils.toInputStream(dc.toXmlString(), "UTF-8")) {
      // Update dublincore catalog
      Catalog[] catalogs = mp.getCatalogs(MediaPackageElements.EPISODE);
      if (catalogs.length > 0) {
        Catalog catalog = catalogs[0];
        URI uri = workspace.put(mp.getIdentifier().toString(), catalog.getIdentifier(), "dublincore.xml", inputStream);
        catalog.setURI(uri);
        // setting the URI to a new source so the checksum will most like be invalid
        catalog.setChecksum(null);
      } else {
        throw new MediaPackageException("Unable to find catalog");
      }
    }
    return mp;
  }

  public String myInfo() {
    return format("[%x thread=%x]", hashCode(), Thread.currentThread().getId());
  }

  class JsonFFprobe {
    protected JsonFormat format;
  }

  class JsonFormat {
    private String duration;
    protected JsonTags tags;

    Float getDuration() {
      return duration == null ? null : Float.parseFloat(duration);
    }

    @Override
    public String toString() {
      return String.format("{duration=%s,tags=%s}", duration, tags);
    }
  }

  class JsonTags {
    @SerializedName(value = "creation_time")
    private String creationTime;

    Date getCreationTime() throws ParseException {
      if (creationTime == null) {
        return  null;
      }
      DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
      return format.parse(creationTime.replaceAll("000Z$", "+0000"));
    }

    @Override
    public String toString() {
      return String.format("{creation_time=%s}", creationTime);
    }
  }
}
