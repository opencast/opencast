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

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.lang.management.ManagementFactory;
import java.util.Dictionary;
import java.util.Hashtable;
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

  /** The key to look up the organization identifier in the service configuration properties */
  private static final String ORGANIZATION_KEY = "org.opencastproject.userdirectory.ldap.org";

  /** The key to look up the user DN to use for performing searches. */
  private static final String SEARCH_USER_DN = "org.opencastproject.userdirectory.ldap.userDn";

  /** The key to look up the password to use for performing searches */
  private static final String SEARCH_PASSWORD = "org.opencastproject.userdirectory.ldap.password";

  /** The key to look up the number of user records to cache */
  private static final String CACHE_SIZE = "org.opencastproject.userdirectory.ldap.cache.size";

  /** The key to look up the number of minutes to cache users */
  private static final String CACHE_EXPIRATION = "org.opencastproject.userdirectory.ldap.cache.expiration";

  /** The key to indicate a prefix that will be added to every role read from the LDAP */
  private static final String ROLE_PREFIX_KEY = "org.opencastproject.userdirectory.ldap.roleprefix";

  /**
   * The key to indicate a comma-separated list of prefixes.
   * The "role prefix" defined with the ROLE_PREFIX_KEY will not be prepended to the roles starting with any of these
   */
  private static final String EXCLUDE_PREFIXES_KEY = "org.opencastproject.userdirectory.ldap.exclude.prefixes";

  /** The key to indicate whether or not the roles should be converted to uppercase */
  private static final String UPPERCASE_KEY = "org.opencastproject.userdirectory.ldap.uppercase";

  /** The key to indicate a unique identifier for each LDAP connection */
  private static final String INSTANCE_ID_KEY = "org.opencastproject.userdirectory.ldap.id";

  /** The key to indicate a comma-separated list of extra roles to add to the authenticated user */
  private static final String EXTRA_ROLES_KEY = "org.opencastproject.userdirectory.ldap.extra.roles";

  /** The key to setup a LDAP connection ID as an OSGI service property */
  private static final String INSTANCE_ID_SERVICE_PROPERTY_KEY = "instanceId";

  /** A map of pid to ldap user provider instance */
  private Map<String, ServiceRegistration> providerRegistrations = new ConcurrentHashMap<>();

  /** A map of pid to ldap authorities populator instance */
  private Map<String, ServiceRegistration> authoritiesPopulatorRegistrations = new ConcurrentHashMap<>();

  /** The OSGI bundle context */
  protected BundleContext bundleContext = null;

  /** The organization directory service */
  private OrganizationDirectoryService orgDirectory;

  /** The group role provider service */
  private JpaGroupRoleProvider groupRoleProvider;

  /** A reference to Opencast's security service */
  private SecurityService securityService;

  /** OSGi callback for setting the organization directory service. */
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  /** OSGi callback for setting the role group service. */
  public void setGroupRoleProvider(JpaGroupRoleProvider groupRoleProvider) {
    this.groupRoleProvider = groupRoleProvider;
  }

  /** OSGi callback for setting the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for activation of this component.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    logger.debug("Activate LdapUserProviderFactory");
    bundleContext = cc.getBundleContext();
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
    String instanceId = (String) properties.get(INSTANCE_ID_KEY);
    if (StringUtils.isBlank(instanceId))
      throw new ConfigurationException(INSTANCE_ID_KEY, "is not set");
    String userDn = (String) properties.get(SEARCH_USER_DN);
    String password = (String) properties.get(SEARCH_PASSWORD);
    String roleAttributes = (String) properties.get(ROLE_ATTRIBUTES_KEY);
    String rolePrefix = (String) properties.get(ROLE_PREFIX_KEY);

    String[] excludePrefixes = null;
    String strExcludePrefixes = (String) properties.get(EXCLUDE_PREFIXES_KEY);
    if (StringUtils.isNotBlank(strExcludePrefixes)) {
      excludePrefixes = strExcludePrefixes.split(",");
    }

    // Make sure that property convertToUppercase is true by default
    String strUppercase = (String) properties.get(UPPERCASE_KEY);
    boolean convertToUppercase = StringUtils.isBlank(strUppercase) ? true : Boolean.valueOf(strUppercase);

    String[] extraRoles = new String[0];
    String strExtraRoles = (String) properties.get(EXTRA_ROLES_KEY);
    if (StringUtils.isNotBlank(strExtraRoles)) {
      extraRoles = strExtraRoles.split(",");
    }

    int cacheSize = 1000;
    logger.debug("Using cache size {} for {}", properties.get(CACHE_SIZE), LdapUserProviderFactory.class.getName());
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

    // Dictionary to include a property to identify this LDAP instance in the security.xml file
    Hashtable<String, String> dict = new Hashtable<>();
    dict.put(INSTANCE_ID_SERVICE_PROPERTY_KEY, instanceId);

    // Instantiate this LDAP instance and register it as such
    LdapUserProviderInstance provider = new LdapUserProviderInstance(pid, org, searchBase, searchFilter, url, userDn,
            password, roleAttributes, rolePrefix, extraRoles, excludePrefixes, convertToUppercase, cacheSize,
            cacheExpiration, securityService);

    providerRegistrations.put(pid, bundleContext.registerService(UserProvider.class.getName(), provider, null));

    OpencastLdapAuthoritiesPopulator authoritiesPopulator = new OpencastLdapAuthoritiesPopulator(roleAttributes,
            rolePrefix, excludePrefixes, convertToUppercase, org, securityService, groupRoleProvider, extraRoles);

    // Also, register this instance as LdapAuthoritiesPopulator so that it can be used within the security.xml file
    authoritiesPopulatorRegistrations.put(pid,
            bundleContext.registerService(LdapAuthoritiesPopulator.class.getName(), authoritiesPopulator, dict));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
   */
  @Override
  public void deleted(String pid) {
    ServiceRegistration providerRegistration = null;
    ServiceRegistration authoritiesPopulatorRegistration = null;

    try {
      providerRegistration = providerRegistrations.remove(pid);
      authoritiesPopulatorRegistration = authoritiesPopulatorRegistrations.remove(pid);
      if ((providerRegistration != null) || (authoritiesPopulatorRegistration != null)) {
        try {
          ManagementFactory.getPlatformMBeanServer().unregisterMBean(LdapUserProviderFactory.getObjectName(pid));
        } catch (Exception e) {
          logger.warn("Unable to unregister mbean for pid='{}': {}", pid, e.getMessage());
        }
      }
    } finally {
      if (providerRegistration != null)
        providerRegistration.unregister();
      if (authoritiesPopulatorRegistration != null)
        authoritiesPopulatorRegistration.unregister();
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
