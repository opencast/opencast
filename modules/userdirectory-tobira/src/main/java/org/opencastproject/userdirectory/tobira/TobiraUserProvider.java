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

package org.opencastproject.userdirectory.tobira;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.userdirectory.UserIdRoleProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A user provider that retrieves user information from Tobira.
 *
 * <p>This enables Opencast to resolve user information for users that are known to Tobira
 * but not to Opencast.
 * Opencast can use this provider to look up the user display names, emails, and user roles.</p>
 *
 * <p>Communication happens via Tobira's GraphQL API using the trusted external key for authentication.</p>
 */
@Component(
    configurationPid = "org.opencastproject.userdirectory.tobira",
    property = {
        "service.description=Provides user information from Tobira"
    },
    immediate = true,
    service = { UserProvider.class, RoleProvider.class }
)
public class TobiraUserProvider implements UserProvider, RoleProvider {

  private static final Logger logger = LoggerFactory.getLogger(TobiraUserProvider.class);

  private static final String PROVIDER_NAME = "tobira";
  private static final int DEFAULT_SEARCH_LIMIT = 1000;
  private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 10;

  // Configuration keys
  private static final String ORGANIZATION_KEY = "org.opencastproject.userdirectory.tobira.org";
  private static final String URL_KEY = "org.opencastproject.userdirectory.tobira.url";
  private static final String TRUSTED_KEY_KEY = "org.opencastproject.userdirectory.tobira.trustedKey";
  private static final String CACHE_SIZE_KEY = "org.opencastproject.userdirectory.tobira.cache.size";
  private static final String CACHE_EXPIRATION_KEY = "org.opencastproject.userdirectory.tobira.cache.expiration";

  // Configuration values
  private Organization organization;
  private URI endpoint;
  private String trustedKey;
  private boolean configured = false;

  // Cache
  private LoadingCache<String, Object> cache;
  private final Object nullToken = new Object();

  private static final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(DEFAULT_REQUEST_TIMEOUT_SECONDS))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  private OrganizationDirectoryService orgDirectory;

  @Reference
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  @Activate
  public void activate(ComponentContext cc) {
    String urlStr = OsgiUtil.getComponentContextProperty(cc, URL_KEY, "");
    if (StringUtils.isBlank(urlStr)) {
      logger.debug("Tobira User Provider not configured (no URL set)");
      return;
    }

    try {
      String normalizedUrl = urlStr.endsWith("/") ? urlStr.substring(0, urlStr.length() - 1) : urlStr;
      endpoint = new URI(normalizedUrl + "/graphql");
    } catch (URISyntaxException e) {
      logger.error("Invalid Tobira URL: {}", urlStr, e);
      return;
    }

    trustedKey = OsgiUtil.getComponentContextProperty(cc, TRUSTED_KEY_KEY, "");
    if (StringUtils.isBlank(trustedKey)) {
      logger.error("Tobira trusted key is not set, TobiraUserProvider will not be active");
      return;
    }

    String orgId = OsgiUtil.getComponentContextProperty(cc, ORGANIZATION_KEY, "mh_default_org");
    try {
      organization = orgDirectory.getOrganization(orgId);
    } catch (NotFoundException e) {
      logger.error("Organization {} not found", orgId);
      return;
    }

    int cacheSize = NumberUtils.toInt(
        OsgiUtil.getComponentContextProperty(cc, CACHE_SIZE_KEY, "500"), 500);
    int cacheExpiration = NumberUtils.toInt(
        OsgiUtil.getComponentContextProperty(cc, CACHE_EXPIRATION_KEY, "60"), 60);

    cache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Object>() {
          @Override
          public Object load(String username) {
            User user = loadUserFromTobira(username);
            return user == null ? nullToken : user;
          }
        });

    configured = true;
    logger.info("Activated TobiraUserProvider for {} with Tobira at {}", orgId, urlStr);
  }

  // -------------------------------------------------------------------------
  // UserProvider methods
  // -------------------------------------------------------------------------

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public Iterator<User> getUsers() {
    // Enumerating all users is not supported; use findUsers() for paginated search instead.
    return Collections.emptyIterator();
  }

  @Override
  public User loadUser(String userName) {
    if (!configured || userName == null) {
      return null;
    }
    try {
      Object user = cache.getUnchecked(userName);
      if (user == nullToken) {
        return null;
      }
      logger.debug("Returning user {} from Tobira cache", userName);
      return (User) user;
    } catch (ExecutionError | UncheckedExecutionException e) {
      logger.warn("Exception while loading user {} from Tobira", userName, e);
      return null;
    }
  }

  @Override
  public long countUsers() {
    // Not meaningful, as we never enumerate users
    return 0;
  }

  @Override
  public String getOrganization() {
    return configured ? organization.getId() : "";
  }

  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }
    if (!configured) {
      return Collections.emptyIterator();
    }

    if ("%".equals(query)) {
      query = "";
    }
    if (query.endsWith("%")) {
      query = query.substring(0, query.length() - 1);
    }
    if (query.isEmpty()) {
      // null query means "no filter" in Tobira: return all users, paginated.
      return findUsersFromTobira(null, offset, effectiveSearchLimit(limit)).iterator();
    }

    // Try exact match via cache first (only valid when offset is 0, as it yields at most one result).
    if (offset == 0) {
      User user = loadUser(query);
      if (user != null) {
        return Collections.singletonList(user).iterator();
      }
    }

    // Partial match: query Tobira
    try {
      List<User> users = findUsersFromTobira(query, offset, limit);
      return users.iterator();
    } catch (Exception e) {
      logger.warn("Error searching users in Tobira for query '{}'", query, e);
      return Collections.emptyIterator();
    }
  }

  @Override
  public void invalidate(String userName) {
    if (configured && cache != null) {
      cache.invalidate(userName);
    }
  }

  // -------------------------------------------------------------------------
  // RoleProvider methods
  // -------------------------------------------------------------------------

  @Override
  public List<Role> getRolesForUser(String userName) {
    if (!configured) {
      return Collections.emptyList();
    }

    User user = loadUser(userName);
    if (user != null) {
      return new ArrayList<>(user.getRoles());
    }
    return Collections.emptyList();
  }

  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    // User-id roles for Tobira users are already produced by UserIdRoleProvider.findRoles,
    // which iterates users returned by the findUsers implementation. Returning roles here too
    // would result in duplicate entries in the ACL role picker.
    return Collections.emptyIterator();
  }

  // -------------------------------------------------------------------------
  // Tobira communication
  // -------------------------------------------------------------------------

  /**
   * Loads a single user from Tobira. Tries an exact username lookup first, then falls back to a
   * case-insensitive search by username or by the expected user-id role.
   */
  private User loadUserFromTobira(String username) {
    logger.debug("Loading user {} from Tobira", username);

    try {
      JSONObject data = graphqlRequest(
          "query($u: String!) { knownUserByUsername(username: $u) "
              + "{ username displayName email userRole } }",
          Map.<String, Object>of("u", username));

      JSONObject userData = (JSONObject) data.get("knownUserByUsername");
      if (userData != null) {
        return createUser(userData);
      }

      // Fallback: try to resolve via search when the username is not an exact match
      // (e.g. sanitized role payloads or different case).
      return findUserByRoleOrSearch(username);
    } catch (Exception e) {
      logger.warn("Error loading user {} from Tobira at {}", username, endpoint, e);
      return null;
    }
  }

  /**
   * Attempts to resolve a user from Tobira when the exact username lookup failed. Searches the
   * Tobira known-users index for the given input and matches against username or the user's
   * generated Opencast user-id role.
   */
  private User findUserByRoleOrSearch(String query) {
    String expectedRole = UserIdRoleProvider.getUserIdRole(query);
    logger.debug("Fallback user lookup: query='{}', transformed expected Opencast role='{}'",
        query, expectedRole);
    for (User user : findUsersFromTobira(query, 0, 10)) {
      if (user.getUsername().equalsIgnoreCase(query)) {
        logger.debug("Fallback lookup matched by username: query='{}', resolvedUser='{}'",
            query, user.getUsername());
        return user;
      }
      for (Role role : user.getRoles()) {
        if (role.getName().equalsIgnoreCase(expectedRole)) {
          logger.debug("Fallback lookup matched by role: query='{}', expectedRole='{}', resolvedUser='{}'",
              query, expectedRole, user.getUsername());
          return user;
        }
      }
    }
    logger.debug("Fallback lookup found no user match for query='{}' and expectedRole='{}'",
        query, expectedRole);
    return null;
  }

  private int effectiveSearchLimit(int limit) {
    return limit > 0 ? limit : DEFAULT_SEARCH_LIMIT;
  }

  /**
   * Searches for users in Tobira by partial match. Pass {@code null} for {@code query} to return
   * all users (no filter). Results are ordered by username; use {@code offset} and {@code limit}
   * for stable pagination.
   */
  private List<User> findUsersFromTobira(String query, int offset, int limit) {
    logger.debug("Searching users in Tobira for '{}' (offset={}, limit={})", query, offset, limit);

    try {
      // query is nullable in Tobira's schema: null means "no filter, return all users".
      Map<String, Object> variables = new HashMap<>();
      variables.put("q", query);  // may be null
      variables.put("l", Math.max(1, limit));
      variables.put("o", Math.max(0, offset));
      JSONObject data = graphqlRequest(
          "query($q: String, $l: Int!, $o: Int!) { findKnownUsers(query: $q, limit: $l, offset: $o) "
              + "{ username displayName email userRole } }",
          variables);

      JSONArray usersData = (JSONArray) data.get("findKnownUsers");
      if (usersData == null || usersData.isEmpty()) {
        return Collections.emptyList();
      }

      List<User> users = new ArrayList<>();
      for (Object obj : usersData) {
        JSONObject userData = (JSONObject) obj;
        User user = createUser(userData);
        if (user != null) {
          users.add(user);
          cache.put(user.getUsername(), user);
        }
      }
      return users;
    } catch (Exception e) {
      logger.warn("Error searching users in Tobira for query '{}'", query, e);
      return Collections.emptyList();
    }
  }

  /**
   * Creates an Opencast User object from Tobira GraphQL response data.
   */
  private User createUser(JSONObject userData) {
    String username = (String) userData.get("username");
    if (username == null) {
      return null;
    }
    String displayName = (String) userData.get("displayName");
    String email = (String) userData.get("email");
    String userRole = (String) userData.get("userRole");
    logger.debug("Creating Opencast user from Tobira data: username='{}', rawUserRole='{}'",
        username, userRole);
    if (StringUtils.isBlank(userRole)) {
      userRole = UserIdRoleProvider.getUserIdRole(username);
      logger.debug("Missing userRole from Tobira for user {}, using fallback role {}", username, userRole);
    }
    logger.debug("Final user role used for Opencast user '{}': '{}'", username, userRole);

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
    Set<JaxbRole> roles = new HashSet<>();
    roles.add(new JaxbRole(userRole, jaxbOrganization, "Tobira User Role", Role.Type.EXTERNAL));

    return new JaxbUser(username, null, displayName, email, PROVIDER_NAME, jaxbOrganization, roles);
  }

  /**
   * Sends a GraphQL request to Tobira and returns the data object from the response.
   */
  private JSONObject graphqlRequest(String query, Map<String, Object> variables)
          throws IOException, InterruptedException, ParseException {
    JSONObject queryObject = new JSONObject();
    queryObject.put("query", query);
    queryObject.put("variables", new JSONObject(variables));

    HttpRequest request = HttpRequest.newBuilder()
        .uri(endpoint)
        .timeout(Duration.ofSeconds(DEFAULT_REQUEST_TIMEOUT_SECONDS))
        .header("Content-Type", "application/json")
        .header("x-tobira-trusted-external-key", trustedKey)
        .POST(HttpRequest.BodyPublishers.ofString(queryObject.toJSONString()))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("Tobira returned HTTP " + response.statusCode());
    }

    JSONObject responseObject = (JSONObject) new JSONParser().parse(response.body());
    JSONArray errors = (JSONArray) responseObject.get("errors");
    if (errors != null && !errors.isEmpty()) {
      logger.warn("GraphQL errors from Tobira: {}", errors);
      throw new IOException("Tobira returned GraphQL errors");
    }

    return (JSONObject) responseObject.get("data");
  }
}
