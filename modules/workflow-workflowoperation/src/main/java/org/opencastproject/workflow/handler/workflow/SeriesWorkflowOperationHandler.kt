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
package org.opencastproject.workflow.handler.workflow

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.CatalogSelector
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreUtil
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.series.api.SeriesException
import org.opencastproject.series.api.SeriesService
import org.opencastproject.util.Checksum
import org.opencastproject.util.ChecksumType
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.XmlNamespaceBinding
import org.opencastproject.util.XmlNamespaceContext
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Fn2
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.HashSet
import java.util.UUID

/**
 * The workflow definition for handling "series" operations
 */
class SeriesWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The authorization service  */
    private var authorizationService: AuthorizationService? = null

    /** The series service  */
    private var seriesService: SeriesService? = null

    /** The workspace  */
    private var workspace: Workspace? = null

    /** The security service  */
    private var securityService: SecurityService? = null

    /** The list series catalog UI adapters  */
    private val seriesCatalogUIAdapters = ArrayList<SeriesCatalogUIAdapter>()

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param authorizationService
     * the authorization service
     */
    fun setAuthorizationService(authorizationService: AuthorizationService) {
        this.authorizationService = authorizationService
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param seriesService
     * the series service
     */
    fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param workspace
     * the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param securityService
     * the securityService
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi callback to add [SeriesCatalogUIAdapter] instance.  */
    fun addCatalogUIAdapter(catalogUIAdapter: SeriesCatalogUIAdapter) {
        seriesCatalogUIAdapters.add(catalogUIAdapter)
    }

    /** OSGi callback to remove [SeriesCatalogUIAdapter] instance.  */
    fun removeCatalogUIAdapter(catalogUIAdapter: SeriesCatalogUIAdapter) {
        seriesCatalogUIAdapters.remove(catalogUIAdapter)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running series workflow operation")

        val mediaPackage = workflowInstance.mediaPackage

        val optSeries = getOptConfig(workflowInstance.currentOperation, SERIES_PROPERTY)
        val optAttachFlavors = getOptConfig(workflowInstance.currentOperation, ATTACH_PROPERTY)
        val applyAcl = getOptConfig(workflowInstance.currentOperation, APPLY_ACL_PROPERTY).map(toBoolean)
                .getOr(false)
        val optCopyMetadata = getOptConfig(workflowInstance.currentOperation, COPY_METADATA_PROPERTY)
        val defaultNamespace = getOptConfig(workflowInstance.currentOperation, DEFAULT_NS_PROPERTY)
                .getOr(DublinCore.TERMS_NS_URI)
        logger.debug("Using default namespace: '{}'", defaultNamespace)

        if (optSeries.isSome && optSeries.get() != mediaPackage.series) {
            logger.info("Changing series id from '{}' to '{}'", StringUtils.trimToEmpty(mediaPackage.series),
                    optSeries.get())
            mediaPackage.series = optSeries.get()
        }

        val seriesId = mediaPackage.series
        if (seriesId == null) {
            logger.info("No series set, skip operation")
            return createResult(mediaPackage, Action.SKIP)
        }

        val series: DublinCoreCatalog
        try {
            series = seriesService!!.getSeries(seriesId)
        } catch (e: NotFoundException) {
            logger.info("No series with the identifier '{}' found, skip operation", seriesId)
            return createResult(mediaPackage, Action.SKIP)
        } catch (e: UnauthorizedException) {
            logger.warn("Not authorized to get series with identifier '{}' found, skip operation", seriesId)
            return createResult(mediaPackage, Action.SKIP)
        } catch (e: SeriesException) {
            logger.error("Unable to get series with identifier '{}', skip operation: {}", seriesId,
                    ExceptionUtils.getStackTrace(e))
            throw WorkflowOperationException(e)
        }

        mediaPackage.seriesTitle = series.getFirst(DublinCore.PROPERTY_TITLE)

        // Process extra metadata
        val extraMetadata = HashSet<EName>()
        if (optCopyMetadata.isSome) {
            for (strEName in optCopyMetadata.get().split(",+\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                try {
                    if (!strEName.isEmpty()) {
                        extraMetadata.add(EName.fromString(strEName, defaultNamespace))
                    }
                } catch (iae: IllegalArgumentException) {
                    logger.warn("Ignoring incorrect dublincore metadata property: '{}'", strEName)
                }

        }

        // Update the episode catalog
        for (episodeCatalog in mediaPackage.getCatalogs(MediaPackageElements.EPISODE)) {
            val episodeDublinCore = DublinCoreUtil.loadDublinCore(workspace!!, episodeCatalog)
            // Make sure the MP catalog has bindings defined
            episodeDublinCore.addBindings(
                    XmlNamespaceContext.mk(XmlNamespaceBinding.mk(DublinCore.TERMS_NS_PREFIX, DublinCore.TERMS_NS_URI)))
            episodeDublinCore.addBindings(XmlNamespaceContext
                    .mk(XmlNamespaceBinding.mk(DublinCore.ELEMENTS_1_1_NS_PREFIX, DublinCore.ELEMENTS_1_1_NS_URI)))
            episodeDublinCore.addBindings(XmlNamespaceContext
                    .mk(XmlNamespaceBinding.mk(DublinCores.OC_PROPERTY_NS_PREFIX, DublinCores.OC_PROPERTY_NS_URI)))
            episodeDublinCore[DublinCore.PROPERTY_IS_PART_OF] = seriesId
            for (property in extraMetadata) {
                if (!episodeDublinCore.hasValue(property) && series.hasValue(property)) {
                    episodeDublinCore[property] = series[property]
                }
            }
            try {
                IOUtils.toInputStream(episodeDublinCore.toXmlString(), "UTF-8").use { `in` ->
                    val filename = FilenameUtils.getName(episodeCatalog.getURI().toString())
                    val uri = workspace!!.put(mediaPackage.identifier.toString(), episodeCatalog.identifier, filename, `in`)
                    episodeCatalog.setURI(uri)
                    // setting the URI to a new source so the checksum will most like be invalid
                    episodeCatalog.checksum = null
                }
            } catch (e: Exception) {
                logger.error("Unable to update episode catalog isPartOf field", e)
                throw WorkflowOperationException(e)
            }

        }

        // Attach series catalogs
        if (optAttachFlavors.isSome) {
            // Remove existing series catalogs
            val catalogSelector = CatalogSelector()
            val seriesFlavors = StringUtils.split(optAttachFlavors.get(), ",")
            for (flavor in seriesFlavors) {
                if ("*" == flavor) {
                    catalogSelector.addFlavor("*/*")
                } else {
                    catalogSelector.addFlavor(flavor)
                }
            }
            for (c in catalogSelector.select(mediaPackage, false)) {
                if (MediaPackageElements.SERIES.equals(c.flavor) || "series" == c.flavor.subtype) {
                    mediaPackage.remove(c)
                }
            }

            val adapters = getSeriesCatalogUIAdapters()
            for (flavorString in seriesFlavors) {
                val flavor: MediaPackageElementFlavor
                if ("*" == flavorString) {
                    flavor = MediaPackageElementFlavor.parseFlavor("*/*")
                } else {
                    flavor = MediaPackageElementFlavor.parseFlavor(flavorString)
                }
                for (a in adapters) {
                    val adapterFlavor = MediaPackageElementFlavor.parseFlavor(a.flavor)
                    if (flavor.matches(adapterFlavor)) {
                        if (MediaPackageElements.SERIES.eq(a.flavor)) {
                            addDublinCoreCatalog(series, MediaPackageElements.SERIES, mediaPackage)
                        } else {
                            try {
                                val seriesElementData = seriesService!!.getSeriesElementData(seriesId, adapterFlavor.type!!)
                                if (seriesElementData.isSome) {
                                    val catalog = DublinCores.read(ByteArrayInputStream(seriesElementData.get()))
                                    addDublinCoreCatalog(catalog, adapterFlavor, mediaPackage)
                                } else {
                                    logger.warn("No extended series catalog found for flavor '{}' and series '{}', skip adding catalog",
                                            adapterFlavor.type, seriesId)
                                }
                            } catch (e: SeriesException) {
                                logger.error("Unable to load extended series metadata for flavor {}", adapterFlavor.type)
                                throw WorkflowOperationException(e)
                            }

                        }
                    }
                }
            }
        }

        if (applyAcl!!) {
            try {
                val acl = seriesService!!.getSeriesAccessControl(seriesId)
                if (acl != null)
                    authorizationService!!.setAcl(mediaPackage, AclScope.Series, acl)
            } catch (e: Exception) {
                logger.error("Unable to update series ACL", e)
                throw WorkflowOperationException(e)
            }

        }
        return createResult(mediaPackage, Action.CONTINUE)
    }

    /**
     * @param organization
     * The organization to filter the results with.
     * @return A [List] of [SeriesCatalogUIAdapter] that provide the metadata to the front end.
     */
    private fun getSeriesCatalogUIAdapters(): List<SeriesCatalogUIAdapter> {
        val organization = securityService!!.organization.id
        return Stream.`$`(seriesCatalogUIAdapters).filter(seriesOrganizationFilter._2(organization)).toList()
    }

    @Throws(WorkflowOperationException::class)
    private fun addDublinCoreCatalog(catalog: DublinCoreCatalog, flavor: MediaPackageElementFlavor,
                                     mediaPackage: MediaPackage): MediaPackage {
        try {
            IOUtils.toInputStream(catalog.toXmlString(), "UTF-8").use { `in` ->
                val elementId = UUID.randomUUID().toString()
                val catalogUrl = workspace!!.put(mediaPackage.identifier.compact(), elementId, "dublincore.xml", `in`)
                logger.info("Adding catalog with flavor {} to mediapackage {}", flavor, mediaPackage)
                val mpe = mediaPackage.add(catalogUrl, MediaPackageElement.Type.Catalog, flavor)
                mpe.identifier = elementId
                mpe.checksum = Checksum.create(ChecksumType.DEFAULT_TYPE, workspace!!.get(catalogUrl))
                return mediaPackage
            }
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        } catch (e: NotFoundException) {
            throw WorkflowOperationException(e)
        }

    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(SeriesWorkflowOperationHandler::class.java)

        /** Name of the configuration option that provides the optional series identifier  */
        val SERIES_PROPERTY = "series"

        /** Name of the configuration option that provides the flavors of the series catalogs to attach  */
        val ATTACH_PROPERTY = "attach"

        /** Name of the configuration option that provides whether the ACL should be applied or not  */
        val APPLY_ACL_PROPERTY = "apply-acl"

        /** Name of the configuration key that specifies the list of series metadata to be copied to the episode  */
        val COPY_METADATA_PROPERTY = "copy-metadata"

        /** Name of the configuration key that specifies the default namespace for the metadata to be copied to the episode  */
        val DEFAULT_NS_PROPERTY = "default-namespace"

        private val seriesOrganizationFilter = object : Fn2<SeriesCatalogUIAdapter, String, Boolean>() {
            override fun apply(catalogUIAdapter: SeriesCatalogUIAdapter, organization: String): Boolean {
                return catalogUIAdapter.organization == organization
            }
        }

        /** Convert a string into a boolean.  */
        private val toBoolean = object : Fn<String, Boolean>() {
            override fun apply(s: String): Boolean {
                return BooleanUtils.toBoolean(s)
            }
        }
    }

}
