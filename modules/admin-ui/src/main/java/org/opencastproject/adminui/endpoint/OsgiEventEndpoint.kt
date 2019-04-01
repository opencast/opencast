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

package org.opencastproject.adminui.endpoint

import org.opencastproject.adminui.impl.AdminUIConfiguration
import org.opencastproject.adminui.index.AdminUISearchIndex
import org.opencastproject.authorization.xacml.manager.api.AclService
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory
import org.opencastproject.capture.admin.api.CaptureAgentStateService
import org.opencastproject.event.comment.EventCommentService
import org.opencastproject.index.service.api.IndexService
import org.opencastproject.scheduler.api.SchedulerService
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.urlsigning.service.UrlSigningService
import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil
import org.opencastproject.workflow.api.WorkflowService

import org.apache.commons.lang3.BooleanUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService

import java.util.Dictionary

import javax.ws.rs.Path

/** OSGi bound implementation.  */
@Path("/")
class OsgiEventEndpoint : AbstractEventEndpoint(), ManagedService {

    private var aclServiceFactory: AclServiceFactory? = null
    /** OSGi DI.  */
    override var index: AdminUISearchIndex? = null
    /** OSGi DI.  */
    override var authorizationService: AuthorizationService? = null
    /** OSGi DI.  */
    override var captureAgentStateService: CaptureAgentStateService? = null
    /** OSGi DI.  */
    override var eventCommentService: EventCommentService? = null
    /** OSGi DI.  */
    override var indexService: IndexService? = null
    /** OSGi DI.  */
    override var jobService: JobEndpoint? = null
    /** OSGi DI.  */
    override var seriesService: SeriesEndpoint? = null
    /** OSGi DI.  */
    override var schedulerService: SchedulerService? = null
    /** OSGi DI.  */
    override var securityService: SecurityService? = null
    /** OSGi DI.  */
    override var urlSigningService: UrlSigningService? = null
    /** OSGi DI.  */
    override var workflowService: WorkflowService? = null
    /** OSGi DI.  */
    override var adminUIConfiguration: AdminUIConfiguration? = null

    override var urlSigningExpireDuration = UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION
        private set
    private var signWithClientIP: Boolean? = UrlSigningServiceOsgiUtil.DEFAULT_SIGN_WITH_CLIENT_IP
    override var onlySeriesWithWriteAccessEventModal: Boolean? = false
        private set

    override val aclService: AclService
        get() = aclServiceFactory!!.serviceFor(securityService!!.organization)

    /** OSGi DI.  */
    fun setAclServiceFactory(aclServiceFactory: AclServiceFactory) {
        this.aclServiceFactory = aclServiceFactory
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            AbstractEventEndpoint.logger.info("No configuration available, using defaults")
            return
        }

        urlSigningExpireDuration = UrlSigningServiceOsgiUtil.getUpdatedSigningExpiration(properties, this.javaClass.simpleName)
        signWithClientIP = UrlSigningServiceOsgiUtil.getUpdatedSignWithClientIP(properties,
                this.javaClass.simpleName)

        val dictionaryValue = properties.get(EVENTMODAL_ONLYSERIESWITHWRITEACCESS_KEY)
        if (dictionaryValue != null) {
            onlySeriesWithWriteAccessEventModal = BooleanUtils.toBoolean(dictionaryValue.toString())
        }
    }

    override fun signWithClientIP(): Boolean? {
        return signWithClientIP
    }

    companion object {

        val EVENTMODAL_ONLYSERIESWITHWRITEACCESS_KEY = "eventModal.onlySeriesWithWriteAccess"
    }

}
