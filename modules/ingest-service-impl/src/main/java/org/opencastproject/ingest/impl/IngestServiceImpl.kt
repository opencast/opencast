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

package org.opencastproject.ingest.impl

import org.apache.commons.lang3.StringUtils.isBlank
import org.opencastproject.util.JobUtil.waitForJob
import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Option.none

import org.opencastproject.capture.CaptureParameters
import org.opencastproject.ingest.api.IngestException
import org.opencastproject.ingest.api.IngestService
import org.opencastproject.ingest.impl.jmx.IngestStatistics
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.MediaPackageSupport
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl
import org.opencastproject.metadata.dublincore.DCMIPeriod
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.metadata.dublincore.DublinCoreValue
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils
import org.opencastproject.scheduler.api.SchedulerException
import org.opencastproject.scheduler.api.SchedulerService
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.security.util.StandAloneTrustedHttpClientImpl
import org.opencastproject.series.api.SeriesService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.smil.api.util.SmilUtil
import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.IoSupport
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.ProgressInputStream
import org.opencastproject.util.XmlUtil
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.functions.Misc
import org.opencastproject.util.jmx.JmxUtil
import org.opencastproject.workflow.api.WorkflowDatabaseException
import org.opencastproject.workflow.api.WorkflowDefinition
import org.opencastproject.workflow.api.WorkflowException
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowService
import org.opencastproject.workingfilerepository.api.WorkingFileRepository

import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition
import org.apache.http.Header
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.jdom.Document
import org.jdom.Element
import org.jdom.JDOMException
import org.jdom.Namespace
import org.jdom.filter.ElementFilter
import org.jdom.input.SAXBuilder
import org.jdom.output.XMLOutputter
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Arrays
import java.util.Date
import java.util.Dictionary
import java.util.HashMap
import java.util.HashSet
import kotlin.collections.Map.Entry
import java.util.Objects
import java.util.UUID
import java.util.concurrent.TimeUnit

import javax.management.ObjectInstance

/**
 * Creates and augments Opencast MediaPackages. Stores media into the Working File Repository.
 */
/**
 * Creates a new ingest service instance.
 */
open class IngestServiceImpl : AbstractJobProducer(JOB_TYPE), IngestService, ManagedService {

    /** The approximate load placed on the system by ingesting a file  */
    private var ingestFileJobLoad = DEFAULT_INGEST_FILE_JOB_LOAD

    /** The approximate load placed on the system by ingesting a zip file  */
    private var ingestZipJobLoad = DEFAULT_INGEST_ZIP_JOB_LOAD

    /** The JMX business object for ingest statistics  */
    private val ingestStatistics = IngestStatistics()

    /** The JMX bean object instance  */
    private var registerMXBean: ObjectInstance? = null

    /** The workflow service  */
    private var workflowService: WorkflowService? = null

    /** The working file repository  */
    private var workingFileRepository: WorkingFileRepository? = null

    /** The http client  */
    private var httpClient: TrustedHttpClient? = null

    /** The series service  */
    private var seriesService: SeriesService? = null

    /** The dublin core service  */
    private var dublinCoreService: DublinCoreCatalogService? = null

    /** The opencast service registry  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getServiceRegistry
     */
    /**
     * Sets the service registry
     *
     * @param serviceRegistry
     * the serviceRegistry to set
     */
    protected override var serviceRegistry: ServiceRegistry? = null
        set

    /** The authorization service  */
    private var authorizationService: AuthorizationService? = null

    /** The security service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getSecurityService
     */
    /**
     * Callback for setting the security service.
     *
     * @param securityService
     * the securityService to set
     */
    override var securityService: SecurityService? = null
        set

    /** The user directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getUserDirectoryService
     */
    /**
     * Callback for setting the user directory service.
     *
     * @param userDirectoryService
     * the userDirectoryService to set
     */
    override var userDirectoryService: UserDirectoryService? = null
        set

    /** The organization directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getOrganizationDirectoryService
     */
    /**
     * Sets a reference to the organization directory service.
     *
     * @param organizationDirectory
     * the organization directory
     */
    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set

    /** The scheduler service  */
    private var schedulerService: SchedulerService? = null

    /** The media inspection service  */
    private var mediaInspectionService: MediaInspectionService? = null

    /** The default workflow identifier, if one is configured  */
    var defaultWorkflowDefinionId: String? = null

    /** The partial track start time map  */
    private val partialTrackStartTimes = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS)
            .build<String, Long>()

    /** Option, if an event ingest may modify metadata from existing series  */
    private var allowSeriesModifications = DEFAULT_ALLOW_SERIES_MODIFICATIONS

    private var skipCatalogs = true
    private var skipAttachments = true

    /**
     * OSGI callback for activating this component
     *
     * @param cc
     * the osgi component context
     */
    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        logger.info("Ingest Service started.")
        defaultWorkflowDefinionId = StringUtils.trimToNull(cc.bundleContext.getProperty(WORKFLOW_DEFINITION_DEFAULT))
        if (defaultWorkflowDefinionId == null) {
            defaultWorkflowDefinionId = "schedule-and-upload"
        }
        registerMXBean = JmxUtil.registerMXBean(ingestStatistics, "IngestStatistics")
    }

    /**
     * Callback from OSGi on service deactivation.
     */
    fun deactivate() {
        JmxUtil.unregisterMXBean(registerMXBean!!)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedService.updated
     */
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            logger.info("No configuration available, using defaults")
            return
        }

        downloadPassword = StringUtils.trimToEmpty(properties.get(DOWNLOAD_PASSWORD) as String)
        downloadUser = StringUtils.trimToEmpty(properties.get(DOWNLOAD_USER) as String)
        downloadSource = StringUtils.trimToEmpty(properties.get(DOWNLOAD_SOURCE) as String)

        skipAttachments = BooleanUtils.toBoolean(Objects.toString(properties.get(SKIP_ATTACHMENTS_KEY), "true"))
        skipCatalogs = BooleanUtils.toBoolean(Objects.toString(properties.get(SKIP_CATALOGS_KEY), "true"))
        logger.debug("Skip attachments sent by agents for scheduled events: {}", skipAttachments)
        logger.debug("Skip metadata catalogs sent by agents for scheduled events: {}", skipCatalogs)

        ingestFileJobLoad = LoadUtil.getConfiguredLoadValue(properties, FILE_JOB_LOAD_KEY, DEFAULT_INGEST_FILE_JOB_LOAD,
                serviceRegistry!!)
        ingestZipJobLoad = LoadUtil.getConfiguredLoadValue(properties, ZIP_JOB_LOAD_KEY, DEFAULT_INGEST_ZIP_JOB_LOAD,
                serviceRegistry!!)
        // try to get overwrite series option from config, use default if not configured
        try {
            allowSeriesModifications = java.lang.Boolean.parseBoolean((properties.get(PROPKEY_OVERWRITE_SERIES) as String).trim { it <= ' ' })
        } catch (e: Exception) {
            allowSeriesModifications = DEFAULT_ALLOW_SERIES_MODIFICATIONS
            logger.warn("Unable to update configuration. {}", e.message)
        }

        logger.info("Configuration updated. It is {} that existing series will be overwritten during ingest.",
                allowSeriesModifications)
    }

    /**
     * Sets the trusted http client
     *
     * @param httpClient
     * the http client
     */
    fun setHttpClient(httpClient: TrustedHttpClient) {
        this.httpClient = httpClient
    }

    /**
     * Sets the authorization service
     *
     * @param authorizationService
     * the authorization service to set
     */
    fun setAuthorizationService(authorizationService: AuthorizationService) {
        this.authorizationService = authorizationService
    }

    /**
     * Sets the media inspection service
     *
     * @param mediaInspectionService
     * the media inspection service to set
     */
    fun setMediaInspectionService(mediaInspectionService: MediaInspectionService) {
        this.mediaInspectionService = mediaInspectionService
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addZippedMediaPackage
     */
    @Throws(IngestException::class, IOException::class, MediaPackageException::class)
    override fun addZippedMediaPackage(zipStream: InputStream): WorkflowInstance {
        try {
            return addZippedMediaPackage(zipStream, null!!, null!!)
        } catch (e: NotFoundException) {
            throw IllegalStateException("A not found exception was thrown without a lookup")
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addZippedMediaPackage
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class, NotFoundException::class)
    override fun addZippedMediaPackage(zipStream: InputStream, wd: String): WorkflowInstance {
        return addZippedMediaPackage(zipStream, wd, null!!)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addZippedMediaPackage
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class, NotFoundException::class)
    override fun addZippedMediaPackage(zipStream: InputStream, wd: String, workflowConfig: Map<String, String>): WorkflowInstance {
        try {
            return addZippedMediaPackage(zipStream, wd, workflowConfig, null)
        } catch (e: UnauthorizedException) {
            throw IllegalStateException(e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addZippedMediaPackage
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class, NotFoundException::class, UnauthorizedException::class)
    override fun addZippedMediaPackage(zipStream: InputStream, workflowDefinitionId: String,
                                       workflowConfig: Map<String, String>, workflowInstanceId: Long?): WorkflowInstance {
        var workflowDefinitionId = workflowDefinitionId
        // Start a job synchronously. We can't keep the open input stream waiting around.
        var job: Job? = null

        if (StringUtils.isNotBlank(workflowDefinitionId)) {
            try {
                workflowService!!.getWorkflowDefinitionById(workflowDefinitionId)
            } catch (e: WorkflowDatabaseException) {
                throw IngestException(e)
            } catch (nfe: NotFoundException) {
                logger.warn("Workflow definition {} not found, using default workflow {} instead", workflowDefinitionId,
                        defaultWorkflowDefinionId)
                workflowDefinitionId = defaultWorkflowDefinionId
            }

        }

        if (workflowInstanceId != null) {
            logger.warn("Deprecated method! Ingesting zipped mediapackage with workflow {}", workflowInstanceId)
        } else {
            logger.info("Ingesting zipped mediapackage")
        }

        var zis: ZipArchiveInputStream? = null
        val collectionFilenames = HashSet<String>()
        try {
            // We don't need anybody to do the dispatching for us. Therefore we need to make sure that the job is never in
            // QUEUED state but set it to INSTANTIATED in the beginning and then manually switch it to RUNNING.
            job = serviceRegistry!!.createJob(JOB_TYPE, INGEST_ZIP, null!!, null!!, false, ingestZipJobLoad)
            job.status = Status.RUNNING
            job = serviceRegistry!!.updateJob(job)

            // Create the working file target collection for this ingest operation
            val wfrCollectionId = java.lang.Long.toString(job.id)

            zis = ZipArchiveInputStream(zipStream)
            val entry: ZipArchiveEntry
            var mp: MediaPackage? = null
            val uris = HashMap<String, URI>()
            // Sequential number to append to file names so that, if two files have the same
            // name, one does not overwrite the other (see MH-9688)
            var seq = 1
            // Folder name to compare with next one to figure out if there's a root folder
            var folderName: String? = null
            // Indicates if zip has a root folder or not, initialized as true
            var hasRootFolder = true
            // While there are entries write them to a collection
            while ((entry = zis.nextZipEntry) != null) {
                try {
                    if (entry.isDirectory || entry.name.contains("__MACOSX"))
                        continue

                    if (entry.name.endsWith("manifest.xml") || entry.name.endsWith("index.xml")) {
                        // Build the mediapackage
                        mp = loadMediaPackageFromManifest(ZipEntryInputStream(zis, entry.size))
                    } else {
                        logger.info("Storing zip entry {}/{} in working file repository collection '{}'", job.id,
                                entry.name, wfrCollectionId)
                        // Since the directory structure is not being mirrored, makes sure the file
                        // name is different than the previous one(s) by adding a sequential number
                        val fileName = (FilenameUtils.getBaseName(entry.name) + "_" + seq++ + "."
                                + FilenameUtils.getExtension(entry.name))
                        val contentUri = workingFileRepository!!.putInCollection(wfrCollectionId, fileName,
                                ZipEntryInputStream(zis, entry.size))
                        collectionFilenames.add(fileName)
                        // Key is the zip entry name as it is
                        val key = entry.name
                        uris[key] = contentUri
                        ingestStatistics.add(entry.size)
                        logger.info("Zip entry {}/{} stored at {}", job.id, entry.name, contentUri)
                        // Figures out if there's a root folder. Does entry name starts with a folder?
                        val pos = entry.name.indexOf('/')
                        if (pos == -1) {
                            // No, we can conclude there's no root folder
                            hasRootFolder = false
                        } else if (hasRootFolder && folderName != null && folderName != entry.name.substring(0, pos)) {
                            // Folder name different from previous so there's no root folder
                            hasRootFolder = false
                        } else if (folderName == null) {
                            // Just initialize folder name
                            folderName = entry.name.substring(0, pos)
                        }
                    }
                } catch (e: IOException) {
                    logger.warn("Unable to process zip entry {}: {}", entry.name, e)
                    throw e
                }

            }

            if (mp == null)
                throw MediaPackageException("No manifest found in this zip")

            // Determine the mediapackage identifier
            if (mp.identifier == null || isBlank(mp.identifier.toString()))
                mp.identifier = UUIDIdBuilderImpl().createNew()

            val mediaPackageId = mp.identifier.toString()

            logger.info("Ingesting mediapackage {} is named '{}'", mediaPackageId, mp.title)

            // Make sure there are tracks in the mediapackage
            if (mp.tracks.size == 0) {
                logger.warn("Mediapackage {} has no media tracks", mediaPackageId)
            }

            // Update the element uris to point to their working file repository location
            for (element in mp.elements()) {
                // Key has root folder name if there is one
                val uri = uris[(if (hasRootFolder) folderName!! + "/" else "") + element.getURI().toString()]
                        ?: throw MediaPackageException("Unable to map element name '" + element.getURI() + "' to workspace uri")

                logger.info("Ingested mediapackage element {}/{} located at {}", mediaPackageId, element.identifier, uri)
                val dest = workingFileRepository!!.moveTo(wfrCollectionId, FilenameUtils.getName(uri.toString()), mediaPackageId,
                        element.identifier, FilenameUtils.getName(element.getURI().toString()))
                element.setURI(dest)

                // TODO: This should be triggered somehow instead of being handled here
                if (MediaPackageElements.SERIES.equals(element.flavor)) {
                    logger.info("Ingested mediapackage {} contains updated series information", mediaPackageId)
                    updateSeries(element.getURI())
                }
            }

            // Now that all elements are in place, start with ingest
            logger.info("Initiating processing of ingested mediapackage {}", mediaPackageId)
            val workflowInstance = ingest(mp, workflowDefinitionId, workflowConfig, workflowInstanceId)
            logger.info("Ingest of mediapackage {} done", mediaPackageId)
            job.status = Job.Status.FINISHED
            return workflowInstance
        } catch (e: ServiceRegistryException) {
            throw IngestException(e)
        } catch (e: MediaPackageException) {
            job!!.setStatus(Job.Status.FAILED, Job.FailureReason.DATA)
            throw e
        } catch (e: Exception) {
            if (e is IngestException)
                throw e
            throw IngestException(e)
        } finally {
            IOUtils.closeQuietly(zis)
            finallyUpdateJob(job)
            for (filename in collectionFilenames) {
                workingFileRepository!!.deleteFromCollection(java.lang.Long.toString(job!!.id), filename, true)
            }
        }
    }

    @Throws(IOException::class, MediaPackageException::class, IngestException::class)
    private fun loadMediaPackageFromManifest(manifest: InputStream): MediaPackage {
        // TODO: Uncomment the following line and remove the patch when the compatibility with pre-1.4 MediaPackages is
        // discarded
        //
        // mp = builder.loadFromXml(manifestStream);
        //
        // =========================================================================================
        // =================================== PATCH BEGIN =========================================
        // =========================================================================================
        var baos: ByteArrayOutputStream? = null
        var bais: ByteArrayInputStream? = null
        try {
            val domMP = SAXBuilder().build(manifest)
            val mpNSUri = "http://mediapackage.opencastproject.org"

            val oldNS = domMP.rootElement.namespace
            val newNS = Namespace.getNamespace(oldNS.prefix, mpNSUri)

            if (newNS != oldNS) {
                val it = domMP.getDescendants(ElementFilter(oldNS))
                while (it.hasNext()) {
                    val elem = it.next() as Element
                    elem.namespace = newNS
                }
            }

            baos = ByteArrayOutputStream()
            XMLOutputter().output(domMP, baos)
            bais = ByteArrayInputStream(baos.toByteArray())
            return MediaPackageParser.getFromXml(IOUtils.toString(bais, "UTF-8"))
        } catch (e: JDOMException) {
            throw IngestException("Error unmarshalling mediapackage", e)
        } finally {
            IOUtils.closeQuietly(bais)
            IOUtils.closeQuietly(baos)
            IOUtils.closeQuietly(manifest)
        }
        // =========================================================================================
        // =================================== PATCH END ===========================================
        // =========================================================================================
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.createMediaPackage
     */
    @Throws(MediaPackageException::class, ConfigurationException::class)
    override fun createMediaPackage(): MediaPackage {
        val mediaPackage: MediaPackage
        try {
            mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        } catch (e: MediaPackageException) {
            logger.error("INGEST:Failed to create media package " + e.localizedMessage)
            throw e
        }

        mediaPackage.date = Date()
        logger.info("Created mediapackage {}", mediaPackage)
        return mediaPackage
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.createMediaPackage
     */
    @Throws(MediaPackageException::class, ConfigurationException::class)
    override fun createMediaPackage(mediaPackageId: String): MediaPackage {
        val mediaPackage: MediaPackage
        try {
            mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                    .createNew(UUIDIdBuilderImpl().fromString(mediaPackageId))
        } catch (e: MediaPackageException) {
            logger.error("INGEST:Failed to create media package " + e.localizedMessage)
            throw e
        }

        mediaPackage.date = Date()
        logger.info("Created mediapackage {}", mediaPackage)
        return mediaPackage
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addTrack
     */
    @Throws(IOException::class, IngestException::class)
    override fun addTrack(uri: URI, flavor: MediaPackageElementFlavor, mediaPackage: MediaPackage): MediaPackage {
        val tags: Array<String>? = null
        return this.addTrack(uri, flavor, tags!!, mediaPackage)

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addTrack
     */
    @Throws(IOException::class, IngestException::class)
    override fun addTrack(uri: URI, flavor: MediaPackageElementFlavor, tags: Array<String>, mediaPackage: MediaPackage): MediaPackage {
        var job: Job? = null
        try {
            job = serviceRegistry!!
                    .createJob(
                            JOB_TYPE, INGEST_TRACK_FROM_URI, Arrays.asList(uri.toString(),
                            flavor?.toString(), MediaPackageParser.getAsXml(mediaPackage)),
                            null!!, false, ingestFileJobLoad)
            job.status = Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val elementId = UUID.randomUUID().toString()
            logger.info("Start adding track {} from URL {} on mediapackage {}", elementId, uri, mediaPackage)
            val newUrl = addContentToRepo(mediaPackage, elementId, uri)
            val mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
                    flavor)
            if (tags != null && tags.size > 0) {
                val trackElement = mp.getTrack(elementId)
                for (tag in tags) {
                    logger.info("Adding Tag: $tag to Element: $elementId")
                    trackElement.addTag(tag)
                }
            }

            job.status = Job.Status.FINISHED
            logger.info("Successful added track {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl)
            return mp
        } catch (e: IOException) {
            throw e
        } catch (e: ServiceRegistryException) {
            throw IngestException(e)
        } catch (e: NotFoundException) {
            throw IngestException("Unable to update ingest job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addTrack
     */
    @Throws(IOException::class, IngestException::class)
    override fun addTrack(`in`: InputStream, fileName: String, flavor: MediaPackageElementFlavor,
                          mediaPackage: MediaPackage): MediaPackage {
        val tags: Array<String>? = null
        return this.addTrack(`in`, fileName, flavor, tags!!, mediaPackage)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addTrack
     */
    @Throws(IOException::class, IngestException::class)
    override fun addTrack(`in`: InputStream, fileName: String, flavor: MediaPackageElementFlavor, tags: Array<String>,
                          mediaPackage: MediaPackage): MediaPackage {
        var job: Job? = null
        try {
            job = serviceRegistry!!.createJob(JOB_TYPE, INGEST_TRACK, null!!, null!!, false, ingestFileJobLoad)
            job.status = Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val elementId = UUID.randomUUID().toString()
            logger.info("Start adding track {} from input stream on mediapackage {}", elementId, mediaPackage)
            val newUrl = addContentToRepo(mediaPackage, elementId, fileName, `in`)
            val mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
                    flavor)
            if (tags != null && tags.size > 0) {
                val trackElement = mp.getTrack(elementId)
                for (tag in tags) {
                    logger.info("Adding Tag: $tag to Element: $elementId")
                    trackElement.addTag(tag)
                }
            }

            job.status = Job.Status.FINISHED
            logger.info("Successful added track {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl)
            return mp
        } catch (e: IOException) {
            throw e
        } catch (e: ServiceRegistryException) {
            throw IngestException(e)
        } catch (e: NotFoundException) {
            throw IngestException("Unable to update ingest job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    @Throws(IOException::class, IngestException::class)
    override fun addPartialTrack(uri: URI, flavor: MediaPackageElementFlavor, startTime: Long,
                                 mediaPackage: MediaPackage): MediaPackage {
        var job: Job? = null
        try {
            job = serviceRegistry!!.createJob(
                    JOB_TYPE,
                    INGEST_TRACK_FROM_URI,
                    Arrays.asList(uri.toString(), flavor?.toString(),
                            MediaPackageParser.getAsXml(mediaPackage)), null!!, false)
            job.status = Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val elementId = UUID.randomUUID().toString()
            logger.info("Start adding partial track {} from URL {} on mediapackage {}", elementId, uri, mediaPackage)
            val newUrl = addContentToRepo(mediaPackage, elementId, uri)
            val mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
                    flavor)
            job.status = Job.Status.FINISHED
            // store startTime
            partialTrackStartTimes.put(elementId, startTime)
            logger.debug("Added start time {} for track {}", startTime, elementId)
            logger.info("Successful added partial track {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl)
            return mp
        } catch (e: ServiceRegistryException) {
            throw IngestException(e)
        } catch (e: NotFoundException) {
            throw IngestException("Unable to update ingest job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    @Throws(IOException::class, IngestException::class)
    override fun addPartialTrack(`in`: InputStream, fileName: String, flavor: MediaPackageElementFlavor, startTime: Long,
                                 mediaPackage: MediaPackage): MediaPackage {
        var job: Job? = null
        try {
            job = serviceRegistry!!.createJob(JOB_TYPE, INGEST_TRACK, null!!, null!!, false)
            job.status = Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val elementId = UUID.randomUUID().toString()
            logger.info("Start adding partial track {} from input stream on mediapackage {}", elementId, mediaPackage)
            val newUrl = addContentToRepo(mediaPackage, elementId, fileName, `in`)
            val mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
                    flavor)
            job.status = Job.Status.FINISHED
            // store startTime
            partialTrackStartTimes.put(elementId, startTime)
            logger.debug("Added start time {} for track {}", startTime, elementId)
            logger.info("Successful added partial track {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl)
            return mp
        } catch (e: ServiceRegistryException) {
            throw IngestException(e)
        } catch (e: NotFoundException) {
            throw IngestException("Unable to update ingest job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addCatalog
     */
    @Throws(IOException::class, IngestException::class)
    override fun addCatalog(uri: URI, flavor: MediaPackageElementFlavor, mediaPackage: MediaPackage): MediaPackage {
        var job: Job? = null
        try {
            job = serviceRegistry!!.createJob(JOB_TYPE, INGEST_CATALOG_FROM_URI,
                    Arrays.asList(uri.toString(), flavor.toString(), MediaPackageParser.getAsXml(mediaPackage)), null!!, false,
                    ingestFileJobLoad)
            job.status = Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val elementId = UUID.randomUUID().toString()
            logger.info("Start adding catalog {} from URL {} on mediapackage {}", elementId, uri, mediaPackage)
            val newUrl = addContentToRepo(mediaPackage, elementId, uri)
            if (MediaPackageElements.SERIES.equals(flavor)) {
                updateSeries(uri)
            }
            val mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Catalog,
                    flavor)
            job.status = Job.Status.FINISHED
            logger.info("Successful added catalog {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl)
            return mp
        } catch (e: ServiceRegistryException) {
            throw IngestException(e)
        } catch (e: NotFoundException) {
            throw IngestException("Unable to update ingest job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    /**
     * Updates the persistent representation of a series based on a potentially modified dublin core document.
     *
     * @param uri
     * the URI to the dublin core document containing series metadata.
     * @return
     * true, if the series is created or overwritten, false if the existing series remains intact.
     */
    @Throws(IOException::class, IngestException::class)
    fun updateSeries(uri: URI): Boolean {
        var response: HttpResponse? = null
        var `in`: InputStream? = null
        var isUpdated = false
        try {
            val getDc = HttpGet(uri)
            response = httpClient!!.execute(getDc)
            `in` = response.entity.content
            val dc = dublinCoreService!!.load(`in`)
            val id = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER)
            if (id == null) {
                logger.warn("Series dublin core document contains no identifier, rejecting ingested series cagtalog.")
            } else {
                try {
                    try {
                        seriesService!!.getSeries(id)
                        if (allowSeriesModifications) {
                            // Update existing series
                            seriesService!!.updateSeries(dc)
                            isUpdated = true
                            logger.debug("Ingest is overwriting the existing series {} with the ingested series", id)
                        } else {
                            logger.debug("Series {} already exists. Ignoring series catalog from ingest.", id)
                        }
                    } catch (e: NotFoundException) {
                        logger.info("Creating new series {} with default ACL", id)
                        seriesService!!.updateSeries(dc)
                        isUpdated = true
                    }

                } catch (e: Exception) {
                    throw IngestException(e)
                }

            }
            `in`!!.close()
        } catch (e: IOException) {
            logger.error("Error updating series from DublinCoreCatalog: {}", e.message)
        } finally {
            IOUtils.closeQuietly(`in`)
            httpClient!!.close(response!!)
        }
        return isUpdated
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addCatalog
     */
    @Throws(IOException::class, IngestException::class)
    override fun addCatalog(`in`: InputStream, fileName: String, flavor: MediaPackageElementFlavor,
                            mediaPackage: MediaPackage): MediaPackage {
        return addCatalog(`in`, fileName, flavor, null!!, mediaPackage)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addCatalog
     */
    @Throws(IOException::class, IngestException::class)
    override fun addCatalog(`in`: InputStream, fileName: String, flavor: MediaPackageElementFlavor, tags: Array<String>,
                            mediaPackage: MediaPackage): MediaPackage {
        var job: Job? = null
        try {
            job = serviceRegistry!!.createJob(JOB_TYPE, INGEST_CATALOG, null!!, null!!, false, ingestFileJobLoad)
            job.status = Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val elementId = UUID.randomUUID().toString()
            logger.info("Start adding catalog {} from input stream on mediapackage {}", elementId, mediaPackage)
            val newUrl = addContentToRepo(mediaPackage, elementId, fileName, `in`)
            if (MediaPackageElements.SERIES.equals(flavor)) {
                updateSeries(newUrl)
            }
            val mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Catalog,
                    flavor)
            if (tags != null && tags.size > 0) {
                val trackElement = mp.getCatalog(elementId)
                for (tag in tags) {
                    logger.info("Adding Tag: $tag to Element: $elementId")
                    trackElement.addTag(tag)
                }
            }

            job.status = Job.Status.FINISHED
            logger.info("Successful added catalog {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl)
            return mp
        } catch (e: ServiceRegistryException) {
            throw IngestException(e)
        } catch (e: NotFoundException) {
            throw IngestException("Unable to update ingest job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addAttachment
     */
    @Throws(IOException::class, IngestException::class)
    override fun addAttachment(uri: URI, flavor: MediaPackageElementFlavor, mediaPackage: MediaPackage): MediaPackage {
        var job: Job? = null
        try {
            job = serviceRegistry!!.createJob(JOB_TYPE, INGEST_ATTACHMENT_FROM_URI,
                    Arrays.asList(uri.toString(), flavor.toString(), MediaPackageParser.getAsXml(mediaPackage)), null!!, false,
                    ingestFileJobLoad)
            job.status = Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val elementId = UUID.randomUUID().toString()
            logger.info("Start adding attachment {} from URL {} on mediapackage {}", elementId, uri, mediaPackage)
            val newUrl = addContentToRepo(mediaPackage, elementId, uri)
            val mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Attachment,
                    flavor)
            job.status = Job.Status.FINISHED
            logger.info("Successful added attachment {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl)
            return mp
        } catch (e: ServiceRegistryException) {
            throw IngestException(e)
        } catch (e: NotFoundException) {
            throw IngestException("Unable to update ingest job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addAttachment
     */
    @Throws(IOException::class, IngestException::class)
    override fun addAttachment(`in`: InputStream, fileName: String, flavor: MediaPackageElementFlavor, tags: Array<String>,
                               mediaPackage: MediaPackage): MediaPackage {
        var job: Job? = null
        try {
            job = serviceRegistry!!.createJob(JOB_TYPE, INGEST_ATTACHMENT, null!!, null!!, false, ingestFileJobLoad)
            job.status = Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val elementId = UUID.randomUUID().toString()
            logger.info("Start adding attachment {} from input stream on mediapackage {}", elementId, mediaPackage)
            val newUrl = addContentToRepo(mediaPackage, elementId, fileName, `in`)
            val mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Attachment,
                    flavor)
            if (tags != null && tags.size > 0) {
                val trackElement = mp.getAttachment(elementId)
                for (tag in tags) {
                    logger.info("Adding Tag: $tag to Element: $elementId")
                    trackElement.addTag(tag)
                }
            }
            job.status = Job.Status.FINISHED
            logger.info("Successful added attachment {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl)
            return mp
        } catch (e: ServiceRegistryException) {
            throw IngestException(e)
        } catch (e: NotFoundException) {
            throw IngestException("Unable to update ingest job", e)
        } finally {
            finallyUpdateJob(job)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.addAttachment
     */
    @Throws(IOException::class, IngestException::class)
    override fun addAttachment(`in`: InputStream, fileName: String, flavor: MediaPackageElementFlavor,
                               mediaPackage: MediaPackage): MediaPackage {
        val tags: Array<String>? = null
        return addAttachment(`in`, fileName, flavor, tags!!, mediaPackage)
    }

    /**
     *
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.ingest
     */
    @Throws(IngestException::class)
    override fun ingest(mp: MediaPackage): WorkflowInstance {
        try {
            return ingest(mp, null!!, null!!, null)
        } catch (e: NotFoundException) {
            throw IngestException(e)
        } catch (e: UnauthorizedException) {
            throw IllegalStateException(e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.ingest
     */
    @Throws(IngestException::class, NotFoundException::class)
    override fun ingest(mp: MediaPackage, wd: String): WorkflowInstance {
        try {
            return ingest(mp, wd, null!!, null)
        } catch (e: UnauthorizedException) {
            throw IllegalStateException(e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.ingest
     */
    @Throws(IngestException::class, NotFoundException::class)
    override fun ingest(mp: MediaPackage, wd: String, properties: Map<String, String>): WorkflowInstance {
        try {
            return ingest(mp, wd, properties, null)
        } catch (e: UnauthorizedException) {
            throw IllegalStateException(e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.ingest
     */
    @Throws(IngestException::class, NotFoundException::class, UnauthorizedException::class)
    override fun ingest(mp: MediaPackage, workflowDefinitionId: String, properties: Map<String, String>,
                        workflowInstanceId: Long?): WorkflowInstance {
        var mp = mp
        var properties = properties
        // Check for legacy media package id
        mp = checkForLegacyMediaPackageId(mp, properties)

        try {
            mp = createSmil(mp)
        } catch (e: IOException) {
            throw IngestException("Unable to add SMIL Catalog", e)
        }

        // Done, update the job status and return the created workflow instance
        if (workflowInstanceId != null) {
            logger.warn(
                    "Resuming workflow {} with ingested mediapackage {} is deprecated, skip resuming and start new workflow",
                    workflowInstanceId, mp)
        }

        if (workflowDefinitionId == null) {
            logger.info("Starting a new workflow with ingested mediapackage {} based on the default workflow definition '{}'",
                    mp, defaultWorkflowDefinionId)
        } else {
            logger.info("Starting a new workflow with ingested mediapackage {} based on workflow definition '{}'", mp,
                    workflowDefinitionId)
        }

        try {
            // Determine the workflow definition
            val workflowDef = getWorkflowDefinition(workflowDefinitionId, mp)

            // Get the final set of workflow properties
            properties = mergeWorkflowConfiguration(properties, mp.identifier.compact())

            // Remove potential workflow configuration prefixes from the workflow properties
            properties = removePrefixFromProperties(properties)

            // Merge scheduled mediapackage with ingested
            mp = mergeScheduledMediaPackage(mp)

            ingestStatistics.successful()
            if (workflowDef != null) {
                logger.info("Starting new workflow with ingested mediapackage '{}' using the specified template '{}'",
                        mp.identifier.toString(), workflowDefinitionId)
            } else {
                logger.info("Starting new workflow with ingested mediapackage '{}' using the default template '{}'",
                        mp.identifier.toString(), defaultWorkflowDefinionId)
            }
            return workflowService!!.start(workflowDef, mp, properties)
        } catch (e: WorkflowException) {
            ingestStatistics.failed()
            throw IngestException(e)
        }

    }

    @Throws(IllegalStateException::class, IngestException::class, NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    override fun schedule(mediaPackage: MediaPackage, workflowDefinitionID: String, properties: Map<String, String>) {
        val mediaPackageElements = mediaPackage.getElementsByFlavor(MediaPackageElements.EPISODE)
        if (mediaPackageElements.size != 1) {
            logger.debug("There can be only one (and exactly one) episode dublin core catalog: https://youtu.be/_J3VeogFUOs")
            throw IngestException("There can be only one (and exactly one) episode dublin core catalog")
        }
        val inputStream: InputStream
        val dublinCoreCatalog: DublinCoreCatalog
        try {
            inputStream = workingFileRepository!!.get(mediaPackage.identifier.toString(),
                    mediaPackageElements[0].identifier)
            dublinCoreCatalog = dublinCoreService!!.load(inputStream)
        } catch (e: IOException) {
            throw IngestException(e)
        }

        val temporal = EName(DublinCore.TERMS_NS_URI, "temporal")
        val periods = dublinCoreCatalog[temporal]
        if (periods.size != 1) {
            logger.debug("There can be only one (and exactly one) period")
            throw IngestException("There can be only one (and exactly one) period")
        }
        val period = EncodingSchemeUtils.decodeMandatoryPeriod(periods[0])
        if (!period.hasStart() || !period.hasEnd()) {
            logger.debug("A scheduled recording needs to have a start and end.")
            throw IngestException("A scheduled recording needs to have a start and end.")
        }
        val createdEName = EName(DublinCore.TERMS_NS_URI, "created")
        val created = dublinCoreCatalog[createdEName]
        if (created.size == 0) {
            logger.debug("Created not set")
        } else if (created.size == 1) {
            val date = EncodingSchemeUtils.decodeMandatoryDate(created[0])
            if (date.time != period.start!!.time) {
                logger.debug("start and created date differ ({} vs {})", date.time, period.start!!.time)
                throw IngestException("Temporal start and created date differ")
            }
        } else {
            logger.debug("There can be only one created date")
            throw IngestException("There can be only one created date")
        }
        // spatial
        val spatial = EName(DublinCore.TERMS_NS_URI, "spatial")
        val captureAgents = dublinCoreCatalog[spatial]
        if (captureAgents.size != 1) {
            logger.debug("Exactly one capture agent needs to be set")
            throw IngestException("Exactly one capture agent needs to be set")
        }
        val captureAgent = captureAgents[0].value

        // Go through properties
        val agentProperties = HashMap<String, String>()
        val workflowProperties = HashMap<String, String>()
        for (key in properties.keys) {
            if (key.startsWith("org.opencastproject.workflow.config.")) {
                workflowProperties[key] = properties[key]
            } else {
                agentProperties[key] = properties[key]
            }
        }
        try {
            schedulerService!!.addEvent(period.start, period.end, captureAgent, HashSet(), mediaPackage,
                    workflowProperties, agentProperties, Opt.none())
        } finally {
            for (mediaPackageElement in mediaPackage.elements) {
                try {
                    workingFileRepository!!.delete(mediaPackage.identifier.toString(), mediaPackageElement.identifier)
                } catch (e: IOException) {
                    logger.warn("Failed to delete media package element", e)
                }

            }
        }
    }

    /**
     * Check whether the mediapackage id is set via the legacy workflow identifier and change the id if existing.
     *
     * @param mp
     * the mediapackage
     * @param properties
     * the workflow properties
     * @return the mediapackage
     */
    @Throws(IngestException::class)
    private fun checkForLegacyMediaPackageId(mp: MediaPackage, properties: MutableMap<String, String>?): MediaPackage {
        if (properties == null || properties.isEmpty())
            return mp

        try {
            val mediaPackageId = properties[LEGACY_MEDIAPACKAGE_ID_KEY]
            if (StringUtils.isNotBlank(mediaPackageId) && schedulerService != null) {
                logger.debug("Check ingested mediapackage {} for legacy mediapackage identifier {}",
                        mp.identifier.compact(), mediaPackageId)
                try {
                    schedulerService!!.getMediaPackage(mp.identifier.compact())
                    return mp
                } catch (e: NotFoundException) {
                    logger.info("No scheduler mediapackage found with ingested id {}, try legacy mediapackage id {}",
                            mp.identifier.compact(), mediaPackageId)
                    try {
                        schedulerService!!.getMediaPackage(mediaPackageId)
                        logger.info("Legacy mediapackage id {} exists, change ingested mediapackage id {} to legacy id",
                                mediaPackageId, mp.identifier.compact())
                        mp.identifier = IdImpl(mediaPackageId)
                        return mp
                    } catch (e1: NotFoundException) {
                        logger.info("No scheduler mediapackage found with legacy mediapackage id {}, skip merging", mediaPackageId)
                    } catch (e1: Exception) {
                        logger.error("Unable to get event mediapackage from scheduler event {}", mediaPackageId, e)
                        throw IngestException(e)
                    }

                } catch (e: Exception) {
                    logger.error("Unable to get event mediapackage from scheduler event {}", mp.identifier.compact(), e)
                    throw IngestException(e)
                }

            }
            return mp
        } finally {
            properties.remove(LEGACY_MEDIAPACKAGE_ID_KEY)
        }
    }

    private fun mergeWorkflowConfiguration(properties: Map<String, String>?, mediaPackageId: String): Map<String, String>? {
        if (isBlank(mediaPackageId) || schedulerService == null)
            return properties

        val mergedProperties = HashMap<String, String>()

        try {
            val recordingProperties = schedulerService!!.getCaptureAgentConfiguration(mediaPackageId)
            logger.debug("Restoring workflow properties from scheduler event {}", mediaPackageId)
            mergedProperties.putAll(recordingProperties)
        } catch (e: SchedulerException) {
            logger.warn("Unable to get workflow properties from scheduler event {}", mediaPackageId, e)
        } catch (e: NotFoundException) {
            logger.info("No capture event found for id {}", mediaPackageId)
        } catch (e: UnauthorizedException) {
            throw IllegalStateException(e)
        }

        if (properties != null) {
            // Merge the properties, this must be after adding the recording properties
            logger.debug("Merge workflow properties with the one from the scheduler event {}", mediaPackageId)
            mergedProperties.putAll(properties)
        }

        return mergedProperties
    }

    /**
     * Merges the ingested mediapackage with the scheduled mediapackage. The ingested mediapackage takes precedence over
     * the scheduled mediapackage.
     *
     * @param mp
     * the ingested mediapackage
     * @return the merged mediapackage
     */
    @Throws(IngestException::class)
    private fun mergeScheduledMediaPackage(mp: MediaPackage): MediaPackage {
        if (schedulerService == null) {
            logger.warn("No scheduler service available to merge mediapackage!")
            return mp
        }

        try {
            val scheduledMp = schedulerService!!.getMediaPackage(mp.identifier.compact())
            logger.info("Found matching scheduled event for id '{}', merging mediapackage...", mp.identifier.compact())
            mergeMediaPackageElements(mp, scheduledMp)
            mergeMediaPackageMetadata(mp, scheduledMp)
            return mp
        } catch (e: NotFoundException) {
            logger.debug("No scheduler mediapackage found with id {}, skip merging", mp.identifier)
            return mp
        } catch (e: Exception) {
            throw IngestException(String.format("Unable to get event media package from scheduler event %s",
                    mp.identifier), e)
        }

    }

    private fun mergeMediaPackageElements(mp: MediaPackage, scheduledMp: MediaPackage) {
        // drop catalogs sent by the capture agent in favor of Opencast's own metadata
        if (skipCatalogs) {
            for (element in mp.catalogs) {
                mp.remove(element)
            }
        }

        // drop attachments the capture agent sent us in favor of Opencast's attachments
        // e.g. prevent capture agents from modifying security rules of schedules events
        if (skipAttachments) {
            for (element in mp.attachments) {
                mp.remove(element)
            }
        }

        for (element in scheduledMp.elements) {
            // Asset manager media package may have a publication element (for live) if retract live has not run yet
            if (element.flavor != null
                    && MediaPackageElement.Type.Publication != element.elementType
                    && mp.getElementsByFlavor(element.flavor).size > 0) {
                logger.info("Ignore scheduled element '{}', there is already an ingested element with flavor '{}'", element,
                        element.flavor)
                continue
            }
            logger.info("Adding new scheduled element '{}' to ingested mediapackage", element)
            mp.add(element)
        }
    }

    private fun mergeMediaPackageMetadata(mp: MediaPackage, scheduledMp: MediaPackage) {
        // Merge media package fields
        if (mp.date == null)
            mp.date = scheduledMp.date

        if (skipCatalogs || isBlank(mp.license))
            mp.license = scheduledMp.license
        if (skipCatalogs || isBlank(mp.series))
            mp.series = scheduledMp.series
        if (skipCatalogs || isBlank(mp.seriesTitle))
            mp.seriesTitle = scheduledMp.seriesTitle
        if (skipCatalogs || isBlank(mp.title))
            mp.title = scheduledMp.title
        if (skipCatalogs || mp.subjects.size == 0) {
            Arrays.stream(mp.subjects).forEach(Consumer<String> { mp.removeSubject(it) })
            for (subject in scheduledMp.subjects) {
                mp.addSubject(subject)
            }
        }
        if (skipCatalogs || mp.contributors.size == 0) {
            Arrays.stream(mp.contributors).forEach(Consumer<String> { mp.removeContributor(it) })
            for (contributor in scheduledMp.contributors) {
                mp.addContributor(contributor)
            }
        }
        if (skipCatalogs || mp.creators.size == 0) {
            Arrays.stream(mp.creators).forEach(Consumer<String> { mp.removeCreator(it) })
            for (creator in scheduledMp.creators) {
                mp.addCreator(creator)
            }
        }
    }

    /**
     * Removes the workflow configuration file prefix from all properties in a map.
     *
     * @param properties
     * The properties to remove the prefixes from
     * @return A Map with the same collection of properties without the prefix
     */
    private fun removePrefixFromProperties(properties: Map<String, String>?): Map<String, String> {
        val fixedProperties = HashMap<String, String>()
        if (properties != null) {
            for ((key, value) in properties) {
                if (key.startsWith(WORKFLOW_CONFIGURATION_PREFIX)) {
                    logger.debug("Removing prefix from key '$key with value '$value'")
                    fixedProperties[key.replace(WORKFLOW_CONFIGURATION_PREFIX, "")] = value
                } else {
                    fixedProperties[key] = value
                }
            }
        }
        return fixedProperties
    }

    @Throws(NotFoundException::class, WorkflowDatabaseException::class, IngestException::class)
    private fun getWorkflowDefinition(workflowDefinitionID: String?, mediapackage: MediaPackage): WorkflowDefinition? {
        var workflowDefinitionID = workflowDefinitionID
        // If the workflow definition and instance ID are null, use the default, or throw if there is none
        if (isBlank(workflowDefinitionID)) {
            val mediaPackageId = mediapackage.identifier.compact()
            if (schedulerService != null) {
                logger.info("Determining workflow template for ingested mediapckage {} from capture event {}", mediapackage,
                        mediaPackageId)
                try {
                    val recordingProperties = schedulerService!!.getCaptureAgentConfiguration(mediaPackageId)
                    workflowDefinitionID = recordingProperties[CaptureParameters.INGEST_WORKFLOW_DEFINITION]
                    if (isBlank(workflowDefinitionID)) {
                        workflowDefinitionID = defaultWorkflowDefinionId
                        logger.debug("No workflow set. Falling back to default.")
                    }
                    if (isBlank(workflowDefinitionID)) {
                        throw IngestException("No value found for key '" + CaptureParameters.INGEST_WORKFLOW_DEFINITION
                                + "' from capture event configuration of scheduler event '" + mediaPackageId + "'")
                    }
                    logger.info("Ingested event {} will be processed using workflow '{}'", mediapackage, workflowDefinitionID)
                } catch (e: NotFoundException) {
                    logger.warn("Specified capture event {} was not found", mediaPackageId)
                } catch (e: UnauthorizedException) {
                    throw IllegalStateException(e)
                } catch (e: SchedulerException) {
                    logger.warn("Unable to get the workflow definition id from scheduler event {}", mediaPackageId, e)
                    throw IngestException(e)
                }

            } else {
                logger.warn(
                        "Scheduler service not bound, unable to determine the workflow template to use for ingested mediapckage {}",
                        mediapackage)
            }

        } else {
            logger.info("Ingested mediapackage {} is processed using workflow template '{}', specified during ingest",
                    mediapackage, workflowDefinitionID)
        }

        // Use the default workflow definition if nothing was determined
        if (isBlank(workflowDefinitionID) && defaultWorkflowDefinionId != null) {
            logger.info("Using default workflow definition '{}' to process ingested mediapackage {}",
                    defaultWorkflowDefinionId, mediapackage)
            workflowDefinitionID = defaultWorkflowDefinionId
        }

        // Check if the workflow definition is valid
        if (StringUtils.isNotBlank(workflowDefinitionID) && StringUtils.isNotBlank(defaultWorkflowDefinionId)) {
            try {
                workflowService!!.getWorkflowDefinitionById(workflowDefinitionID)
            } catch (e: WorkflowDatabaseException) {
                throw IngestException(e)
            } catch (nfe: NotFoundException) {
                logger.warn("Workflow definition {} not found, using default workflow {} instead", workflowDefinitionID,
                        defaultWorkflowDefinionId)
                workflowDefinitionID = defaultWorkflowDefinionId
            }

        }

        // Have we been able to find a workflow definition id?
        if (isBlank(workflowDefinitionID)) {
            ingestStatistics.failed()
            throw IllegalStateException(
                    "Can not ingest a workflow without a workflow definition or an existing instance. No default definition is specified")
        }

        // Let's make sure the workflow definition exists
        return workflowService!!.getWorkflowDefinitionById(workflowDefinitionID)
    }

    /**
     *
     * {@inheritDoc}
     *
     * @see org.opencastproject.ingest.api.IngestService.discardMediaPackage
     */
    @Throws(IOException::class)
    override fun discardMediaPackage(mp: MediaPackage) {
        val mediaPackageId = mp.identifier.compact()
        for (element in mp.elements) {
            if (!workingFileRepository!!.delete(mediaPackageId, element.identifier))
                logger.warn("Unable to find (and hence, delete), this mediapackage element")
        }
        logger.info("Sucessful discarded mediapackage {}", mp)
    }

    /**
     * Creates a StandAloneTrustedHttpClientImpl
     *
     * @param user
     * @param password
     * @return
     */
    protected open fun createStandaloneHttpClient(user: String, password: String): TrustedHttpClient {
        return StandAloneTrustedHttpClientImpl(this.downloadUser, this.downloadPassword, none(), none(), none())
    }

    @Throws(IOException::class)
    protected fun addContentToRepo(mp: MediaPackage, elementId: String, uri: URI): URI {
        var `in`: InputStream? = null
        var response: HttpResponse? = null
        var httpClientStandAlone = httpClient
        try {
            if (uri.toString().startsWith("http")) {
                val get = HttpGet(uri)

                if (uri.host.matches(this.downloadSource.toRegex())) {
                    httpClientStandAlone = this.createStandaloneHttpClient(downloadUser, downloadPassword)
                }
                response = httpClientStandAlone!!.execute(get)

                val httpStatusCode = response.statusLine.statusCode
                if (httpStatusCode != 200) {
                    throw IOException("$uri returns http $httpStatusCode")
                }
                `in` = response.entity.content
            } else {
                `in` = uri.toURL().openStream()
            }
            var fileName: String? = FilenameUtils.getName(uri.path)
            if (isBlank(FilenameUtils.getExtension(fileName)))
                fileName = getContentDispositionFileName(response)

            if (isBlank(FilenameUtils.getExtension(fileName)))
                throw IOException("No filename extension found: " + fileName!!)
            return addContentToRepo(mp, elementId, fileName, `in`)
        } finally {
            IOUtils.closeQuietly(`in`)
            httpClientStandAlone!!.close(response!!)
        }
    }

    private fun getContentDispositionFileName(response: HttpResponse?): String? {
        if (response == null)
            return null

        val header = response.getFirstHeader("Content-Disposition")
        val contentDisposition = ContentDisposition(header.value)
        return contentDisposition.getParameter("filename")
    }

    @Throws(IOException::class)
    private fun addContentToRepo(mp: MediaPackage, elementId: String, filename: String, file: InputStream?): URI {
        val progressInputStream = ProgressInputStream(file!!)
        progressInputStream.addPropertyChangeListener(PropertyChangeListener { evt ->
            val totalNumBytesRead = evt.newValue as Long
            val oldTotalNumBytesRead = evt.oldValue as Long
            ingestStatistics.add(totalNumBytesRead - oldTotalNumBytesRead)
        })
        return workingFileRepository!!.put(mp.identifier.compact(), elementId, filename, progressInputStream)
    }

    private fun addContentToMediaPackage(mp: MediaPackage, elementId: String, uri: URI,
                                         type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): MediaPackage {
        logger.info("Adding element of type {} to mediapackage {}", type, mp)
        val mpe = mp.add(uri, type, flavor)
        mpe.identifier = elementId
        return mp
    }

    // ---------------------------------------------
    // --------- bind and unbind bundles ---------
    // ---------------------------------------------
    fun setWorkflowService(workflowService: WorkflowService) {
        this.workflowService = workflowService
    }

    fun setWorkingFileRepository(workingFileRepository: WorkingFileRepository) {
        this.workingFileRepository = workingFileRepository
    }

    fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    fun setDublinCoreService(dublinCoreService: DublinCoreCatalogService) {
        this.dublinCoreService = dublinCoreService
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(Exception::class)
    override fun process(job: Job): String {
        throw IllegalStateException("Ingest jobs are not expected to be dispatched")
    }

    /**
     * Callback for setting the scheduler service.
     *
     * @param schedulerService
     * the scheduler service to set
     */
    fun setSchedulerService(schedulerService: SchedulerService) {
        this.schedulerService = schedulerService
    }

    @Throws(IOException::class, IngestException::class)
    private fun createSmil(mediaPackage: MediaPackage): MediaPackage {
        var partialTracks = Stream.empty<Track>()
        for (track in mediaPackage.tracks) {
            val startTime = partialTrackStartTimes.getIfPresent(track.identifier) ?: continue
            partialTracks = partialTracks.append(Opt.nul(track))
        }

        // No partial track available return without adding SMIL catalog
        if (partialTracks.isEmpty)
            return mediaPackage

        // Inspect the partial tracks
        val tracks = partialTracks.map(newEnrichJob(mediaInspectionService).toFn())
                .map(payloadAsTrack(serviceRegistry).toFn())
                .each(MediaPackageSupport.updateElement(mediaPackage).toFn().toFx()).toList()

        // Create the SMIL document
        var smilDocument: org.w3c.dom.Document = SmilUtil.createSmil()
        for (track in tracks) {
            val startTime = partialTrackStartTimes.getIfPresent(track.identifier)
            if (startTime == null) {
                logger.error("No start time found for track {}", track)
                throw IngestException("No start time found for track " + track.identifier)
            }
            smilDocument = addSmilTrack(smilDocument, track, startTime)
            partialTrackStartTimes.invalidate(track.identifier)
        }

        // Store the SMIL document in the mediapackage
        return addSmilCatalog(smilDocument, mediaPackage)
    }

    /**
     * Adds a SMIL catalog to a mediapackage if it's not already existing.
     *
     * @param smilDocument
     * the smil document
     * @param mediaPackage
     * the mediapackage to extend with the SMIL catalog
     * @return the augmented mediapcakge
     * @throws IOException
     * if reading or writing of the SMIL catalog fails
     * @throws IngestException
     * if the SMIL catalog already exists
     */
    @Throws(IOException::class, IngestException::class)
    private fun addSmilCatalog(smilDocument: org.w3c.dom.Document, mediaPackage: MediaPackage): MediaPackage {
        val optSmilDocument = loadSmilDocument(workingFileRepository, mediaPackage)
        if (optSmilDocument.isSome)
            throw IngestException("SMIL already exists!")

        var `in`: InputStream? = null
        try {
            `in` = XmlUtil.serializeDocument(smilDocument)
            val elementId = UUID.randomUUID().toString()
            val uri = workingFileRepository!!.put(mediaPackage.identifier.compact(), elementId, PARTIAL_SMIL_NAME, `in`)
            val mpe = mediaPackage.add(uri, MediaPackageElement.Type.Catalog, MediaPackageElements.SMIL)
            mpe.identifier = elementId
            // Reset the checksum since it changed
            mpe.checksum = null
            mpe.mimeType = MimeTypes.SMIL
            return mediaPackage
        } finally {
            IoSupport.closeQuietly(`in`)
        }
    }

    /**
     * Load a SMIL document of a media package.
     *
     * @return the document or none if no media package element found.
     */
    private fun loadSmilDocument(workingFileRepository: WorkingFileRepository?,
                                 mp: MediaPackage): Option<org.w3c.dom.Document> {
        return mlist(mp.elements).filter(MediaPackageSupport.Filters.isSmilCatalog).headOpt()
                .map(object : Function<MediaPackageElement, org.w3c.dom.Document>() {
                    override fun apply(mpe: MediaPackageElement): org.w3c.dom.Document {
                        var `in`: InputStream? = null
                        try {
                            `in` = workingFileRepository!!.get(mpe.mediaPackage.identifier.compact(), mpe.identifier)
                            return SmilUtil.loadSmilDocument(`in`, mpe)
                        } catch (e: Exception) {
                            logger.warn("Unable to load smil document from catalog '{}'", mpe, e)
                            return Misc.chuck(e)
                        } finally {
                            IOUtils.closeQuietly(`in`)
                        }
                    }
                })
    }

    /**
     * Adds a SMIL track by a mediapackage track to a SMIL document
     *
     * @param smilDocument
     * the SMIL document to extend
     * @param track
     * the mediapackage track
     * @param startTime
     * the start time
     * @return the augmented SMIL document
     * @throws IngestException
     * if the partial flavor type is not valid
     */
    @Throws(IngestException::class)
    private fun addSmilTrack(smilDocument: org.w3c.dom.Document, track: Track, startTime: Long): org.w3c.dom.Document {
        if (MediaPackageElements.PRESENTER_SOURCE.getType().equals(track.flavor.type)) {
            return SmilUtil.addTrack(smilDocument, SmilUtil.TrackType.PRESENTER, track.hasVideo(), startTime,
                    track.duration!!, track.getURI(), track.identifier)
        } else if (MediaPackageElements.PRESENTATION_SOURCE.getType().equals(track.flavor.type)) {
            return SmilUtil.addTrack(smilDocument, SmilUtil.TrackType.PRESENTATION, track.hasVideo(), startTime,
                    track.duration!!, track.getURI(), track.identifier)
        } else {
            logger.warn("Invalid partial flavor type {} of track {}", track.flavor, track)
            throw IngestException(
                    "Invalid partial flavor type " + track.flavor.type + " of track " + track.getURI().toString())
        }
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(IngestServiceImpl::class.java)

        /** The source SMIL name  */
        private val PARTIAL_SMIL_NAME = "source_partial.smil"

        /** The configuration key that defines the default workflow definition  */
        protected val WORKFLOW_DEFINITION_DEFAULT = "org.opencastproject.workflow.default.definition"

        /** The workflow configuration property prefix  */
        protected val WORKFLOW_CONFIGURATION_PREFIX = "org.opencastproject.workflow.config."

        /** The key for the legacy mediapackage identifier  */
        val LEGACY_MEDIAPACKAGE_ID_KEY = "org.opencastproject.ingest.legacy.mediapackage.id"

        val JOB_TYPE = "org.opencastproject.ingest"

        /** Managed Property key to overwrite existing series  */
        val PROPKEY_OVERWRITE_SERIES = "org.opencastproject.series.overwrite"

        /** Methods that ingest zips create jobs with this operation type  */
        val INGEST_ZIP = "zip"

        /** Methods that ingest tracks directly create jobs with this operation type  */
        val INGEST_TRACK = "track"

        /** Methods that ingest tracks from a URI create jobs with this operation type  */
        val INGEST_TRACK_FROM_URI = "uri-track"

        /** Methods that ingest attachments directly create jobs with this operation type  */
        val INGEST_ATTACHMENT = "attachment"

        /** Methods that ingest attachments from a URI create jobs with this operation type  */
        val INGEST_ATTACHMENT_FROM_URI = "uri-attachment"

        /** Methods that ingest catalogs directly create jobs with this operation type  */
        val INGEST_CATALOG = "catalog"

        /** Methods that ingest catalogs from a URI create jobs with this operation type  */
        val INGEST_CATALOG_FROM_URI = "uri-catalog"

        /** The approximate load placed on the system by ingesting a file  */
        val DEFAULT_INGEST_FILE_JOB_LOAD = 0.2f

        /** The approximate load placed on the system by ingesting a zip file  */
        val DEFAULT_INGEST_ZIP_JOB_LOAD = 0.2f

        /** The key to look for in the service configuration file to override the [DEFAULT_INGEST_FILE_JOB_LOAD]  */
        val FILE_JOB_LOAD_KEY = "job.load.ingest.file"

        /** The key to look for in the service configuration file to override the [DEFAULT_INGEST_ZIP_JOB_LOAD]  */
        val ZIP_JOB_LOAD_KEY = "job.load.ingest.zip"

        /** The source to download from   */
        val DOWNLOAD_SOURCE = "org.opencastproject.download.source"

        /** The user for download from external sources  */
        val DOWNLOAD_USER = "org.opencastproject.download.user"

        /** The password for download from external sources  */
        val DOWNLOAD_PASSWORD = "org.opencastproject.download.password"

        /** By default, do not allow event ingest to modify existing series metadata  */
        internal val DEFAULT_ALLOW_SERIES_MODIFICATIONS = false

        /** Control if catalogs sent by capture agents for scheduled events are skipped.  */
        private val SKIP_CATALOGS_KEY = "skip.catalogs.for.existing.events"

        /** Control if attachments sent by capture agents for scheduled events are skipped.  */
        private val SKIP_ATTACHMENTS_KEY = "skip.attachments.for.existing.events"

        /** The user for download from external sources  */
        private var downloadUser = DOWNLOAD_USER

        /** The password for download from external sources  */
        private var downloadPassword = DOWNLOAD_PASSWORD

        /** The external source dns name  */
        private var downloadSource = DOWNLOAD_SOURCE

        /** Create a media inspection job for a mediapackage element.  */
        fun newEnrichJob(svc: MediaInspectionService?): Function<MediaPackageElement, Job> {
            return object : Function.X<MediaPackageElement, Job>() {
                @Throws(Exception::class)
                public override fun xapply(e: MediaPackageElement): Job {
                    return svc!!.enrich(e, true)
                }
            }
        }

        /**
         * Interpret the payload of a completed Job as a MediaPackageElement. Wait for the job to complete if necessary.
         */
        fun payloadAsTrack(reg: ServiceRegistry): Function<Job, Track> {
            return object : Function.X<Job, Track>() {
                @Throws(MediaPackageException::class)
                public override fun xapply(job: Job): Track {
                    waitForJob(reg, none(0L), job)
                    return MediaPackageElementParser.getFromXml(job.payload) as Track
                }
            }
        }
    }
}
