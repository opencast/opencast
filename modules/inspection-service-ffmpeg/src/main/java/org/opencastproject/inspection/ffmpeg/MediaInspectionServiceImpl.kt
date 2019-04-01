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

package org.opencastproject.inspection.ffmpeg

import org.opencastproject.inspection.api.MediaInspectionException
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.inspection.api.util.Options
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.LoadUtil
import org.opencastproject.workspace.api.Workspace

import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.util.Arrays
import java.util.Dictionary

/** Inspects media via ffprobe.  */
/** Creates a new media inspection service instance.  */
class MediaInspectionServiceImpl : AbstractJobProducer(JOB_TYPE), MediaInspectionService, ManagedService {

    /** The load introduced on the system by creating an inspect job  */
    private var inspectJobLoad = DEFAULT_INSPECT_JOB_LOAD

    /** The load introduced on the system by creating an enrich job  */
    private var enrichJobLoad = DEFAULT_ENRICH_JOB_LOAD

    private var workspace: Workspace? = null
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getServiceRegistry
     */
    protected override var serviceRegistry: ServiceRegistry? = null
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

    @Volatile
    private var inspector: MediaInspector? = null

    /** List of available operations on jobs  */
    private enum class Operation {
        Inspect, Enrich
    }

    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        /* Configure analyzer */
        val path = cc.bundleContext.getProperty(FFmpegAnalyzer.FFPROBE_BINARY_CONFIG)
        val ffprobeBinary: String
        if (path == null) {
            logger.debug("DEFAULT " + FFmpegAnalyzer.FFPROBE_BINARY_CONFIG + ": " + FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT)
            ffprobeBinary = FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT
        } else {
            logger.debug("FFprobe config binary: {}", path)
            ffprobeBinary = path
        }
        inspector = MediaInspector(workspace, ffprobeBinary)
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>?) {
        if (properties == null)
            return

        inspectJobLoad = LoadUtil.getConfiguredLoadValue(properties, INSPECT_JOB_LOAD_KEY, DEFAULT_INSPECT_JOB_LOAD,
                serviceRegistry!!)
        enrichJobLoad = LoadUtil.getConfiguredLoadValue(properties, ENRICH_JOB_LOAD_KEY, DEFAULT_ENRICH_JOB_LOAD,
                serviceRegistry!!)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(Exception::class)
    override fun process(job: Job): String {
        var op: Operation? = null
        val operation = job.operation
        val arguments = job.arguments
        try {
            op = Operation.valueOf(operation)
            var inspectedElement: MediaPackageElement? = null
            var options: Map<String, String>? = null
            when (op) {
                MediaInspectionServiceImpl.Operation.Inspect -> {
                    val uri = URI.create(arguments[0])
                    options = Options.fromJson(arguments[1])
                    inspectedElement = inspector!!.inspectTrack(uri, options)
                }
                MediaInspectionServiceImpl.Operation.Enrich -> {
                    val element = MediaPackageElementParser.getFromXml(arguments[0])
                    val overwrite = java.lang.Boolean.parseBoolean(arguments[1])
                    options = Options.fromJson(arguments[2])
                    inspectedElement = inspector!!.enrich(element, overwrite, options)
                }
                else -> throw IllegalStateException("Don't know how to handle operation '$operation'")
            }
            return MediaPackageElementParser.getAsXml(inspectedElement)
        } catch (e: IllegalArgumentException) {
            throw ServiceRegistryException("This service can't handle operations of type '$op'", e)
        } catch (e: IndexOutOfBoundsException) {
            throw ServiceRegistryException("This argument list for operation '$op' does not meet expectations", e)
        } catch (e: Exception) {
            throw ServiceRegistryException("Error handling operation '$op'", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.inspection.api.MediaInspectionService.inspect
     */
    @Throws(MediaInspectionException::class)
    override fun inspect(uri: URI): Job {
        return inspect(uri, Options.NO_OPTION)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.inspection.api.MediaInspectionService.inspect
     */
    @Throws(MediaInspectionException::class)
    override fun inspect(uri: URI, options: Map<String, String>): Job {
        assert(options != null)
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Inspect.toString(), Arrays.asList<T>(uri.toString(),
                    Options.toJson(options)), inspectJobLoad)
        } catch (e: ServiceRegistryException) {
            throw MediaInspectionException(e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.inspection.api.MediaInspectionService.enrich
     */
    @Throws(MediaInspectionException::class, MediaPackageException::class)
    override fun enrich(element: MediaPackageElement, override: Boolean): Job {
        return enrich(element, override, Options.NO_OPTION)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.inspection.api.MediaInspectionService.enrich
     */
    @Throws(MediaInspectionException::class, MediaPackageException::class)
    override fun enrich(element: MediaPackageElement, override: Boolean, options: Map<String, String>): Job {
        assert(options != null)
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Enrich.toString(),
                    Arrays.asList<T>(MediaPackageElementParser.getAsXml(element), java.lang.Boolean.toString(override),
                            Options.toJson(options)), enrichJobLoad)
        } catch (e: ServiceRegistryException) {
            throw MediaInspectionException(e)
        }

    }

    protected fun setWorkspace(workspace: Workspace) {
        logger.debug("setting $workspace")
        this.workspace = workspace
    }

    companion object {

        /** The load introduced on the system by creating an inspect job  */
        val DEFAULT_INSPECT_JOB_LOAD = 0.2f

        /** The load introduced on the system by creating an enrich job  */
        val DEFAULT_ENRICH_JOB_LOAD = 0.2f

        /** The key to look for in the service configuration file to override the [DEFAULT_INSPECT_JOB_LOAD]  */
        val INSPECT_JOB_LOAD_KEY = "job.load.inspect"

        /** The key to look for in the service configuration file to override the [DEFAULT_ENRICH_JOB_LOAD]  */
        val ENRICH_JOB_LOAD_KEY = "job.load.enrich"

        private val logger = LoggerFactory.getLogger(MediaInspectionServiceImpl::class.java)
    }
}
