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

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClientImpl;
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

public class BrightspaceUserProviderFactory implements ManagedServiceFactory {

  public static final String PID = "org.opencastproject.userdirectory.brightspace";
  private static final Logger logger = LoggerFactory.getLogger(BrightspaceUserProviderFactory.class);

  private static final String ORGANIZATION_KEY = "org.opencastproject.userdirectory.brightspace.org";
  private static final String BRIGHTSPACE_USER_ID = "org.opencastproject.userdirectory.brightspace.systemuser.id";
  private static final String BRIGHTSPACE_USER_KEY = "org.opencastproject.userdirectory.brightspace.systemuser.key";
  private static final String BRIGHTSPACE_URL = "org.opencastproject.userdirectory.brightspace.url";
  private static final String BRIGHTSPACE_APP_ID = "org.opencastproject.userdirectory.brightspace.application.id";
  private static final String BRIGHTSPACE_APP_KEY = "org.opencastproject.userdirectory.brightspace.application.key";

  private static final String CACHE_SIZE = "org.opencastproject.userdirectory.brightspace.cache.size";
  private static final String CACHE_EXPIRATION = "org.opencastproject.userdirectory.brightspace.cache.expiration";
  private static final String BRIGHTSPACE_NAME = "org.opencastproject.userdirectory.brightspace";
  private static final int DEFAULT_CACHE_SIZE_VALUE = 1000;
  private static final int DEFAULT_CACHE_EXPIRATION_VALUE = 60;

  protected BundleContext bundleContext;
  private Map<String, ServiceRegistration> providerRegistrations = new ConcurrentHashMap<>();
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
    return new ObjectName(pid + ":type=BrightspaceRequests");
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
    String organization = (String) properties.get(ORGANIZATION_KEY);
    String urlStr = (String) properties.get(BRIGHTSPACE_URL);
    String systemUserId = (String) properties.get(BRIGHTSPACE_USER_ID);
    String systemUserKey = (String) properties.get(BRIGHTSPACE_USER_KEY);
    final String applicationId = (String) properties.get(BRIGHTSPACE_APP_ID);
    final String applicationKey = (String) properties.get(BRIGHTSPACE_APP_KEY);

    int cacheSize = parseCacheSizeProperty(properties);
    int cacheExpiration = parseCacheExpirationProperty(properties);

    validateOrganization(organization);
    validateUrl(urlStr);
    validateSystemUserKey(systemUserKey);
    validateSystemUserId(systemUserId);
    validateApplicationId(applicationId);
    validateApplicationKey(applicationKey);

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

    logger.debug("creating new MoodleUserProviderInstance for pid=" + pid);


    BrightspaceUserProviderInstance provider = new BrightspaceUserProviderInstance(pid,
            new BrightspaceClientImpl(urlStr, applicationId, applicationKey, systemUserId, systemUserKey), org, cacheSize,
            cacheExpiration);
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
    logger.debug("delete BrightspaceUserProviderInstance for pid=" + pid);
    ServiceRegistration registration = providerRegistrations.remove(pid);
    if (registration != null) {
      registration.unregister();

      try {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(getObjectName(pid));
      } catch (Exception var4) {
        logger.warn("Unable to unregister mbean for pid='{}': {}", pid, var4.getMessage());
      }
    }
  }

  private int parseCacheExpirationProperty(Dictionary properties) {
    try {
      if (properties.get(CACHE_EXPIRATION) != null) {
        return Integer.parseInt(properties.get(CACHE_EXPIRATION).toString());
      }
    } catch (NumberFormatException nfe) {
      logger.warn("{} could not be loaded, default value is used: {}", CACHE_EXPIRATION, DEFAULT_CACHE_EXPIRATION_VALUE);
    }
    return DEFAULT_CACHE_EXPIRATION_VALUE;
  }

  private int parseCacheSizeProperty(Dictionary properties) {
    try {
      if (properties.get(CACHE_SIZE) != null) {
        return  Integer.parseInt((String) properties.get(CACHE_SIZE));
      }
    } catch (NumberFormatException nfe) {
      logger.warn("{} could not be loaded, default value is used: {}", CACHE_SIZE, DEFAULT_CACHE_SIZE_VALUE);
    }
    return DEFAULT_CACHE_SIZE_VALUE;
  }

  private void validateSystemUserKey(String userKey) throws ConfigurationException {
    if (StringUtils.isBlank(userKey)) {
      throw new ConfigurationException(BRIGHTSPACE_USER_KEY, "is not set");
    }
  }

  private void validateApplicationId(String applicationId) throws ConfigurationException {
    if (StringUtils.isBlank(applicationId)) {
      throw new ConfigurationException(BRIGHTSPACE_APP_ID, "is not set");
    }
  }

  private void validateApplicationKey(String applicationKey) throws ConfigurationException {
    if (StringUtils.isBlank(applicationKey)) {
      throw new ConfigurationException(BRIGHTSPACE_APP_KEY, "is not set");
    }
  }

  private void validateSystemUserId(String userId) throws ConfigurationException {
    if (StringUtils.isBlank(userId)) {
      throw new ConfigurationException(BRIGHTSPACE_USER_ID, "is not set");
    }
  }

  private void validateUrl(String urlStr) throws ConfigurationException {
    if (StringUtils.isBlank(urlStr)) {
      throw new ConfigurationException(BRIGHTSPACE_URL, "is not set");
    } else {
      URI url;
      try {
        url = new URI(urlStr);
      } catch (URISyntaxException var21) {
        throw new ConfigurationException(BRIGHTSPACE_URL, "not a URL");
      }
    }
  }

  private void validateOrganization(String organization) throws ConfigurationException {
    if (StringUtils.isBlank(organization)) {
      throw new ConfigurationException(ORGANIZATION_KEY, "is not set");
    }
  }
}
