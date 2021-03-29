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

package org.opencastproject.kernel.security;

import static org.opencastproject.security.util.SecurityUtil.hostAndPort;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Collections.toList;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.kernel.security.persistence.OrganizationDatabase;
import org.opencastproject.kernel.security.persistence.OrganizationDatabaseException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryListener;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implements the organizational directory. As long as no organizations are published in the service registry, the
 * directory will contain the default organization as the only instance.
 */
@Component(
  property = {
    "service.pid=org.opencastproject.organization",
    "service.description=Organization Directory Service"
  },
  immediate = true,
  service = { OrganizationDirectoryService.class, ManagedServiceFactory.class }
)
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
  public static final String ORG_SERVER_PREFIX = "prop.org.opencastproject.host.";

  /** The default in case no server is configured */
  public static final String DEFAULT_SERVER = "localhost";

  /** The managed property that specifies the server port */
  public static final String ORG_PORT_KEY = "port";

  /** The managed property that specifies the organization administrative role */
  public static final String ORG_ADMIN_ROLE_KEY = "admin_role";

  /** The managed property that specifies the organization anonymous role */
  public static final String ORG_ANONYMOUS_ROLE_KEY = "anonymous_role";

  /** The configuration admin service */
  protected ConfigurationAdmin configAdmin = null;

  /** To enable threading when dispatching jobs */
  private final ExecutorService executor = Executors.newCachedThreadPool();

  /** The organization database */
  private OrganizationDatabase persistence = null;

  /** The list of directory listeners */
  private final List<OrganizationDirectoryListener> listeners = new ArrayList<OrganizationDirectoryListener>();

  private OrgCache cache;

  /** OSGi DI */
  @Reference(name = "persistence")
  public void setOrgPersistence(OrganizationDatabase setOrgPersistence) {
    this.persistence = setOrgPersistence;
    this.cache = new OrgCache(60000, persistence);
  }

  /**
   * @param configAdmin
   *          the configAdmin to set
   */
  @Reference(name = "configAdmin")
  public void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }

  @Override
  public Organization getOrganization(final String id) throws NotFoundException {
    Organization org = cache.get(id);
    if (org == null)
      throw new NotFoundException();
    return org;
  }

  @Override
  public Organization getOrganization(final URL url) throws NotFoundException {
    Organization org = cache.get(url);
    if (org == null)
      throw new NotFoundException();
    return org;
  }

  @Override
  public List<Organization> getOrganizations() {
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
    fireOrganizationRegistered(organization);
  }

  @Override
  public String getName() {
    return PID;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void updated(String pid, Dictionary properties) throws ConfigurationException {
    logger.debug("Updating organization pid='{}'", pid);

    // Gather the properties
    final String id = (String) properties.get(ORG_ID_KEY);
    final String name = (String) properties.get(ORG_NAME_KEY);

    // Make sure the configuration meets the minimum requirements
    if (StringUtils.isBlank(id))
      throw new ConfigurationException(ORG_ID_KEY, ORG_ID_KEY + " must be set");

    final String portAsString = StringUtils.trimToNull((String) properties.get(ORG_PORT_KEY));
    final int port = portAsString != null ? Integer.parseInt(portAsString) : 80;
    final String adminRole = (String) properties.get(ORG_ADMIN_ROLE_KEY);
    final String anonRole = (String) properties.get(ORG_ANONYMOUS_ROLE_KEY);

    // Build the properties map
    final Map<String, String> orgProperties = new HashMap<String, String>();
    ArrayList<String> serverUrls = new ArrayList();

    for (Enumeration<?> e = properties.keys(); e.hasMoreElements();) {
      final String key = (String) e.nextElement();

      if (!key.startsWith(ORG_PROPERTY_PREFIX)) {
        continue;
      }

      if (key.startsWith(ORG_SERVER_PREFIX)) {
        String tenantSpecificHost = StringUtils.trimToNull((String) properties.get(key));
        serverUrls.add(tenantSpecificHost);
      }

      orgProperties.put(key.substring(ORG_PROPERTY_PREFIX.length()), (String) properties.get(key));
    }

    if (serverUrls.isEmpty()) {
      logger.debug("No server URL configured for organization " + name + ", setting default localhost");
      serverUrls.add(DEFAULT_SERVER);
    }

    // Load the existing organization or create a new one
    try {
      JpaOrganization org;
      try {
        org = (JpaOrganization) persistence.getOrganization(id);
        org.setName(name);
        for (String serverUrl : serverUrls) {
          if (StringUtils.isNotBlank(serverUrl)) {
            org.addServer(serverUrl, port);
          }
        }
        org.setAdminRole(adminRole);
        org.setAnonymousRole(anonRole);
        org.setProperties(orgProperties);
        logger.info("Updating organization '{}'", id);
        persistence.storeOrganization(org);
        fireOrganizationUpdated(org);
      } catch (NotFoundException e) {
        HashMap<String, Integer> servers = new HashMap<String, Integer>();
        for (String serverUrl : serverUrls) {
          if (StringUtils.isNotBlank(serverUrl)) {
            servers.put(serverUrl, port);
          }
        }
        org = new JpaOrganization(id, name, servers, adminRole, anonRole, orgProperties);
        logger.info("Creating organization '{}'", id);
        persistence.storeOrganization(org);
        fireOrganizationRegistered(org);
      }
      cache.invalidate();
    } catch (OrganizationDatabaseException e) {
      logger.error("Unable to register organization '{}': {}", id, e);
    }
  }

  @Override
  public void deleted(String pid) {
    try {
      Organization organization = getOrganization(pid);
      persistence.deleteOrganization(pid);
      cache.invalidate();
      fireOrganizationUnregistered(organization);
    } catch (NotFoundException e) {
      logger.warn("Can't delete organization with id {}, organization not found.", pid);
    }
  }

  @Override
  public void addOrganizationDirectoryListener(OrganizationDirectoryListener listener) {
    if (listener == null)
      return;
    if (!listeners.contains(listener))
      listeners.add(listener);
  }

  @Override
  public void removeOrganizationDirectoryListener(OrganizationDirectoryListener listener) {
    if (listener == null)
      return;
    listeners.remove(listener);
  }

  /**
   * Notifies registered listeners about a newly registered organization.
   *
   * @param organization
   *          the organization
   */
  private void fireOrganizationRegistered(final Organization organization) {
    executor.submit(new Runnable() {
      @Override
      public void run() {
        for (OrganizationDirectoryListener listener : listeners) {
          logger.debug("Notifying {} about newly registered organization '{}'", listener, organization);
          listener.organizationRegistered(organization);
        }
      }
    });
  }

  /**
   * Notifies registered listeners about an unregistered organization.
   *
   * @param organization
   *          the organization
   */
  private void fireOrganizationUnregistered(final Organization organization) {
    executor.submit(new Runnable() {
      @Override
      public void run() {
        for (OrganizationDirectoryListener listener : listeners) {
          logger.debug("Notifying {} about unregistered organization '{}'", listener, organization);
          listener.organizationUnregistered(organization);
        }
      }
    });
  }

  /**
   * Notifies registered listeners about an updated organization.
   *
   * @param organization
   *          the organization
   */
  private void fireOrganizationUpdated(final Organization organization) {
    executor.submit(new Runnable() {
      @Override
      public void run() {
        for (OrganizationDirectoryListener listener : listeners) {
          logger.debug("Notifying {} about updated organization '{}'", listener, organization);
          listener.organizationUpdated(organization);
        }
      }
    });
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

    OrgCache(long refreshInterval, OrganizationDatabase persistence) {
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
