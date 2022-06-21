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
import org.opencastproject.userdirectory.brightspace.client.api.OrgUnitItem;
import org.opencastproject.userdirectory.brightspace.client.api.OrgUnitResponse;
import org.opencastproject.userdirectory.brightspace.client.api.PagingInfo;
import org.opencastproject.userdirectory.brightspace.client.api.UsersResponse;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import be.ugent.brightspace.idkeyauth.AuthenticationSecurityFactory;
import be.ugent.brightspace.idkeyauth.ID2LAppContext;
import be.ugent.brightspace.idkeyauth.ID2LUserContext;

public class BrightspaceClientImpl implements BrightspaceClient {

  private static final Logger logger = LoggerFactory.getLogger(BrightspaceClientImpl.class);

  private static final String GET_USER_BY_USERNAME = "/d2l/api/lp/1.31/users/?UserName=";
  private static final String GET_ALL_USERS = "/d2l/api/lp/1.31/users/";
  private static final String GET_COURSES_BY_BRIGHTSPACE_USER_ID
      = "/d2l/api/lp/1.31/enrollments/users/{brightspace-userid}/orgUnits/?orgUnitTypeId=3";
  private static final String UNEXPECTED_JSON_RESPONSE = "The brightspace API returned a unexpected json response";
  private static final String SUPER_ADMIN = "Super Administrator";
  private static final String LTI_LEARNER_ROLE = "Learner";
  private static final String LTI_INSTRUCTOR_ROLE = "Instructor";

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

    try {
      String response = httpGetRequest(request);
      logger.debug(response);

      BrightspaceUser brightspaceUser = objectMapper
              .readerFor(BrightspaceUser.class)
              .readValue(response);
      return brightspaceUser;
    } catch (BrightspaceNotFoundException nfe) {
      return null;
    } catch (IOException e) {
      logger.debug(e.toString());
      throw new BrightspaceClientException(UNEXPECTED_JSON_RESPONSE, e);
    }
  }

  @Override public List<String> getRolesFromBrightspace(String userid, Set<String> instructorRoles)
                            throws BrightspaceClientException {
    logger.debug("Retrieving subscribed courses for user: {}", userid);

    boolean hasMoreItems;
    String bookmark = null;
    List<String> roleList = new ArrayList<>();
    try {
      do {
        String request = composePagedUrl(bookmark, userid);
        OrgUnitResponse orgUnitPage = findOrgUnitPage(request);

        if (!orgUnitPage.getItems().isEmpty() && SUPER_ADMIN.equals(orgUnitPage.getItems().get(0).getRole().getName())) {
          roleList.add("BRIGHTSPACE_ADMIN");
          return roleList;
        }

        for (OrgUnitItem course: orgUnitPage.getItems()) {
          String brightspaceRole = course.getRole().getName();
          String ltiRole = instructorRoles.contains(brightspaceRole) ? LTI_INSTRUCTOR_ROLE : LTI_LEARNER_ROLE;
          String opencastRole = String.format("%s_%s", course.getOrgUnit().getId(), ltiRole);
          roleList.add(opencastRole);
        }

        PagingInfo pagingInfo = orgUnitPage.getPagingInfo();
        hasMoreItems = pagingInfo.hasMoreItems();
        bookmark = pagingInfo.getBookmark();

      } while (hasMoreItems);

      return roleList;
    } catch (BrightspaceClientException e) {
      logger.warn("Exception getting site/role membership for brightspace user {}: {}", userid, e);
      throw new BrightspaceClientException(UNEXPECTED_JSON_RESPONSE, e);
    }

  }

  @Override
  public List<BrightspaceUser> findAllUsers() throws BrightspaceClientException {
    try {
      String response = httpGetRequest(GET_ALL_USERS);
      UsersResponse usersResponse = objectMapper.readValue(response, UsersResponse.class);
      return usersResponse.getItems();
    } catch (IOException e) {
      throw new BrightspaceClientException(UNEXPECTED_JSON_RESPONSE, e);
    } catch (BrightspaceNotFoundException nfe) {
      throw new BrightspaceClientException(UNEXPECTED_JSON_RESPONSE, nfe);
    }

  }

  public String getURL() {
    return this.url;
  }

  private String httpGetRequest(String request) throws BrightspaceClientException, BrightspaceNotFoundException {
    URL url = createUrl(request);

    try {
      HttpsURLConnection urlConnection = (HttpsURLConnection) getURLConnection(url);

      if (urlConnection.getResponseCode() == 404) {
        logger.debug("Not found, 404 response");
        throw new BrightspaceNotFoundException("not found");
      }

      InputStream inputStream = urlConnection.getInputStream();
      return readInputStream(inputStream);
    } catch (IOException io) {
      logger.warn("error in brightspace data fetching", io);
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
    try {
      String response = httpGetRequest(request);
      return objectMapper.readValue(response, OrgUnitResponse.class);
    } catch (IOException e) {
      logger.error(UNEXPECTED_JSON_RESPONSE);
      throw new BrightspaceClientException(UNEXPECTED_JSON_RESPONSE, e);
    } catch (BrightspaceNotFoundException nfe) {
      logger.error(UNEXPECTED_JSON_RESPONSE);
      throw new BrightspaceClientException(UNEXPECTED_JSON_RESPONSE, nfe);
    }
  }

  private String composePagedUrl(String bookmark, String brightspaceUserId) {
    String request = GET_COURSES_BY_BRIGHTSPACE_USER_ID.replaceAll("\\{\\S+}", brightspaceUserId);
    if (bookmark != null) {
      request += "&bookmark=" + bookmark;
    }
    return request;
  }
}
