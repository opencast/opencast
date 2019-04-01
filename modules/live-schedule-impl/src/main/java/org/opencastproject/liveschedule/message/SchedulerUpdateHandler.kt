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
package org.opencastproject.liveschedule.message

import org.opencastproject.message.broker.api.MessageItem
import org.opencastproject.message.broker.api.scheduler.SchedulerItem
import org.opencastproject.message.broker.api.scheduler.SchedulerItemList
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.scheduler.api.RecordingState
import org.opencastproject.scheduler.api.SchedulerException
import org.opencastproject.scheduler.api.SchedulerService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.util.NotFoundException

import org.apache.commons.lang3.BooleanUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SchedulerUpdateHandler : UpdateHandler(DESTINATION_SCHEDULER) {

    protected var schedulerService: SchedulerService

    override fun execute(messageItem: MessageItem) {
        val schedulerItemList = messageItem as SchedulerItemList
        for (item in schedulerItemList.items) {
            executeSingle(schedulerItemList.id, item)
        }
    }

    private fun executeSingle(mpId: String, schedulerItem: SchedulerItem) {
        try {
            logger.debug("Scheduler message handler START for mp {} event type {} in thread {}", mpId,
                    schedulerItem.type, Thread.currentThread().id)

            when (schedulerItem.type) {
                SchedulerItem.Type.UpdateCatalog -> if (isLive(mpId))
                    liveScheduleService.createOrUpdateLiveEvent(mpId, schedulerItem.event!!)
                SchedulerItem.Type.UpdateAcl -> if (isLive(mpId))
                    liveScheduleService.updateLiveEventAcl(mpId, schedulerItem.acl!!)
                SchedulerItem.Type.UpdateProperties -> {
                    // Workflow properties may have been updated (publishLive configuration)
                    val publishLive = schedulerItem.properties[UpdateHandler.PUBLISH_LIVE_PROPERTY]
                    if (publishLive == null)
                    // Not specified so we do nothing. We don't want to delete if we got incomplete props.
                        return
                    else if (BooleanUtils.toBoolean(publishLive)) {
                        val episodeDC = schedulerService.getDublinCore(mpId)
                        liveScheduleService.createOrUpdateLiveEvent(mpId, episodeDC)
                    } else
                        liveScheduleService.deleteLiveEvent(mpId)
                }
                SchedulerItem.Type.Delete, SchedulerItem.Type.DeleteRecordingStatus ->
                    // We can't check workflow config here to determine if the event is live because the
                    // event has already been deleted. The live scheduler service will do that.
                    liveScheduleService.deleteLiveEvent(mpId)
                SchedulerItem.Type.UpdateAgentId, SchedulerItem.Type.UpdateStart, SchedulerItem.Type.UpdateEnd -> if (isLive(mpId)) {
                    val episodeDC = schedulerService.getDublinCore(mpId)
                    liveScheduleService.createOrUpdateLiveEvent(mpId, episodeDC)
                }
                SchedulerItem.Type.UpdateRecordingStatus -> {
                    val state = schedulerItem.recordingState
                    if (RecordingState.CAPTURE_FINISHED == state || RecordingState.UPLOADING == state
                            || RecordingState.UPLOADING == state || RecordingState.CAPTURE_ERROR == state
                            || RecordingState.UPLOAD_ERROR == state)
                        if (isLive(mpId))
                            liveScheduleService.deleteLiveEvent(mpId)
                }
                SchedulerItem.Type.UpdatePresenters -> {
                }
                else -> throw IllegalArgumentException("Unhandled type of SchedulerItem")
            }
        } catch (e: Exception) {
            logger.warn(String.format("Exception occurred for mp %s, event type %s", mpId, schedulerItem.type), e)
        } finally {
            logger.debug("Scheduler message handler END for mp {} event type {} in thread {}", mpId, schedulerItem.type,
                    Thread.currentThread().id)
        }
    }

    protected fun isLive(mpId: String): Boolean {
        try {
            val config = schedulerService.getWorkflowConfig(mpId)
            return BooleanUtils.toBoolean(config[UpdateHandler.PUBLISH_LIVE_PROPERTY] as String)
        } catch (e: UnauthorizedException) {
            logger.debug("Could not get workflow configuration for mp {}. This is probably ok.", mpId)
            return false // Assume non-live
        } catch (e: NotFoundException) {
            logger.debug("Could not get workflow configuration for mp {}. This is probably ok.", mpId)
            return false
        } catch (e: SchedulerException) {
            logger.debug("Could not get workflow configuration for mp {}. This is probably ok.", mpId)
            return false
        }

    }

    // === Set by OSGI begin
    fun setSchedulerService(service: SchedulerService) {
        this.schedulerService = service
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SchedulerUpdateHandler::class.java)

        private val DESTINATION_SCHEDULER = "SCHEDULER.Liveschedule"
    }
    // === Set by OSGI end

}
