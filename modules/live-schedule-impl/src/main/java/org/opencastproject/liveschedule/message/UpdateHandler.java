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
import org.opencastproject.message.broker.api.MessageItem;
import org.opencastproject.util.Log;

import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

public abstract class UpdateHandler {

  protected static final String PUBLISH_LIVE_PROPERTY = "publishLive";

  private static final Log logger = new Log(LoggerFactory.getLogger(SchedulerUpdateHandler.class));

  /** Services */
  protected LiveScheduleService liveScheduleService;

  protected String destinationId;

  public void activate(ComponentContext cc) {
    logger.info("Activating {}", this.getClass().getName());
  }

  public UpdateHandler(String destinationId) {
    this.destinationId = destinationId;
  }

  protected abstract void execute(MessageItem message);

  public String getDestinationId() {
    return destinationId;
  }

  // === Set by OSGI begin
  public void setLiveScheduleService(LiveScheduleService service) {
    this.liveScheduleService = service;
  }
  // === Set by OSGI end

}
