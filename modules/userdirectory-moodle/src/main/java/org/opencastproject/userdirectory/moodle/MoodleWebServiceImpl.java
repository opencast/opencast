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

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of the Moodle web service client.
 */
public class MoodleWebServiceImpl implements MoodleWebService {
  /**
   * The logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(MoodleUserProviderInstance.class);

  /**
   * HTTP user agent when performing requests.
   */
  private static final String OC_USERAGENT = "Opencast";

  /**
   * The URL of the Moodle instance.
   */
  private URI url;

  /**
   * The token used to call Moodle REST webservices.
   */
  private String token;

  /**
   * Constructs a new Moodle web service client.
   *
   * @param url   URL of the Moodle instance
   * @param token Web service token
   */
  public MoodleWebServiceImpl(URI url, String token) {
    this.url = url;
    this.token = token;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MoodleUser> coreUserGetUsersByField(CoreUserGetUserByFieldFilters filter, List<String> values)
          throws URISyntaxException, IOException, MoodleWebServiceException, ParseException {
    logger.debug("coreUserGetUsersByField(({}, {}))", filter, values);

    List<NameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("field", filter.toString()));

    for (int i = 0; i < values.size(); ++i)
      params.add(new BasicNameValuePair("values[" + i + "]", values.get(i)));

    Object resp = executeMoodleRequest(MOODLE_FUNCTION_CORE_USER_GET_USERS_BY_FIELD, params);

    // Parse response
    if (resp == null || !(resp instanceof JSONArray))
      throw new MoodleWebServiceException("Moodle responded in unexpected format");

    JSONArray respArray = (JSONArray) resp;
    List<MoodleUser> users = new ArrayList<>(respArray.size());

    for (Object userObj : respArray) {
      if (!(userObj instanceof JSONObject))
        throw new MoodleWebServiceException("Moodle responded in unexpected format");

      JSONObject userJsonObj = (JSONObject) userObj;
      MoodleUser user = new MoodleUser();

      if (userJsonObj.containsKey("id"))
        user.setId(userJsonObj.get("id").toString());
      if (userJsonObj.containsKey("username"))
        user.setUsername(userJsonObj.get("username").toString());
      if (userJsonObj.containsKey("fullname"))
        user.setFullname(userJsonObj.get("fullname").toString());
      if (userJsonObj.containsKey("idnumber"))
        user.setIdnumber(userJsonObj.get("idnumber").toString());
      if (userJsonObj.containsKey("email"))
        user.setEmail(userJsonObj.get("email").toString());
      if (userJsonObj.containsKey("auth"))
        user.setAuth(userJsonObj.get("auth").toString());

      users.add(user);
    }

    return users;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.userdirectory.moodle.MoodleWebService#toolOpencastGetCoursesForInstructor(String)
   */
  @Override
  public List<String> toolOpencastGetCoursesForInstructor(String username)
          throws URISyntaxException, IOException, MoodleWebServiceException, ParseException {
    logger.debug("toolOpencastGetCoursesForInstructor({})", username);

    List<NameValuePair> params = Collections
            .singletonList((NameValuePair) new BasicNameValuePair("username", username));

    return parseIdList(executeMoodleRequest(MOODLE_FUNCTION_TOOL_OPENCAST_GET_COURSES_FOR_INSTRUCTOR, params));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.userdirectory.moodle.MoodleWebService#toolOpencastGetCoursesForLearner(String)
   */
  @Override
  public List<String> toolOpencastGetCoursesForLearner(String username)
          throws URISyntaxException, IOException, MoodleWebServiceException, ParseException {
    logger.debug("toolOpencastGetCoursesForLearner({})", username);

    List<NameValuePair> params = Collections
            .singletonList((NameValuePair) new BasicNameValuePair("username", username));

    return parseIdList(executeMoodleRequest(MOODLE_FUNCTION_TOOL_OPENCAST_GET_COURSES_FOR_LEARNER, params));
  }

  @Override
  public List<String> toolOpencastGetGroupsForLearner(String username)
          throws URISyntaxException, IOException, MoodleWebServiceException, ParseException {
    logger.debug("toolOpencastGetGroupsForLearner({})", username);

    List<NameValuePair> params = Collections
            .singletonList((NameValuePair) new BasicNameValuePair("username", username));

    return parseIdList(executeMoodleRequest(MOODLE_FUNCTION_TOOL_OPENCAST_GET_GROUPS_FOR_LEARNER, params));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.userdirectory.moodle.MoodleWebService#getURL()
   */
  @Override
  public String getURL() {
    return url.toString();
  }

  /**
   * Parses the returned Moodle response for a list of IDs.
   *
   * @param resp The Moodle response. It should be of type {@link JSONArray}.
   * @return A list of Moodle IDs.
   * @throws MoodleWebServiceException If the parsing failed because the response format was unexpected.
   */
  private List<String> parseIdList(Object resp) throws MoodleWebServiceException {
    if (resp == null)
      return new LinkedList<>();

    if (!(resp instanceof JSONArray))
      throw new MoodleWebServiceException("Moodle responded in unexpected format");

    JSONArray respArray = (JSONArray) resp;
    List<String> ids = new ArrayList<>(respArray.size());

    for (Object courseObj : respArray) {
      if (!(courseObj instanceof JSONObject) || ((JSONObject) courseObj).get("id") == null)
        throw new MoodleWebServiceException("Moodle responded in unexpected format");

      ids.add(((JSONObject) courseObj).get("id").toString());
    }

    return ids;
  }

  /**
   * Executes a Moodle webservice request.
   *
   * @param function The function to execute.
   * @param params   Additional parameters to pass.
   * @return A JSON object, array, String, Number, Boolean, or null.
   * @throws URISyntaxException        In case the URL cannot be constructed.
   * @throws IOException               In case of an IO error.
   * @throws MoodleWebServiceException In case Moodle returns an error.
   * @throws ParseException            In case the Moodle response cannot be parsed.
   */
  private Object executeMoodleRequest(String function, List<NameValuePair> params)
          throws URISyntaxException, IOException, MoodleWebServiceException, ParseException {
    // Build URL
    URIBuilder url = new URIBuilder(this.url);
    url.addParameters(params);
    url.addParameter("wstoken", token);
    url.addParameter("wsfunction", function);
    url.addParameter("moodlewsrestformat", "json");

    // Execute request
    HttpGet get = new HttpGet(url.build());
    get.setHeader("User-Agent", OC_USERAGENT);

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      try (CloseableHttpResponse resp = client.execute(get)) {
        // Parse response
        BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(reader);

        // Check for errors
        if (obj instanceof JSONObject) {
          JSONObject jObj = (JSONObject) obj;
          if (jObj.containsKey("exception") || jObj.containsKey("errorcode"))
            throw new MoodleWebServiceException("Moodle returned an error: " + jObj.toJSONString());
        }

        return obj;
      }
    }
  }
}
