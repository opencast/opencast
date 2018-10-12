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
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Used by the {@link InboxScannerService} to do the actual ingest. */
public class Ingestor {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(Ingestor.class);

  public static final String WFR_COLLECTION = "inbox";

  private final IngestService ingestService;

  private final SecurityContext secCtx;

  private final String workflowDefinition;

  private Map<String, String> workflowConfig;

  private final MediaPackageElementFlavor mediaFlavor;

  private final File inbox;

  /** Thread pool to run the ingest worker. */
  private final ExecutorService executorService;

  /**
   * Create new ingestor.
   *
   * @param ingestService
   *          media packages are passed to the ingest service
   * @param workingFileRepository
   *          inbox files are put in the working file repository collection {@link #WFR_COLLECTION}.
   * @param secCtx
   *          security context needed for ingesting with the IngestService or for putting files into the working file
   *          repository
   * @param workflowDefinition
   *          workflow to apply to ingested media packages
   * @param workflowConfig
   *          the workflow definition configuration
   * @param mediaFlavor
   *          media flavor to use by default
   * @param inbox
   *          inbox directory to watch
   * @param maxThreads
   *          maximum worker threads doing the actual ingest
   */
  public Ingestor(IngestService ingestService, WorkingFileRepository workingFileRepository, SecurityContext secCtx,
      String workflowDefinition, Map<String, String> workflowConfig, String mediaFlavor, File inbox, int maxThreads) {
    this.ingestService = ingestService;
    this.secCtx = secCtx;
    this.workflowDefinition = workflowDefinition;
    this.workflowConfig = workflowConfig;
    this.mediaFlavor = MediaPackageElementFlavor.parseFlavor(mediaFlavor);
    this.inbox = inbox;
    this.executorService = Executors.newFixedThreadPool(maxThreads);
  }

  /** Asynchronous ingest of an artifact. */
  public void ingest(final File artifact) {
    logger.info("Try ingest of file `{}`", artifact.getName());
    executorService.execute(getIngestRunnable(artifact));
  }

  private Runnable getIngestRunnable(final File artifact) {
    return () -> secCtx.runInContext(() -> {
      try (InputStream in = new FileInputStream(artifact)) {
        if ("zip".equalsIgnoreCase(FilenameUtils.getExtension(artifact.getName()))) {
          ingestService.addZippedMediaPackage(in, workflowDefinition, workflowConfig);
          logger.info("Ingested {} as a mediapackage from inbox", artifact.getName());
        } else {
          /* Create MediaPackage and add Track */
          MediaPackage mp = ingestService.createMediaPackage();
          ingestService.addTrack(in, artifact.getName(), mediaFlavor, mp);
          logger.info("Added track to mediapackage for ingest from inbox");

          /* Add title */
          DublinCoreCatalog dcc = DublinCores.mkOpencastEpisode().getCatalog();
          dcc.add(DublinCore.PROPERTY_TITLE, artifact.getName());
          ByteArrayOutputStream dcout = new ByteArrayOutputStream();
          dcc.toXml(dcout, true);
          InputStream dcin = new ByteArrayInputStream(dcout.toByteArray());
          ingestService.addCatalog(dcin, "dublincore.xml", MediaPackageElements.EPISODE, mp);
          logger.info("Added DC catalog to mediapackage for ingest from inbox");

          /* Ingest media */
          ingestService.ingest(mp, workflowDefinition, workflowConfig);
          logger.info("Ingested {} from inbox", artifact.getName());
        }
      } catch (IOException e) {
        logger.error("Error accessing inbox file '{}'", artifact.getName(), e);
      } catch (Exception e) {
        logger.error("Error ingesting inbox file '{}'", artifact.getName(), e);
      }
      try {
        FileUtils.forceDelete(artifact);
      } catch (IOException e) {
        logger.error("Unable to delete file {}", artifact.getAbsolutePath(), e);
      }
    });
  }

  /** Return true if the passed artifact can be handled by this ingestor, i.e. it lies in its inbox and its name does
   * not start with a ".". */
  public boolean canHandle(final File artifact) {
    logger.debug("CanHandle {}, {}", myInfo(), artifact.getAbsolutePath());
    File dir = artifact.getParentFile();
    try {
      return dir != null
        && inbox.getCanonicalPath().equals(dir.getCanonicalPath())
        && !artifact.getName().startsWith(".");
    } catch (IOException e) {
      logger.warn("Unable to determine canonical path of {} ", artifact.getAbsolutePath());
      return false;
    }
  }

  public String myInfo() {
    return format("[%x thread=%x]", hashCode(), Thread.currentThread().getId());
  }
}
