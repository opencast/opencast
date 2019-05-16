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


package org.opencastproject.userdirectory.brightspace;

import org.opencastproject.security.api.CachingUserProviderMXBean;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.Role.Target;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClient;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClientException;
import org.opencastproject.userdirectory.brightspace.client.api.BrightspaceUser;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class BrightspaceUserProviderInstance implements UserProvider, RoleProvider, CachingUserProviderMXBean {

  private static final Logger logger = LoggerFactory.getLogger(BrightspaceUserProviderInstance.class);
  private static final String PROVIDER_NAME = "brightspace";

  private String pid;
  private BrightspaceClient client;
  private Organization organization;
  private LoadingCache<String, Object> cache;
  private Object nullToken = new Object();
  private AtomicLong loadUserRequests;
  private AtomicLong brightspaceWebServiceRequests;

  /**
   * Constructs a Brighspace user provider with the needed settings
   *
   * @param pid             The PID of this service.
   * @param client          The Brightspace rest service client.
   * @param organization    The organisation.
   * @param cacheSize       The number of users to cache.
   * @param cacheExpiration The number of minutes to cache users.
   */
  public BrightspaceUserProviderInstance(String pid, BrightspaceClient client, Organization organization, int cacheSize,
          int cacheExpiration) {
    this.pid = pid;
    this.client = client;
    this.organization = organization;
    logger.info("Creating new BrightspaceUserProviderInstance(pid={}, url={}, cacheSize={}, cacheExpiration={})", pid,
            client.getURL(), cacheSize, cacheExpiration);

    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Object>() {
              @Override
              public Object load(String username) {
                User user = loadUserFromBrightspace(username);
                return user == null ? nullToken : user;
              }
            });

    this.registerMBean(pid);
  }

  @Override
  public float getCacheHitRatio() {
    if (loadUserRequests.get() == 0)
      return 0;
    return (float) (loadUserRequests.get() - brightspaceWebServiceRequests.get()) / loadUserRequests.get();
  }

  private void registerMBean(String pid) {
    this.loadUserRequests = new AtomicLong();
    this.brightspaceWebServiceRequests = new AtomicLong();

    try {
      ObjectName name = BrightspaceUserProviderFactory.getObjectName(pid);
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

      try {
        mbs.unregisterMBean(name);
      } catch (InstanceNotFoundException ife) {
        logger.debug("{} was not registered", name);
      }

      mbs.registerMBean(this, name);
    } catch (Exception e) {
      logger.error("Unable to register {} as an mbean: {}", this, e);
    }

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
  public Iterator<Role> getRoles() {
    return Collections.emptyIterator();
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
    } else if (!"admin".equals(username) && !"".equals(username) && !"anonymous".equals(username)) {

      JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

      this.brightspaceWebServiceRequests.incrementAndGet();
      Thread currentThread = Thread.currentThread();
      ClassLoader originalClassloader = currentThread.getContextClassLoader();
      BrightspaceUser brightspaceUser;

      try {
        brightspaceUser = this.client.findUser(username);
        if (brightspaceUser != null) {
          String brightspaceUserId = brightspaceUser.getUserId();

          Set<String> courseIds = client.findCourseIds(brightspaceUserId);

          Set<JaxbRole> roles = courseIds.stream()
                  .map(courseId -> new JaxbRole(String.format("ROLE_%s", courseId), jaxbOrganization))
                  .collect(Collectors.toSet());

          return new JaxbUser(brightspaceUser.getUserName(), null, brightspaceUser.getDisplayName(),
                  brightspaceUser.getExternalEmail(), this.getName(), true, jaxbOrganization, roles);
        } else {
          cache.put(username, nullToken);
          logger.debug("User {} not found in Brightspace system", username);
          return null;
        }
      } catch (BrightspaceClientException e) {
        logger.error("A Brightspace API error occurred, user {} could not be retrieved", username);
        return null;
      } finally {
        currentThread.setContextClassLoader(originalClassloader);
      }
    } else {
      logger.debug("We don't answer for: " + username);
      return null;
    }
  }
}
