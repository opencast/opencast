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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.ingest.scanner

import java.lang.String.format

import org.opencastproject.ingest.api.IngestService
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.security.util.SecurityContext
import org.opencastproject.series.api.SeriesService
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workingfilerepository.api.WorkingFileRepository

import com.google.common.util.concurrent.RateLimiter

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.CompletionService
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Used by the [InboxScannerService] to do the actual ingest.  */
class Ingestor
/**
 * Create new ingestor.
 *
 * @param ingestService         media packages are passed to the ingest service
 * @param workingFileRepository inbox files are put in the working file repository collection [.WFR_COLLECTION].
 * @param secCtx                security context needed for ingesting with the IngestService or for putting files into the working file
 * repository
 * @param workflowDefinition    workflow to apply to ingested media packages
 * @param workflowConfig        the workflow definition configuration
 * @param mediaFlavor           media flavor to use by default
 * @param inbox                 inbox directory to watch
 * @param maxThreads            maximum worker threads doing the actual ingest
 * @param maxTries              maximum tries for a ingest job
 */
(private val ingestService: IngestService, workingFileRepository: WorkingFileRepository, private val secCtx: SecurityContext,
 private val workflowDefinition: String, private val workflowConfig: Map<String, String>, mediaFlavor: String, private val inbox: File, maxThreads: Int,
 private val seriesService: SeriesService, private val maxTries: Int, private val secondsBetweenTries: Int) : Runnable {

    private val mediaFlavor: MediaPackageElementFlavor

    private val throttle = RateLimiter.create(1.0)

    /**
     * Thread pool to run the ingest worker.
     */
    private val executorService: ExecutorService

    /**
     * Completion service to manage internal completion queue of ingest jobs including retries
     */
    private val completionService: CompletionService<RetriableIngestJob>


    private inner class RetriableIngestJob internal constructor(val artifact: File, secondsBetweenTries: Int) : Callable<RetriableIngestJob> {
        var retryCount: Int = 0
            private set
        private var failed: Boolean = false
        private val throttle: RateLimiter

        init {
            this.retryCount = 0
            this.failed = false
            throttle = RateLimiter.create(1.0 / secondsBetweenTries)
        }

        fun hasFailed(): Boolean {
            return this.failed
        }

        override fun call(): RetriableIngestJob {
            return secCtx.runInContext<RetriableIngestJob>({
                if (hasFailed()) {
                    logger.warn("This is retry number {} for file {}. We will wait for {} seconds before trying again",
                            retryCount, artifact.name, secondsBetweenTries)
                    throttle.acquire()
                }
                try {
                    FileInputStream(artifact).use { `in` ->
                        failed = false
                        ++retryCount
                        if ("zip".equals(FilenameUtils.getExtension(artifact.name), ignoreCase = true)) {
                            logger.info("Start ingest inbox file {} as a zipped mediapackage", artifact.name)
                            val workflowInstance = ingestService.addZippedMediaPackage(`in`, workflowDefinition, workflowConfig)
                            logger.info("Ingested {} as a zipped mediapackage from inbox as {}. Started workflow {}.",
                                    artifact.name, workflowInstance.mediaPackage.identifier.compact(),
                                    workflowInstance.id)
                        } else {
                            /* Create MediaPackage and add Track */
                            var mp = ingestService.createMediaPackage()
                            logger.info("Start ingest track from file {} to mediapackage {}",
                                    artifact.name, mp.identifier.compact())

                            val dcc = DublinCores.mkOpencastEpisode().catalog
                            dcc.add(DublinCore.PROPERTY_TITLE, artifact.name)

                            /* Check if we have a subdir and if its name matches an existing series */
                            val dir = artifact.parentFile
                            val seriesID: String
                            if (FileUtils.directoryContains(inbox, dir)) {
                                /* cut away inbox path and trailing slash from artifact path */
                                seriesID = dir.name
                                if (seriesService.getSeries(seriesID) != null) {
                                    logger.info("Ingest from inbox into series with id {}", seriesID)
                                    dcc.add(DublinCore.PROPERTY_IS_PART_OF, seriesID)
                                }
                            }

                            if (logger.isDebugEnabled) {
                                logger.debug("episode dublincore for the inbox file {}: {}", artifact.name, dcc.toXml())
                            }

                            ByteArrayOutputStream().use { dcout ->
                                dcc.toXml(dcout, true)
                                ByteArrayInputStream(dcout.toByteArray()).use { dcin ->
                                    mp = ingestService.addCatalog(dcin, "dublincore.xml", MediaPackageElements.EPISODE, mp)
                                    logger.info("Added DC catalog to media package for ingest from inbox")
                                }
                            }
                            /* Ingest media*/
                            mp = ingestService.addTrack(`in`, artifact.name, mediaFlavor, mp)
                            logger.info("Ingested track from file {} to mediapackage {}",
                                    artifact.name, mp.identifier.compact())
                            /* Ingest mediapackage */
                            val workflowInstance = ingestService.ingest(mp, workflowDefinition, workflowConfig)
                            logger.info("Ingested {} from inbox, workflow {} started", artifact.name, workflowInstance.id)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error ingesting inbox file {}", artifact.name, e)
                    failed = true
                    return@secCtx.runInContext this@RetriableIngestJob
                }

                try {
                    FileUtils.forceDelete(artifact)
                } catch (e: IOException) {
                    logger.error("Unable to delete file {}", artifact.absolutePath, e)
                }

                this@RetriableIngestJob
            })
        }
    }

    override fun run() {
        while (true) {
            try {
                val f = completionService.take()
                val task = f.get()
                if (task.hasFailed()) {
                    if (task.retryCount < maxTries) {
                        throttle.acquire()
                        logger.warn("Retrying inbox ingest of {}", task.artifact.absolutePath)
                        completionService.submit(task)
                    } else {
                        logger.error("Inbox ingest failed after {} tries for {}", maxTries, task.artifact.absolutePath)
                    }
                }
            } catch (e: InterruptedException) {
                logger.debug("Ingestor check interrupted", e)
                return
            } catch (e: ExecutionException) {
                logger.error("Ingestor check interrupted", e)
            }

        }
    }

    init {
        this.mediaFlavor = MediaPackageElementFlavor.parseFlavor(mediaFlavor)
        this.executorService = Executors.newFixedThreadPool(maxThreads)
        this.completionService = ExecutorCompletionService(executorService)
    }

    /**
     * Asynchronous ingest of an artifact.
     */
    fun ingest(artifact: File) {
        logger.info("Try ingest of file {}", artifact.name)
        completionService.submit(RetriableIngestJob(artifact, secondsBetweenTries))
    }

    /**
     * Return true if the passed artifact can be handled by this ingestor,
     * false if not (e.g. it lies outside of inbox or its name starts with a ".")
     */
    fun canHandle(artifact: File): Boolean {
        logger.trace("canHandle() {}, {}", myInfo(), artifact.absolutePath)
        val dir = artifact.parentFile
        try {
            /* Stop if dir is empty, stop if artifact is dotfile, stop if artifact lives outside of inbox path */
            return (dir != null && !artifact.name.startsWith(".")
                    && FileUtils.directoryContains(inbox, artifact)
                    && artifact.canRead() && artifact.length() > 0)
        } catch (e: IOException) {
            logger.warn("Unable to determine canonical path of {}", artifact.absolutePath, e)
            return false
        }

    }

    fun cleanup(artifact: File) {
        try {
            val parentDir = artifact.parentFile
            if (FileUtils.directoryContains(inbox, parentDir)) {
                val filesList = parentDir.list()
                if (filesList == null || filesList.size == 0) {
                    logger.info("Delete empty inbox for series {}",
                            StringUtils.substring(parentDir.canonicalPath, inbox.canonicalPath.length + 1))
                    FileUtils.deleteDirectory(parentDir)
                }
            }
        } catch (e: Exception) {
            logger.error("Unable to cleanup inbox for the artifact {}", artifact, e)
        }

    }

    fun myInfo(): String {
        return format("[%x thread=%x]", hashCode(), Thread.currentThread().id)
    }

    companion object {

        /**
         * The logger
         */
        private val logger = LoggerFactory.getLogger(Ingestor::class.java)

        val WFR_COLLECTION = "inbox"
    }
}
