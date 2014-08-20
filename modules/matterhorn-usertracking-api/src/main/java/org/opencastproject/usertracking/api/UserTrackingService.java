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
package org.opencastproject.usertracking.api;

import org.opencastproject.util.NotFoundException;

import java.text.ParseException;



/**
 * Provides annotation capabilities, possibly to the engage tools, possibly to other services.
 */
public interface UserTrackingService {

  /**
   * Adds a new annotation to the database and returns the event with an updated annotationId, to make sure the
   * annotationId stays unique
   *
   * @param a
   *          The UserAction that will be added to the database
   * @param session
   *          The UserSession associated with this footprint
   * @return the updated annotation, with a new ID. NULL if there are errors while adding the annotation.
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  UserAction addUserFootprint(UserAction a, UserSession session) throws UserTrackingException;

  /**
   * Adds a new tracking event to the database and returns the event with an updated annotationId, to make sure the
   * annotationId stays unique
   *
   * @param a
   *          The UserAction that will be added to the database
   * @param session
   *          The UserSession associated with this footprint
   * @return the updated annotation, with a new ID. NULL if there are errors while adding the annotation.
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  UserAction addUserTrackingEvent(UserAction a, UserSession session) throws UserTrackingException;

  /**
   * Returns annotations
   *
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  UserActionList getUserActions(int offset, int limit) throws UserTrackingException;

  /**
   * Returns annotations of a given key
   *
   * @param key
   *          The annotation key
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  UserActionList getUserActionsByType(String key, int offset, int limit) throws UserTrackingException;

  /**
   * Returns annotations of a given day (YYYYMMDD)
   *
   * @param day
   *          The day in the format of YYYYMMDD
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  UserActionList getUserActionsByDay(String day, int offset, int limit) throws UserTrackingException;

  /**
   * Returns annotations of a given key and day
   *
   * @param key
   *          the annotation key
   * @param day
   *          the day
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  UserActionList getUserActionsByTypeAndDay(String key, String day, int offset, int limit) throws UserTrackingException;

  /**
   * Returns annotations of a given key and mediapackage id
   *
   * @param key
   *          the annotation key
   * @param mediapackageId
   *          the mediapackage id
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  UserActionList getUserActionsByTypeAndMediapackageId(String key, String mediapackageId, int offset, int limit)
          throws UserTrackingException;

  /**
   * Returns annotations of a given key and mediapackage id ordered by date.
   *
   * @param key
   *          the annotation key
   * @param mediapackageId
   *          the mediapackage id
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  UserActionList getUserActionsByTypeAndMediapackageIdByDate(String key, String mediapackageId, int offset, int limit)
          throws UserTrackingException;


  /**
   * Returns annotations of a given key and mediapackage id ordered descending by date.
   *
   * @param key
   *          the annotation key
   * @param mediapackageId
   *          the mediapackage id
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  UserActionList getUserActionsByTypeAndMediapackageIdByDescendingDate(String key, String mediapackageId, int offset, int limit)
          throws UserTrackingException;

  /**
   * Returns the views of a mediapackage
   *
   * @param mediapackageId
   *          the mediapackeId
   * @return the views
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  int getViews(String mediapackageId) throws UserTrackingException;

  /**
   * Returns a report
   *
   * @param from
   *          The from day key
   * @param to
   *          The to day key
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the report
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  Report getReport(String from, String to, int offset, int limit) throws UserTrackingException, ParseException;

  /**
   * Returns a report
   *
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the report
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  Report getReport(int offset, int limit) throws UserTrackingException;

  /**
   * Gets the summary of user activity for a given media package and type of activity.
   *
   * @param key
   *          The annotation key to look for.
   * @param mediapackageId
   *          The mediapackage to collect the summaries from.
   * @param offset
   *          The offset
   * @param limit
   *          The limit
   * @return A summary of all user activity that the user is able to see.
   */
  UserSummaryList getUserSummaryByTypeAndMediaPackage(String key, String mediapackageId);

  /**
   * Returns a list of footprints, if a userId is passed only the footprints of that user are returned.
   *
   * @param mediapackageId
   *          The mediapackageId
   * @param userId
   *          The userId is optional
   * @return the footprintList
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   */
  FootprintList getFootprints(String mediapackageId, String userId) throws UserTrackingException;

  /**
   * Get a single user action by its identifier.
   *
   * @param id
   *          the user action identifier
   * @return the user action
   * @throws UserTrackingException
   *           if the user tracking service encounters an error
   * @throws NotFoundException
   *           if the no user action with this identifier exists
   */
  UserAction getUserAction(Long id) throws UserTrackingException, NotFoundException;

  /**
   * Returns the flag turning user tracking on or off.
   * Turning user tracking off disables the detailed information gathering, but does *not* disable footprint gathering.
   *
   * @return True if detailed user tracking should be gathered, false otherwise
   */
  boolean getUserTrackingEnabled();

}
