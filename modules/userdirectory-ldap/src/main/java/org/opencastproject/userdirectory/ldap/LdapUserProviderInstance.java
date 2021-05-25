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

package org.opencastproject.userdirectory.ldap;

import org.opencastproject.security.api.CachingUserProviderMXBean;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A UserProvider that reads user roles from LDAP entries.
 */
public class LdapUserProviderInstance implements UserProvider, CachingUserProviderMXBean {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LdapUserProviderInstance.class);

  public static final String PROVIDER_NAME = "ldap";

  /** The spring ldap userdetails service delegate */
  private LdapUserDetailsService delegate = null;

  /** The organization id */
  private Organization organization = null;

  /** Total number of requests made to load users */
  private AtomicLong requests = null;

  /** The number of requests made to ldap */
  private AtomicLong ldapLoads = null;

  /** A cache of users, which lightens the load on the LDAP server */
  private LoadingCache<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /** Opencast's security service */
  private SecurityService securityService;

  /**
   * Constructs an ldap user provider with the needed settings.
   *
   * @param pid
   *          the pid of this service
   * @param organization
   *          the organization
   * @param searchBase
   *          the ldap search base
   * @param searchFilter
   *          the ldap search filter
   * @param url
   *          the url of the ldap server
   * @param userDn
   *          the user to authenticate as
   * @param password
   *          the user credentials
   * @param roleAttributesGlob
   *          the comma separate list of ldap attributes to treat as roles or to consider for the ldapAssignmentRoleMap
   * @param convertToUppercase
   *          whether or not the role names will be converted to uppercase
   * @param cacheSize
   *          the number of users to cache
   * @param cacheExpiration
   *          the number of minutes to cache users
   * @param securityService
   *          a reference to Opencast's security service
   */
  // CHECKSTYLE:OFF
  LdapUserProviderInstance(String pid, Organization organization, String searchBase, String searchFilter, String url,
          String userDn, String password, String roleAttributesGlob,
          boolean convertToUppercase, int cacheSize, int cacheExpiration, SecurityService securityService) {
    // CHECKSTYLE:ON
    this.organization = organization;
    this.securityService = securityService;
    logger.debug("Creating LdapUserProvider instance with pid=" + pid + ", and organization=" + organization
            + ", to LDAP server at url:  " + url);

    DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(url);
    if (StringUtils.isNotBlank(userDn)) {
      contextSource.setPassword(password);
      contextSource.setUserDn(userDn);
      // Required so that authentication will actually be used
      contextSource.setAnonymousReadOnly(false);
    } else {
      // No password set so try to connect anonymously.
      contextSource.setAnonymousReadOnly(true);
    }

    try {
      contextSource.afterPropertiesSet();
    } catch (Exception e) {
      throw new org.opencastproject.util.ConfigurationException("Unable to create a spring context source", e);
    }
    FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(searchBase, searchFilter, contextSource);
    userSearch.setReturningAttributes(roleAttributesGlob.split(","));
    delegate = new LdapUserDetailsService(userSearch);

    if (StringUtils.isNotBlank(roleAttributesGlob)) {
      LdapUserDetailsMapper mapper = new LdapUserDetailsMapper();

      mapper.setConvertToUpperCase(convertToUppercase);

      mapper.setRoleAttributes(roleAttributesGlob.split(","));

      // The default prefix value is "ROLE_", so we must explicitly set it to "" by default
      // Because of the parameters extraRoles and excludePrefixes, we must add the prefix manually
      mapper.setRolePrefix("");
      delegate.setUserDetailsMapper(mapper);
    }

    // Setup the caches
    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Object>() {
              @Override
              public Object load(String id) throws Exception {
                User user = loadUserFromLdap(id);
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
    ldapLoads = new AtomicLong();
    try {
      ObjectName name;
      name = LdapUserProviderFactory.getObjectName(pid);
      Object mbean = this;
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      try {
        mbs.unregisterMBean(name);
      } catch (InstanceNotFoundException e) {
        logger.debug(name + " was not registered");
      }
      mbs.registerMBean(mbean, name);
    } catch (Exception e) {
      logger.warn("Unable to register {} as an mbean: {}", this, e);
    }
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
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    logger.debug("LdapUserProvider is loading user " + userName);
    requests.incrementAndGet();
    try {
      // use #getUnchecked since the loader does not throw any checked exceptions
      Object user = cache.getUnchecked(userName);
      if (user == nullToken) {
        return null;
      } else {
        return (JaxbUser) user;
      }
    } catch (UncheckedExecutionException e) {
      logger.warn("Exception while loading user " + userName, e);
      return null;
    }
  }

  /**
   * Loads a user from LDAP.
   *
   * @param userName
   *          the username
   * @return the user
   */
  protected User loadUserFromLdap(String userName) {
    if (delegate == null || cache == null) {
      throw new IllegalStateException("The LDAP user detail service has not yet been configured");
    }
    ldapLoads.incrementAndGet();
    UserDetails userDetails = null;

    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassloader = currentThread.getContextClassLoader();
    try {
      currentThread.setContextClassLoader(LdapUserProviderFactory.class.getClassLoader());
      try {
        userDetails = delegate.loadUserByUsername(userName);
      } catch (UsernameNotFoundException e) {
        cache.put(userName, nullToken);
        return null;
      }

      JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
      User user = new JaxbUser(userDetails.getUsername(), PROVIDER_NAME, jaxbOrganization, new HashSet());
      cache.put(userName, user);
      return user;
    } finally {
      currentThread.setContextClassLoader(originalClassloader);
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
    return (float) (requests.get() - ldapLoads.get()) / requests.get();
  }

  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    // TODO implement a LDAP wildcard search
    // FIXME We return the current user, rather than an empty list, to make sure the current user's role is displayed in
    // the admin UI (MH-12526).
    User currentUser = securityService.getUser();
    if (loadUser(currentUser.getUsername()) != null) {
      List<User> retVal = new ArrayList<>();
      retVal.add(securityService.getUser());
      return retVal.iterator();
    }
    return Collections.emptyIterator();
  }

  @Override
  public Iterator<User> getUsers() {
    // TODO implement LDAP get all users
    // FIXME We return the current user, rather than an empty list, to make sure the current user's role is displayed in
    // the admin UI (MH-12526).
    User currentUser = securityService.getUser();
    if (loadUser(currentUser.getUsername()) != null) {
      List<User> retVal = new ArrayList<>();
      retVal.add(securityService.getUser());
      return retVal.iterator();
    }
    return Collections.emptyIterator();
  }

  @Override
  public long countUsers() {
    // TODO implement LDAP count users
    // FIXME Because of MH-12526, we return conditionally 1 when the previous methods return the current user
    if (loadUser(securityService.getUser().getUsername()) != null) {
      return 1;
    }
    return 0;
  }

  @Override
  public void invalidate(String userName) {
    cache.invalidate(userName);
  }
}
