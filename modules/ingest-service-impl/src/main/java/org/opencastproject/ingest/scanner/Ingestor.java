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

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
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
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Used by the {@link InboxScannerService} to do the actual ingest. */
public class Ingestor implements Runnable {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(Ingestor.class);

  public static final String WFR_COLLECTION = "inbox";

  private final IngestService ingestService;

  private final SecurityContext secCtx;

  private final String workflowDefinition;

  private Map<String, String> workflowConfig;

  private final MediaPackageElementFlavor mediaFlavor;

  private final File inbox;

  private final SeriesService seriesService;

  private final int maxTries;

  private final int secondsBetweenTries;

  private RateLimiter throttle = RateLimiter.create(1.0);

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
                      artifact.getName(), workflowInstance.getMediaPackage().getIdentifier().compact(),
                      workflowInstance.getId());
            } else {
              /* Create MediaPackage and add Track */
              MediaPackage mp = ingestService.createMediaPackage();
              logger.info("Start ingest track from file {} to mediapackage {}",
                      artifact.getName(), mp.getIdentifier().compact());

              DublinCoreCatalog dcc = DublinCores.mkOpencastEpisode().getCatalog();
              dcc.add(DublinCore.PROPERTY_TITLE, artifact.getName());

              /* Check if we have a subdir and if its name matches an existing series */
              File dir = artifact.getParentFile();
              String seriesID;
              if (FileUtils.directoryContains(inbox, dir)) {
                /* cut away inbox path and trailing slash from artifact path */
                seriesID = dir.getName();
                if (seriesService.getSeries(seriesID) != null) {
                  logger.info("Ingest from inbox into series with id {}", seriesID);
                  dcc.add(DublinCore.PROPERTY_IS_PART_OF, seriesID);
                }
              }

              if (logger.isDebugEnabled()) {
                logger.debug("episode dublincore for the inbox file {}: {}", artifact.getName(), dcc.toXml());
              }

              try (ByteArrayOutputStream dcout = new ByteArrayOutputStream()) {
                dcc.toXml(dcout, true);
                try (InputStream dcin = new ByteArrayInputStream(dcout.toByteArray())) {
                  mp = ingestService.addCatalog(dcin, "dublincore.xml", MediaPackageElements.EPISODE, mp);
                  logger.info("Added DC catalog to media package for ingest from inbox");
                }
              }
              /* Ingest media*/
              mp = ingestService.addTrack(in, artifact.getName(), mediaFlavor, mp);
              logger.info("Ingested track from file {} to mediapackage {}",
                      artifact.getName(), mp.getIdentifier().compact());
              /* Ingest mediapackage */
              WorkflowInstance workflowInstance = ingestService.ingest(mp, workflowDefinition, workflowConfig);
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
   * @param maxTries              maximum tries for a ingest job
   */
  public Ingestor(IngestService ingestService, SecurityContext secCtx,
          String workflowDefinition, Map<String, String> workflowConfig, String mediaFlavor, File inbox, int maxThreads,
          SeriesService seriesService, int maxTries, int secondsBetweenTries) {
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
