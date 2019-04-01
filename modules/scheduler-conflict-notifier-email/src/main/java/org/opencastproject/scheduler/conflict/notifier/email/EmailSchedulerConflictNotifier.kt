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
package org.opencastproject.scheduler.conflict.notifier.email

import org.opencastproject.scheduler.impl.SchedulerUtil.eventOrganizationFilter
import org.opencastproject.scheduler.impl.SchedulerUtil.toHumanReadableString
import org.opencastproject.scheduler.impl.SchedulerUtil.uiAdapterToFlavor
import org.opencastproject.util.OsgiUtil.getContextProperty

import org.opencastproject.kernel.mail.SmtpService
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter
import org.opencastproject.scheduler.api.ConflictNotifier
import org.opencastproject.scheduler.api.ConflictResolution.Strategy
import org.opencastproject.scheduler.api.ConflictingEvent
import org.opencastproject.security.api.SecurityService
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.UrlSupport
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Stream

import org.apache.commons.lang3.CharUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Dictionary

import javax.mail.Message.RecipientType
import javax.mail.MessagingException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Email implementation of a scheduler conflict notifier
 */
class EmailSchedulerConflictNotifier : ConflictNotifier, ManagedService {

    /** The SMTP service  */
    private var smptService: SmtpService? = null

    /** The security service  */
    private var securityService: SecurityService? = null

    /** The workspace  */
    private var workspace: Workspace? = null

    /** The list of registered event catalog UI adapters  */
    private val eventCatalogUIAdapters = ArrayList<EventCatalogUIAdapter>()

    private var recipient: String? = null
    private var subject: String? = null
    private var template: String? = null
    private var serverUrl: String? = null

    private val eventCatalogUIAdapterFlavors: List<MediaPackageElementFlavor>
        get() {
            val organization = securityService!!.organization.id
            return Stream.`$`(eventCatalogUIAdapters).filter(eventOrganizationFilter._2(organization)).map(uiAdapterToFlavor)
                    .toList()
        }

    /** OSGi callback to add [SmtpService] instance.  */
    internal fun setSmtpService(smtpService: SmtpService) {
        this.smptService = smtpService
    }

    /** OSGi callback to add [SecurityService] instance.  */
    internal fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi callback to add [Workspace] instance.  */
    internal fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /** OSGi callback to add [EventCatalogUIAdapter] instance.  */
    internal fun addCatalogUIAdapter(catalogUIAdapter: EventCatalogUIAdapter) {
        eventCatalogUIAdapters.add(catalogUIAdapter)
    }

    /** OSGi callback to remove [EventCatalogUIAdapter] instance.  */
    internal fun removeCatalogUIAdapter(catalogUIAdapter: EventCatalogUIAdapter) {
        eventCatalogUIAdapters.remove(catalogUIAdapter)
    }

    /** OSGi callback.  */
    fun activate(cc: ComponentContext) {
        serverUrl = getContextProperty(cc, OpencastConstants.SERVER_URL_PROPERTY)
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>) {
        // Lookup the name of the recipient and subject
        recipient = StringUtils.trimToNull(properties.get(TO_PROPERTY) as String)
        subject = StringUtils.defaultString(properties.get(SUBJECT_PROPERTY) as String, DEFAULT_SUBJECT)
        template = StringUtils.defaultString(properties.get(TEMPLATE_PROPERTY) as String, DEFAULT_TEMPLATE)
        if (StringUtils.isNotBlank(recipient)) {
            logger.info("Updated email scheduler conflict notifier with recipient '{}'", recipient)
        }
    }

    fun notifyConflicts(conflicts: List<ConflictingEvent>) {
        if (StringUtils.isBlank(recipient)) {
            // Abort if the recipient is not properly configured
            return
        }
        var adminBaseUrl: String? = securityService!!.organization.properties[OpencastConstants.ADMIN_URL_ORG_PROPERTY]
        if (StringUtils.isBlank(adminBaseUrl))
            adminBaseUrl = serverUrl

        val eventDetailsUrl = UrlSupport.concat(adminBaseUrl,
                "admin-ng/index.html#/events/events?modal=event-details&tab=general&resourceId=")

        val sb = StringBuilder()
        var i = 1
        for (c in conflicts) {
            sb.append(i).append(". ")
            if (Strategy.OLD == c.conflictStrategy) {
                sb.append(
                        "This scheduled event has been overwritten with conflicting (data from the scheduling source OR manual changes). Find below the new version:")
                        .append(CharUtils.LF)
                sb.append(CharUtils.LF)
                val humanReadableString = toHumanReadableString(workspace, eventCatalogUIAdapterFlavors,
                        c.newEvent)
                sb.append(humanReadableString.replaceFirst(c.newEvent.eventId.toRegex(), eventDetailsUrl + c.newEvent.eventId))
                sb.append(CharUtils.LF)
            } else {
                sb.append(
                        "This scheduled event has been overwritten with conflicting (data from the scheduling source OR manual changes). Find below the preceding version:")
                        .append(CharUtils.LF)
                sb.append(CharUtils.LF)
                val humanReadableString = toHumanReadableString(workspace, eventCatalogUIAdapterFlavors,
                        c.oldEvent)
                sb.append(humanReadableString.replaceFirst(c.oldEvent.eventId.toRegex(), eventDetailsUrl + c.oldEvent.eventId))
                sb.append(CharUtils.LF)
            }
            i++
        }
        // Create the mail message
        try {
            val message = smptService!!.createMessage()
            message.addRecipient(RecipientType.TO, InternetAddress(recipient))
            message.subject = subject
            message.setText(template!!.replace("\${recordings}", sb.toString()))
            message.saveChanges()

            smptService!!.send(message)
            logger.info("E-mail scheduler conflict notification sent to {}", recipient)
        } catch (e: MessagingException) {
            logger.error("Unable to send email scheduler conflict notification", e)
        }

    }

    companion object {

        private val logger = LoggerFactory.getLogger(EmailSchedulerConflictNotifier::class.java)

        // Configuration properties used in the workflow definition
        private val TO_PROPERTY = "to"
        private val SUBJECT_PROPERTY = "subject"
        private val TEMPLATE_PROPERTY = "template"

        // Configuration defaults
        private val DEFAULT_SUBJECT = "Scheduling conflict"
        private val DEFAULT_TEMPLATE = ("Dear Administrator,\n"
                + "the following scheduled recordings are conflicting with existing ones:\n\n"
                + "\${recordings}")
    }

}
