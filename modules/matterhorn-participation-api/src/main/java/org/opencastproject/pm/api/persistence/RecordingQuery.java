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

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Room;
import org.opencastproject.util.data.Option;

import java.util.Date;

/**
 * Represents a query to find recordings.
 */
public final class RecordingQuery {
  private Person[] staff = new Person[0];
  private Person[] assistedStudents = new Person[0];
  private Person[] participation = new Person[0];
  private Option<Date> startDate = none();
  private Option<Date> endDate = none();
  private Option<Date> startDateRange = none();
  private Option<Date> endDateRange = none();
  private Option<Course> course = none();
  private Option<String> fullText = none();
  private Option<Boolean> deleted = none();
  private Option<String> activityId = none();
  private Option<Room> room = none();
  private Option<Boolean> event = none();

  private RecordingQuery() {
  }

  public static RecordingQuery create() {
    return new RecordingQuery();
  }

  public static RecordingQuery createWithoutDeleted() {
    return new RecordingQuery().withoutDeleted();
  }

  public RecordingQuery withOnlyDeleted() {
    this.deleted = some(true);
    return this;
  }

  public RecordingQuery withDeleted() {
    this.deleted = none();
    return this;
  }

  public RecordingQuery withoutDeleted() {
    this.deleted = some(false);
    return this;
  }

  public RecordingQuery withRoom(Room room) {
    this.room = Option.option(room);
    return this;
  }

  public RecordingQuery withActivityId(String activityId) {
    this.activityId = Option.option(activityId);
    return this;
  }

  public RecordingQuery withStaff(Person[] staff) {
    this.staff = staff;
    return this;
  }

  public RecordingQuery withEvent() {
    this.event = some(true);
    return this;
  }

  public RecordingQuery withAssistedStudents(Person[] assistedStudents) {
    this.assistedStudents = assistedStudents;
    return this;
  }

  public RecordingQuery withParticipation(Person[] participation) {
    this.participation = participation;
    return this;
  }

  public RecordingQuery withCourse(Course course) {
    this.course = Option.option(course);
    return this;
  }

  /**
   * Search for recordings older then the given start date
   */
  public RecordingQuery withStartDateRange(Date start) {
    this.startDateRange = Option.option(start);
    return this;
  }

  /**
   * Search for recordings younger then the given end date
   */
  public RecordingQuery withEndDateRange(Date end) {
    this.endDateRange = Option.option(end);
    return this;
  }

  /**
   * Search for recordings with exactly the given start date
   */
  public RecordingQuery withStartDate(Date start) {
    this.startDate = Option.option(start);
    return this;
  }

  /**
   * Search for recordings with exactly the given end date
   */
  public RecordingQuery withEndDate(Date end) {
    this.endDate = Option.option(end);
    return this;
  }

  public RecordingQuery withFullText(String fullText) {
    this.fullText = Option.option(fullText);
    return this;
  }

  public Option<Boolean> isDeleted() {
    return deleted;
  }

  public Option<Boolean> isEvent() {
    return event;
  }

  public Person[] getStaff() {
    return staff;
  }

  public Person[] getAssistedStudents() {
    return assistedStudents;
  }

  public Person[] getParticipation() {
    return participation;
  }

  public Option<Course> getCourse() {
    return course;
  }

  public Option<Room> getRoom() {
    return room;
  }

  public Option<String> getActivityId() {
    return activityId;
  }

  public Option<Date> getStartDate() {
    return startDate;
  }

  public Option<Date> getEndDate() {
    return endDate;
  }

  public Option<Date> getStartDateRange() {
    return startDateRange;
  }

  public Option<Date> getEndDateRange() {
    return endDateRange;
  }

  public Option<String> getFullText() {
    return fullText;
  }
}
