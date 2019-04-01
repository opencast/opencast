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

import org.opencastproject.assetmanager.api.fn.Enrichments.enrich

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.AssetManagerException
import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.assetmanager.api.query.AResult
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
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
import org.opencastproject.security.api.User
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URI

/**
 * Responds to series events by re-distributing metadata and security policy files to episodes.
 */
class AssetManagerUpdatedEventHandler {

    /** The archive  */
    protected var assetManager: AssetManager? = null

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

            val q = assetManager!!.createQuery()
            val result = q.select(q.snapshot()).where(q.seriesId().eq(seriesId).and(q.version().isLatest)).run()
            for (snapshot in enrich(result).getSnapshots()) {
                val orgId = snapshot.organizationId
                val organization = organizationDirectoryService!!.getOrganization(orgId)
                if (organization == null) {
                    logger.warn("Skipping update of episode {} since organization {} is unknown",
                            snapshot.mediaPackage.identifier.compact(), orgId)
                    continue
                }
                securityService!!.organization = organization

                val mp = snapshot.mediaPackage

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

                // Update the series dublin core or extended metadata
                if (SeriesItem.Type.UpdateCatalog == seriesItem.type || SeriesItem.Type.UpdateElement == seriesItem.type) {
                    var seriesDublinCore: DublinCoreCatalog? = null
                    var catalogType: MediaPackageElementFlavor? = null
                    if (SeriesItem.Type.UpdateCatalog == seriesItem.type) {
                        seriesDublinCore = seriesItem.metadata
                        mp.seriesTitle = seriesDublinCore!!.getFirst(DublinCore.PROPERTY_TITLE)
                        catalogType = MediaPackageElements.SERIES
                    } else {
                        seriesDublinCore = seriesItem.extendedMetadata
                        catalogType = MediaPackageElementFlavor.flavor(seriesItem.elementType, "series")
                    }

                    // Update the series dublin core
                    val seriesCatalogs = mp.getCatalogs(catalogType!!)
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

                // Remove the series catalogs and isPartOf from episode catalog
                if (SeriesItem.Type.Delete == seriesItem.type) {
                    mp.series = null
                    mp.seriesTitle = null
                    for (seriesCatalog in mp.getCatalogs(MediaPackageElements.SERIES)) {
                        mp.remove(seriesCatalog)
                    }
                    authorizationService!!.removeAcl(mp, AclScope.Series)
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
                    // here we don't know the series extended metadata types,
                    // we assume that all series catalog flavors have a fixed subtype: series
                    val seriesFlavor = MediaPackageElementFlavor.flavor("*", "series")
                    for (catalog in mp.catalogs) {
                        if (catalog.flavor.matches(seriesFlavor))
                            mp.remove(catalog)
                    }
                }

                try {
                    // Update the asset manager with the modified mediapackage
                    assetManager!!.takeSnapshot(snapshot.owner, mp)
                } catch (e: AssetManagerException) {
                    logger.error("Error updating mediapackage {}", mp.identifier.compact(), e)
                }

            }
        } catch (e: NotFoundException) {
            logger.warn(e.message)
        } catch (e: IOException) {
            logger.warn(e.message)
        } finally {
            securityService!!.organization = prevOrg
            securityService!!.user = prevUser
        }
    }

    companion object {

        /** The logger  */
        protected val logger = LoggerFactory.getLogger(AssetManagerUpdatedEventHandler::class.java)
    }
}
