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

import org.apache.commons.lang.StringUtils;

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
public class Organization {

  /** The organizational identifier */
  @XmlID
  @XmlAttribute
  protected String id = null;

  /** The friendly name of the organization */
  protected String name = null;

  /** The host name */
  protected String serverName = null;

  /** The host port */
  protected int serverPort = 80;

  /** The local admin role name */
  protected String adminRole = null;

  /** The local anonymous role name */
  protected String anonymousRole = null;

  /** Arbitrary string properties associated with this organization */
  @XmlElement(name = "property")
  @XmlElementWrapper(name = "properties")
  protected List<OrgProperty> properties = null;

  /**
   * No-arg constructor needed by JAXB
   */
  public Organization() {
  }

  /**
   * Constructs an organization with its attributes and a default server port of <code>80</code>.
   * 
   * @param id
   *          the unique identifier
   * @param name
   *          the friendly name
   * @param serverName
   *          the host name
   * @param adminRole
   *          name of the local admin role
   * @param anonymousRole
   *          name of the local anonymous role
   */
  public Organization(String id, String name, String serverName, String adminRole, String anonymousRole) {
    this(id, name, serverName, 80, adminRole, anonymousRole, null);
  }

  /**
   * Constructs an organization with its attributes and a default server port of <code>80</code>.
   * 
   * @param id
   *          the unique identifier
   * @param name
   *          the friendly name
   * @param serverName
   *          the host name
   * @param adminRole
   *          name of the local admin role
   * @param anonymousRole
   *          name of the local anonymous role
   * @param properties
   *          arbitrary properties defined for this organization, which might include branding, etc.
   */
  public Organization(String id, String name, String serverName, String adminRole, String anonymousRole,
          Map<String, String> properties) {
    this(id, name, serverName, 80, adminRole, anonymousRole, properties);
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
  public Organization(String id, String name, String serverName, int serverPort, String adminRole,
          String anonymousRole, Map<String, String> properties) {
    this();
    this.id = id;
    this.name = name;
    this.serverName = StringUtils.strip(serverName, "/");
    this.serverPort = serverPort;
    this.adminRole = adminRole;
    this.anonymousRole = anonymousRole;
    this.properties = new ArrayList<Organization.OrgProperty>();
    if (properties != null && !properties.isEmpty()) {
      for (Entry<String, String> entry : properties.entrySet()) {
        this.properties.add(new OrgProperty(entry.getKey(), entry.getValue()));
      }
    }
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the serverName
   */
  public String getServerName() {
    return serverName;
  }

  /**
   * @return the serverPort
   */
  public int getServerPort() {
    return serverPort;
  }

  /**
   * Returns the name for the local admin role.
   * 
   * @return the admin role name
   */
  public String getAdminRole() {
    return adminRole;
  }

  /**
   * Returns the name for the local anonymous role.
   * 
   * @return the anonymous role name
   */
  public String getAnonymousRole() {
    return anonymousRole;
  }

  /**
   * Returns the organizational properties
   * 
   * @return the properties
   */
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
    return ((Organization) obj).id.equals(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * An organization property. To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "property", namespace = "org.opencastproject.security")
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
