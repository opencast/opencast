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
package org.opencastproject.security.api;

import org.opencastproject.util.EqualsUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * An organization that is hosted on this Matterhorn instance.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "organization", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "organization", namespace = "http://org.opencastproject.security")
public class JaxbOrganization implements Organization {

  /** The organizational identifier */
  @XmlID
  @XmlAttribute
  protected String id = null;

  /** The friendly name of the organization */
  @XmlElement(name = "name")
  protected String name = null;

  /** Server and port mapping */
  @XmlElement(name = "server")
  @XmlElementWrapper(name = "servers")
  protected List<OrgServer> servers = null;

  /** The local admin role name */
  @XmlElement(name = "adminRole")
  protected String adminRole = null;

  /** The local anonymous role name */
  @XmlElement(name = "anonymousRole")
  protected String anonymousRole = null;

  /** Arbitrary string properties associated with this organization */
  @XmlElement(name = "property")
  @XmlElementWrapper(name = "properties")
  protected List<OrgProperty> properties = null;

  /**
   * No-arg constructor needed by JAXB
   */
  public JaxbOrganization() {
  }

  public JaxbOrganization(String orgId) {
    this.id = orgId;
  }

  /**
   * Constructs an organization with its attributes.
   * 
   * @param id
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
  public JaxbOrganization(String id, String name, Map<String, Integer> servers, String adminRole, String anonymousRole,
          Map<String, String> properties) {
    this();
    this.id = id;
    this.name = name;
    this.servers = new ArrayList<JaxbOrganization.OrgServer>();
    if (servers != null && !servers.isEmpty()) {
      for (Entry<String, Integer> entry : servers.entrySet()) {
        this.servers.add(new OrgServer(entry.getKey(), entry.getValue()));
      }
    }
    this.adminRole = adminRole;
    this.anonymousRole = anonymousRole;
    this.properties = new ArrayList<JaxbOrganization.OrgProperty>();
    if (properties != null && !properties.isEmpty()) {
      for (Entry<String, String> entry : properties.entrySet()) {
        this.properties.add(new OrgProperty(entry.getKey(), entry.getValue()));
      }
    }
  }

  /**
   * Constructs an organization from an organization
   * 
   * @param org
   *          the organization
   */
  public static JaxbOrganization fromOrganization(Organization org) {
    if (org instanceof JaxbOrganization)
      return (JaxbOrganization) org;
    return new JaxbOrganization(org.getId(), org.getName(), org.getServers(), org.getAdminRole(),
            org.getAnonymousRole(), org.getProperties());
  }

  /**
   * @see org.opencastproject.security.api.Organization#getId()
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getServers()
   */
  @Override
  public Map<String, Integer> getServers() {
    Map<String, Integer> map = new HashMap<String, Integer>();
    if (servers != null) {
      for (OrgServer server : servers) {
        map.put(server.getName(), server.getPort());
      }
    }
    return map;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getId()
   */
  @Override
  public String getAdminRole() {
    return adminRole;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getId()
   */
  @Override
  public String getAnonymousRole() {
    return anonymousRole;
  }

  /**
   * @see org.opencastproject.security.api.Organization#getProperties()
   */
  @Override
  public Map<String, String> getProperties() {
    Map<String, String> map = new HashMap<String, String>();
    for (OrgProperty prop : properties) {
      map.put(prop.getKey(), prop.getValue());
    }
    return map;
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
   * An organization property. To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "server", namespace = "http://org.opencastproject.security")
  public static class OrgServer {

    /** The server name */
    @XmlAttribute
    protected String name;

    /** The server port */
    @XmlAttribute
    protected int port;

    /**
     * No-arg constructor needed by JAXB
     */
    public OrgServer() {
    }

    /**
     * Constructs an organization server mapping with a server name and a port.
     * 
     * @param name
     *          the name
     * @param port
     *          the port
     */
    public OrgServer(String name, int port) {
      this.name = name;
      this.port = port;
    }

    /**
     * @return the server name
     */
    public String getName() {
      return name;
    }

    /**
     * @return the port
     */
    public int getPort() {
      return port;
    }
  }

  /**
   * An organization property. To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "property", namespace = "http://org.opencastproject.security")
  public static class OrgProperty {

    /** The property key */
    @XmlAttribute
    protected String key;

    /** The property value */
    @XmlValue
    protected String value;

    /**
     * No-arg constructor needed by JAXB
     */
    public OrgProperty() {
    }

    /**
     * Constructs an organization property with a key and a value.
     * 
     * @param key
     *          the key
     * @param value
     *          the value
     */
    public OrgProperty(String key, String value) {
      this.key = key;
      this.value = value;
    }

    /**
     * @return the key
     */
    public String getKey() {
      return key;
    }

    /**
     * @return the value
     */
    public String getValue() {
      return value;
    }
  }

}
