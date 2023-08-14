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

package org.opencastproject.userdirectory.moodle;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Client class for the Moodle web service.
 */
public interface MoodleWebService {
  String MOODLE_FUNCTION_CORE_USER_GET_USERS_BY_FIELD = "core_user_get_users_by_field";
  String MOODLE_FUNCTION_TOOL_OPENCAST_GET_COURSES_FOR_INSTRUCTOR = "tool_opencast_get_courses_for_instructor";
  String MOODLE_FUNCTION_TOOL_OPENCAST_GET_COURSES_FOR_LEARNER = "tool_opencast_get_courses_for_learner";
  String MOODLE_FUNCTION_TOOL_OPENCAST_GET_GROUPS_FOR_LEARNER = "tool_opencast_get_groups_for_learner";

  /**
   * Searches for a user in Moodle.
   *
   * @param filter Filter type to apply
   * @param values Value for the filter.
   * @return A {@link MoodleUser} list.
   * @throws URISyntaxException        In case the URL cannot be constructed.
   * @throws IOException               In case of an IO error.
   * @throws MoodleWebServiceException In case Moodle returns an error.
   * @throws ParseException            In case the Moodle response cannot be parsed.
   */
  List<MoodleUser> coreUserGetUsersByField(CoreUserGetUserByFieldFilters filter, List<String> values)
          throws URISyntaxException, IOException, MoodleWebServiceException, ParseException;

  /**
   * Returns the list of Moodle course IDs the given user has the instructor capability.
   *
   * @param username The username to search for.
   * @return A list of Moodle course IDs.
   * @throws URISyntaxException        In case the URL cannot be constructed.
   * @throws IOException               In case of an IO error.
   * @throws MoodleWebServiceException In case Moodle returns an error.
   * @throws ParseException            In case the Moodle response cannot be parsed.
   */
  List<String> toolOpencastGetCoursesForInstructor(String username)
          throws URISyntaxException, IOException, MoodleWebServiceException, ParseException;

  /**
   * Returns the list of Moodle course IDs the given user has the learner capability.
   *
   * @param username The username to search for.
   * @return A list of Moodle course IDs.
   * @throws URISyntaxException        In case the URL cannot be constructed.
   * @throws IOException               In case of an IO error.
   * @throws MoodleWebServiceException In case Moodle returns an error.
   * @throws ParseException            In case the Moodle response cannot be parsed.
   */
  List<String> toolOpencastGetCoursesForLearner(String username)
          throws URISyntaxException, IOException, MoodleWebServiceException, ParseException;

  /**
   * Returns the list of Moodle group IDs where the given user has the learner capability.
   *
   * @param username The username to search for.
   * @return A list of Moodle group IDs.
   * @throws URISyntaxException        In case the URL cannot be constructed.
   * @throws IOException               In case of an IO error.
   * @throws MoodleWebServiceException In case Moodle returns an error.
   * @throws ParseException            In case the Moodle response cannot be parsed.
   */
  List<String> toolOpencastGetGroupsForLearner(String username)
          throws URISyntaxException, IOException, MoodleWebServiceException, ParseException;

  /**
   * Returns the URL to the Moodle web service.
   *
   * @return URL to the Moodle web service.
   */
  String getURL();

  enum CoreUserGetUserByFieldFilters {
    id, lastname, firstname, idnumber, username, email, auth
  }
}
