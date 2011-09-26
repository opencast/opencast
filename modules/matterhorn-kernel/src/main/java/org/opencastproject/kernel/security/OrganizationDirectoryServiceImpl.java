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

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
  
  /** The registered organizations */
  protected Map<String, Organization> organizations = new ConcurrentHashMap<String, Organization>();

  /**
   * Sets the default organization to return when no organization directory is registered.
   * 
   * @param cc
   *          the OSGI componentContext
   */
  protected void activate(ComponentContext cc) throws Exception {
    String configuredServerName = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    Organization defaultOrganization = null;
    if (configuredServerName == null) {
      defaultOrganization = new DefaultOrganization();
    } else {
      URL url = new URL(configuredServerName);
      defaultOrganization = new DefaultOrganization(url.getHost(), url.getPort());
    }
    Configuration config = configAdmin.createFactoryConfiguration(PID);
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put(ORG_ID_KEY, defaultOrganization.getId());
    props.put(ORG_NAME_KEY, defaultOrganization.getName());
    props.put(ORG_SERVER_KEY, defaultOrganization.getServerName());
    props.put(ORG_PORT_KEY, Integer.toString(defaultOrganization.getServerPort()));
    props.put(ORG_ADMIN_ROLE_KEY, defaultOrganization.getAdminRole());
    props.put(ORG_ANONYMOUS_ROLE_KEY, defaultOrganization.getAnonymousRole());
    config.update(props);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.OrganizationDirectoryService#getOrganization(java.lang.String)
   */
  @Override
  public Organization getOrganization(String id) throws NotFoundException {
    for (Organization o : organizations.values()) {
      if (o.getId().equals(id)) {
        return o;
      }
    }
    throw new NotFoundException(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.OrganizationDirectoryService#getOrganization(java.net.URL)
   */
  @Override
  public Organization getOrganization(URL url) throws NotFoundException {
    for (Organization o : organizations.values()) {
      if (o.getServerName().equals(url.getHost()) && o.getServerPort() == url.getPort())
        return o;
    }
    throw new NotFoundException(url.toExternalForm());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.OrganizationDirectoryService#getOrganizations()
   */
  @Override
  public List<Organization> getOrganizations() {
    List<Organization> result = new ArrayList<Organization>(organizations.size());
    result.addAll(organizations.values());
    return result;
  }

  /**
   * Adds the organization to the list of organizations.
   * 
   * @param organization
   *          the organization
   */
  public void addOrganization(Organization organization) {
    for (Organization o : organizations.values()) {
      if (o.getId().equals(organization.getId())) {
        throw new IllegalStateException("Can not register an organization with id '" + organization.getId()
                + "' since an organization with that identifier has already been registered");
      }
    }
    organizations.put(organization.getId(), organization);
  }

  /**
   * Removes the organization from the list of organizations.
   * 
   * @param organization
   *          the organization
   */
  public void removeOrganization(Organization organization) {
    for (Iterator<Entry<String, Organization>> entryIter = organizations.entrySet().iterator(); entryIter.hasNext();) {
      Entry<String, Organization> entry = entryIter.next();
      if (entry.getValue().equals(organization)) {
        entryIter.remove();
        return;
      }
    }
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
    logger.info("Updating organization pid='{}'", pid);
    
    // Gather the properties
    String id = (String) properties.get(ORG_ID_KEY);
    String name = (String) properties.get(ORG_NAME_KEY);
    String server = (String) properties.get(ORG_SERVER_KEY);
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

    // Replace the old immutable organization with a new one
    organizations.put(pid, new Organization(id, name, server, port, adminRole, anonRole, orgProperties));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
   */
  @Override
  public void deleted(String pid) {
    organizations.remove(pid);
  }
  
  /**
   * @param configAdmin the configAdmin to set
   */
  public void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }
  
}
