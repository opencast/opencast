/*
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

import static org.opencastproject.liveschedule.api.LiveScheduleService.LIVE_CHANNEL_ID;

import org.opencastproject.liveschedule.api.LiveScheduleService;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.TakeSnapshot;
import org.opencastproject.message.broker.api.update.AssetManagerUpdateHandler;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@Component(service = { AssetManagerUpdateHandler.class }, property = {
    "service.description=Asset Manager Update Listener for Live Schedule Service" })
public class SnapshotHandlerForLiveEvents implements AssetManagerUpdateHandler {

  private static final Logger logger = LoggerFactory.getLogger(SnapshotHandlerForLiveEvents.class);

  private LiveScheduleService liveScheduleService;

  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating {}", this.getClass().getName());
  }

  @Override
  public void execute(AssetManagerItem item) {
    String mpId = item.getId();

    try {
      // new snapshot for live event?
      if (item.getType().equals(AssetManagerItem.Type.Update) && item instanceof TakeSnapshot snapshotItem) {
        logger.debug("Snapshot notification received for event {} snapshot {}", mpId, snapshotItem.getVersion());
        if (Arrays.stream(snapshotItem.getMediapackage().getPublications())
            .anyMatch(p -> p.getChannel().equals(LIVE_CHANNEL_ID))) {
          logger.debug("Updating live event {}", mpId);
          liveScheduleService.updateLiveEvent(snapshotItem.getMediapackage(), Long.toString(snapshotItem.getVersion()));
        } else {
          logger.debug("Event {} is not live, not updating.", mpId);
        }
      }

    } catch (Exception e) {
      logger.warn("Updating event {} failed", mpId, e);
    }
  }

  @Reference
  public void setLiveScheduleService(LiveScheduleService liveScheduleService) {
    this.liveScheduleService = liveScheduleService;
  }

}
