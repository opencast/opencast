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
package org.opencastproject.kernel.security;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.kernel.security.persistence.OrganizationDatabase;
import org.opencastproject.kernel.security.persistence.OrganizationDatabaseException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Tuple;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencastproject.util.data.Collections.getOrCreate;
import static org.opencastproject.util.data.Tuple.tuple;

/**
 * Implements the organizational directory. As long as no organizations are published in the service registry, the
 * directory will contain the default organization as the only instance.
 */
public class OrganizationDirectoryServiceImpl implements OrganizationDirectoryService, ManagedServiceFactory {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OrganizationDirectoryServiceImpl.class);

  /** The organization service PID */
  public static final String PID = "org.opencastproject.organization";

  /** The prefix for configurations to use for arbitrary organization properties */
  public static final String ORG_PROPERTY_PREFIX = "prop.";

  /** The managed property that specifies the organization id */
  public static final String ORG_ID_KEY = "id";

  /** The managed property that specifies the organization name */
  public static final String ORG_NAME_KEY = "name";

  /** The managed property that specifies the organization server name */
  public static final String ORG_SERVER_KEY = "server";

  /** The managed property that specifies the server port */
  public static final String ORG_PORT_KEY = "port";

  /** The managed property that specifies the organization administrative role */
  public static final String ORG_ADMIN_ROLE_KEY = "admin_role";

  /** The managed property that specifies the organization anonymous role */
  public static final String ORG_ANONYMOUS_ROLE_KEY = "anonymous_role";

  /** The configuration admin service */
  protected ConfigurationAdmin configAdmin = null;

  protected OrganizationDatabase persistence;

  // Local caches. Organizations change rarely so a simple hash map is sufficient.
  // No need to deal with soft references or an LRU map.
  private final Map<Tuple<String, Integer>, Organization> orgsByHost = new HashMap<Tuple<String, Integer>, Organization>();
  private final Map<String, Organization> orgsById = new HashMap<String, Organization>();

  /**
   * OSGi callback to set the security service.
   */
  public void setOrgPersistence(OrganizationDatabase setOrgPersistence) {
    this.persistence = setOrgPersistence;
  }

  /**
   * @param configAdmin
   *          the configAdmin to set
   */
  public void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.OrganizationDirectoryService#getOrganization(java.lang.String)
   */
  @Override
  public Organization getOrganization(final String id) throws NotFoundException {
    synchronized (orgsById) {
      return getOrCreate(orgsById, id, new Function0.X<Organization>() {
        @Override public Organization xapply() throws Exception {
          return persistence.getOrganization(id);
        }
      });
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.OrganizationDirectoryService#getOrganization(java.net.URL)
   */
  @Override
  public Organization getOrganization(final URL url) throws NotFoundException {
    final String host = url.getHost();
    final int port = url.getPort();
    synchronized (orgsByHost) {
      return getOrCreate(orgsByHost, tuple(host, port), new Function0.X<Organization>() {
        @Override public Organization xapply() throws Exception {
          return persistence.getOrganizationByHost(host, port);
        }
      });
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.OrganizationDirectoryService#getOrganizations()
   */
  @Override
  public List<Organization> getOrganizations() {
    return persistence.getOrganizations();
  }

  /**
   * Adds the organization to the list of organizations.
   * 
   * @param organization
   *          the organization
   */
  public void addOrganization(Organization organization) {
    boolean contains = persistence.containsOrganization(organization.getId());
    if (contains)
      throw new IllegalStateException("Can not add an organization with id '" + organization.getId()
              + "' since an organization with that identifier has already been registered");
    persistence.storeOrganization(organization);
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
  @SuppressWarnings("rawtypes")
  public void updated(String pid, Dictionary properties) throws ConfigurationException {
    logger.debug("Updating organization pid='{}'", pid);

    // Gather the properties
    String id = (String) properties.get(ORG_ID_KEY);
    String name = (String) properties.get(ORG_NAME_KEY);
    String server = (String) properties.get(ORG_SERVER_KEY);

    // Make sure the configuration meets the minimum requirements
    if (StringUtils.isBlank(id))
      throw new ConfigurationException(ORG_ID_KEY, ORG_ID_KEY + " must be set");
    if (StringUtils.isBlank(server))
      throw new ConfigurationException(ORG_SERVER_KEY, ORG_SERVER_KEY + " must be set");

    String portAsString = StringUtils.trimToNull((String) properties.get(ORG_PORT_KEY));
    int port = 80;
    if (portAsString != null) {
      port = Integer.parseInt(portAsString);
    }
    String adminRole = (String) properties.get(ORG_ADMIN_ROLE_KEY);
    String anonRole = (String) properties.get(ORG_ANONYMOUS_ROLE_KEY);

    // Build the properties map
    Map<String, String> orgProperties = new HashMap<String, String>();
    for (Enumeration<?> e = properties.keys(); e.hasMoreElements();) {
      String key = (String) e.nextElement();
      if (!key.startsWith(ORG_PROPERTY_PREFIX)) {
        continue;
      }
      orgProperties.put(key.substring(ORG_PROPERTY_PREFIX.length()), (String) properties.get(key));
    }

    // Clear cache so it gets updated later
    orgsByHost.clear();
    orgsById.clear();

    // Load the existing organization or create a new one
    try {
      JpaOrganization org;
      try {
        org = (JpaOrganization) persistence.getOrganization(id);
        org.setName(name);
        org.addServer(server, port);
        org.setAdminRole(adminRole);
        org.setAnonymousRole(anonRole);
        org.setProperties(orgProperties);
        logger.info("Registering organization '{}'", id);
      } catch (NotFoundException e) {
        org = new JpaOrganization(id, name, server, port, adminRole, anonRole, orgProperties);
        logger.info("Updating organization '{}'", id);
      }
      persistence.storeOrganization(org);
    } catch (OrganizationDatabaseException e) {
      logger.error("Unable to register organization '{}': {}", id, e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
   */
  @Override
  public void deleted(String pid) {
    try {
      persistence.deleteOrganization(pid);
    } catch (NotFoundException e) {
      logger.warn("Can't delete organization with id {}, organization not found.", pid);
    }
  }

}
