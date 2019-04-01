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

package org.opencastproject.authorization.xacml

import org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_EPISODE
import org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_SERIES
import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.security.api.AccessControlEntry
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.Role
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.series.api.SeriesService
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Tuple
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Arrays
import java.util.Dictionary
import java.util.Optional

import javax.xml.bind.JAXBException

/**
 * A XACML implementation of the [AuthorizationService].
 */
class XACMLAuthorizationService : AuthorizationService, ManagedService {

    /** The workspace  */
    protected var workspace: Workspace

    /** The security service  */
    protected var securityService: SecurityService

    /** The series service  */
    protected var seriesService: SeriesService

    internal enum class MergeMode {
        OVERRIDE, ROLES, ACTIONS
    }

    fun activate(cc: ComponentContext) {
        updated(cc.properties)
    }

    fun modified(config: Map<String, Any>) {
        // this prevents the service from restarting on configuration updated.
        // updated() will handle the configuration update.
    }

    @Synchronized
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            mergeMode = MergeMode.OVERRIDE
            logger.debug("Merge mode set to {}", mergeMode)
            return
        }
        val mode = StringUtils.defaultIfBlank(properties.get(CONFIG_MERGE_MODE) as String,
                MergeMode.OVERRIDE.toString())
        try {
            mergeMode = MergeMode.valueOf(mode.toUpperCase())
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid value set for ACL merge mode, defaulting to {}", MergeMode.OVERRIDE)
            mergeMode = MergeMode.OVERRIDE
        }

        logger.debug("Merge mode set to {}", mergeMode)
    }

    override fun getActiveAcl(mp: MediaPackage): Tuple<AccessControlList, AclScope> {
        logger.debug("getActiveACl for media package {}", mp.identifier)
        return getAcl(mp, AclScope.Episode)
    }

    /** Returns an ACL based on a given file/inputstream.  */
    @Throws(IOException::class)
    override fun getAclFromInputStream(`in`: InputStream): AccessControlList {
        logger.debug("Get ACL from inputstream")
        try {
            return XACMLUtils.parseXacml(`in`)
        } catch (e: XACMLParsingException) {
            throw IOException(e)
        }

    }

    override fun getAcl(mp: MediaPackage, scope: AclScope): Tuple<AccessControlList, AclScope> {
        var episode = Optional.empty<AccessControlList>()
        var series = Optional.empty<AccessControlList>()

        // Start with the requested scope but fall back to the less specific scope if it does not exist.
        // The order is: episode -> series -> general (deprecated) -> global
        if (AclScope.Episode == scope || AclScope.Merged == scope) {
            for (xacml in mp.getAttachments(XACML_POLICY_EPISODE)) {
                episode = loadAcl(xacml.getURI())
            }
        }
        if (Arrays.asList(AclScope.Episode, AclScope.Series, AclScope.Merged).contains(scope)) {
            for (xacml in mp.getAttachments(XACML_POLICY_SERIES)) {
                series = loadAcl(xacml.getURI())
            }
        }

        if (episode.isPresent && series.isPresent) {
            logger.debug("Found event and series ACL for media package {}", mp.identifier)
            when (mergeMode) {
                XACMLAuthorizationService.MergeMode.ACTIONS -> {
                    logger.debug("Merging ACLs based on individual actions")
                    return tuple(series.get().mergeActions(episode.get()), AclScope.Merged)
                }
                XACMLAuthorizationService.MergeMode.ROLES -> {
                    logger.debug("Merging ACLs based on roles")
                    return tuple(series.get().merge(episode.get()), AclScope.Merged)
                }
                else -> {
                    logger.debug("Episode ACL overrides series ACL")
                    return tuple(episode.get(), AclScope.Merged)
                }
            }
        }
        if (episode.isPresent) {
            logger.debug("Found event ACL for media package {}", mp.identifier)
            return tuple(episode.get(), AclScope.Episode)
        }
        if (series.isPresent) {
            logger.debug("Found series ACL for media package {}", mp.identifier)
            return tuple(series.get(), AclScope.Series)
        }

        logger.debug("Falling back to global default ACL")
        return tuple(AccessControlList(), AclScope.Global)
    }

    @Throws(MediaPackageException::class)
    override fun setAcl(mp: MediaPackage, scope: AclScope, acl: AccessControlList): Tuple<MediaPackage, Attachment> {
        // Get XACML representation of these role + action tuples
        val xacmlContent: String
        try {
            xacmlContent = XACMLUtils.getXacml(mp, acl)
        } catch (e: JAXBException) {
            throw MediaPackageException("Unable to generate xacml for media package " + mp.identifier)
        }

        // Remove the old xacml file(s)
        var attachment: Attachment? = removeFromMediaPackageAndWorkspace(mp, toFlavor(scope)).b

        // add attachment
        val elementId = toElementId(scope)
        var uri: URI
        try {
            IOUtils.toInputStream(xacmlContent, "UTF-8").use { `in` -> uri = workspace.put(mp.identifier.toString(), elementId, XACML_FILENAME, `in`) }
        } catch (e: IOException) {
            throw MediaPackageException("Error storing xacml for media package " + mp.identifier)
        }

        if (attachment == null) {
            attachment = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                    .elementFromURI(uri, Attachment.TYPE, toFlavor(scope)) as Attachment
        }
        attachment.setURI(uri)
        attachment.identifier = elementId
        attachment.mimeType = MimeTypes.XML
        // setting the URI to a new source so the checksum will most like be invalid
        attachment.checksum = null
        mp.add(attachment)

        logger.debug("Saved XACML as {}", uri)

        // return augmented media package
        return tuple(mp, attachment)
    }

    override fun removeAcl(mp: MediaPackage, scope: AclScope): MediaPackage {
        return removeFromMediaPackageAndWorkspace(mp, toFlavor(scope)).a
    }

    /**
     * Remove all attachments of the given flavors from media package and workspace.
     *
     * @return the a tuple with the mutated (!) media package as A and the deleted Attachment as B
     */
    private fun removeFromMediaPackageAndWorkspace(mp: MediaPackage,
                                                   flavor: MediaPackageElementFlavor): Tuple<MediaPackage, Attachment> {
        var attachment: Attachment? = null
        for (a in mp.getAttachments(flavor)) {
            attachment = a.clone() as Attachment
            try {
                workspace.delete(a.getURI())
            } catch (e: Exception) {
                logger.warn("Unable to delete XACML file:", e)
            }

            mp.remove(a)
        }
        return Tuple.tuple(mp, attachment)
    }

    /** Load an ACL from the given URI.  */
    private fun loadAcl(uri: URI): Optional<AccessControlList> {
        logger.debug("Load Acl from {}", uri)
        try {
            workspace.read(uri).use { `is` ->
                val acl = XACMLUtils.parseXacml(`is`)
                if (acl != null) {
                    return Optional.of(acl)
                }
            }
        } catch (e: NotFoundException) {
            logger.debug("URI {} not found", uri)
        } catch (e: Exception) {
            logger.warn("Unable to load or parse Acl", e)
        }

        return Optional.empty()
    }

    override fun hasPermission(mp: MediaPackage, action: String): Boolean {
        val acl = getActiveAcl(mp).a
        var allowed = false
        val user = securityService.user
        for (entry in acl.entries!!) {
            // ignore entries for other actions
            if (entry.action != action) {
                continue
            }
            for (role in user.roles) {
                if (entry.role == role.name) {
                    // immediately abort on matching deny rules
                    // (never allow if a deny rule matches, even if another allow rule matches)
                    if (!entry.isAllow) {
                        logger.debug("Access explicitly denied for role({}), action({})", role.name, action)
                        return false
                    }
                    allowed = true
                }
            }
        }
        logger.debug("XACML file allowed access")
        return allowed
    }

    /**
     * Sets the workspace to use for retrieving XACML policies
     *
     * @param workspace
     * the workspace to set
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * Declarative services callback to set the security service.
     *
     * @param securityService
     * the security service
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /**
     * Declarative services callback to set the series service.
     *
     * @param seriesService
     * the series service
     */
    protected fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(XACMLAuthorizationService::class.java)

        /** The default filename for XACML attachments  */
        private val XACML_FILENAME = "xacml.xml"

        private val CONFIG_MERGE_MODE = "merge.mode"

        /** Definition of how merging of series and episode ACLs work  */
        private var mergeMode = MergeMode.OVERRIDE

        /** Get the flavor associated with a scope.  */
        private fun toFlavor(scope: AclScope): MediaPackageElementFlavor {
            when (scope) {
                AclScope.Episode -> return XACML_POLICY_EPISODE
                AclScope.Series -> return XACML_POLICY_SERIES
                else -> throw IllegalArgumentException("No flavors match the given ACL scope")
            }
        }

        /** Get the element id associated with a scope.  */
        private fun toElementId(scope: AclScope): String {
            when (scope) {
                AclScope.Episode -> return "security-policy-episode"
                AclScope.Series -> return "security-policy-series"
                else -> throw IllegalArgumentException("No element id matches the given ACL scope")
            }
        }
    }

}
