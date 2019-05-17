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

import org.opencastproject.security.api.CachingUserProviderMXBean;
import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.userdirectory.moodle.MoodleWebService.CoreUserGetUserByFieldFilters;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.PatternSyntaxException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A UserProvider that reads user roles from Moodle.
 */
public class MoodleUserProviderInstance implements UserProvider, RoleProvider, CachingUserProviderMXBean {
  /**
   * User and role provider name.
   */
  private static final String PROVIDER_NAME = "moodle";

  /**
   * The logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(MoodleUserProviderInstance.class);

  /**
   * Suffix for Moodle roles with the learner capability.
   */
  private static final String LEARNER_ROLE_SUFFIX = "Learner";

  /**
   * Suffix for Moodle roles with the instructor capability.
   */
  private static final String INSTRUCTOR_ROLE_SUFFIX = "Instructor";

  /**
   * Prefix for Moodle group roles.
   */
  private static final String GROUP_ROLE_PREFIX = "G";

  /**
   * Suffix for Moodle group roles.
   */
  private static final String GROUP_ROLE_SUFFIX = "Learner";

  /**
   * The Moodle web service client.
   */
  private MoodleWebService client;

  /**
   * The organization.
   */
  private Organization organization;

  /**
   * Whether to create group roles.
   */
  private boolean groupRoles;

  /**
   * Regular expression for matching valid courses.
   */
  private String coursePattern;

  /**
   * Regular expression for matching valid users.
   */
  private String userPattern;

  /**
   * Regular expression for matching valid groups.
   */
  private String groupPattern;

  /**
   * A cache of users, which lightens the load on Moodle.
   */
  private LoadingCache<String, Object> cache;

  /**
   * A token to store in the miss cache.
   */
  private Object nullToken = new Object();

  /**
   * The total number of requests made to load users.
   */
  private AtomicLong loadUserRequests;

  /**
   * The number of requests made to Moodle.
   */
  private AtomicLong moodleWebServiceRequests;

  private final List<String> ignoredUsernames;

  /**
   * Constructs an Moodle user provider with the needed settings.
   *
   * @param pid             The pid of this service.
   * @param client          The Moodle web service client.
   * @param organization    The organization.
   * @param coursePattern   The pattern of a Moodle course ID.
   * @param userPattern     The pattern of a Moodle user ID.
   * @param groupPattern    The pattern of a Moodle group ID.
   * @param groupRoles      Whether to activate groupRoles
   * @param cacheSize       The number of users to cache.
   * @param cacheExpiration The number of minutes to cache users.
   * @param adminUserName   Name of the global admin user.
   */
  public MoodleUserProviderInstance(String pid, MoodleWebService client, Organization organization,
          String coursePattern, String userPattern, String groupPattern, boolean groupRoles, int cacheSize,
          int cacheExpiration, String adminUserName) {
    this.client = client;
    this.organization = organization;
    this.groupRoles = groupRoles;
    this.coursePattern = coursePattern;
    this.userPattern = userPattern;
    this.groupPattern = groupPattern;

    // initialize user filter
    this.ignoredUsernames = new ArrayList<>();
    this.ignoredUsernames.add("");
    this.ignoredUsernames.add(SecurityConstants.GLOBAL_ANONYMOUS_USERNAME);
    if (StringUtils.isNoneEmpty(adminUserName)) {
      ignoredUsernames.add(adminUserName);
    }

    logger.info("Creating new MoodleUserProviderInstance(pid={}, url={}, cacheSize={}, cacheExpiration={})", pid,
            client.getURL(), cacheSize, cacheExpiration);

    // Setup the caches
    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Object>() {
              @Override
              public Object load(String username) {
                User user = loadUserFromMoodle(username);
                return user == null ? nullToken : user;
              }
            });

    registerMBean(pid);
  }

  ////////////////////////////
  // CachingUserProviderMXBean

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.CachingUserProviderMXBean#getCacheHitRatio()
   */
  @Override
  public float getCacheHitRatio() {
    if (loadUserRequests.get() == 0)
      return 0;
    return (float) (loadUserRequests.get() - moodleWebServiceRequests.get()) / loadUserRequests.get();
  }

  /**
   * Registers an MXBean.
   */
  private void registerMBean(String pid) {
    // register with jmx
    loadUserRequests = new AtomicLong();
    moodleWebServiceRequests = new AtomicLong();
    try {
      ObjectName name;
      name = MoodleUserProviderFactory.getObjectName(pid);
      Object mbean = this;
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      try {
        mbs.unregisterMBean(name);
      } catch (InstanceNotFoundException e) {
        logger.debug("{} was not registered", name);
      }
      mbs.registerMBean(mbean, name);
    } catch (Exception e) {
      logger.error("Unable to register {} as an mbean: {}", this, e);
    }
  }

  ///////////////////////
  // UserProvider methods

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#getName()
   */
  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#getUsers()
   */
  @Override
  public Iterator<User> getUsers() {
    // We never enumerate all users
    return Collections.emptyIterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    loadUserRequests.incrementAndGet();
    try {
      Object user = cache.getUnchecked(userName);
      if (user == nullToken) {
        logger.debug("Returning null user from cache");
        return null;
      } else {
        logger.debug("Returning user {} from cache", userName);
        return (User) user;
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
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#countUsers()
   */
  @Override
  public long countUsers() {
    // Not meaningful, as we never enumerate users
    return 0;
  }

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
   * @see org.opencastproject.security.api.UserProvider#findUsers(java.lang.String, int, int)
   */
  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");

    if (query.endsWith("%"))
      query = query.substring(0, query.length() - 1);

    if (query.isEmpty())
      return Collections.emptyIterator();

    // Check if user matches pattern
    try {
      if ((userPattern != null) && !query.matches(userPattern)) {
        logger.debug("verify user {} failed regexp {}", query, userPattern);
        return Collections.emptyIterator();
      }
    } catch (PatternSyntaxException e) {
      logger.warn("Invalid regular expression for user pattern {} - disabling checks", userPattern);
      userPattern = null;
    }

    // Load User
    List<User> users = new LinkedList<>();

    User user = loadUser(query);
    if (user != null)
      users.add(user);

    return users.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#invalidate(java.lang.String)
   */
  @Override
  public void invalidate(String userName) {
    cache.invalidate(userName);
  }

  ///////////////////////
  // RoleProvider methods

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    // We won't ever enumerate all Moodle courses, so return an empty list here
    return Collections.emptyIterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(java.lang.String)
   */
  @Override
  public List<Role> getRolesForUser(String username) {
    List<Role> roles = new LinkedList<>();

    // Don't answer for admin, anonymous or empty user
    if (ignoredUsernames.stream().anyMatch(u -> u.equals(username))) {
      logger.debug("we don't answer for: {}", username);
      return roles;
    }

    User user = loadUser(username);
    if (user != null) {
      logger.debug("Returning cached role set for {}", username);
      return new ArrayList<>(user.getRoles());
    }

    // Not found
    logger.debug("Return empty role set for {} - not found in Moodle", username);
    return new LinkedList<>();
  }

  /**
   * {@inheritDoc}
   * <p>
   * We search for COURSEID, COURSEID_Learner, COURSEID_Instructor
   *
   * @see org.opencastproject.security.api.RoleProvider#findRoles(java.lang.String, org.opencastproject.security.api.Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    // Don't return roles for users or groups
    if (target == Role.Target.USER)
      return Collections.emptyIterator();

    boolean exact = true;
    boolean ltirole = false;

    if (query.endsWith("%")) {
      exact = false;
      query = query.substring(0, query.length() - 1);
    }

    if (query.isEmpty())
      return Collections.emptyIterator();

    // Verify that role name ends with LEARNER_ROLE_SUFFIX or INSTRUCTOR_ROLE_SUFFIX
    if (exact
            && !query.endsWith("_" + LEARNER_ROLE_SUFFIX)
            && !query.endsWith("_" + INSTRUCTOR_ROLE_SUFFIX)
            && !query.endsWith("_" + GROUP_ROLE_SUFFIX))
      return Collections.emptyIterator();

    boolean findGroupRole = groupRoles && query.startsWith(GROUP_ROLE_PREFIX);

    // Extract Moodle id
    String moodleId = findGroupRole ? query.substring(GROUP_ROLE_PREFIX.length()) : query;
    if (query.endsWith("_" + LEARNER_ROLE_SUFFIX)) {
      moodleId = query.substring(0, query.lastIndexOf("_" + LEARNER_ROLE_SUFFIX));
      ltirole = true;
    } else if (query.endsWith("_" + INSTRUCTOR_ROLE_SUFFIX)) {
      moodleId = query.substring(0, query.lastIndexOf("_" + INSTRUCTOR_ROLE_SUFFIX));
      ltirole = true;
    } else if (query.endsWith("_" + GROUP_ROLE_SUFFIX)) {
      moodleId = query.substring(0, query.lastIndexOf("_" + GROUP_ROLE_SUFFIX));
      ltirole = true;
    }

    // Check if matches patterns
    String pattern = findGroupRole ? groupPattern : coursePattern;
    try {
      if ((pattern != null) && !moodleId.matches(pattern)) {
        logger.debug("Verify Moodle ID {} failed regexp {}", moodleId, pattern);
        return Collections.emptyIterator();
      }
    } catch (PatternSyntaxException e) {
      logger.warn("Invalid regular expression for pattern {} - disabling checks", pattern);
      if (findGroupRole) {
        groupPattern = null;
      } else {
        coursePattern = null;
      }
    }

    // Roles list
    List<Role> roles = new LinkedList<>();
    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
    if (ltirole) {
      // Query is for a Moodle ID and an LTI role (Instructor/Learner/Group)
      roles.add(new JaxbRole(query, jaxbOrganization, "Moodle Site Role", Role.Type.EXTERNAL));
    } else if (findGroupRole) {
      // Group ID
      roles.add(new JaxbRole(GROUP_ROLE_PREFIX + moodleId + "_" + GROUP_ROLE_SUFFIX, jaxbOrganization,
              "Moodle Group Learner Role", Role.Type.EXTERNAL));
    } else {
      // Course ID - return both roles
      roles.add(new JaxbRole(moodleId + "_" + INSTRUCTOR_ROLE_SUFFIX, jaxbOrganization,
              "Moodle Course Instructor Role", Role.Type.EXTERNAL));
      roles.add(new JaxbRole(moodleId + "_" + LEARNER_ROLE_SUFFIX, jaxbOrganization, "Moodle Course Learner Role",
              Role.Type.EXTERNAL));
    }

    return roles.iterator();
  }

  /////////////////
  // Helper methods

  /**
   * Loads a user from Moodle.
   *
   * @param username The username.
   * @return The user.
   */
  private User loadUserFromMoodle(String username) {
    logger.debug("loadUserFromMoodle({})", username);

    if (cache == null)
      throw new IllegalStateException("The Moodle user detail service has not yet been configured");

    // Don't answer for admin, anonymous or empty user
    if (ignoredUsernames.stream().anyMatch(u -> u.equals(username))) {
      logger.debug("We don't answer for: " + username);
      return null;
    }

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

    // update cache statistics
    moodleWebServiceRequests.incrementAndGet();

    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassloader = currentThread.getContextClassLoader();

    try {
      // Load user
      List<MoodleUser> moodleUsers = client
              .coreUserGetUsersByField(CoreUserGetUserByFieldFilters.username, Collections.singletonList(username));

      if (moodleUsers.isEmpty()) {
        logger.debug("User {} not found in Moodle system", username);
        return null;
      }
      MoodleUser moodleUser = moodleUsers.get(0);

      // Load Roles
      List<String> courseIdsInstructor = client.toolOpencastGetCoursesForInstructor(username);
      List<String> courseIdsLearner = client.toolOpencastGetCoursesForLearner(username);
      List<String> groupIdsLearner = groupRoles ? client.toolOpencastGetGroupsForLearner(username) : Collections.emptyList();

      // Create Opencast Objects
      Set<JaxbRole> roles = new HashSet<>();
      roles.add(new JaxbRole(Group.ROLE_PREFIX + "MOODLE", jaxbOrganization, "Moodle Users", Role.Type.EXTERNAL_GROUP));
      for (String courseId : courseIdsInstructor) {
        roles.add(new JaxbRole(courseId + "_" + INSTRUCTOR_ROLE_SUFFIX, jaxbOrganization, "Moodle Course Instructor Role",
                Role.Type.EXTERNAL));
      }
      for (String courseId : courseIdsLearner) {
        roles.add(new JaxbRole(courseId + "_" + LEARNER_ROLE_SUFFIX, jaxbOrganization, "Moodle Course Learner Role",
                Role.Type.EXTERNAL));
      }
      for (String groupId : groupIdsLearner) {
        roles.add(new JaxbRole(GROUP_ROLE_PREFIX + groupId + "_" + GROUP_ROLE_SUFFIX, jaxbOrganization,
                "Moodle Group Learner Role", Role.Type.EXTERNAL));
      }

      return new JaxbUser(moodleUser.getUsername(), null, moodleUser.getFullname(), moodleUser.getEmail(),
              this.getName(), true, jaxbOrganization, roles);
    } catch (Exception e) {
      logger.warn("Exception loading Moodle user {} at {}: {}", username, client.getURL(), e.getMessage());
    } finally {
      currentThread.setContextClassLoader(originalClassloader);
    }

    return null;
  }
}
