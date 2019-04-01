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

package org.opencastproject.dataloader

import com.entwinemedia.fn.Prelude.chuck
import org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.metadata.dublincore.OpencastDctermsDublinCore
import org.opencastproject.metadata.dublincore.OpencastDctermsDublinCore.Series
import org.opencastproject.scheduler.api.SchedulerService
import org.opencastproject.security.api.AccessControlEntry
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.Permissions
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.api.User
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.series.api.SeriesException
import org.opencastproject.series.api.SeriesService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.Checksum
import org.opencastproject.util.ChecksumType
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.WorkflowDefinition
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowParser
import org.opencastproject.workflow.api.WorkflowService
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.data.Opt

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Date
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A data loader to populate events provider with sample data for testing scalability.
 */
class EventsLoader {

    /** The series service  */
    protected var seriesService: SeriesService? = null

    /** The workflow service  */
    protected var workflowService: WorkflowService? = null

    /** The scheduler service  */
    protected var schedulerService: SchedulerService? = null

    protected var assetManager: AssetManager? = null

    /** The security service  */
    protected var securityService: SecurityService? = null

    /** The service registry  */
    protected var serviceRegistry: ServiceRegistry? = null

    /** The authorization service  */
    protected var authorizationService: AuthorizationService? = null

    /** The workspace  */
    protected var workspace: Workspace? = null

    private val singleThreadExecutor = Executors.newSingleThreadExecutor()

    private var systemUserName: String? = null

    private val defaultAcl = AccessControlList(
            AccessControlEntry("ROLE_ADMIN", Permissions.Action.WRITE.toString(), true),
            AccessControlEntry("ROLE_ADMIN", Permissions.Action.READ.toString(), true),
            AccessControlEntry("ROLE_USER", Permissions.Action.READ.toString(), true))

    /**
     * Callback on component activation.
     */
    @Throws(Exception::class)
    protected fun activate(cc: ComponentContext) {

        val csvPath = StringUtils.trimToNull(cc.bundleContext.getProperty("org.opencastproject.dataloader.csv"))
        if (StringUtils.isBlank(csvPath)) {
            return  // no file is set
        }

        systemUserName = cc.bundleContext.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)

        val csv = File(csvPath)

        // Load the demo users, if necessary
        if (csv.exists() && serviceRegistry!!.count(null!!, null!!) == 0L) {
            // Load events by CSV file
            Loader(csv).start()
        }
    }

    private fun addArchiveEntry(mediaPackage: MediaPackage) {
        val user = securityService!!.user
        val organization = securityService!!.organization
        singleThreadExecutor.execute { SecurityUtil.runAs(securityService!!, organization, user, { assetManager!!.takeSnapshot(DEFAULT_OWNER, mediaPackage) }) }
    }

    @Throws(Exception::class)
    private fun addWorkflowEntry(mediaPackage: MediaPackage) {
        val def = workflowService!!.getWorkflowDefinitionById("full")
        val workflowInstance = WorkflowInstanceImpl(def, mediaPackage, null, securityService!!.user,
                securityService!!.organization, HashMap())
        workflowInstance.state = WorkflowState.SUCCEEDED

        val xml = WorkflowParser.toXml(workflowInstance)

        // create job
        var job = serviceRegistry!!.createJob(WorkflowService.JOB_TYPE, "START_WORKFLOW", null!!, null!!, false)
        job.status = Status.FINISHED
        job.payload = xml
        job = serviceRegistry!!.updateJob(job)

        workflowInstance.id = job.id
        workflowService!!.update(workflowInstance)
    }

    @Throws(Exception::class)
    private fun addSchedulerEntry(event: EventEntry, mediaPackage: MediaPackage) {
        val endDate = DateTime(event.recordingDate.time).plusMinutes(event.duration).toDate()
        schedulerService!!.addEvent(event.recordingDate, endDate, event.captureAgent,
                emptySet(), mediaPackage, emptyMap(),
                emptyMap(), Opt.some("org.opencastproject.dataloader"))
        cleanUpMediaPackage(mediaPackage)
    }

    @Throws(Exception::class)
    private fun execute(csv: CSVParser?) {
        val events = parseCSV(csv!!)
        logger.info("Found {} events to populate", events.size)

        var i = 1
        val now = Date()
        for (event in events) {
            logger.info("Populating event {}", i)

            createSeries(event)

            val mediaPackage = getBasicMediaPackage(event)

            if (now.after(event.recordingDate)) {
                addWorkflowEntry(mediaPackage)
                if (event.isArchive)
                    addArchiveEntry(mediaPackage)
            } else {
                addSchedulerEntry(event, mediaPackage)
            }
            logger.info("Finished populating event {}", i++)
        }
    }

    private fun cleanUpMediaPackage(mp: MediaPackage) {
        for (element in mp.elements) {
            try {
                workspace!!.delete(element.getURI())
            } catch (e: NotFoundException) {
                logger.warn("Unable to find (and hence, delete), this mediapackage '{}' element '{}'", mp.identifier,
                        element.identifier)
            } catch (e: IOException) {
                chuck<Any>(e)
            }

        }
    }

    @Throws(Exception::class)
    private fun getBasicMediaPackage(event: EventEntry): MediaPackage {
        val baseMediapackageUrl = EventsLoader::class.java.getResource("/base_mediapackage.xml")
        val mediaPackage = MediaPackageParser.getFromXml(IOUtils.toString(baseMediapackageUrl))
        val episodeDublinCore = getBasicEpisodeDublinCore(event)
        mediaPackage.date = event.recordingDate
        mediaPackage.identifier = IdImpl(episodeDublinCore.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER)!!)
        mediaPackage.title = event.title
        addDublinCoreCatalog(IOUtils.toInputStream(episodeDublinCore.toXmlString(), "UTF-8"), MediaPackageElements.EPISODE,
                mediaPackage)

        // assign to a series
        if (event.series.isSome) {
            val seriesCatalog = seriesService!!.getSeries(event.series.get())
            mediaPackage.series = event.series.get()
            mediaPackage.seriesTitle = seriesCatalog.getFirst(DublinCoreCatalog.PROPERTY_TITLE)
            addDublinCoreCatalog(IOUtils.toInputStream(seriesCatalog.toXmlString(), "UTF-8"), MediaPackageElements.SERIES,
                    mediaPackage)

            val acl = seriesService!!.getSeriesAccessControl(event.series.get())
            if (acl != null) {
                authorizationService!!.setAcl(mediaPackage, AclScope.Series, acl)
            }
        }

        // Set track URI's to demo file
        for (track in mediaPackage.tracks) {
            var `in`: InputStream? = null
            try {
                `in` = javaClass.getResourceAsStream("/av.mov")
                val uri = workspace!!.put(mediaPackage.identifier.compact(), track.identifier,
                        FilenameUtils.getName(track.toString()), `in`)
                track.setURI(uri)
                track.checksum = Checksum.create(ChecksumType.DEFAULT_TYPE, javaClass.getResourceAsStream("/av.mov"))
            } finally {
                IOUtils.closeQuietly(`in`)
            }
        }
        return mediaPackage
    }

    @Throws(SeriesException::class, UnauthorizedException::class, NotFoundException::class)
    private fun createSeries(event: EventEntry) {
        if (event.series.isNone)
            return

        try {
            // Test if the series already exist, it does not create it.
            seriesService!!.getSeries(event.series.get())
        } catch (e: NotFoundException) {
            val catalog = DublinCores.mkOpencastSeries(event.series.get())
            catalog.updateTitle(event.seriesName.toOpt())

            // If the series does not exist, we create it.
            seriesService!!.updateSeries(catalog.catalog)
            seriesService!!.updateAccessControl(event.series.get(), defaultAcl)
        }

    }

    @Throws(IOException::class)
    private fun getBasicEpisodeDublinCore(event: EventEntry): DublinCoreCatalog {
        val catalog = DublinCores.mkOpencastEpisode(Opt.none())
        catalog.setTitle(event.title)
        catalog.setSpatial(event.captureAgent)
        catalog.setSource(event.source)
        catalog.setCreated(event.recordingDate)
        catalog.setContributor(event.contributor)
        catalog.setCreators(event.presenters)
        catalog.updateDescription(event.description.toOpt())
        catalog.setTemporal(event.recordingDate,
                DateTime(event.recordingDate.time).plusMinutes(event.duration).toDate())
        catalog.updateIsPartOf(event.series.toOpt())
        return catalog.catalog
    }

    private fun parseCSV(csv: CSVParser): List<EventEntry> {
        val arrayList = ArrayList<EventEntry>()
        for (record in csv) {
            val title = record.get(0)
            val description = StringUtils.trimToNull(record.get(1))
            val series = StringUtils.trimToNull(record.get(2))
            val seriesName = StringUtils.trimToNull(record.get(3))

            val days = Integer.parseInt(record.get(4))
            val signum = Math.signum(days.toFloat())
            var now = DateTime.now()
            if (signum > 0) {
                now = now.plusDays(days)
            } else if (signum < 0) {
                now = now.minusDays(days * -1)
            }

            val duration = Integer.parseInt(record.get(5))
            val archive = BooleanUtils.toBoolean(record.get(6))
            val agent = StringUtils.trimToNull(record.get(7))
            val source = StringUtils.trimToNull(record.get(8))
            val contributor = StringUtils.trimToNull(record.get(9))
            val presenters = Arrays.asList(*StringUtils.split(StringUtils.trimToEmpty(record.get(10)), ","))
            val eventEntry = EventEntry(title, now.toDate(), duration, archive, series, agent, source,
                    contributor, description, seriesName, presenters)
            arrayList.add(eventEntry)
        }
        return arrayList
    }

    protected inner class Loader @Throws(IOException::class)
    constructor(csvData: File) : Thread() {

        private var csvParser: CSVParser? = null

        init {
            try {
                logger.info("Reading event test data from csv {}...", csvData)
                csvParser = CSVParser.parse(csvData, Charset.forName("UTF-8"), CSVFormat.RFC4180)
            } catch (e: IOException) {
                logger.error("Unable to parse CSV data from {}", csvData)
                throw e
            }

        }

        override fun run() {
            val org = DefaultOrganization()
            val createSystemUser = SecurityUtil.createSystemUser(systemUserName!!, org)
            SecurityUtil.runAs(securityService!!, org, createSystemUser, {
                try {
                    logger.info("Start populating event test data...")
                    execute(csvParser)
                    logger.info("Finished populating event test data")
                } catch (e: Exception) {
                    logger.error("Unable to populate event test data", e)
                }
            })
        }
    }

    @Throws(IOException::class)
    private fun addDublinCoreCatalog(`in`: InputStream, flavor: MediaPackageElementFlavor, mediaPackage: MediaPackage): MediaPackage {
        try {
            val elementId = UUID.randomUUID().toString()
            val catalogUrl = workspace!!.put(mediaPackage.identifier.compact(), elementId, "dublincore.xml", `in`)
            logger.info("Adding catalog with flavor {} to mediapackage {}", flavor, mediaPackage)
            val mpe = mediaPackage.add(catalogUrl, MediaPackageElement.Type.Catalog, flavor)
            mpe.identifier = elementId
            mpe.checksum = Checksum.create(ChecksumType.DEFAULT_TYPE, workspace!!.get(catalogUrl))
            return mediaPackage
        } catch (e: NotFoundException) {
            throw RuntimeException(e)
        }

    }

    /**
     * @param seriesService
     * the seriesService to set
     */
    fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    /**
     * @param schedulerService
     * the schedulerService to set
     */
    fun setSchedulerService(schedulerService: SchedulerService) {
        this.schedulerService = schedulerService
    }

    /**
     * @param assetManager
     * the asset manager to set
     */
    fun setAssetManager(assetManager: AssetManager) {
        this.assetManager = assetManager
    }

    /**
     * @param securityService
     * the securityService to set
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /**
     * @param workflowService
     * the workflowService to set
     */
    fun setWorkflowService(workflowService: WorkflowService) {
        this.workflowService = workflowService
    }

    /**
     * @param serviceRegistry
     * the serviceRegistry to set
     */
    fun setServiceRegistry(serviceRegistry: ServiceRegistry) {
        this.serviceRegistry = serviceRegistry
    }

    /**
     * @param authorizationService
     * the authorizationService to set
     */
    fun setAuthorizationService(authorizationService: AuthorizationService) {
        this.authorizationService = authorizationService
    }

    /**
     * @param workspace
     * the workspace to set
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    companion object {

        /** The logger  */
        protected val logger = LoggerFactory.getLogger(EventsLoader::class.java)
    }

}
