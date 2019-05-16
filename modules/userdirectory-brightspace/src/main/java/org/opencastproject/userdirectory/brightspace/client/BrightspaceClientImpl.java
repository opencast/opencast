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

package org.opencastproject.userdirectory.brightspace.client;

import org.opencastproject.userdirectory.brightspace.client.api.BrightspaceUser;
import org.opencastproject.userdirectory.brightspace.client.api.OrgUnitResponse;
import org.opencastproject.userdirectory.brightspace.client.api.PagingInfo;
import org.opencastproject.userdirectory.brightspace.client.api.UsersResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import be.ugent.brightspace.idkeyauth.AuthenticationSecurityFactory;
import be.ugent.brightspace.idkeyauth.ID2LAppContext;
import be.ugent.brightspace.idkeyauth.ID2LUserContext;

public class BrightspaceClientImpl implements BrightspaceClient {

  private static final Logger logger = LoggerFactory.getLogger(BrightspaceClientImpl.class);

  private static final String COURSE_ID = "Course Offering";
  private static final String GET_USER_BY_USERNAME = "/d2l/api/lp/1.0/users/?orgDefinedId=";
  private static final String GET_ALL_USERS = "/d2l/api/lp/1.0/users/";
  private static final String GET_COURSES_BY_BRIGHTSPACE_USER_ID = "/d2l/api/lp/1.0/enrollments/users/{brightspace-userid}/orgUnits/";
  private static final String UNEXPECTED_JSON_RESPONSE = "The brightspace API returned a unexpected json response";
  private static final String SUPER_ADMIN = "Super Administrator";


  private final String url;
  private final String applicationId;
  private final String applicationKey;
  private final String systemUserId;
  private final String systemUserKey;
  private final ID2LUserContext userContext;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public BrightspaceClientImpl(String url, String applicationId, String applicationKey, String systemUserId,
          String systemUserKey) {
    this.url = url;
    this.applicationId = applicationId;
    this.applicationKey = applicationKey;
    this.systemUserId = systemUserId;
    this.systemUserKey = systemUserKey;
    userContext = createUserContext();
  }

  public BrightspaceUser findUser(String userName) throws BrightspaceClientException {
    String request = GET_USER_BY_USERNAME + userName;
    String response = httpGetRequest(request);

    try {
      List<BrightspaceUser> brightspaceUserList = objectMapper
              .readValue(response, new TypeReference<List<BrightspaceUser>>() {
              });
      return brightspaceUserList.stream().findFirst().orElse(null);
    } catch (IOException e) {
      throw new BrightspaceClientException(UNEXPECTED_JSON_RESPONSE, e);
    }
  }

  @Override
  public Set<String> findCourseIds(String brightspaceUserId) throws BrightspaceClientException {
    Set<String> courseIds = new HashSet<>();
    boolean hasMoreItems;
    String bookmark = null;
    do {
      String request = composePagedUrl(bookmark, brightspaceUserId);
      OrgUnitResponse orgUnitPage = findOrgUnitPage(request);
      if (SUPER_ADMIN.equals(orgUnitPage.getItems().get(0).getRole().getName())) {
        courseIds.add("BRIGHTSPACE_ADMIN");
        return courseIds;
      }

      Set<String> pagedCourseIds = orgUnitPage.getItems()
              .stream()
              .map(orgUnitItem -> orgUnitItem.getOrgUnit().getCode())
              .collect(Collectors.toSet());
      courseIds.addAll(pagedCourseIds);

      PagingInfo pagingInfo = orgUnitPage.getPagingInfo();
      hasMoreItems = pagingInfo.hasMoreItems();
      bookmark = pagingInfo.getBookmark();

    } while (hasMoreItems);

    return courseIds;
  }

  @Override
  public List<BrightspaceUser> findAllUsers() throws BrightspaceClientException {
    String response = httpGetRequest(GET_ALL_USERS);
    try {
      UsersResponse usersResponse = objectMapper.readValue(response, UsersResponse.class);
      return usersResponse.getItems();
    } catch (IOException e) {
      throw new BrightspaceClientException(UNEXPECTED_JSON_RESPONSE);
    }

  }

  public String getURL() {
    return this.url;
  }

  private String httpGetRequest(String request) throws BrightspaceClientException {
    URL url = createUrl(request);

    HttpsURLConnection urlConnection = (HttpsURLConnection) getURLConnection(url);
    try (InputStream inputStream = urlConnection.getInputStream()) {
      return readInputStream(inputStream);
    } catch (IOException io) {
      logger.error("error in brightspace data fetching", io);
      throw new BrightspaceClientException("could not read response");
    }
  }

  private URL createUrl(String request) throws BrightspaceClientException {
    URI uri = userContext.createAuthenticatedUri(request, "GET");
    URL url;
    try {
      url = uri.toURL();
    } catch (MalformedURLException mue) {
      throw new BrightspaceClientException("url was malformed", mue);
    }
    logger.debug("about to make GET request to : {}", uri);
    return url;
  }

  private ID2LUserContext createUserContext() {
    ID2LAppContext securityContext = AuthenticationSecurityFactory
            .createSecurityContext(applicationId, applicationKey, url);
    return securityContext.createUserContext(systemUserId, systemUserKey);
  }

  private URLConnection getURLConnection(URL url) throws BrightspaceClientException {
    try {
      HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
      urlConnection.setRequestMethod("GET");
      urlConnection.setRequestProperty("Content-Type", "application/json");
      urlConnection.setRequestProperty("Accept-Charset", "utf-8");
      urlConnection.setDoInput(true);
      urlConnection.setDoOutput(true);
      return urlConnection;
    } catch (IOException ioe) {
      throw new BrightspaceClientException("Brightspace api unreachable", ioe);
    }
  }

  private String readInputStream(InputStream inputStream) throws BrightspaceClientException {
    StringBuilder content = new StringBuilder();
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      String inputline;
      while ((inputline = bufferedReader.readLine()) != null) {
        content.append(inputline);
      }
      logger.debug("call to brightspace api: {}", content);
      return content.toString();
    } catch (IOException io) {
      throw new BrightspaceClientException("Could not read response", io);
    }

  }

  private OrgUnitResponse findOrgUnitPage(String request) throws BrightspaceClientException {
    String response = httpGetRequest(request);
    try {
      return objectMapper.readValue(response, OrgUnitResponse.class);
    } catch (IOException e) {
      logger.error(UNEXPECTED_JSON_RESPONSE);
      throw new BrightspaceClientException(UNEXPECTED_JSON_RESPONSE, e);
    }
  }

  private String composePagedUrl(String bookmark, String brightspaceUserId) {
    String request = GET_COURSES_BY_BRIGHTSPACE_USER_ID.replaceAll("\\{\\S+}", brightspaceUserId);
    if (bookmark != null) {
      request += "?bookmark=" + bookmark + "&orgUnitTypeId=3";
    }
    return request;
  }
}
