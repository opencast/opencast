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

package org.opencastproject.userdirectory.studip;

import org.opencastproject.security.api.CachingUserProviderMXBean;
import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A UserProvider that reads user roles from Studip.
 */
public class StudipUserProviderInstance implements UserProvider, RoleProvider, CachingUserProviderMXBean {

  public static final String PROVIDER_NAME = "studip";

  private static final String OC_USERAGENT = "Opencast";
  private static final String STUDIP_GROUP = Group.ROLE_PREFIX + "STUDIP";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(StudipUserProviderInstance.class);

  /** The organization */
  private Organization organization = null;

  /** Total number of requests made to load users */
  private AtomicLong requests = null;

  /** The number of requests made to Studip */
  private AtomicLong studipLoads = null;

  /** A cache of users, which lightens the load on Studip */
  private LoadingCache<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /** The URL of the Studip instance */
  private String studipUrl = null;

  /** The URL of the Studip instance */
  private String studipToken = null;

  /**
   * Constructs an Studip user provider with the needed settings.
   *
   * @param pid
   *          the pid of this service
   * @param organization
   *          the organization
   * @param url
   *          the url of the Studip server
   * @param token
   *          the token to authenticate with
   * @param cacheSize
   *          the number of users to cache
   * @param cacheExpiration
   *          the number of minutes to cache users
   */
  public StudipUserProviderInstance(
      String pid,
      Organization organization,
      String url,
      String token,

      int cacheSize,
      int cacheExpiration
  ) {

    this.organization = organization;
    this.studipUrl = url;
    this.studipToken = token;

    logger.info("Creating new StudipUserProviderInstance(pid={}, url={}, cacheSize={}, cacheExpiration={})",
                 pid, url, cacheSize, cacheExpiration);

    // Setup the caches
    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Object>() {
          @Override
          public Object load(String id) throws Exception {
            User user = loadUserFromStudip(id);
            return user == null ? nullToken : user;
          }
        });

    registerMBean(pid);
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  /**
   * Registers an MXBean.
   */
  protected void registerMBean(String pid) {
    // register with jmx
    requests = new AtomicLong();
    studipLoads = new AtomicLong();
    try {
      ObjectName name;
      name = StudipUserProviderFactory.getObjectName(pid);
      Object mbean = this;
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      try {
        mbs.unregisterMBean(name);
      } catch (InstanceNotFoundException e) {
        logger.debug("{} was not registered", name);
      }
      mbs.registerMBean(mbean, name);
    } catch (Exception e) {
      logger.error("Unable to register {} as an mbean", this, e);
    }
  }

  // UserProvider methods

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return organization.getId();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    logger.debug("loaduser(" + userName + ")");

    requests.incrementAndGet();
    try {
      Object user = cache.getUnchecked(userName);
      if (user == nullToken) {
        logger.debug("Returning null user from cache");
        return null;
      } else {
        logger.debug("Returning user " + userName + " from cache");
        return (JaxbUser) user;
      }
    } catch (ExecutionError e) {
      logger.warn("Exception while loading user {}", userName, e);
      return null;
    } catch (UncheckedExecutionException e) {
      logger.warn("Exception while loading user {}", userName, e);
      return null;
    }
  }

  /**
   * Loads a user from Studip.
   * 
   * @param userName
   *          the username
   * @return the user
   */
  protected User loadUserFromStudip(String userName) {
    if (cache == null) {
      throw new IllegalStateException("The Studip user detail service has not yet been configured");
    }

    // Don't answer for admin, anonymous or empty user
    if ("admin".equals(userName) || "".equals(userName) || "anonymous".equals(userName)) {
      cache.put(userName, nullToken);
      logger.debug("we don't answer for: " + userName);
      return null;
    }

    logger.debug("In loadUserFromStudip, currently processing user : {}", userName);

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

    // update cache statistics
    studipLoads.incrementAndGet();

    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassloader = currentThread.getContextClassLoader();
    try {
      // Stud.IP userId (internal id), email address and display name
      JSONObject userJsonObj = getStudipUser(userName);
      if (userJsonObj == null) {
        return null;
      }

      Set<JaxbRole> roles = new HashSet<>();
      if (userJsonObj.containsKey("roles")) {
        JSONArray rolesArray = (JSONArray) userJsonObj.get("roles");
        for (Object r : rolesArray) {
          roles.add(new JaxbRole(r.toString(), jaxbOrganization, "Studip external role", Role.Type.EXTERNAL));
        }
      }

      // Group role for all Stud.IP users
      roles.add(new JaxbRole(STUDIP_GROUP, jaxbOrganization, "Studip Users", Role.Type.EXTERNAL_GROUP));
      logger.debug("Returning JaxbRoles: " + roles);

      // Email address
      var email = Objects.toString(userJsonObj.get("email"), null);
      var name = Objects.toString(userJsonObj.get("fullname"), null);

      User user = new JaxbUser(userName, null, name, email, PROVIDER_NAME, jaxbOrganization, roles);

      cache.put(userName, user);
      logger.debug("Returning user {}", userName);

      return user;

    } catch (ParseException e) {
      logger.error("Exception while parsing response from provider for user {}", userName, e);
      return null;
    } catch (IOException e) {
      logger.error(e.getMessage());
      return null;
    } catch (URISyntaxException e) {
      logger.error("Misspelled URI", e);
      return null;
    } finally {
      currentThread.setContextClassLoader(originalClassloader);
    }
  }

  /**
   * Get the internal Stud.IP user Id for the supplied user. If the user exists, set the user's email address.
   * 
   * @param uid Identifier of the user to look for
   * @return JSON object containing user information
   */
  private JSONObject getStudipUser(String uid) throws URISyntaxException, IOException, ParseException {
    // Build URL
    URIBuilder url = new URIBuilder(studipUrl + "opencast/user/" + uid);
    url.addParameter("token", studipToken);

    // Execute request
    HttpGet get = new HttpGet(url.build());
    get.setHeader("User-Agent", OC_USERAGENT);

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      try (CloseableHttpResponse resp = client.execute(get)) {
        var statusCode = resp.getStatusLine().getStatusCode();
        if (statusCode == 404) {
          // Stud.IP does not know about the user
          return null;
        } else if (statusCode / 100 != 2) {
          throw new IOException("HttpRequest unsuccessful, reason: " + resp.getStatusLine().getReasonPhrase());
        }

        // Parse response
        BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(reader);

        // Check for errors
        if (!(obj instanceof JSONObject)) {
          throw new IOException("StudIP responded in unexpected format");
        }

        JSONObject jObj = (JSONObject) obj;
        if (jObj.containsKey("errors")) {
          throw new IOException("Stud.IP returned an error: " + jObj.toJSONString());
        }

        return jObj;
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.CachingUserProviderMXBean#getCacheHitRatio()
   */
  @Override
  public float getCacheHitRatio() {
    if (requests.get() == 0) {
      return 0;
    }
    return (float) (requests.get() - studipLoads.get()) / requests.get();
  }

  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {

    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }

    if (query.endsWith("%")) {
      query = query.substring(0, query.length() - 1);
    }

    if (query.isEmpty()) {
      return Collections.emptyIterator();
    }

    List<User> users = new LinkedList<User>();
    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
    JaxbUser queryUser = new JaxbUser(query, PROVIDER_NAME, jaxbOrganization, new HashSet<JaxbRole>());
    users.add(queryUser);

    return users.iterator();
  }

  @Override
  public Iterator<User> getUsers() {
    // We never enumerate all users
    return Collections.emptyIterator();
  }

  @Override
  public void invalidate(String userName) {
    cache.invalidate(userName);
  }

  @Override
  public long countUsers() {
    // Not meaningful, as we never enumerate users
    return 0;
  }

  // RoleProvider methods

  @Override
  public List<Role> getRolesForUser(String userName) {

    List<Role> roles = new LinkedList<Role>();

    // Don't answer for admin, anonymous or empty user
    if ("admin".equals(userName) || "".equals(userName) || "anonymous".equals(userName)) {
      logger.debug("we don't answer for: " + userName);
      return roles;
    }

    logger.debug("getRolesForUser(" + userName + ")");

    User user = loadUser(userName);
    if (user != null) {
      logger.debug("Returning cached roleset for {}", userName);
      return new ArrayList<Role>(user.getRoles());
    }

    // Not found
    logger.debug("Return empty roleset for {} - not found on Studip");
    return new LinkedList<Role>();
  }

  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    logger.debug("findRoles(query=" + query + " offset=" + offset + " limit=" + limit + ")");

    // Don't return roles for users or groups
    if (target == Role.Target.USER) {
      return Collections.emptyIterator();
    }

    if (query.endsWith("%")) {
      query = query.substring(0, query.length() - 1);
    }

    if (query.isEmpty()) {
      return Collections.emptyIterator();
    }

    // Roles list
    List<Role> roles = new LinkedList<Role>();

    return roles.iterator();
  }

}
