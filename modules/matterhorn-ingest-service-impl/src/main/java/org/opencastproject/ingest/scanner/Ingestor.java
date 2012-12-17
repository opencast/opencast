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
package org.opencastproject.ingest.scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static org.opencastproject.util.IoSupport.fileInputStream;
import static org.opencastproject.util.IoSupport.withStream;
import static org.opencastproject.util.data.Collections.append;

/** Used by the {@link InboxScannerService} to do the actual ingest. */
public class Ingestor {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(Ingestor.class);

  public static final String WFR_COLLECTION = "inbox";

  /** The working file repository, we deliberately don't use the Workspace! */
  private final WorkingFileRepository workingFileRepository;

  private final IngestService ingestService;

  private final SecurityContext secCtx;

  private final String workflowDefinition;

  private final File inbox;

  /** Thread pool to run the ingest worker. */
  private final ExecutorService executorService;

  /**
   * Create new ingestor.
   *
   * @param ingestService
   *         media packages are passed to the ingest service
   * @param workingFileRepository
   *         inbox files are put in the working file repository collection {@link #WFR_COLLECTION}.
   * @param secCtx
   *         security context needed for ingesting with the IngestService or for putting files
   *         into the working file repository
   * @param workflowDefinition
   *         workflow to apply to ingested media packages
   * @param inbox
   *         inbox directory to watch
   * @param maxThreads
   *         maximum worker threads doing the actual ingest
   */
  public Ingestor(IngestService ingestService,
                  WorkingFileRepository workingFileRepository,
                  SecurityContext secCtx,
                  String workflowDefinition,
                  File inbox,
                  int maxThreads) {
    this.workingFileRepository = workingFileRepository;
    this.ingestService = ingestService;
    this.secCtx = secCtx;
    this.workflowDefinition = workflowDefinition;
    this.inbox = inbox;
    this.executorService = Executors.newFixedThreadPool(maxThreads);
  }

  /** Asynchronous ingest of an artifact. */
  public void ingest(final File artifact) {
    logger.info("Install {} {}", myInfo(), artifact.getName());
    executorService.execute(getIngestRunnable(artifact));
  }

  private Runnable getIngestRunnable(final File artifact) {
    return new Runnable() {
      @Override
      public void run() {
        secCtx.runInContext(new Effect0() {
          @Override
          protected void run() {
            boolean ignore = "zip".equalsIgnoreCase(FilenameUtils.getExtension(artifact.getName()))
                    &&
                    withStream(fileInputStream(artifact),
                               logWarn("Unable to ingest mediapackage '{}', {}", artifact.getAbsolutePath()),
                               new Function.X<InputStream, Boolean>() {
                                 @Override
                                 public Boolean xapply(InputStream in) throws Exception {
                                   ingestService.addZippedMediaPackage(in, workflowDefinition);
                                   logger.info("Ingested {} as a mediapackage", artifact.getAbsolutePath());
                                   return true;
                                 }
                               }).isRight()
                    ||
                    withStream(fileInputStream(artifact),
                               logWarn("Unable to process inbox file '{}', {}", artifact.getAbsolutePath()),
                               new Function.X<InputStream, Boolean>() {
                                 @Override
                                 public Boolean xapply(InputStream in) throws Exception {
                                   workingFileRepository.putInCollection(WFR_COLLECTION, artifact.getName(), in);
                                   logger.info("Ingested {} as an inbox file", artifact.getAbsolutePath());
                                   return true;
                                 }
                               }).isRight();
            try {
              FileUtils.forceDelete(artifact);
            } catch (IOException e) {
              logger.warn("Unable to delete file {}, {}", artifact.getAbsolutePath(), e);
            }
          }
        });
      }
    };
  }

  /** Return true if the passed artifact can be handled by this ingestor, i.e. it lies in its inbox. */
  public boolean canHandle(final File artifact) {
    logger.debug("CanHandle {}, {}", myInfo(), artifact.getAbsolutePath());
    File dir = artifact.getParentFile();
    try {
      return dir != null && inbox.getCanonicalPath().equals(dir.getCanonicalPath());
    } catch (IOException e) {
      logger.warn("Unable to determine canonical path of {} ", artifact.getAbsolutePath());
      return false;
    }
  }

  public String myInfo() {
    return format("[%x thread=%x]", hashCode(), Thread.currentThread().getId());
  }

  /**
   * Create a function that logs a warning.
   *
   * @param msg
   *         a message string to be used with {@link org.slf4j.Logger}
   * @param args
   *         args for the Logger. The function argument (the exception) will be the last arg.
   */
  public static Function<Exception, Exception> logWarn(final String msg, final Object... args) {
    return new Function<Exception, Exception>() {
      @Override
      public Exception apply(Exception e) {
        logger.warn(msg, append(args, e.getMessage()));
        return e;
      }
    };
  }
}
