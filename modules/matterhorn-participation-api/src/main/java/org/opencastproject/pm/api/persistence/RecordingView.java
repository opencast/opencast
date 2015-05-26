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

package org.opencastproject.pm.api.persistence;

import org.opencastproject.util.data.Option;

import java.util.Date;

/**
 * Business object for a recording view.
 */
public class RecordingView {
  private final long id;

  private final Option<Long> eventId;

  private final String title;

  private final String presenter;

  private final String course;

  private final Date startDate;

  private final Date endDate;

  private final String room;

  private final String agent;

  private final String actions;

  private final boolean trim;

  public RecordingView(long id, Option<Long> eventId, String title, String presenter, String course, Date start,
          Date end, String room, String agent, String actions, boolean trim) {
    this.id = id;
    this.eventId = eventId;
    this.title = title;
    this.presenter = presenter;
    this.course = course;
    this.startDate = start;
    this.endDate = end;
    this.room = room;
    this.agent = agent;
    this.actions = actions;
    this.trim = trim;
  }

  public String getTitle() {
    return title;
  }

  public String getPresenter() {
    return presenter;
  }

  public String getCourse() {
    return course;
  }

  public Date getStartDate() {
    return startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public String getRoom() {
    return room;
  }

  public String getAgent() {
    return agent;
  }

  public String getActions() {
    return actions;
  }

  public long getId() {
    return id;
  }

  public Option<Long> getEventId() {
    return eventId;
  }

  public boolean isTrim() {
    return trim;
  }
}
