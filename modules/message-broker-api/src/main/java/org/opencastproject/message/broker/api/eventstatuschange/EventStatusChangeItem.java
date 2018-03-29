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

package org.opencastproject.message.broker.api.eventstatuschange;

import org.opencastproject.message.broker.api.MessageItem;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * {@link Serializable} class that represents all of the possible messages sent through an event status change queue.
 */
public class EventStatusChangeItem implements MessageItem, Serializable {

  private static final long serialVersionUID = -89800732905682262L;

  public static final String EVENT_STATUS_CHANGE_QUEUE_PREFIX = "EVENT_STATUS_CHANGE.";
  public static final String EVENT_STATUS_CHANGE_QUEUE = EVENT_STATUS_CHANGE_QUEUE_PREFIX + "QUEUE";

  public enum Type {
    Starting, Failed
  };

  private Type type;
  private List<String> eventIds;
  private String message;
  private String id;

  /**
   * @param type
   *          the new status for the event(s)
   * @param eventIds
   *          the id(s) of the event(s) to change the status for
   * @param message
   *          the message which describes why the status changes
   */
  public EventStatusChangeItem(Type type, List<String> eventIds, String message) {
    this.id = UUID.randomUUID().toString();
    this.type = type;
    this.eventIds = eventIds;
    this.message = message;
  }

  @Override
  public String getId() {
    return id;
  }

  public List<String> getEventIds() {
    return this.eventIds;
  }

  public String getMessage() {
    return this.message;
  }

  public Type getType() {
    return type;
  }

}
