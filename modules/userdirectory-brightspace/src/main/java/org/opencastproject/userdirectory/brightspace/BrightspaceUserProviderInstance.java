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


package org.opencastproject.userdirectory.brightspace;

import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.Role.Target;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClient;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClientException;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClientImpl;
import org.opencastproject.userdirectory.brightspace.client.api.BrightspaceUser;
import org.opencastproject.util.NotFoundException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component(
    immediate = true,
    configurationPid = "org.opencastproject.userdirectory.brightspace",
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = { UserProvider.class, RoleProvider.class },
    property = {
        "service.description=Provides Brightspace user directory instances"
    }
)
public class BrightspaceUserProviderInstance implements UserProvider, RoleProvider {

  private static final Logger logger = LoggerFactory.getLogger(BrightspaceUserProviderInstance.class);

  private static final String LTI_LEARNER_ROLE = "Learner";
  private static final String LTI_INSTRUCTOR_ROLE = "Instructor";

  private static final String ORGANIZATION_KEY = "org.opencastproject.userdirectory.brightspace.org";
  private static final String BRIGHTSPACE_USER_ID = "org.opencastproject.userdirectory.brightspace.systemuser.id";
  private static final String BRIGHTSPACE_USER_KEY = "org.opencastproject.userdirectory.brightspace.systemuser.key";
  private static final String BRIGHTSPACE_URL = "org.opencastproject.userdirectory.brightspace.url";
  private static final String BRIGHTSPACE_APP_ID = "org.opencastproject.userdirectory.brightspace.application.id";
  private static final String BRIGHTSPACE_APP_KEY = "org.opencastproject.userdirectory.brightspace.application.key";

  private static final String CACHE_SIZE_KEY = "org.opencastproject.userdirectory.brightspace.cache.size";
  private static final String CACHE_EXPIRATION_KEY = "org.opencastproject.userdirectory.brightspace.cache.expiration";
  private static final int DEFAULT_CACHE_SIZE_VALUE = 1000;
  private static final int DEFAULT_CACHE_EXPIRATION_VALUE = 60;

  /** The keys to look up which roles in Brightspace should be considered as instructor roles */
  private static final String BRIGHTSPACE_INSTRUCTOR_ROLES_KEY =
                                "org.opencastproject.userdirectory.brightspace.instructor.roles";
  private static final String DEFAULT_BRIGHTSPACE_INSTRUCTOR_ROLES = "teacher,ta";
  /** The keys to look up which users should be ignored */
  private static final String IGNORED_USERNAMES_KEY = "org.opencastproject.userdirectory.brightspace.ignored.usernames";
  private static final String DEFAULT_IGNORED_USERNAMES = "admin,anonymous";

  private String pid;
  private BrightspaceClient client;
  private Organization organization;
  private LoadingCache<String, Object> cache;
  private Object nullToken = new Object();
  private AtomicLong loadUserRequests;
  private AtomicLong brightspaceWebServiceRequests;
  private Set<String> instructorRoles;
  private Set<String> ignoredUsernames;

  /** The organization directory service */
  private OrganizationDirectoryService orgDirectory;

  /** OSGi DI */
  @Reference
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  @Activate
  @Modified
  public void updated(Map<String, Object> properties) throws ConfigurationException {
    pid = (String) properties.get(Constants.SERVICE_PID);

    String organizationId = (String) properties.get(ORGANIZATION_KEY);
    String urlStr = (String) properties.get(BRIGHTSPACE_URL);
    String systemUserId = (String) properties.get(BRIGHTSPACE_USER_ID);
    String systemUserKey = (String) properties.get(BRIGHTSPACE_USER_KEY);
    final String applicationId = (String) properties.get(BRIGHTSPACE_APP_ID);
    final String applicationKey = (String) properties.get(BRIGHTSPACE_APP_KEY);

    int cacheSize;
    String cacheSizeStr = (String) properties.get(CACHE_SIZE_KEY);
    if (StringUtils.isBlank(cacheSizeStr)) {
      cacheSize = DEFAULT_CACHE_SIZE_VALUE;
    } else {
      cacheSize = NumberUtils.toInt(cacheSizeStr);
    }

    int cacheExpiration;
    String cacheExpirationStr = (String) properties.get(CACHE_EXPIRATION_KEY);
    if (StringUtils.isBlank(cacheExpirationStr)) {
      cacheExpiration = DEFAULT_CACHE_EXPIRATION_VALUE;
    } else {
      cacheExpiration = NumberUtils.toInt(cacheExpirationStr);
    }

    String rolesStr = (String) properties.get(BRIGHTSPACE_INSTRUCTOR_ROLES_KEY);
    if (StringUtils.isBlank(rolesStr)) {
      rolesStr = DEFAULT_BRIGHTSPACE_INSTRUCTOR_ROLES;
    }
    instructorRoles = parsePropertyLineAsSet(rolesStr);
    logger.debug("Brightspace instructor roles: {}", instructorRoles);

    String ignoredUsersStr = (String) properties.get(IGNORED_USERNAMES_KEY);
    if (StringUtils.isBlank(ignoredUsersStr)) {
      ignoredUsersStr = DEFAULT_IGNORED_USERNAMES;
    }
    ignoredUsernames = parsePropertyLineAsSet(ignoredUsersStr);
    logger.debug("Ignored users: {}", ignoredUsernames);

    validateUrl(urlStr);
    validateConfigurationKey(ORGANIZATION_KEY, organizationId);
    validateConfigurationKey(BRIGHTSPACE_USER_ID, systemUserId);
    validateConfigurationKey(BRIGHTSPACE_USER_KEY, systemUserKey);
    validateConfigurationKey(BRIGHTSPACE_APP_ID, applicationId);
    validateConfigurationKey(BRIGHTSPACE_APP_KEY, applicationKey);

    try {
      organization = orgDirectory.getOrganization(organizationId);
    } catch (NotFoundException nfe) {
      logger.warn("Organization {} not found!", organizationId);
      throw new ConfigurationException(ORGANIZATION_KEY, "not found");
    }

    client = new BrightspaceClientImpl(urlStr, applicationId, applicationKey, systemUserId, systemUserKey);

    logger.info("Configured BrightspaceUserProviderInstance(pid={}, url={}, cacheSize={}, cacheExpiration={}, "
                  + "InstructorRoles={}, ignoredUserNames={})", pid, client.getURL(), cacheSize, cacheExpiration,
                  instructorRoles, ignoredUsernames);

    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Object>() {
              @Override
              public Object load(String username) {
                User user = loadUserFromBrightspace(username);
                return user == null ? nullToken : user;
              }
            });
  }

  private void validateConfigurationKey(String key, String value) throws ConfigurationException {
    if (StringUtils.isBlank(value)) {
      throw new ConfigurationException(key, "is not set");
    }
  }

  private void validateUrl(String urlStr) throws ConfigurationException {
    if (StringUtils.isBlank(urlStr)) {
      throw new ConfigurationException(BRIGHTSPACE_URL, "is not set");
    } else {
      try {
        new URI(urlStr);
      } catch (URISyntaxException e) {
        throw new ConfigurationException(BRIGHTSPACE_URL, "not a URL");
      }
    }
  }

  private Set<String> parsePropertyLineAsSet(String configLine) {
    Set<String> set = new HashSet<>();
    String[] configs = configLine.split(",");
    for (String config: configs) {
      set.add(config.trim());
    }
    return set;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#getName()
   */
  @Override
  public String getName() {
    return pid;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#getUsers()
   */
  @Override
  public Iterator<User> getUsers() {
    return Collections.emptyIterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    this.loadUserRequests.incrementAndGet();
    logger.debug("getting user from cache");

    try {
      Object user = this.cache.getUnchecked(userName);
      if (user != this.nullToken) {
        logger.debug("Returning user {} from cache", userName);
        return (User) user;
      } else {
        logger.debug("Returning null user from cache");
        return null;
      }
    } catch (ExecutionError | UncheckedExecutionException ee) {
      logger.warn("Exception while loading user {}", userName, ee);
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
    return 0L;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return this.organization.getId();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#findUsers(java.lang.String, int, int)
   */
  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {
    return Collections.emptyIterator();
  }

  @Override
  public void invalidate(String userName) {
    this.cache.invalidate(userName);
  }

  @Override
  public List<Role> getRolesForUser(String username) {
    User user = this.loadUser(username);
    if (user != null) {
      logger.debug("Returning cached role set for {}", username);
      return new ArrayList<>(user.getRoles());
    }
    logger.debug("Return empty role set for {} - not found in Brightspace", username);
    return Collections.emptyList();
  }

  @Override
  public Iterator<Role> findRoles(String query, Target target, int offset, int limit) {
    return Collections.emptyIterator();
  }

  private User loadUserFromBrightspace(String username) {
    if (this.cache == null) {
      throw new IllegalStateException("The Brightspace user detail service has not yet been configured");
    } else if (ignoredUsernames.stream().anyMatch(u -> u.equals(username))) {
      logger.debug("We don't answer for: " + username);
      return null;
    } else {

      logger.debug("In loadUserFromBrightspace, currently processing user: {}", username);
      JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

      this.brightspaceWebServiceRequests.incrementAndGet();
      Thread currentThread = Thread.currentThread();
      ClassLoader originalClassloader = currentThread.getContextClassLoader();
      BrightspaceUser brightspaceUser;

      try {
        brightspaceUser = this.client.findUser(username);

        if (brightspaceUser != null) {
          logger.info("Retrieved user {}", brightspaceUser.getUserId());
          String brightspaceUserId = brightspaceUser.getUserId();

          List<String> roleList = client.getRolesFromBrightspace(brightspaceUserId, instructorRoles);
          logger.debug("Brightspace user {} with id {} with roles: {}", username, brightspaceUserId, roleList);

          Set<JaxbRole> roles = new HashSet<>();
          boolean isInstructor = false;
          for (String roleStr: roleList) {
            roles.add(new JaxbRole(roleStr, jaxbOrganization, "Brightspace external role", Role.Type.EXTERNAL));
            if (roleStr.endsWith(LTI_INSTRUCTOR_ROLE)) {
              isInstructor = true;
            }
          }
          roles.add(new JaxbRole(Group.ROLE_PREFIX + "BRIGHTSPACE", jaxbOrganization, "Brightspace User",
                  Role.Type.EXTERNAL_GROUP));
          if (isInstructor) {
            roles.add(new JaxbRole(Group.ROLE_PREFIX + "BRIGHTSPACE_INSTRUCTOR", jaxbOrganization,
                      "Brightspace Instructor", Role.Type.EXTERNAL_GROUP));
          }
          logger.debug("Returning JaxbRoles: {}", roles);

          User user =  new JaxbUser(username, null, brightspaceUser.getDisplayName(),
                  brightspaceUser.getExternalEmail(), this.getName(), jaxbOrganization, roles);
          cache.put(username, user);
          logger.debug("Returning user {}", user);
          return user;
        } else {
          cache.put(username, nullToken);
          logger.debug("User {} not found in Brightspace system", username);
          return null;
        }
      } catch (BrightspaceClientException e) {
        logger.error("A Brightspace API error ( {} ) occurred, user {} could not be retrieved", e, username);
        return null;
      } finally {
        currentThread.setContextClassLoader(originalClassloader);
      }
    }
  }
}
