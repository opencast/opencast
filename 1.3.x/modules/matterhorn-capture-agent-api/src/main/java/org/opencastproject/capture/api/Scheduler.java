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
package org.opencastproject.capture.api;

import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Interface to OSGi service for fetching capture schedules and starting captures (MH-1052).
 */
 
public interface Scheduler {

  /**
   * Sets the schedule data {@code URL} from which to gather scheduling data.  This should be a endpoint which generates iCal (RFC 2445) format scheduling data.
   * @param url The {@code URL} to pull the calendaring data from.
   */
  void setScheduleEndpoint(URL url);

  /**
   * Gets the current schedule data {@code URL}.  This should be an endpoint which generates iCal (RFC 2445) format scheduling data.
   * @return The current schedule data {@code URL}.
   */
  URL getScheduleEndpoint();

  /**
   * Polls the current schedule endpoint {@code URL} for new scheduling data.
   * If the new schedule data contains an error or is unreachable the previous recording schedule is used instead.
   */
  void updateCalendar();

  /**
   * Gets the time between refreshes of the scheduling data.
   * @return The number of seconds between refreshes of the scheduling data.
   */
  int getPollingTime();

  /**
   * Enables polling for new calendar data.
   * @param enable True to enable polling, false otherwise.
   */
  void enablePolling(boolean enable);

  /**
   * Checks to see if the is set to automatically poll for new scheduling data.
   * @return True if the system is set to poll for new data, false otherwise.
   */
  boolean isCalendarPollingEnabled();

  /**
   * Starts the scheduling system.  Calling this enables scheduled captures.
   * @return True if the start succeeds, false otherwise.
   */
  boolean startScheduler();

  /**
   * Checks to see if the system is set to capture from its calendar data.
   * @return True if the system is set to capture from a schedule, false otherwise.
   */
  boolean isSchedulerEnabled();

  /**
   * Stops the scheduling system.  Calling this disables scheduled captures.
   * @return True if the stop succeeds, false otherwise.
   */
  boolean stopScheduler();

  /**
   * Returns the current schedule in a {@code List} of {@code ScheduledEvent}s.
   * @return The current schedule in a {@code List} of {@code ScheduledEvent}s or null in the case of an error.
   */
  List<ScheduledEvent> getSchedule();
  
  /**
   * Schedules an immediate {@code IngestJob} for the recording.  This method does not create a manifest or zip the recording.
   * @param recordingID The ID of the recording to it ingest.
   * @return True if the job was scheduled correctly, false otherwise.
   */
  boolean scheduleIngest(String id);

  /**
   * Schedules a {@code StopCaptureJob} to stop a capture at a given time.
   * @param recordingID The recordingID of the recording you wish to stop.
   * @param stop The time (in seconds since 1970) in a {@code Date} at which to stop the capture.
   * @return True if the job was scheduled, false otherwise.
   */
  boolean scheduleUnscheduledStopCapture(String recordingID, Date stop);

  /**
   * Schedules an immediate {@code SerializeJob} for the recording.  This method will manifest and zip the recording before ingesting it.
   * @param recordingID The ID of the recording to it ingest.
   * @return True if the job was scheduled correctly, false otherwise.
   */
  boolean scheduleSerializationAndIngest(String id);
}
