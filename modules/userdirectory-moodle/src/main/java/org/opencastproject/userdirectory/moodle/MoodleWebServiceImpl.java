/*
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

import org.opencastproject.util.GsonUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
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
          throws URISyntaxException, IOException, MoodleWebServiceException {
    logger.debug("coreUserGetUsersByField(({}, {}))", filter, values);

    List<NameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("field", filter.toString()));

    for (int i = 0; i < values.size(); ++i) {
      params.add(new BasicNameValuePair("values[" + i + "]", values.get(i)));
    }

    JsonElement resp = executeMoodleRequest(MOODLE_FUNCTION_CORE_USER_GET_USERS_BY_FIELD, params);

    // Parse response
    if (resp == null || !resp.isJsonArray()) {
      throw new MoodleWebServiceException("Moodle responded in unexpected format");
    }

    JsonArray respArray = resp.getAsJsonArray();
    List<MoodleUser> users = new ArrayList<>(respArray.size());

    for (JsonElement userObj : respArray) {
      if (!userObj.isJsonObject()) {
        throw new MoodleWebServiceException("Moodle responded in unexpected format");
      }

      JsonObject userJsonObj = userObj.getAsJsonObject();
      MoodleUser user = new MoodleUser();

      if (userJsonObj.has("id")) {
        user.setId(GsonUtil.asText(userJsonObj.get("id")));
      }
      if (userJsonObj.has("username")) {
        user.setUsername(GsonUtil.asText(userJsonObj.get("username")));
      }
      if (userJsonObj.has("fullname")) {
        user.setFullname(GsonUtil.asText(userJsonObj.get("fullname")));
      }
      if (userJsonObj.has("idnumber")) {
        user.setIdnumber(GsonUtil.asText(userJsonObj.get("idnumber")));
      }
      if (userJsonObj.has("email")) {
        user.setEmail(GsonUtil.asText(userJsonObj.get("email")));
      }
      if (userJsonObj.has("auth")) {
        user.setAuth(GsonUtil.asText(userJsonObj.get("auth")));
      }

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
          throws URISyntaxException, IOException, MoodleWebServiceException {
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
          throws URISyntaxException, IOException, MoodleWebServiceException {
    logger.debug("toolOpencastGetCoursesForLearner({})", username);

    List<NameValuePair> params = Collections
            .singletonList((NameValuePair) new BasicNameValuePair("username", username));

    return parseIdList(executeMoodleRequest(MOODLE_FUNCTION_TOOL_OPENCAST_GET_COURSES_FOR_LEARNER, params));
  }

  @Override
  public List<String> toolOpencastGetGroupsForLearner(String username)
          throws URISyntaxException, IOException, MoodleWebServiceException {
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
  private List<String> parseIdList(JsonElement resp) throws MoodleWebServiceException {
    if (resp == null || resp.isJsonNull()) {
      return new LinkedList<>();
    }

    if (!resp.isJsonArray()) {
      throw new MoodleWebServiceException("Moodle responded in unexpected format");
    }

    JsonArray respArray = resp.getAsJsonArray();
    List<String> ids = new ArrayList<>(respArray.size());

    for (JsonElement courseObj : respArray) {
      if (!courseObj.isJsonObject() || courseObj.getAsJsonObject().get("id") == null) {
        throw new MoodleWebServiceException("Moodle responded in unexpected format");
      }

      ids.add(GsonUtil.asText(courseObj.getAsJsonObject().get("id")));
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
   */
  private JsonElement executeMoodleRequest(String function, List<NameValuePair> params)
          throws URISyntaxException, IOException, MoodleWebServiceException {
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
        JsonElement obj = GsonUtil.gson().fromJson(reader, JsonElement.class);

        // Check for errors
        if (obj != null && obj.isJsonObject()) {
          JsonObject jObj = obj.getAsJsonObject();
          if (jObj.has("exception") || jObj.has("errorcode")) {
            throw new MoodleWebServiceException("Moodle returned an error: " + jObj);
          }
        }

        return obj;
      }
    }
  }
}
