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

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClientImpl;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

@Component(
    immediate = true,
    service = ManagedServiceFactory.class,
    property = {
        "service.pid=org.opencastproject.userdirectory.brightspace",
        "service.description=Provides Brightspace user directory instances"
    }
)
public class BrightspaceUserProviderFactory implements ManagedServiceFactory {

  private static final Logger logger = LoggerFactory.getLogger(BrightspaceUserProviderFactory.class);

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
  private static final String BRIGHTSPACE_NAME = "org.opencastproject.userdirectory.brightspace";
  private static final int DEFAULT_CACHE_SIZE_VALUE = 1000;
  private static final int DEFAULT_CACHE_EXPIRATION_VALUE = 60;

  /** The keys to look up which roles in Brightspace should be considered as instructor roles */
  private static final String BRIGHTSPACE_INSTRUCTOR_ROLES_KEY =
                                "org.opencastproject.userdirectory.brightspace.instructor.roles";
  private static final String DEFAULT_BRIGHTSPACE_INSTRUCTOR_ROLES = "teacher,ta";
  /** The keys to look up which users should be ignored */
  private static final String IGNORED_USERNAMES_KEY = "org.opencastproject.userdirectory.brightspace.ignored.usernames";
  private static final String DEFAULT_IGNORED_USERNAMES = "admin,anonymous";

  protected BundleContext bundleContext;
  private Map<String, ServiceRegistration> providerRegistrations = new ConcurrentHashMap<>();
  private OrganizationDirectoryService orgDirectory;
  private int cacheSize;
  private int cacheExpiration;

  /**
   * Builds a JMX object name for a given PID
   *
   * @param pid the PID
   * @return the object name
   * @throws NullPointerException
   * @throws MalformedObjectNameException
   */
  public static final ObjectName getObjectName(String pid) throws MalformedObjectNameException, NullPointerException {
    return new ObjectName(pid + ":type=BrightspaceRequests");
  }

  /**
   * OSGi callback for setting the organization directory service.
   */
  @Reference
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  /**
   * Callback for the activation of this component
   *
   * @param cc the component context
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.debug("Activate BrightspaceUserProviderFactory");
    this.bundleContext = cc.getBundleContext();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedServiceFactory#getName()
   */
  @Override
  public String getName() {
    return BRIGHTSPACE_NAME;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
   */
  @Override
  public void updated(String pid, Dictionary properties) throws ConfigurationException {
    logger.debug("updated BrightspaceUserProviderFactory");
    String adminUserName = StringUtils.trimToNull(
        bundleContext.getProperty(SecurityConstants.GLOBAL_ADMIN_USER_PROPERTY));
    String organization = (String) properties.get(ORGANIZATION_KEY);
    String urlStr = (String) properties.get(BRIGHTSPACE_URL);
    String systemUserId = (String) properties.get(BRIGHTSPACE_USER_ID);
    String systemUserKey = (String) properties.get(BRIGHTSPACE_USER_KEY);
    final String applicationId = (String) properties.get(BRIGHTSPACE_APP_ID);
    final String applicationKey = (String) properties.get(BRIGHTSPACE_APP_KEY);

    String cacheSizeStr = (String) properties.get(CACHE_SIZE_KEY);
    if (StringUtils.isBlank(cacheSizeStr)) {
      cacheSize = DEFAULT_CACHE_SIZE_VALUE;
    } else {
      cacheSize = NumberUtils.toInt(cacheSizeStr);
    }


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
    Set instructorRoles = parsePropertyLineAsSet(rolesStr);
    logger.debug("Brightspace instructor roles: {}", instructorRoles);

    String ignoredUsersStr = (String) properties.get(IGNORED_USERNAMES_KEY);
    if (StringUtils.isBlank(ignoredUsersStr)) {
      ignoredUsersStr = DEFAULT_IGNORED_USERNAMES;
    }
    Set ignoredUsernames = parsePropertyLineAsSet(ignoredUsersStr);
    logger.debug("Ignored users: {}", ignoredUsernames);

    validateUrl(urlStr);
    validateConfigurationKey(ORGANIZATION_KEY, organization);
    validateConfigurationKey(BRIGHTSPACE_USER_ID, systemUserId);
    validateConfigurationKey(BRIGHTSPACE_USER_KEY, systemUserKey);
    validateConfigurationKey(BRIGHTSPACE_APP_ID, applicationId);
    validateConfigurationKey(BRIGHTSPACE_APP_KEY, applicationKey);

    ServiceRegistration existingRegistration = this.providerRegistrations.remove(pid);
    if (existingRegistration != null) {
      existingRegistration.unregister();
    }

    Organization org;
    try {
      org = this.orgDirectory.getOrganization(organization);
    } catch (NotFoundException nfe) {
      logger.warn("Organization {} not found!", organization);
      throw new ConfigurationException(ORGANIZATION_KEY, "not found");
    }

    logger.debug("creating new brightspace user provider for pid={}", pid);

    BrightspaceClientImpl clientImpl
        = new BrightspaceClientImpl(urlStr, applicationId, applicationKey, systemUserId, systemUserKey);
    BrightspaceUserProviderInstance provider
        = new BrightspaceUserProviderInstance(pid, clientImpl, org, cacheSize, cacheExpiration  ,
            instructorRoles, ignoredUsernames);
    this.providerRegistrations
        .put(pid, this.bundleContext.registerService(UserProvider.class.getName(), provider, null));
    this.providerRegistrations
        .put(pid, this.bundleContext.registerService(RoleProvider.class.getName(), provider, null));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
   */
  @Override
  public void deleted(String pid) {
    logger.debug("delete BrightspaceUserProviderInstance for pid={}", pid);
    ServiceRegistration registration = providerRegistrations.remove(pid);
    if (registration != null) {
      registration.unregister();

      try {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(getObjectName(pid));
      } catch (Exception e) {
        logger.warn("Unable to unregister mbean for pid='{}'", pid, e);
      }
    }
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

}
