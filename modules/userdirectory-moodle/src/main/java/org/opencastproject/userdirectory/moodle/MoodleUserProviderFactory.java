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

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Moodle implementation of the spring UserDetailsService, taking configuration information from the component context.
 */
public class MoodleUserProviderFactory implements ManagedServiceFactory {
  /**
   * This service factory's PID
   */
  public static final String PID = "org.opencastproject.userdirectory.moodle";

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(MoodleUserProviderFactory.class);

  /**
   * The key to look up the organization identifer in the service configuration properties
   */
  private static final String ORGANIZATION_KEY = "org.opencastproject.userdirectory.moodle.org";

  /**
   * The key to look up the REST webservice URL of the Moodle instance
   */
  private static final String MOODLE_URL_KEY = "org.opencastproject.userdirectory.moodle.url";

  /**
   * The key to look up the user token to use for performing searches.
   */
  private static final String MOODLE_TOKEN_KEY = "org.opencastproject.userdirectory.moodle.token";

  /**
   * The key to look up the number of user records to cache
   */
  private static final String CACHE_SIZE = "org.opencastproject.userdirectory.moodle.cache.size";

  /**
   * The key to look up the number of minutes to cache users
   */
  private static final String CACHE_EXPIRATION = "org.opencastproject.userdirectory.moodle.cache.expiration";

  /**
   * The key to look up whether to activate group roles
   */
  private static final String GROUP_ROLES_KEY = "org.opencastproject.userdirectory.moodle.group.roles.enabled";

  /**
   * The key to look up the regular expression used to validate courses
   */
  private static final String COURSE_PATTERN_KEY = "org.opencastproject.userdirectory.moodle.course.pattern";

  /**
   * The key to look up the regular expression used to validate users
   */
  private static final String USER_PATTERN_KEY = "org.opencastproject.userdirectory.moodle.user.pattern";

  /**
   * The key to look up the regular expression used to validate groups
   */
  private static final String GROUP_PATTERN_KEY = "org.opencastproject.userdirectory.moodle.group.pattern";

  /**
   * The OSGI bundle context
   */
  protected BundleContext bundleContext = null;

  /**
   * A map of pid to moodle user provider instance
   */
  private Map<String, ServiceRegistration> providerRegistrations = new ConcurrentHashMap<String, ServiceRegistration>();

  /**
   * The organization directory service
   */
  private OrganizationDirectoryService orgDirectory;

  /**
   * Builds a JMX object name for a given PID
   *
   * @param pid the PID
   * @return the object name
   * @throws NullPointerException
   * @throws MalformedObjectNameException
   */
  public static final ObjectName getObjectName(String pid) throws MalformedObjectNameException, NullPointerException {
    return new ObjectName(pid + ":type=MoodleRequests");
  }

  /**
   * OSGi callback for setting the organization directory service.
   */
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  /**
   * Callback for the activation of this component
   *
   * @param cc the component context
   */
  public void activate(ComponentContext cc) {
    logger.debug("Activate MoodleUserProviderFactory");
    this.bundleContext = cc.getBundleContext();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedServiceFactory#getName()
   */
  @Override
  public String getName() {
    return PID;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
   */
  @Override
  public void updated(String pid, Dictionary properties) throws ConfigurationException {
    logger.debug("updated MoodleUserProviderFactory");

    String adminUserName = StringUtils.trimToNull(bundleContext.getProperty(SecurityConstants.GLOBAL_ADMIN_USER_PROPERTY));

    String organization = (String) properties.get(ORGANIZATION_KEY);
    if (StringUtils.isBlank(organization))
      throw new ConfigurationException(ORGANIZATION_KEY, "is not set");

    String urlStr = (String) properties.get(MOODLE_URL_KEY);
    URI url;
    if (StringUtils.isBlank(urlStr))
      throw new ConfigurationException(MOODLE_URL_KEY, "is not set");
    try {
      url = new URI(urlStr);
    } catch (URISyntaxException e) {
      throw new ConfigurationException(MOODLE_URL_KEY, "not a URL");
    }

    String token = (String) properties.get(MOODLE_TOKEN_KEY);
    if (StringUtils.isBlank(token))
      throw new ConfigurationException(MOODLE_TOKEN_KEY, "is not set");

    boolean groupRoles = false;
    String groupRolesStr = (String) properties.get(GROUP_ROLES_KEY);
    if ("true".equals(groupRolesStr))
      groupRoles = true;

    String coursePattern = (String) properties.get(COURSE_PATTERN_KEY);
    String userPattern = (String) properties.get(USER_PATTERN_KEY);
    String groupPattern = (String) properties.get(GROUP_PATTERN_KEY);

    int cacheSize = 1000;
    try {
      if (properties.get(CACHE_SIZE) != null)
        cacheSize = Integer.parseInt(properties.get(CACHE_SIZE).toString());
    } catch (NumberFormatException e) {
      logger.warn("{} could not be loaded, default value is used: {}", CACHE_SIZE, cacheSize);
    }

    int cacheExpiration = 60;
    try {
      if (properties.get(CACHE_EXPIRATION) != null)
        cacheExpiration = Integer.parseInt(properties.get(CACHE_EXPIRATION).toString());
    } catch (NumberFormatException e) {
      logger.warn("{} could not be loaded, default value is used: {}", CACHE_EXPIRATION, cacheExpiration);
    }

    // Now that we have everything we need, go ahead and activate a new provider, removing an old one if necessary
    ServiceRegistration existingRegistration = providerRegistrations.remove(pid);
    if (existingRegistration != null)
      existingRegistration.unregister();

    Organization org;
    try {
      org = orgDirectory.getOrganization(organization);
    } catch (NotFoundException e) {
      logger.warn("Organization {} not found!", organization);
      throw new ConfigurationException(ORGANIZATION_KEY, "not found");
    }

    logger.debug("creating new MoodleUserProviderInstance for pid=" + pid);
    MoodleUserProviderInstance provider = new MoodleUserProviderInstance(pid, new MoodleWebServiceImpl(url, token), org,
            coursePattern, userPattern, groupPattern, groupRoles, cacheSize, cacheExpiration, adminUserName);

    providerRegistrations.put(pid, bundleContext.registerService(UserProvider.class.getName(), provider, null));
    providerRegistrations.put(pid, bundleContext.registerService(RoleProvider.class.getName(), provider, null));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
   */
  @Override
  public void deleted(String pid) {
    logger.debug("delete MoodleUserProviderInstance for pid=" + pid);
    ServiceRegistration registration = providerRegistrations.remove(pid);
    if (registration != null) {
      registration.unregister();
      try {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(MoodleUserProviderFactory.getObjectName(pid));
      } catch (Exception e) {
        logger.warn("Unable to unregister mbean for pid='{}': {}", pid, e.getMessage());
      }
    }
  }
}
