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

package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Business object for a recording.
 */
public class Recording {

  /** The recording identifier */
  private Option<Long> id;

  /** The syllabus+ activity id */
  private String activityId;

  /** The MH scheduled event identifier */
  private Option<Long> eventId;

  /** The title of the recording */
  private String title;

  /** The staff for this recording */
  private List<Person> staff = new ArrayList<Person>();

  /** The course */
  private Option<Course> course = Option.<Course> none();

  /** The room */
  private Room room;

  /** The modification date */
  private Date modificationDate;

  /** Whether the recording is deleted */
  private boolean deleted;

  /** The start date */
  private Date start;

  /** The stop date */
  private Date stop;

  /** The participation list */
  private List<Person> participation = new ArrayList<Person>();

  /** The list of messages */
  private List<Message> messages = new ArrayList<Message>();

  /** The capture agent */
  private CaptureAgent captureAgent;

  /** The fingerprint */
  private Option<String> fingerprint;

  /** The scheduling schedulingSource */
  private Option<SchedulingSource> schedulingSource;

  /** Trim flag. If true the recording will be handled by the TrimWOH. */
  private boolean trim;

  /** The actions triggered by the recording */
  private List<Action> actions = new ArrayList<Action>();

  public Recording(Option<Long> id, String activityId, Option<Long> eventId, String title, List<Person> staff,
          Option<Course> course, Room room, Date modificationDate, boolean deleted, Date start, Date stop,
          List<Person> participation, List<Message> messages, CaptureAgent captureAgent, List<Action> actions,
          Option<String> fingerprint, boolean trim) {
    this.id = id;
    this.activityId = activityId;
    this.eventId = eventId;
    this.title = title;
    // since this field is mutable a copy has to be created to both prevent side effects on the passed list
    // and to ensure the list is mutable
    this.staff = new ArrayList<Person>(staff);
    this.course = course;
    this.room = room;
    this.modificationDate = modificationDate;
    this.deleted = deleted;
    this.start = start;
    this.stop = stop;
    this.participation = new ArrayList<Person>(participation);
    this.messages = new ArrayList<Message>(messages);
    this.captureAgent = captureAgent;
    this.actions = new ArrayList<Action>(actions);
    this.fingerprint = fingerprint;
    this.trim = trim;
    this.schedulingSource = Option.none(SchedulingSource.class);
  }

  /**
   * Creates a recording
   *
   * @param activityId
   *          the activity id
   * @param title
   *          the recording title
   * @param staff
   *          the staff list
   * @param course
   *          the course
   * @param room
   *          the room
   * @param modificationDate
   *          the modification date
   * @param start
   *          the start date
   * @param stop
   *          the end date
   * @param participation
   *          the participation list
   * @param captureAgent
   *          the capture agent
   */
  public static Recording recording(String activityId, String title, List<Person> staff, Course course, Room room,
          Date modificationDate, Date start, Date stop, List<Person> participation, CaptureAgent captureAgent) {
    return new Recording(none(Long.class), activityId, none(Long.class), title, staff, some(course), room,
            modificationDate, false, start, stop, participation, nil(Message.class), captureAgent, nil(Action.class),
            none(String.class), false);
  }

  /**
   * Creates a recording
   *
   * @param activityId
   *          the activity id
   * @param title
   *          the recording title
   * @param staff
   *          the staff list
   * @param course
   *          the course
   * @param room
   *          the room
   * @param modificationDate
   *          the modification date
   * @param start
   *          the start date
   * @param stop
   *          the end date
   * @param participation
   *          the participation list
   * @param messages
   *          the message list
   * @param eventId
   *          the event id
   * @param captureAgent
   *          the capture agent
   * @param actions
   *          the actions triggered by this recording
   * @param deleted
   *          the deleted flag
   */
  public static Recording recording(String activityId, String title, List<Person> staff, Option<Course> course,
          Room room, Date modificationDate, Date start, Date stop, List<Person> participation, List<Message> messages,
          Option<Long> eventId, CaptureAgent captureAgent, List<Action> actions, boolean deleted, boolean trim) {
    return new Recording(none(Long.class), activityId, eventId, title, staff, course, room, modificationDate, deleted,
            start, stop, participation, messages, captureAgent, actions, none(String.class), trim);
  }

  /**
   * Sets the recording identifier
   *
   * @param id
   *          the recording id
   */
  public void setId(Option<Long> id) {
    this.id = id;
  }

  /**
   * Returns the recording identifier
   *
   * @return the recording id
   */
  public Option<Long> getId() {
    return id;
  }

  public static final Function<Recording, Option<Long>> getId = new Function<Recording, Option<Long>>() {
    @Override
    public Option<Long> apply(Recording recording) {
      return recording.getId();
    }
  };

  /**
   * Sets the activity identifier
   *
   * @param activityId
   *          the activity id
   */
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /**
   * Returns the activity id
   *
   * @return the activity id
   */
  public String getActivityId() {
    return activityId;
  }

  /**
   * Sets the recording title
   *
   * @param title
   *          the recording tile
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Returns the recording title
   *
   * @return the recording tile
   */
  public String getTitle() {
    return this.title;
  }

  /**
   * Sets the event identifier
   *
   * @param eventId
   *          the event id
   */
  public void setEventId(Long eventId) {
    this.eventId = option(eventId);
  }

  /**
   * Sets whether the recording is deleted
   *
   * @param deleted
   *          the deleted flag
   */
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  /**
   * Returns whether the recording is deleted
   *
   * @return the deleted flag
   */
  public boolean isDeleted() {
    return deleted;
  }

  /**
   * Returns the event identifier
   *
   * @return the event id
   */
  public Option<Long> getEventId() {
    return eventId;
  }

  /**
   * Sets the action
   *
   * @param action
   *          the action
   */
  public void setAction(List<Action> action) {
    this.actions = action;
  }

  /**
   * Returns the action
   *
   * @return the action
   */
  public List<Action> getAction() {
    return actions;
  }

  /**
   * Add an action to trigger with this recording
   *
   * @param action
   *          the action to be triggered
   * @return true if this collection changed as a result of the call
   */
  public boolean addAction(Action action) {
    if (action == null)
      throw new IllegalArgumentException("The action must not be null!");

    return actions.add(action);
  }

  /**
   * Remove an action to trigger with this recording
   *
   * @param action
   *          the action to remove
   * @return true if this collection changed as a result of the call
   */
  public boolean removeAction(Action action) {
    if (action == null)
      throw new IllegalArgumentException("The action must not be null!");

    return actions.remove(action);
  }

  /**
   * Sets the capture agent
   *
   * @param captureAgent
   *          the capture agent
   */
  public void setCaptureAgent(CaptureAgent captureAgent) {
    this.captureAgent = captureAgent;
  }

  /**
   * Returns the capture agent
   *
   * @return the capture agent
   */
  public CaptureAgent getCaptureAgent() {
    return captureAgent;
  }

  /**
   * Sets the course
   *
   * @param course
   *          the course
   */
  public void setCourse(Option<Course> course) {
    this.course = course;
  }

  /**
   * Returns the course
   *
   * @return the course
   */
  public Option<Course> getCourse() {
    return course;
  }

  public static final Function<Recording, Option<Course>> getCourse = new Function<Recording, Option<Course>>() {
    @Override
    public Option<Course> apply(Recording recording) {
      return recording.getCourse();
    }
  };

  /**
   * Sets the modification date
   *
   * @param modificationDate
   *          the modification date
   */
  public void setModificationDate(Date modificationDate) {
    this.modificationDate = modificationDate;
  }

  /**
   * Returns the modification date
   *
   * @return the modificationDate date
   */
  public Date getModificationDate() {
    return modificationDate;
  }

  /**
   * Sets the start date
   *
   * @param start
   *          the start date
   */
  public void setStart(Date start) {
    this.start = start;
  }

  /**
   * Returns the start date
   *
   * @return the start date
   */
  public Date getStart() {
    return start;
  }

  /**
   * Sets the stop date
   *
   * @param stop
   *          the stop date
   */
  public void setStop(Date stop) {
    this.stop = stop;
  }

  /**
   * Returns the stop date
   *
   * @return the stop date
   */
  public Date getStop() {
    return stop;
  }

  /**
   * Sets the room
   *
   * @param room
   *          the room
   */
  public void setRoom(Room room) {
    this.room = room;
  }

  /**
   * Returns the room
   *
   * @return the room
   */
  public Room getRoom() {
    return room;
  }

  /**
   * Sets the staff list
   *
   * @param staff
   *          the staff list
   */
  public void setStaff(List<Person> staff) {
    this.staff = notNull(staff, "staff");
  }

  /**
   * Returns the staff list
   *
   * @return the staff list
   */
  public List<Person> getStaff() {
    return staff;
  }

  public static final Function<Recording, List<Person>> getStaff = new Function<Recording, List<Person>>() {
    @Override
    public List<Person> apply(Recording recording) {
      return recording.getStaff();
    }
  };

  /**
   * Add a member to the staff
   *
   * @param staffMember
   *          the new staff member
   * @return true if this collection changed as a result of the call
   */
  public boolean addStaffMember(Person staffMember) {
    return staff.add(notNull(staffMember, "staffMember"));
  }

  /**
   * Remove a member of the staff
   *
   * @param staffMember
   *          the member to remove from the staff
   * @return true if this collection changed as a result of the call
   */
  public boolean removeStaffMember(Person staffMember) {
    return staff.remove(notNull(staffMember, "staffMember"));
  }

  /**
   * Sets the participation list
   *
   * @param participation
   *          the participation list
   */
  public void setParticipation(List<Person> participation) {
    this.participation = notNull(participation, "participation");
  }

  /**
   * Returns the participation list
   *
   * @return the participation list
   */
  public List<Person> getParticipation() {
    return participation;
  }

  /**
   * Add a participant to the recording
   *
   * @param participant
   *          the participant to add to this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean addParticipant(Person participant) {
    return participation.add(notNull(participant, "participant"));
  }

  /**
   * Remove a participant from this recording
   *
   * @param participant
   *          the participant to remove from this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean removeParticipant(Person participant) {
    return participation.remove(notNull(participant, "participant"));
  }

  /**
   * Sets the message list
   *
   * @param messages
   *          the message list
   */
  public void setMessages(List<Message> messages) {
    this.messages = notNull(messages, "messages");
  }

  /**
   * Returns the message list
   *
   * @return the message list
   */
  public List<Message> getMessages() {
    return messages;
  }

  /**
   * Add a message to the recording
   *
   * @param message
   *          the message to add to this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean addMessage(Message message) {
    return messages.add(notNull(message, "message"));
  }

  /**
   * Remove a message from the recording
   *
   * @param message
   *          the message to remove from this recording
   * @return true if this collection changed as a result of the call
   */
  public boolean removeMessage(Message message) {
    return messages.remove(notNull(message, "message"));
  }

  /**
   * Sets the recording's fingerprint, which should not exceed 32 bits.
   *
   * @param fingerprint
   *          the fingerprint
   */
  public void setFingerprint(Option<String> fingerprint) {
    this.fingerprint = fingerprint;
  }

  /**
   * Returns the recording's fingerprint.
   *
   * @return the fingerprint
   */
  public Option<String> getFingerprint() {
    return fingerprint;
  }

  public boolean isTrim() {
    return trim;
  }

  public void setTrim(boolean trim) {
    this.trim = trim;
  }

  public void setSchedulingSource(Option<SchedulingSource> schedulingSource) {
    this.schedulingSource = schedulingSource;
  }

  public Option<SchedulingSource> getSchedulingSource() {
    return schedulingSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Recording recording = (Recording) o;
    return id.equals(recording.getId()) && activityId.equals(recording.getActivityId())
            && title.equals(recording.getTitle())
            && staff.equals(recording.getStaff()) && course.equals(recording.getCourse())
            && room.equals(recording.getRoom()) && modificationDate.equals(recording.getModificationDate())
            && start.equals(recording.getStart()) && stop.equals(recording.getStop())
            && participation.equals(recording.getParticipation()) && messages.equals(recording.getMessages())
            && eventId.equals(recording.getEventId()) && captureAgent.equals(recording.getCaptureAgent())
            && actions.equals(recording.getAction()) && deleted == recording.isDeleted() && trim == recording.trim
            && schedulingSource.equals(recording.getSchedulingSource());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, activityId, title, staff, course, room, modificationDate, start, stop, participation,
            messages, eventId, captureAgent, actions, deleted, trim, schedulingSource);
  }

  @Override
  public String toString() {
    return "Recording:" + id;
  }

  public Obj toJson() {
    return Jsons.obj(Jsons.p("title", title), Jsons.p("start", DateTimeSupport.toUTC(start.getTime())),
            Jsons.p("end", DateTimeSupport.toUTC(start.getTime())), Jsons.p("location", room.toJson()));
  }
}
