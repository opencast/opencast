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

import org.opencastproject.liveschedule.api.LiveScheduleService
import org.opencastproject.mediapackage.Publication
import org.opencastproject.message.broker.api.MessageItem
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.DeleteEpisode
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.TakeSnapshot

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AssetManagerUpdateHandler : UpdateHandler(DESTINATION_ASSET_MANAGER) {

    override fun execute(messageItem: MessageItem) {
        val item = messageItem as AssetManagerItem
        val mpId = item.id

        try {
            logger.debug("Asset Manager message handler START for mp {} event type {} in thread {}", mpId, item.type,
                    Thread.currentThread().id)

            when (item.type) {
                AssetManagerItem.Type.Update -> if (item is TakeSnapshot) { // Check class just in case
// If no episopde dc, there's nothing to do.
                    if (item.episodeDublincore.isNone)
                        break
                    // Does media package have a live publication channel? This is to ignore non-live
                    // and past events.
                    // Note: we never create live events when getting asset manager
                    // notifications, only when getting scheduler notifications
                    for (pub in item.mediapackage.publications) {
                        if (LiveScheduleService.CHANNEL_ID.equals(pub.channel))
                            liveScheduleService.createOrUpdateLiveEvent(mpId, item.episodeDublincore.get())
                    }
                }
                AssetManagerItem.Type.Delete -> if (item is DeleteEpisode)
                // Episode is being deleted
                    liveScheduleService.deleteLiveEvent(mpId)
                else -> throw IllegalArgumentException("Unhandled type of AssetManagerItem")
            }// No action needed when a snapshot is deleted
        } catch (e: Exception) {
            logger.warn(String.format("Exception occurred for mp %s, event type %s", mpId, item.type), e)
        } finally {
            logger.debug("Asset Manager message handler END for mp {} event type {} in thread {}", mpId, item.type,
                    Thread.currentThread().id)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AssetManagerUpdateHandler::class.java)

        private val DESTINATION_ASSET_MANAGER = "ASSETMANAGER.Liveschedule"
    }
}
