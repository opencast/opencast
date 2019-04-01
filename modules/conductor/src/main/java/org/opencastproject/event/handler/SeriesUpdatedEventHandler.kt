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

import org.opencastproject.job.api.Job.Status.FINISHED
import org.opencastproject.mediapackage.MediaPackageElementParser.getFromXml
import org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_EPISODE
import org.opencastproject.workflow.handler.distribution.EngagePublicationChannel.CHANNEL_ID

import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.job.api.JobBarrier.Result
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.message.broker.api.series.SeriesItem
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.metadata.dublincore.DublinCoreUtil
import org.opencastproject.search.api.SearchException
import org.opencastproject.search.api.SearchQuery
import org.opencastproject.search.api.SearchResult
import org.opencastproject.search.api.SearchResultItem
import org.opencastproject.search.api.SearchService
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.api.User
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.NotFoundException
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URI

/** Responds to series events by re-distributing metadata and security policy files for published mediapackages.  */
class SeriesUpdatedEventHandler {

    /** The service registry  */
    protected var serviceRegistry: ServiceRegistry? = null

    /** The distribution service  */
    protected var distributionService: DistributionService? = null

    /** The search service  */
    protected var searchService: SearchService? = null

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
     * @param serviceRegistry
     * the serviceRegistry to set
     */
    fun setServiceRegistry(serviceRegistry: ServiceRegistry) {
        this.serviceRegistry = serviceRegistry
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
     * @param distributionService
     * the distributionService to set
     */
    fun setDistributionService(distributionService: DistributionService) {
        this.distributionService = distributionService
    }

    /**
     * @param searchService
     * the searchService to set
     */
    fun setSearchService(searchService: SearchService) {
        this.searchService = searchService
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

            val q = SearchQuery().withSeriesId(seriesId)
            val result = searchService!!.getForAdministrativeRead(q)

            for (item in result.items) {
                val mp = item.mediaPackage
                val org = organizationDirectoryService!!.getOrganization(item.organization)
                securityService!!.organization = org

                // If the security policy has been updated, make sure to distribute that change
                // to the distribution channels as well
                if (SeriesItem.Type.UpdateAcl == seriesItem.type) {
                    if (seriesItem.overrideEpisodeAcl!!) {

                        val distributedEpisodeAcls = mp.getElementsByFlavor(XACML_POLICY_EPISODE)
                        authorizationService!!.removeAcl(mp, AclScope.Episode)

                        for (distributedEpisodeAcl in distributedEpisodeAcls) {
                            val retractJob = distributionService!!.retract(CHANNEL_ID, mp, distributedEpisodeAcl.identifier)
                            val barrier = JobBarrier(null!!, serviceRegistry!!, retractJob)
                            val jobResult = barrier.waitForJobs()
                            if (jobResult!!.status[retractJob] != FINISHED) {
                                logger.error("Unable to retract episode XACML {}", distributedEpisodeAcl.identifier)
                            }
                        }
                    }

                    val fileRepoCopy = authorizationService!!.setAcl(mp, AclScope.Series, seriesItem.acl!!).b

                    // Distribute the updated XACML file
                    val distributionJob = distributionService!!.distribute(CHANNEL_ID, mp, fileRepoCopy.identifier)
                    val barrier = JobBarrier(null!!, serviceRegistry!!, distributionJob)
                    val jobResult = barrier.waitForJobs()
                    if (jobResult!!.status[distributionJob] == FINISHED) {
                        mp.remove(fileRepoCopy)
                        mp.add(getFromXml(serviceRegistry!!.getJob(distributionJob.id).payload))
                    } else {
                        logger.error("Unable to distribute series XACML {}", fileRepoCopy.identifier)
                        continue
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

                        // Distribute the updated series dc
                        val distributionJob = distributionService!!.distribute(CHANNEL_ID, mp, c.identifier)
                        val barrier = JobBarrier(null!!, serviceRegistry!!, distributionJob)
                        val jobResult = barrier.waitForJobs()
                        if (jobResult!!.status[distributionJob] == FINISHED) {
                            mp.remove(c)
                            mp.add(getFromXml(serviceRegistry!!.getJob(distributionJob.id).payload))
                        } else {
                            logger.error("Unable to distribute series catalog {}", c.identifier)
                            continue
                        }
                    }
                }

                // Remove the series catalog and isPartOf from episode catalog
                if (SeriesItem.Type.Delete == seriesItem.type) {
                    mp.series = null
                    mp.seriesTitle = null

                    val retractSeriesCatalog = retractSeriesCatalog(mp)
                    val updateEpisodeCatalog = updateEpisodeCatalog(mp)

                    if (!retractSeriesCatalog || !updateEpisodeCatalog)
                        continue
                }

                // Update the search index with the modified mediapackage
                val searchJob = searchService!!.add(mp)
                val barrier = JobBarrier(null!!, serviceRegistry!!, searchJob)
                barrier.waitForJobs()
            }
        } catch (e: SearchException) {
            logger.warn("Unable to find mediapackages in search: ", e.message)
        } catch (e: UnauthorizedException) {
            logger.warn(e.message)
        } catch (e: MediaPackageException) {
            logger.warn(e.message)
        } catch (e: ServiceRegistryException) {
            logger.warn(e.message)
        } catch (e: NotFoundException) {
            logger.warn(e.message)
        } catch (e: IOException) {
            logger.warn(e.message)
        } catch (e: DistributionException) {
            logger.warn(e.message)
        } finally {
            securityService!!.organization = prevOrg
            securityService!!.user = prevUser
        }
    }

    @Throws(DistributionException::class)
    private fun retractSeriesCatalog(mp: MediaPackage): Boolean {
        // Retract the series catalog
        for (c in mp.getCatalogs(MediaPackageElements.SERIES)) {
            val retractJob = distributionService!!.retract(CHANNEL_ID, mp, c.identifier)
            val barrier = JobBarrier(null!!, serviceRegistry!!, retractJob)
            val jobResult = barrier.waitForJobs()
            if (jobResult!!.status[retractJob] == FINISHED) {
                mp.remove(c)
            } else {
                logger.error("Unable to retract series catalog {}", c.identifier)
                return false
            }
        }
        return true
    }

    @Throws(DistributionException::class, MediaPackageException::class, NotFoundException::class, ServiceRegistryException::class, IllegalArgumentException::class, IOException::class)
    private fun updateEpisodeCatalog(mp: MediaPackage): Boolean {
        // Update the episode catalog
        for (episodeCatalog in mp.getCatalogs(MediaPackageElements.EPISODE)) {
            val episodeDublinCore = DublinCoreUtil.loadDublinCore(workspace, episodeCatalog)
            episodeDublinCore.remove(DublinCore.PROPERTY_IS_PART_OF)
            val filename = FilenameUtils.getName(episodeCatalog.getURI().toString())
            val uri = workspace!!.put(mp.identifier.toString(), episodeCatalog.identifier, filename,
                    dublinCoreService!!.serialize(episodeDublinCore))
            episodeCatalog.setURI(uri)
            // setting the URI to a new source so the checksum will most like be invalid
            episodeCatalog.checksum = null

            // Distribute the updated episode dublincore
            val distributionJob = distributionService!!.distribute(CHANNEL_ID, mp, episodeCatalog.identifier)
            val barrier = JobBarrier(null!!, serviceRegistry!!, distributionJob)
            val jobResult = barrier.waitForJobs()
            if (jobResult!!.status[distributionJob] == FINISHED) {
                mp.remove(episodeCatalog)
                mp.add(getFromXml(serviceRegistry!!.getJob(distributionJob.id).payload))
            } else {
                logger.error("Unable to distribute episode catalog {}", episodeCatalog.identifier)
                return false
            }
        }
        return true
    }

    companion object {

        /** The logger  */
        protected val logger = LoggerFactory.getLogger(SeriesUpdatedEventHandler::class.java)
    }
}
