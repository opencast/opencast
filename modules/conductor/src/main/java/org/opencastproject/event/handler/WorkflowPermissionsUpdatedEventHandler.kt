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

package org.opencastproject.event.handler

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.message.broker.api.series.SeriesItem
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.metadata.dublincore.DublinCoreUtil
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.api.User
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.WorkflowException
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowQuery
import org.opencastproject.workflow.api.WorkflowService
import org.opencastproject.workflow.api.WorkflowSet
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URI

/** Responds to series events by re-distributing metadata and security policy files to workflows.  */
class WorkflowPermissionsUpdatedEventHandler {

    /** The workflow service  */
    protected var workflowService: WorkflowService? = null

    /** The security service  */
    protected var securityService: SecurityService? = null

    /** The authorization service  */
    protected var authorizationService: AuthorizationService? = null

    /** The organization directory  */
    protected var organizationDirectoryService: OrganizationDirectoryService? = null

    /** Dublin core catalog service  */
    protected var dublinCoreService: DublinCoreCatalogService? = null

    /** The workspace  */
    protected var workspace: Workspace? = null

    /** The system account to use for running asynchronous events  */
    protected var systemAccount: String? = null

    /**
     * OSGI callback for component activation.
     *
     * @param bundleContext
     * the OSGI bundle context
     */
    protected fun activate(bundleContext: BundleContext) {
        this.systemAccount = bundleContext.getProperty("org.opencastproject.security.digest.user")
    }

    /**
     * @param workspace
     * the workspace to set
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * @param dublinCoreService
     * the dublin core service to set
     */
    fun setDublinCoreCatalogService(dublinCoreService: DublinCoreCatalogService) {
        this.dublinCoreService = dublinCoreService
    }

    /**
     * @param workflowService
     * the workflow service to set
     */
    fun setWorkflowService(workflowService: WorkflowService) {
        this.workflowService = workflowService
    }

    /**
     * @param securityService
     * the securityService to set
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /**
     * @param authorizationService
     * the authorizationService to set
     */
    fun setAuthorizationService(authorizationService: AuthorizationService) {
        this.authorizationService = authorizationService
    }

    /**
     * @param organizationDirectoryService
     * the organizationDirectoryService to set
     */
    fun setOrganizationDirectoryService(organizationDirectoryService: OrganizationDirectoryService) {
        this.organizationDirectoryService = organizationDirectoryService
    }

    fun handleEvent(seriesItem: SeriesItem) {
        // A series or its ACL has been updated. Find any mediapackages with that series, and update them.
        logger.debug("Handling {}", seriesItem)
        val seriesId = seriesItem.seriesId

        // We must be an administrative user to make this query
        val prevUser = securityService!!.user
        val prevOrg = securityService!!.organization
        try {
            securityService!!.user = SecurityUtil.createSystemUser(systemAccount!!, prevOrg)

            // Note: getWorkflowInstances will only return a given number of results (default 20)
            var q = WorkflowQuery().withSeriesId(seriesId)
            var result = workflowService!!.getWorkflowInstancesForAdministrativeRead(q)
            var offset: Int? = 0

            while (result.size() > 0) {
                for (instance in result.items) {
                    if (!instance.isActive)
                        continue

                    val org = organizationDirectoryService!!.getOrganization(instance.organizationId)
                    securityService!!.organization = org

                    val mp = instance.mediaPackage

                    // Update the series XACML file
                    if (SeriesItem.Type.UpdateAcl == seriesItem.type) {
                        // Build a new XACML file for this mediapackage
                        try {
                            if (seriesItem.overrideEpisodeAcl!!) {
                                authorizationService!!.removeAcl(mp, AclScope.Episode)
                            }
                            authorizationService!!.setAcl(mp, AclScope.Series, seriesItem.acl!!)
                        } catch (e: MediaPackageException) {
                            logger.error("Error setting ACL for media package {}", mp.identifier, e)
                        }

                    }

                    // Update the series dublin core
                    if (SeriesItem.Type.UpdateCatalog == seriesItem.type) {
                        val seriesDublinCore = seriesItem.metadata
                        mp.seriesTitle = seriesDublinCore.getFirst(DublinCore.PROPERTY_TITLE)

                        // Update the series dublin core
                        val seriesCatalogs = mp.getCatalogs(MediaPackageElements.SERIES)
                        if (seriesCatalogs.size == 1) {
                            val c = seriesCatalogs[0]
                            val filename = FilenameUtils.getName(c.getURI().toString())
                            val uri = workspace!!.put(mp.identifier.toString(), c.identifier, filename,
                                    dublinCoreService!!.serialize(seriesDublinCore))
                            c.setURI(uri)
                            // setting the URI to a new source so the checksum will most like be invalid
                            c.checksum = null
                        }
                    }

                    // Remove the series catalog and isPartOf from episode catalog
                    if (SeriesItem.Type.Delete == seriesItem.type) {
                        mp.series = null
                        mp.seriesTitle = null
                        for (c in mp.getCatalogs(MediaPackageElements.SERIES)) {
                            mp.remove(c)
                            try {
                                workspace!!.delete(c.getURI())
                            } catch (e: NotFoundException) {
                                logger.info("No series catalog to delete found {}", c.getURI())
                            }

                        }
                        for (episodeCatalog in mp.getCatalogs(MediaPackageElements.EPISODE)) {
                            val episodeDublinCore = DublinCoreUtil.loadDublinCore(workspace, episodeCatalog)
                            episodeDublinCore.remove(DublinCore.PROPERTY_IS_PART_OF)
                            val filename = FilenameUtils.getName(episodeCatalog.getURI().toString())
                            val uri = workspace!!.put(mp.identifier.toString(), episodeCatalog.identifier, filename,
                                    dublinCoreService!!.serialize(episodeDublinCore))
                            episodeCatalog.setURI(uri)
                            // setting the URI to a new source so the checksum will most like be invalid
                            episodeCatalog.checksum = null
                        }
                    }

                    // Update the search index with the modified mediapackage
                    workflowService!!.update(instance)
                }
                offset++
                q = q.withStartPage(offset!!.toLong())
                result = workflowService!!.getWorkflowInstancesForAdministrativeRead(q)
            }
        } catch (e: WorkflowException) {
            logger.warn(e.message)
        } catch (e: NotFoundException) {
            logger.warn(e.message)
        } catch (e: IOException) {
            logger.warn(e.message)
        } catch (e: UnauthorizedException) {
            logger.warn(e.message)
        } finally {
            securityService!!.organization = prevOrg
            securityService!!.user = prevUser
        }
    }

    companion object {

        /** The logger  */
        protected val logger = LoggerFactory.getLogger(WorkflowPermissionsUpdatedEventHandler::class.java)
    }

}
