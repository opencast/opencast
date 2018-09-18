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

import org.opencastproject.message.broker.api.MessageItem;
import org.opencastproject.message.broker.api.scheduler.SchedulerItem;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.scheduler.api.RecordingState;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SchedulerUpdateHandler extends UpdateHandler {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerUpdateHandler.class);

  private static final String DESTINATION_SCHEDULER = "SCHEDULER.Liveschedule";

  protected SchedulerService schedulerService;

  public SchedulerUpdateHandler() {
    super(DESTINATION_SCHEDULER);
  }

  @Override
  protected void execute(MessageItem messageItem) {
    SchedulerItem schedulerItem = (SchedulerItem) messageItem;
    String mpId = schedulerItem.getMediaPackageId();

    try {
      logger.debug("Scheduler message handler START for mp {} event type {} in thread {}", mpId,
              schedulerItem.getType(), Thread.currentThread().getId());

      switch (schedulerItem.getType()) {
        case UpdateCatalog:
          if (isLive(mpId))
            liveScheduleService.createOrUpdateLiveEvent(mpId, schedulerItem.getEvent());
          break;
        case UpdateAcl:
          if (isLive(mpId))
            liveScheduleService.updateLiveEventAcl(mpId, schedulerItem.getAcl());
          break;
        case UpdateProperties:
          // Workflow properties may have been updated (publishLive configuration)
          String publishLive = schedulerItem.getProperties().get(PUBLISH_LIVE_PROPERTY);
          if (publishLive == null)
            // Not specified so we do nothing. We don't want to delete if we got incomplete props.
            return;
          else if (BooleanUtils.toBoolean(publishLive)) {
            DublinCoreCatalog episodeDC = schedulerService.getDublinCore(mpId);
            liveScheduleService.createOrUpdateLiveEvent(mpId, episodeDC);
          } else
            liveScheduleService.deleteLiveEvent(mpId);
          break;
        case Delete:
        case DeleteRecordingStatus:
          // We can't check workflow config here to determine if the event is live because the
          // event has already been deleted. The live scheduler service will do that.
          liveScheduleService.deleteLiveEvent(mpId);
          break;
        case UpdateAgentId:
        case UpdateStart:
        case UpdateEnd:
          if (isLive(mpId)) {
            DublinCoreCatalog episodeDC = schedulerService.getDublinCore(mpId);
            liveScheduleService.createOrUpdateLiveEvent(mpId, episodeDC);
          }
          break;
        case UpdateRecordingStatus:
          String state = schedulerItem.getRecordingState();
          if (RecordingState.CAPTURE_FINISHED.equals(state) || RecordingState.UPLOADING.equals(state)
                  || RecordingState.UPLOADING.equals(state) || RecordingState.CAPTURE_ERROR.equals(state)
                  || RecordingState.UPLOAD_ERROR.equals(state))
            if (isLive(mpId))
              liveScheduleService.deleteLiveEvent(mpId);
          break;
        case UpdateOptOut:
        case UpdateBlacklist:
        case UpdatePresenters:
        case UpdateReviewStatus:
          break;
        default:
          throw new IllegalArgumentException("Unhandled type of SchedulerItem");
      }
    } catch (Exception e) {
      logger.warn(String.format("Exception occurred for mp %s, event type %s", mpId, schedulerItem.getType()), e);
    } finally {
      logger.debug("Scheduler message handler END for mp {} event type {} in thread {}", mpId, schedulerItem.getType(),
              Thread.currentThread().getId());
    }
  }

  protected boolean isLive(String mpId) {
    try {
      Map<String, String> config = schedulerService.getWorkflowConfig(mpId);
      return BooleanUtils.toBoolean((String) config.get(PUBLISH_LIVE_PROPERTY));
    } catch (UnauthorizedException | NotFoundException | SchedulerException e) {
      logger.debug("Could not get workflow configuration for mp {}. This is probably ok.", mpId);
      return false; // Assume non-live
    }
  }

  // === Set by OSGI begin
  public void setSchedulerService(SchedulerService service) {
    this.schedulerService = service;
  }
  // === Set by OSGI end

}
