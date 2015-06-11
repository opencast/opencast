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

import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.Blacklistable;
import org.opencastproject.pm.api.Building;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Period;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.SchedulingSource;
import org.opencastproject.pm.api.Synchronization;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import java.util.List;

/**
 * API that defines persistent storage of participation management.
 */
public interface ParticipationManagementDatabase {

  public enum SortType {
    DATE, DATE_DESC, SENDER, SENDER_DESC
  }

  Synchronization getLastSynchronization() throws ParticipationManagementDatabaseException;

  Synchronization storeLatestSynchronization(Synchronization sync) throws ParticipationManagementDatabaseException;

  long countTotalRecordings() throws ParticipationManagementDatabaseException;

  long countWeekRecordings() throws ParticipationManagementDatabaseException;

  long countDailyRecordings() throws ParticipationManagementDatabaseException;

  long countMessagesSent() throws ParticipationManagementDatabaseException;

  long countQuarterMessagesSent() throws ParticipationManagementDatabaseException;

  long countDailyMessagesSent() throws ParticipationManagementDatabaseException;

  long countMessageErrors() throws ParticipationManagementDatabaseException;

  long countPersons() throws ParticipationManagementDatabaseException;

  Course updateCourse(Course course) throws ParticipationManagementDatabaseException;

  Course findCourseBySeries(String series) throws ParticipationManagementDatabaseException, NotFoundException;

  List<CaptureAgent> getCaptureAgents() throws ParticipationManagementDatabaseException;

  List<Course> getCourses() throws ParticipationManagementDatabaseException;

  List<Message> getMessagesByRecordingId(long recordingId, Option<SortType> sort)
          throws ParticipationManagementDatabaseException;

  List<Message> getMessagesBySeriesId(String seriesId, Option<SortType> sortType)
          throws ParticipationManagementDatabaseException;

  Message updateMessage(Message message) throws ParticipationManagementDatabaseException;

  List<RecordingView> findRecordingsAsView(RecordingQuery query) throws ParticipationManagementDatabaseException;

  List<Recording> findRecordings(RecordingQuery query) throws ParticipationManagementDatabaseException;

  Recording getRecordingByEvent(long eventId) throws ParticipationManagementDatabaseException, NotFoundException;

  Recording getRecording(long id) throws ParticipationManagementDatabaseException, NotFoundException;

  Recording trimRecording(long id, boolean trim) throws ParticipationManagementDatabaseException, NotFoundException;

  Recording updateRecording(Recording recording) throws ParticipationManagementDatabaseException;

  void deleteRecording(long id) throws ParticipationManagementDatabaseException, NotFoundException;

  Blacklist getBlacklist(long id) throws ParticipationManagementDatabaseException, NotFoundException;

  Blacklist getBlacklistByPeriodId(long periodId) throws ParticipationManagementDatabaseException, NotFoundException;

  Period getPeriod(long periodId) throws ParticipationManagementDatabaseException, NotFoundException;

  List<Blacklist> findBlacklists(String type, Option<Integer> limit, Option<Integer> offset, Option<String> name,
          Option<String> purpose, Option<String> sort) throws ParticipationManagementDatabaseException;

  List<Blacklist> findBlacklists(Blacklistable... blacklistable) throws ParticipationManagementDatabaseException;

  Blacklist updateBlacklist(Blacklist blacklist) throws ParticipationManagementDatabaseException;

  Period updatePeriod(Period period) throws ParticipationManagementDatabaseException, NotFoundException;

  void deleteBlacklist(long blacklistId, Option<Long> periodId) throws ParticipationManagementDatabaseException,
          NotFoundException;

  void deletePeriod(long periodId) throws ParticipationManagementDatabaseException, NotFoundException;

  Building updateBuilding(Building building) throws ParticipationManagementDatabaseException;

  void deleteBuilding(Long id) throws ParticipationManagementDatabaseException, NotFoundException;

  List<Building> getBuildings() throws ParticipationManagementDatabaseException;

  List<String> getPurposesByType(String type) throws ParticipationManagementDatabaseException;

  List<Room> getRooms() throws ParticipationManagementDatabaseException;

  Room getRoom(long id) throws ParticipationManagementDatabaseException, NotFoundException;

  Room updateRoom(Room room) throws ParticipationManagementDatabaseException;

  List<Person> getPersons() throws ParticipationManagementDatabaseException;

  List<Person> findPersons(String emailQuery, int offset, int limit) throws ParticipationManagementDatabaseException;

  Person getPerson(long id) throws ParticipationManagementDatabaseException, NotFoundException;

  Person getPerson(String email) throws ParticipationManagementDatabaseException, NotFoundException;

  Person updatePerson(Person person) throws ParticipationManagementDatabaseException;

  void deletePerson(Long id) throws ParticipationManagementDatabaseException, NotFoundException;

  List<Person> getInstructors() throws ParticipationManagementDatabaseException;

  SchedulingSource updateSchedulingSource(SchedulingSource source) throws ParticipationManagementDatabaseException;

  boolean clearDatabase() throws ParticipationManagementDatabaseException;

  long countRecordingsByDateRangeAndRoom(long roomId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException;

  List<Recording> findRecordingsByDateRangeAndRoom(long roomId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException;

  long countAffectedCoursesByDateRangeAndRoom(long roomId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException;

  List<Course> findAffectedCoursesByDateRangeAndRoom(long roomId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException;

  long countRecordingsByDateRangeAndPerson(long personId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException;

  List<Recording> findRecordingsByDateRangeAndPerson(long personId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException;

  long countAffectedCoursesByDateRangeAndPerson(long personId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException;

  List<Course> findAffectedCoursesByDateRangeAndPerson(long personId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException;
}
