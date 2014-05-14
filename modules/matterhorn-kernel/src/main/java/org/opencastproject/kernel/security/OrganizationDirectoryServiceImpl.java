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

import static org.opencastproject.security.util.SecurityUtil.hostAndPort;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Collections.toList;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.kernel.security.persistence.OrganizationDatabase;
import org.opencastproject.kernel.security.persistence.OrganizationDatabaseException;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

  /**
   * The default organization. This is a hack needed by the capture agent implementation see MH-9363
   */
  private final Organization defaultOrganization = new DefaultOrganization();

  /**
   * The list of organizations to handle later. This is a hack needed by the capture agent implementation see MH-9363
   */
  private final Map<String, Dictionary> unhandledOrganizations = new HashMap<String, Dictionary>();

  private OrgCache cache;

  /** OSGi DI */
  public void setOrgPersistence(OrganizationDatabase setOrgPersistence) {
    this.persistence = setOrgPersistence;
    this.cache = new OrgCache(60000, persistence);
    for (Entry<String, Dictionary> entry : unhandledOrganizations.entrySet()) {
      try {
        updated(entry.getKey(), entry.getValue());
      } catch (ConfigurationException e) {
        logger.error(e.getMessage());
      }
    }
  }

  /**
   * @param configAdmin
   *          the configAdmin to set
   */
  public void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }

  @Override
  public Organization getOrganization(final String id) throws NotFoundException {
    if (persistence == null) {
      logger.debug("No persistence available: Returning default organization for id {}", id);
      return defaultOrganization;
    }
    Organization org = cache.get(id);
    if (org == null)
      throw new NotFoundException();
    return org;
  }

  @Override
  public Organization getOrganization(final URL url) throws NotFoundException {
    if (persistence == null) {
      logger.debug("No persistence available: Returning default organization for url {}", url);
      return defaultOrganization;
    }
    Organization org = cache.get(url);
    if (org == null)
      throw new NotFoundException();
    return org;
  }

  @Override
  public List<Organization> getOrganizations() {
    if (persistence == null) {
      logger.debug("No persistence available: Returning only default organization");
      List<Organization> orgs = new ArrayList<Organization>();
      orgs.add(defaultOrganization);
      return orgs;
    }
    return cache.getAll();
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
    cache.invalidate();
  }

  @Override
  public String getName() {
    return PID;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void updated(String pid, Dictionary properties) throws ConfigurationException {
    if (persistence == null) {
      logger.debug("No persistence available: Ignoring organization update for pid='{}'", pid);
      unhandledOrganizations.put(pid, properties);
      return;
    }
    logger.debug("Updating organization pid='{}'", pid);

    // Gather the properties
    final String id = (String) properties.get(ORG_ID_KEY);
    final String name = (String) properties.get(ORG_NAME_KEY);
    final String server = (String) properties.get(ORG_SERVER_KEY);

    // Make sure the configuration meets the minimum requirements
    if (StringUtils.isBlank(id))
      throw new ConfigurationException(ORG_ID_KEY, ORG_ID_KEY + " must be set");
    if (StringUtils.isBlank(server))
      throw new ConfigurationException(ORG_SERVER_KEY, ORG_SERVER_KEY + " must be set");

    final String portAsString = StringUtils.trimToNull((String) properties.get(ORG_PORT_KEY));
    final int port = portAsString != null ? Integer.parseInt(portAsString) : 80;
    final String adminRole = (String) properties.get(ORG_ADMIN_ROLE_KEY);
    final String anonRole = (String) properties.get(ORG_ANONYMOUS_ROLE_KEY);

    // Build the properties map
    final Map<String, String> orgProperties = new HashMap<String, String>();
    for (Enumeration<?> e = properties.keys(); e.hasMoreElements();) {
      final String key = (String) e.nextElement();
      if (!key.startsWith(ORG_PROPERTY_PREFIX)) {
        continue;
      }
      orgProperties.put(key.substring(ORG_PROPERTY_PREFIX.length()), (String) properties.get(key));
    }

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
      cache.invalidate();
    } catch (OrganizationDatabaseException e) {
      logger.error("Unable to register organization '{}': {}", id, e);
    }
  }

  @Override
  public void deleted(String pid) {
    try {
      persistence.deleteOrganization(pid);
      cache.invalidate();
    } catch (NotFoundException e) {
      logger.warn("Can't delete organization with id {}, organization not found.", pid);
    }
  }

  /**
   * Very simple cache that does a <em>complete</em> refresh after a given interval. This type of cache is only suitable
   * for small sets.
   */
  private static final class OrgCache {
    private final Object lock = new Object();

    // A simple hash map is sufficient here.
    // No need to deal with soft references or an LRU map since the number of organizations
    // will be quite low.
    private final Map<Tuple<String, Integer>, Organization> byHost = map();
    private final Map<String, Organization> byId = map();
    private final long refreshInterval;
    private long lastRefresh;

    private final OrganizationDatabase persistence;

    public OrgCache(long refreshInterval, OrganizationDatabase persistence) {
      this.refreshInterval = refreshInterval;
      this.persistence = persistence;
      invalidate();
    }

    public Organization get(URL url) {
      synchronized (lock) {
        refresh();
        return byHost.get(hostAndPort(url));
      }
    }

    public Organization get(String id) {
      synchronized (lock) {
        refresh();
        return byId.get(id);
      }
    }

    public List<Organization> getAll() {
      synchronized (lock) {
        refresh();
        return toList(byId.values());
      }
    }

    public void invalidate() {
      this.lastRefresh = System.currentTimeMillis() - 2 * refreshInterval;
    }

    private void refresh() {
      final long now = System.currentTimeMillis();
      if (now - lastRefresh > refreshInterval) {
        byId.clear();
        byHost.clear();
        for (Organization org : persistence.getOrganizations()) {
          byId.put(org.getId(), org);
          // (host, port)
          for (Map.Entry<String, Integer> server : org.getServers().entrySet()) {
            byHost.put(tuple(server.getKey(), server.getValue()), org);
          }
        }
        lastRefresh = now;
      }
    }
  }
}
