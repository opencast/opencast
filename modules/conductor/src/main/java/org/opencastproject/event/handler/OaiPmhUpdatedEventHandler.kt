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

import org.opencastproject.util.OsgiUtil.getOptCfg
import org.opencastproject.util.OsgiUtil.getOptCfgAsBoolean

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase
import org.opencastproject.oaipmh.persistence.QueryBuilder
import org.opencastproject.oaipmh.persistence.SearchResult
import org.opencastproject.oaipmh.persistence.SearchResultItem
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.util.data.Collections
import org.opencastproject.util.data.Option

import org.osgi.framework.BundleContext
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Dictionary
import java.util.HashSet

class OaiPmhUpdatedEventHandler : ManagedService {

    /** Whether to propagate episode meta data changes to OAI-PMH or not  */
    private var propagateEpisode = false

    /** List of flavors to redistribute  */
    private var flavors: Set<String> = HashSet()

    /** List of tags to redistribute  */
    private var tags: Set<String> = HashSet()

    /** The security service  */
    private var securityService: SecurityService? = null

    /** The OAI-PMH database  */
    private var oaiPmhPersistence: OaiPmhDatabase? = null

    /** The OAI-PMH publication service  */
    private var oaiPmhPublicationService: OaiPmhPublicationService? = null

    /** The system account to use for running asynchronous events  */
    var systemAccount: String? = null

    /**
     * OSGI callback for component activation.
     *
     * @param bundleContext
     * the OSGI bundle context
     */
    protected fun activate(bundleContext: BundleContext) {
        this.systemAccount = bundleContext.getProperty("org.opencastproject.security.digest.user")
    }

    @Throws(ConfigurationException::class)
    override fun updated(dictionary: Dictionary<String, *>) {
        val propagateEpisode = getOptCfgAsBoolean(dictionary, CFG_PROPAGATE_EPISODE)
        if (propagateEpisode.isSome) {
            this.propagateEpisode = propagateEpisode.get()
        }

        val flavorsRaw = getOptCfg(dictionary, CFG_FLAVORS)
        if (flavorsRaw.isSome) {
            val flavorStrings = flavorsRaw.get().split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            this.flavors = Collections.set(*flavorStrings)
        } else {
            this.flavors = HashSet()
        }

        val tagsRaw = getOptCfg(dictionary, CFG_TAGS)
        if (tagsRaw.isSome) {
            val tags = tagsRaw.get().split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            this.tags = Collections.set(*tags)
        } else {
            this.tags = HashSet()
        }
    }

    fun handleEvent(snapshotItem: AssetManagerItem.TakeSnapshot) {
        if (!propagateEpisode) {
            logger.trace("Skipping automatic propagation of episode meta data to OAI-PMH since it is turned off.")
            return
        }

        //An episode or its ACL has been updated. Construct the MediaPackage and publish it to OAI-PMH.
        logger.debug("Handling update event for media package {}", snapshotItem.id)

        // We must be an administrative user to make a query to the OaiPmhPublicationService
        val prevUser = securityService!!.user
        val prevOrg = securityService!!.organization

        try {
            securityService!!.user = SecurityUtil.createSystemUser(systemAccount!!, prevOrg)

            // Check weather the media package contains elements to republish
            val snapshotMp = snapshotItem.mediapackage
            val mpeSelector = SimpleElementSelector()
            for (flavor in flavors) {
                mpeSelector.addFlavor(flavor)
            }
            for (tag in tags) {
                mpeSelector.addTag(tag)
            }
            val elementsToUpdate = mpeSelector.select(snapshotMp, true)
            if (elementsToUpdate == null || elementsToUpdate.isEmpty()) {
                logger.debug("The media package {} does not contain any elements matching the given flavors and tags",
                        snapshotMp.identifier.compact())
                return
            }

            val result = oaiPmhPersistence!!.search(QueryBuilder.query().mediaPackageId(snapshotMp)
                    .isDeleted(false).build())
            for (searchResultItem in result.items) {
                try {
                    val job = oaiPmhPublicationService!!
                            .updateMetadata(snapshotMp, searchResultItem.repository, flavors, tags, false)
                    // we don't want to wait for job completion here because it will block the message queue
                } catch (e: Exception) {
                    logger.error("Unable to update OAI-PMH publication for the media package {} in repository {}",
                            snapshotItem.id, searchResultItem.repository, e)
                }

            }
        } finally {
            securityService!!.organization = prevOrg
            securityService!!.user = prevUser
        }
    }

    fun setOaiPmhPersistence(oaiPmhPersistence: OaiPmhDatabase) {
        this.oaiPmhPersistence = oaiPmhPersistence
    }

    fun setOaiPmhPublicationService(oaiPmhPublicationService: OaiPmhPublicationService) {
        this.oaiPmhPublicationService = oaiPmhPublicationService
    }

    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    companion object {

        /** The logger  */
        protected val logger = LoggerFactory.getLogger(OaiPmhUpdatedEventHandler::class.java)

        // config keys
        val CFG_PROPAGATE_EPISODE = "propagate.episode"
        val CFG_FLAVORS = "flavors"
        val CFG_TAGS = "tags"
    }
}
