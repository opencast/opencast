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

import org.opencastproject.security.util.SecurityUtil.getUserAndOrganization
import org.opencastproject.util.data.Collections.dict
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.ingest.api.IngestService
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.security.util.SecurityContext
import org.opencastproject.series.api.SeriesService
import org.opencastproject.util.data.Effect
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple
import org.opencastproject.workingfilerepository.api.WorkingFileRepository

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.apache.felix.fileinstall.ArtifactInstaller
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import org.osgi.service.cm.Configuration
import org.osgi.service.cm.ConfigurationAdmin
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.util.Dictionary
import java.util.Enumeration
import java.util.HashMap
import java.util.Objects

/**
 * The inbox scanner monitors a directory for incoming media packages.
 *
 *
 * There is one InboxScanner instance per inbox. Each instance is configured by a config file in
 * `.../etc/load` named `<inbox-scanned-pid>-<name>.cfg` where `name`
 * can be arbitrarily chosen and has no further meaning. `inbox-scanned-pid` must confirm to the PID given to
 * the InboxScanner in the declarative service (DS) configuration `OSGI-INF/inbox-scanner-service.xml`.
 *
 * <h3>Implementation notes</h3>
 * Monitoring leverages Apache FileInstall by implementing [ArtifactInstaller].
 *
 * @see Ingestor
 */
class InboxScannerService : ArtifactInstaller, ManagedService {

    private var ingestService: IngestService? = null
    private var workingFileRepository: WorkingFileRepository? = null
    private var securityService: SecurityService? = null
    private var userDir: UserDirectoryService? = null
    private var orgDir: OrganizationDirectoryService? = null
    private var seriesService: SeriesService? = null

    private var cc: ComponentContext? = null

    @Volatile
    private var ingestor = none()
    @Volatile
    private var fileInstallCfg = none()

    /** OSGi callback.  */
    // synchronized with updated(Dictionary)
    @Synchronized
    fun activate(cc: ComponentContext) {
        this.cc = cc
    }

    /** OSGi callback.  */
    fun deactivate() {
        fileInstallCfg.foreach(removeFileInstallCfg)
    }

    // synchronized with activate(ComponentContext)
    @Synchronized
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>) {
        // build scanner configuration
        val orgId = getCfg(properties, USER_ORG)
        val userId = getCfg(properties, USER_NAME)
        val mediaFlavor = getCfg(properties, MEDIA_FLAVOR)
        val workflowDefinition = Objects.toString(properties.get(WORKFLOW_DEFINITION), null)
        val workflowConfig = getCfgAsMap(properties, WORKFLOW_CONFIG)
        val interval = NumberUtils.toInt(Objects.toString(properties.get(INBOX_POLL), "5000"))
        val inbox = File(getCfg(properties, INBOX_PATH))
        if (!inbox.isDirectory) {
            try {
                FileUtils.forceMkdir(inbox)
            } catch (e: IOException) {
                throw ConfigurationException(INBOX_PATH,
                        String.format("%s does not exists and could not be created", inbox.absolutePath))
            }

        }
        /* We need to be able to read from the inbox to get files from there */
        if (!inbox.canRead()) {
            throw ConfigurationException(INBOX_PATH, String.format("Cannot read from %s", inbox.absolutePath))
        }
        /* We need to be able to write to the inbox to remove files after they have been ingested */
        if (!inbox.canWrite()) {
            throw ConfigurationException(INBOX_PATH, String.format("Cannot write to %s", inbox.absolutePath))
        }
        val maxThreads = NumberUtils.toInt(Objects.toString(properties.get(INBOX_THREADS), "1"))
        val maxTries = NumberUtils.toInt(Objects.toString(properties.get(INBOX_TRIES), "3"))
        val secondsBetweenTries = NumberUtils.toInt(Objects.toString(properties.get(INBOX_TRIES_BETWEEN_SEC), "300"))
        val secCtx = getUserAndOrganization(securityService, orgDir, orgId, userDir, userId)
                .bind(object : Function<Tuple<User, Organization>, Option<SecurityContext>>() {
                    override fun apply(a: Tuple<User, Organization>): Option<SecurityContext> {
                        return some(SecurityContext(securityService!!, a.b, a.a))
                    }
                })
        // Only setup new inbox if security context could be aquired
        if (secCtx.isSome) {
            // remove old file install configuration
            fileInstallCfg.foreach(removeFileInstallCfg)
            // set up new file install config
            fileInstallCfg = some(configureFileInstall(cc!!.bundleContext, inbox, interval))
            // create new scanner
            val ingestor = Ingestor(ingestService, workingFileRepository, secCtx.get(), workflowDefinition,
                    workflowConfig, mediaFlavor, inbox, maxThreads, seriesService, maxTries, secondsBetweenTries)
            this.ingestor = some(ingestor)
            Thread(ingestor).start()
            logger.info("Now watching inbox {}", inbox.absolutePath)
        } else {
            logger.warn("Cannot create security context for user {}, organization {}. " + "Either the organization or the user does not exist", userId, orgId)
        }
    }

    // --

    // FileInstall callback, called on a different thread
    // Attention: This method may be called _before_ the updated(Dictionary) which means that config parameters
    // are not set yet.
    override fun canHandle(artifact: File): Boolean {
        return ingestor.fmap(object : Function<Ingestor, Boolean>() {
            override fun apply(ingestor: Ingestor): Boolean? {
                return ingestor.canHandle(artifact)
            }
        }).getOrElse(false)
    }

    @Throws(Exception::class)
    override fun install(artifact: File) {
        logger.trace("install(): {}", artifact.name)
        ingestor.foreach(object : Effect<Ingestor>() {
            override fun run(ingestor: Ingestor) {
                ingestor.ingest(artifact)
            }
        })
    }

    override fun update(artifact: File) {
        logger.trace("update(): {}", artifact.name)
    }

    override fun uninstall(artifact: File) {
        logger.trace("uninstall(): {}", artifact.name)
        ingestor.foreach(object : Effect<Ingestor>() {
            override fun run(ingestor: Ingestor) {
                ingestor.cleanup(artifact)
            }
        })
    }

    // --

    /** OSGi callback to set the ingest service.  */
    fun setIngestService(ingestService: IngestService) {
        this.ingestService = ingestService
    }

    /** OSGi callback to set the workspace  */
    fun setWorkingFileRepository(workingFileRepository: WorkingFileRepository) {
        this.workingFileRepository = workingFileRepository
    }

    /** OSGi callback to set the security service.  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi callback to set the user directory.  */
    fun setUserDirectoryService(userDirectoryService: UserDirectoryService) {
        this.userDir = userDirectoryService
    }

    /** OSGi callback to set the organization directory server.  */
    fun setOrganizationDirectoryService(organizationDirectoryService: OrganizationDirectoryService) {
        this.orgDir = organizationDirectoryService
    }

    fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(InboxScannerService::class.java)

        /** The configuration key to use for determining the user to run as for ingest  */
        val USER_NAME = "user.name"

        /** The configuration key to use for determining the user's organization  */
        val USER_ORG = "user.organization"

        /** The configuration key to use for determining the workflow definition to use for ingest  */
        val WORKFLOW_DEFINITION = "workflow.definition"

        /** The configuration key to use for determining the default media flavor  */
        val MEDIA_FLAVOR = "media.flavor"


        /** The configuration key to use for determining the workflow configuration to use for ingest  */
        val WORKFLOW_CONFIG = "workflow.config"

        /** The configuration key to use for determining the inbox path  */
        val INBOX_PATH = "inbox.path"

        /** The configuration key to use for determining the polling interval in ms.  */
        val INBOX_POLL = "inbox.poll"

        val INBOX_THREADS = "inbox.threads"
        val INBOX_TRIES = "inbox.tries"
        val INBOX_TRIES_BETWEEN_SEC = "inbox.tries.between.sec"

        private val removeFileInstallCfg = object : Effect.X<Configuration>() {
            @Throws(Exception::class)
            override fun xrun(cfg: Configuration) {
                cfg.delete()
            }
        }

        /**
         * Setup an Apache FileInstall configuration for the inbox folder this scanner is responsible for.
         *
         * see section 104.4.1 Location Binding, paragraph 4, of the OSGi Spec 4.2 The correct permissions are needed in order
         * to set configuration data for a bundle other than the calling bundle itself.
         */
        private fun configureFileInstall(bc: BundleContext, inbox: File, interval: Int): Configuration {
            val caRef = bc.getServiceReference(ConfigurationAdmin::class.java.name)
                    ?: throw Error("Cannot obtain a reference to the ConfigurationAdmin service")
            val fileInstallConfig = dict(tuple("felix.fileinstall.dir", inbox.absolutePath),
                    tuple("felix.fileinstall.poll", Integer.toString(interval)),
                    tuple("felix.fileinstall.subdir.mode", "recurse"))

            // update file install config with the new directory
            try {
                val fileInstallBundleLocation = bc.getServiceReferences("org.osgi.service.cm.ManagedServiceFactory",
                        "(service.pid=org.apache.felix.fileinstall)")[0].bundle.location
                val conf = (bc.getService<Any>(caRef) as ConfigurationAdmin).createFactoryConfiguration(
                        "org.apache.felix.fileinstall", fileInstallBundleLocation)
                conf.update(fileInstallConfig)
                return conf
            } catch (e: Exception) {
                throw Error(e)
            }

        }

        /**
         * Get a mandatory, non-blank value from a dictionary.
         *
         * @throws ConfigurationException
         * key does not exist or its value is blank
         */
        @Throws(ConfigurationException::class)
        private fun getCfg(d: Dictionary<*, *>, key: String): String {
            val p = d.get(key) ?: throw ConfigurationException(key, "does not exist")
            val ps = p.toString()
            if (StringUtils.isBlank(ps))
                throw ConfigurationException(key, "is blank")
            return ps
        }

        private fun getCfgAsMap(d: Dictionary<*, *>?, key: String): Map<String, String> {

            val config = HashMap<String, String>()
            if (d == null) return config
            val e = d.keys()
            while (e.hasMoreElements()) {
                val dKey = Objects.toString(e.nextElement())
                if (dKey.startsWith(key)) {
                    config[dKey.substring(key.length + 1)] = Objects.toString(d.get(dKey), null)
                }
            }
            return config
        }
    }
}
