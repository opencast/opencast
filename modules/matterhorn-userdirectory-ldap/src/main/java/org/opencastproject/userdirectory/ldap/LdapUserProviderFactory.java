/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.userdirectory.ldap;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * LDAP implementation of the spring UserDetailsService, taking configuration information from the component context.
 */
public class LdapUserProviderFactory implements ManagedServiceFactory {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(LdapUserProviderFactory.class);

  /** This service factory's PID */
  public static final String PID = "org.opencastproject.userdirectory.ldap";

  /** The key to look up the ldap search filter in the service configuration properties */
  private static final String SEARCH_FILTER_KEY = "org.opencastproject.userdirectory.ldap.searchfilter";

  /** The key to look up the ldap search base in the service configuration properties */
  private static final String SEARCH_BASE_KEY = "org.opencastproject.userdirectory.ldap.searchbase";

  /** The key to look up the ldap server URL in the service configuration properties */
  private static final String LDAP_URL_KEY = "org.opencastproject.userdirectory.ldap.url";

  /** The key to look up the role attributes in the service configuration properties */
  private static final String ROLE_ATTRIBUTES_KEY = "org.opencastproject.userdirectory.ldap.roleattributes";

  /** The key to look up the organization identifer in the service configuration properties */
  private static final String ORGANIZATION_KEY = "org.opencastproject.userdirectory.ldap.org";

  /** The key to look up the user DN to use for performing searches. */
  private static final String SEARCH_USER_DN = "org.opencastproject.userdirectory.ldap.userDn";

  /** The key to look up the password to use for performing searches */
  private static final String SEARCH_PASSWORD = "org.opencastproject.userdirectory.ldap.password";

  /** The key to look up the number of user records to cache */
  private static final String CACHE_SIZE = "org.opencastproject.userdirectory.ldap.cache.size";

  /** The key to look up the number of minutes to cache users */
  private static final String CACHE_EXPIRATION = "org.opencastproject.userdirectory.ldap.cache.expiration";

  /** A map of pid to ldap user provider instance */
  private Map<String, ServiceRegistration> providerRegistrations = new ConcurrentHashMap<String, ServiceRegistration>();

  /** The OSGI bundle context */
  protected BundleContext bundleContext = null;

  /** The organization directory service */
  private OrganizationDirectoryService orgDirectory;

  /** OSGi callback for setting the organization directory service. */
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  /**
   * Callback for activation of this component.
   * 
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    logger.debug("Activate LdapUserProviderFactory");
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
    logger.debug("Updating LdapUserProviderFactory");
    String organization = (String) properties.get(ORGANIZATION_KEY);
    if (StringUtils.isBlank(organization))
      throw new ConfigurationException(ORGANIZATION_KEY, "is not set");
    String searchBase = (String) properties.get(SEARCH_BASE_KEY);
    if (StringUtils.isBlank(searchBase))
      throw new ConfigurationException(SEARCH_BASE_KEY, "is not set");
    String searchFilter = (String) properties.get(SEARCH_FILTER_KEY);
    if (StringUtils.isBlank(searchFilter))
      throw new ConfigurationException(SEARCH_FILTER_KEY, "is not set");
    String url = (String) properties.get(LDAP_URL_KEY);
    if (StringUtils.isBlank(url))
      throw new ConfigurationException(LDAP_URL_KEY, "is not set");
    String userDn = (String) properties.get(SEARCH_USER_DN);
    String password = (String) properties.get(SEARCH_PASSWORD);
    String roleAttributesGlob = (String) properties.get(ROLE_ATTRIBUTES_KEY);

    int cacheSize = 1000;
    logger.debug("Using cache size " + properties.get(CACHE_SIZE) + " for " + LdapUserProviderFactory.class.getName());
    try {
      if (properties.get(CACHE_SIZE) != null) {
        Integer configuredCacheSize = Integer.parseInt(properties.get(CACHE_SIZE).toString());
        if (configuredCacheSize != null) {
          cacheSize = configuredCacheSize.intValue();
        }
      }
    } catch (Exception e) {
      logger.warn("{} could not be loaded, default value is used: {}", CACHE_SIZE, cacheSize);
    }

    int cacheExpiration = 1;
    try {
      if (properties.get(CACHE_EXPIRATION) != null) {
        Integer configuredCacheExpiration = Integer.parseInt(properties.get(CACHE_EXPIRATION).toString());
        if (configuredCacheExpiration != null) {
          cacheExpiration = configuredCacheExpiration.intValue();
        }
      }
    } catch (Exception e) {
      logger.warn("{} could not be loaded, default value is used: {}", CACHE_EXPIRATION, cacheExpiration);
    }

    // Now that we have everything we need, go ahead and activate a new provider, removing an old one if necessary
    ServiceRegistration existingRegistration = providerRegistrations.remove(pid);
    if (existingRegistration != null) {
      existingRegistration.unregister();
    }

    Organization org;
    try {
      org = orgDirectory.getOrganization(organization);
    } catch (NotFoundException e) {
      logger.warn("Organization {} not found!", organization);
      throw new ConfigurationException(ORGANIZATION_KEY, "not found");
    }
    LdapUserProviderInstance provider = new LdapUserProviderInstance(pid, org, searchBase, searchFilter, url, userDn,
            password, roleAttributesGlob, cacheSize, cacheExpiration);
    providerRegistrations.put(pid, bundleContext.registerService(UserProvider.class.getName(), provider, null));

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
   */
  @Override
  public void deleted(String pid) {
    ServiceRegistration registration = providerRegistrations.remove(pid);
    if (registration != null) {
      registration.unregister();
      try {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(LdapUserProviderFactory.getObjectName(pid));
      } catch (Exception e) {
        logger.warn("Unable to unregister mbean for pid='{}': {}", pid, e.getMessage());
      }
    }
  }

  /**
   * Builds a JMX object name for a given PID
   * 
   * @param pid
   *          the PID
   * @return the object name
   * @throws NullPointerException
   * @throws MalformedObjectNameException
   */
  public static final ObjectName getObjectName(String pid) throws MalformedObjectNameException, NullPointerException {
    return new ObjectName(pid + ":type=LDAPRequests");
  }

}
