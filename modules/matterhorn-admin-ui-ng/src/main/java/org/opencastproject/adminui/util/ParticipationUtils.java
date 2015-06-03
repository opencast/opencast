/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.adminui.util;

import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase.SortType;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

/**
 * Utils method for the Participation implementation
 */
public final class ParticipationUtils {

  private static final Logger logger = LoggerFactory.getLogger(ParticipationUtils.class);

  private ParticipationUtils() {
  }

  /**
   * Return the list of recordings by the given event identifiers.
   *
   * @param schedulerService
   *          the scheduler service
   * @param participationDatabase
   *          the participation database
   * @param eventIds
   *          the event identifiers
   * @return the list of recordings
   */
  public static List<Recording> getRecordingsByEventId(SchedulerService schedulerService,
          ParticipationManagementDatabase participationDatabase, List<String> eventIds) {
    List<Recording> recordings = new ArrayList<Recording>();
    for (String eventId : eventIds) {
      try {
        Long scheduledEventId = schedulerService.getEventId(eventId);
        recordings.add(participationDatabase.getRecordingByEvent(scheduledEventId));
      } catch (ParticipationManagementDatabaseException e) {
        logger.error("Unable to get recordings by event {}: {}", eventId, ExceptionUtils.getStackTrace(e));
        throw new WebApplicationException(e);
      } catch (NotFoundException e) {
        logger.info("Didn't find any recorings for event {}", eventId);
        continue;
      } catch (SchedulerException e) {
        logger.error("Unable to get scheduled event id by event {}: {}", eventId, ExceptionUtils.getStackTrace(e));
        throw new WebApplicationException(e);
      }
    }
    return recordings;
  }

  /**
   * @param input
   *          The input text from the endpoint.
   * @return The enum that matches the input string or null if none can be found.
   */
  public static Option<SortType> getMessagesSortField(String input) {
    if (StringUtils.isNotBlank(input)) {
      String upperCase = input.toUpperCase();
      SortType sortType = null;
      try {
        sortType = ParticipationManagementDatabase.SortType.valueOf(upperCase);
      } catch (IllegalArgumentException e) {
        return Option.<SortType> none();
      }
      return Option.option(sortType);
    }
    return Option.<SortType> none();
  }

}
