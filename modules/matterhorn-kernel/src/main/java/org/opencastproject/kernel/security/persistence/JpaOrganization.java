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
package org.opencastproject.kernel.security.persistence;

import org.opencastproject.security.api.Organization;
import org.opencastproject.util.EqualsUtil;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA-annotated organization object.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "mh_organization")
@NamedQueries({
        @NamedQuery(name = "Organization.findAll", query = "Select o FROM JpaOrganization o"),
        @NamedQuery(name = "Organization.findById", query = "Select o FROM JpaOrganization o where o.id = :id"),
        @NamedQuery(name = "Organization.findByHost", query = "Select o FROM JpaOrganization o JOIN o.servers s where key(s) = :serverName AND value(s) = :port"),
        @NamedQuery(name = "Organization.getCount", query = "Select COUNT(o) FROM JpaOrganization o") })
public class JpaOrganization implements Organization {

  /** The organizational identifier */
  @Id
  @Column(name = "id", length = 128)
  protected String id;

  /** The friendly name of the organization */
  @Column(name = "name")
  protected String name;

  @ElementCollection
  @MapKeyColumn(name = "name")
  @Column(name = "port")
  @CollectionTable(name = "mh_organization_node", joinColumns = @JoinColumn(name = "organization"))
  protected Map<String, Integer> servers;

  /** The local admin role name */
  @Column(name = "admin_role")
  protected String adminRole;

  /** The local anonymous role name */
  @Column(name = "anonymous_role")
  protected String anonymousRole;

  @ElementCollection
  @MapKeyColumn(name = "name")
  @Column(name = "value")
  @CollectionTable(name = "mh_organization_property", joinColumns = @JoinColumn(name = "organization"))
  protected Map<String, String> properties;

  /**
   * No-arg constructor needed by JPA
   */
  public JpaOrganization() {
  }

  /**
   * Constructs an organization with its attributes.
   *
   * @param orgId
   *          the unique identifier
   * @param name
   *          the friendly name
   * @param serverName
   *          the host name
   * @param serverPort
   *          the host port
   * @param adminRole
   *          name of the local admin role
   * @param anonymousRole
   *          name of the local anonymous role
   * @param properties
   *          arbitrary properties defined for this organization, which might include branding, etc.
   */
  public JpaOrganization(String orgId, String name, String serverName, Integer serverPort, String adminRole,
          String anonymousRole, Map<String, String> properties) {
    super();
    this.id = orgId;
    this.name = name;
    this.servers = new HashMap<String, Integer>();
    this.servers.put(serverName, serverPort);
    this.adminRole = adminRole;
    this.anonymousRole = anonymousRole;
    this.properties = properties;
  }

  /**
   * Constructs an organization with its attributes.
   *
   * @param orgId
   *          the unique identifier
   * @param name
   *          the friendly name
   * @param servers
   *          the servers
   * @param adminRole
   *          name of the local admin role
   * @param anonymousRole
   *          name of the local anonymous role
   * @param properties
   *          arbitrary properties defined for this organization, which might include branding, etc.
   */
  public JpaOrganization(String orgId, String name, Map<String, Integer> servers, String adminRole,
          String anonymousRole, Map<String, String> properties) {
    super();
    this.id = orgId;
    this.name = name;
    this.servers = servers;
    this.adminRole = adminRole;
    this.anonymousRole = anonymousRole;
    this.properties = properties;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getId()
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getAnonymousRole()
   */
  public String getAnonymousRole() {
    return anonymousRole;
  }

  public void setAnonymousRole(String anonymousRole) {
    this.anonymousRole = anonymousRole;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getAdminRole()
   */
  public String getAdminRole() {
    return adminRole;
  }

  public void setAdminRole(String adminRole) {
    this.adminRole = adminRole;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getName()
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getProperties()
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getServers()
   */
  @Override
  public Map<String, Integer> getServers() {
    return servers;
  }

  /**
   * Replaces the existing servers.
   *
   * @param servers
   *          the servers
   */
  public void setServers(Map<String, Integer> servers) {
    this.servers = servers;
  }

  /**
   * Adds the server - port mapping.
   *
   * @param serverName
   *          the server name
   * @param port
   *          the port
   */
  public void addServer(String serverName, Integer port) {
    if (servers == null)
      servers = new HashMap<String, Integer>();
    servers.put(serverName, port);
  }

  /**
   * Removes the given server - port mapping.
   *
   * @param serverName
   *          the server name
   * @param port
   *          the port
   */
  public void remove(String serverName, Integer port) {
    if (port.equals(servers.get(serverName))) {
      servers.remove(serverName);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Organization))
      return false;
    return ((Organization) obj).getId().equals(id);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return EqualsUtil.hash(id);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return id;
  }

}
