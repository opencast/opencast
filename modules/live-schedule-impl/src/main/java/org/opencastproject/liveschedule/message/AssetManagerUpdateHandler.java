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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.liveschedule.message;

import org.opencastproject.liveschedule.api.LiveScheduleService;
import org.opencastproject.liveschedule.impl.LiveScheduleServiceImpl;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.message.broker.api.MessageItem;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.DeleteEpisode;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.TakeSnapshot;
import org.opencastproject.util.Log;

import org.slf4j.LoggerFactory;

public class AssetManagerUpdateHandler extends UpdateHandler {

  private static final Log logger = new Log(LoggerFactory.getLogger(AssetManagerUpdateHandler.class));

  private static final String DESTINATION_ASSET_MANAGER = "ASSETMANAGER.Liveschedule";

  public AssetManagerUpdateHandler() {
    super(DESTINATION_ASSET_MANAGER);
  }

  @Override
  protected void execute(MessageItem messageItem) {
    AssetManagerItem item = (AssetManagerItem) messageItem;
    String mpId = item.getId();

    try {
      logger.debug("Asset Manager message handler START for mp {} event type {} in thread {}", mpId, item.getType(),
              Thread.currentThread().getId());

      switch (item.getType()) {
        case Update:
          if (item instanceof TakeSnapshot) { // Check class just in case
            TakeSnapshot snapshotItem = (TakeSnapshot) item;
            // If notification was originated by the live schedule service, there's nothing to do.
            // Same for no episode dc.
            if (LiveScheduleServiceImpl.SNAPSHOT_OWNER.equals(snapshotItem.getOwner())
                    || snapshotItem.getEpisodeDublincore().isNone())
              break;
            // Does media package have a live publication channel? This is to ignore non-live
            // and past events.
            // Note: we never create live events when getting asset manager
            // notifications, only when getting scheduler notifications
            for (Publication pub : snapshotItem.getMediapackage().getPublications()) {
              if (LiveScheduleService.CHANNEL_ID.equals(pub.getChannel()))
                liveScheduleService.createOrUpdateLiveEvent(mpId, snapshotItem.getEpisodeDublincore().get());
            }
          }
          break;
        case Delete:
          if (item instanceof DeleteEpisode)
            // Episode is being deleted
            liveScheduleService.deleteLiveEvent(mpId);

          // No action needed when a snapshot is deleted
          break;
        default:
          throw new IllegalArgumentException("Unhandled type of AssetManagerItem");
      }
    } catch (Exception e) {
      logger.warn(e, String.format("Exception occurred for mp %s, event type %s", mpId, item.getType()));
    } finally {
      logger.debug("Asset Manager message handler END for mp {} event type {} in thread {}", mpId, item.getType(),
              Thread.currentThread().getId());
    }
  }
}
