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
package org.opencastproject.scheduler.conflict.notifier.comment

import org.opencastproject.scheduler.impl.SchedulerUtil.eventOrganizationFilter
import org.opencastproject.scheduler.impl.SchedulerUtil.toHumanReadableString
import org.opencastproject.scheduler.impl.SchedulerUtil.uiAdapterToFlavor

import org.opencastproject.event.comment.EventComment
import org.opencastproject.event.comment.EventCommentException
import org.opencastproject.event.comment.EventCommentService
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter
import org.opencastproject.scheduler.api.ConflictNotifier
import org.opencastproject.scheduler.api.ConflictResolution.Strategy
import org.opencastproject.scheduler.api.ConflictingEvent
import org.opencastproject.security.api.SecurityService
import org.opencastproject.util.data.Option
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Stream

import org.apache.commons.lang3.CharUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList

/**
 * Comment implementation of a scheduler conflict notifier
 */
class CommentSchedulerConflictNotifier : ConflictNotifier {

    /** The event comment service  */
    private var eventCommentService: EventCommentService? = null

    /** The security service  */
    private var securityService: SecurityService? = null

    /** The workspace  */
    private var workspace: Workspace? = null

    /** The list of registered event catalog UI adapters  */
    private val eventCatalogUIAdapters = ArrayList<EventCatalogUIAdapter>()

    private val eventCatalogUIAdapterFlavors: List<MediaPackageElementFlavor>
        get() {
            val organization = securityService!!.organization.id
            return Stream.`$`(eventCatalogUIAdapters).filter(eventOrganizationFilter._2(organization)).map(uiAdapterToFlavor)
                    .toList()
        }

    /** OSGi callback to add [EventCommentService] instance.  */
    fun setEventCommentService(eventCommentService: EventCommentService) {
        this.eventCommentService = eventCommentService
    }

    /** OSGi callback to add [SecurityService] instance.  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi callback to add [Workspace] instance.  */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /** OSGi callback to add [EventCatalogUIAdapter] instance.  */
    fun addCatalogUIAdapter(catalogUIAdapter: EventCatalogUIAdapter) {
        eventCatalogUIAdapters.add(catalogUIAdapter)
    }

    /** OSGi callback to remove [EventCatalogUIAdapter] instance.  */
    fun removeCatalogUIAdapter(catalogUIAdapter: EventCatalogUIAdapter) {
        eventCatalogUIAdapters.remove(catalogUIAdapter)
    }

    fun notifyConflicts(conflicts: List<ConflictingEvent>) {
        for (c in conflicts) {
            val sb = StringBuilder(
                    "This scheduled event has been overwritten with conflicting (data from the scheduling source OR manual changes). ")
            if (Strategy.OLD == c.conflictStrategy) {
                sb.append("Find below the new version:").append(CharUtils.LF)
                sb.append(CharUtils.LF)
                sb.append(toHumanReadableString(workspace, eventCatalogUIAdapterFlavors, c.newEvent))
            } else {
                sb.append("Find below the preceding version:").append(CharUtils.LF)
                sb.append(CharUtils.LF)
                sb.append(toHumanReadableString(workspace, eventCatalogUIAdapterFlavors, c.oldEvent))
            }
            try {
                val comment = EventComment.create(Option.none(), c.newEvent.eventId,
                        securityService!!.organization.id, sb.toString(), securityService!!.user, COMMENT_REASON,
                        false)
                eventCommentService!!.updateComment(comment)
            } catch (e: EventCommentException) {
                logger.error("Unable to create a comment on the event {}: {}", c.oldEvent.eventId,
                        ExceptionUtils.getStackTrace(e))
            }

        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(CommentSchedulerConflictNotifier::class.java)

        private val COMMENT_REASON = "conflict"
    }

}
