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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.usertracking.api

import org.opencastproject.util.NotFoundException

import java.text.ParseException


/**
 * Provides annotation capabilities, possibly to the engage tools, possibly to other services.
 */
interface UserTrackingService {

    /**
     * Returns the flag turning user tracking on or off.
     * Turning user tracking off disables the detailed information gathering, but does *not* disable footprint gathering.
     *
     * @return True if detailed user tracking should be gathered, false otherwise
     */
    val userTrackingEnabled: Boolean

    /**
     * Adds a new annotation to the database and returns the event with an updated annotationId, to make sure the
     * annotationId stays unique
     *
     * @param a
     * The UserAction that will be added to the database
     * @param session
     * The UserSession associated with this footprint
     * @return the updated annotation, with a new ID. NULL if there are errors while adding the annotation.
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun addUserFootprint(a: UserAction, session: UserSession): UserAction

    /**
     * Adds a new tracking event to the database and returns the event with an updated annotationId, to make sure the
     * annotationId stays unique
     *
     * @param a
     * The UserAction that will be added to the database
     * @param session
     * The UserSession associated with this footprint
     * @return the updated annotation, with a new ID. NULL if there are errors while adding the annotation.
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun addUserTrackingEvent(a: UserAction, session: UserSession): UserAction

    /**
     * Returns annotations
     *
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getUserActions(offset: Int, limit: Int): UserActionList

    /**
     * Returns annotations of a given key
     *
     * @param key
     * The annotation key
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getUserActionsByType(key: String, offset: Int, limit: Int): UserActionList

    /**
     * Returns annotations of a given day (YYYYMMDD)
     *
     * @param day
     * The day in the format of YYYYMMDD
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getUserActionsByDay(day: String, offset: Int, limit: Int): UserActionList

    /**
     * Returns annotations of a given key and day
     *
     * @param key
     * the annotation key
     * @param day
     * the day
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getUserActionsByTypeAndDay(key: String, day: String, offset: Int, limit: Int): UserActionList

    /**
     * Returns annotations of a given key and mediapackage id
     *
     * @param key
     * the annotation key
     * @param mediapackageId
     * the mediapackage id
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getUserActionsByTypeAndMediapackageId(key: String, mediapackageId: String, offset: Int, limit: Int): UserActionList

    /**
     * Returns annotations of a given key and mediapackage id ordered by date.
     *
     * @param key
     * the annotation key
     * @param mediapackageId
     * the mediapackage id
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getUserActionsByTypeAndMediapackageIdByDate(key: String, mediapackageId: String, offset: Int, limit: Int): UserActionList


    /**
     * Returns annotations of a given key and mediapackage id ordered descending by date.
     *
     * @param key
     * the annotation key
     * @param mediapackageId
     * the mediapackage id
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getUserActionsByTypeAndMediapackageIdByDescendingDate(key: String, mediapackageId: String, offset: Int, limit: Int): UserActionList

    /**
     * Returns the views of a mediapackage
     *
     * @param mediapackageId
     * the mediapackeId
     * @return the views
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getViews(mediapackageId: String): Int

    /**
     * Returns a report
     *
     * @param from
     * The from day key
     * @param to
     * The to day key
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the report
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class, ParseException::class)
    fun getReport(from: String, to: String, offset: Int, limit: Int): Report

    /**
     * Returns a report
     *
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the report
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getReport(offset: Int, limit: Int): Report

    /**
     * Returns a list of footprints, if a userId is passed only the footprints of that user are returned.
     *
     * @param mediapackageId
     * The mediapackageId
     * @param userId
     * The userId is optional
     * @return the footprintList
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     */
    @Throws(UserTrackingException::class)
    fun getFootprints(mediapackageId: String, userId: String): FootprintList

    /**
     * Get a single user action by its identifier.
     *
     * @param id
     * the user action identifier
     * @return the user action
     * @throws UserTrackingException
     * if the user tracking service encounters an error
     * @throws NotFoundException
     * if the no user action with this identifier exists
     */
    @Throws(UserTrackingException::class, NotFoundException::class)
    fun getUserAction(id: Long?): UserAction

}
