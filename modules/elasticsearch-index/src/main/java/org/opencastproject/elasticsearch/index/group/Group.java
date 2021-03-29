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

package org.opencastproject.elasticsearch.index.group;

import org.opencastproject.elasticsearch.index.IndexObject;
import org.opencastproject.security.api.JaxbGroup;
import org.opencastproject.util.IoSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.stream.StreamSource;

/**
 * Object wrapper for a group.
 */
@XmlType(name = "group", namespace = IndexObject.INDEX_XML_NAMESPACE, propOrder = { "identifier", "name",
        "description", "organization", "role", "roles", "members" })
@XmlRootElement(name = "group", namespace = IndexObject.INDEX_XML_NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class Group implements IndexObject {

  /** The name of the surrounding XML tag to wrap a result of multiple groups */
  public static final String XML_SURROUNDING_TAG = "groups";

  /** The document type */
  public static final String DOCUMENT_TYPE = "group";

  /** The identifier */
  @XmlElement(name = "identifier")
  private String identifier = null;

  /** The name for the group */
  @XmlElement(name = "name")
  private String name = null;

  /** The name of the group role */
  @XmlElement(name = "role")
  private String role = null;

  /** The description */
  @XmlElement(name = "description")
  private String description = null;

  /** The organization for the group */
  @XmlElement(name = "organization")
  private String organization = null;

  @XmlElementWrapper(name = "roles")
  @XmlElement(name = "role")
  private Set<String> roles = null;

  @XmlElementWrapper(name = "members")
  @XmlElement(name = "member")
  private Set<String> members = null;

  /** Context for serializing and deserializing */
  private static JAXBContext context = null;

  /**
   * Required default no arg constructor for JAXB.
   */
  public Group() {

  }

  public Group(String identifier, String organization) {
    this.identifier = identifier;
    this.organization = organization;
    this.role = JaxbGroup.ROLE_PREFIX + identifier.toUpperCase();
  }

  /**
   * Create a new group with all of the properties populated.
   *
   * @param identifier
   *          The identifier for this group
   * @param name
   *          The easy to read name of this group
   * @param description
   *          The description of the group
   * @param organization
   *          The organization applying to this group
   * @param roles
   *          The roles that are given with being in this group
   * @param members
   *          The user names of the current members of the group.
   */
  public Group(String identifier, String name, String description, String organization, Set<String> roles,
          Set<String> members) {
    this.identifier = identifier;
    this.name = name;
    this.description = description;
    this.organization = organization;
    this.role = JaxbGroup.ROLE_PREFIX + identifier.toUpperCase();
    this.roles = roles;
    this.members = members;
  }

  public String getIdentifier() {
    return identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRole() {
    return role;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Add role to this group
   *
   * @param role
   *          The role.
   */
  public void addRole(String role) {
    if (roles == null) {
      roles = new HashSet<String>();
    }
    roles.add(role);
  }

  /**
   * Sets the list of roles.
   *
   * @param roles
   *          the roles for this group
   */
  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }

  /**
   * Returns the group's roles.
   *
   * @return the roles
   */
  public Set<String> getRoles() {
    return roles;
  }

  /**
   * Add a member
   *
   * @param member
   *          The member's name.
   */
  public void addMember(String member) {
    if (members == null) {
      members = new HashSet<String>();
    }
    members.add(member);
  }

  /**
   * Sets the list of members.
   *
   * @param members
   *          the members for this group
   */
  public void setMembers(Set<String> members) {
    this.members = members;
  }

  /**
   * Returns the group members.
   *
   * @return the members
   */
  public Set<String> getMembers() {
    return members;
  }

  /**
   * Create an unmarshaller for groups, which can be re-used for performance (from a single thread)
   * @return An unmarshaller for groups
   * @throws IOException if something went from with the creation
   */
  public static Unmarshaller createUnmarshaller() throws IOException {
    try {
      if (context == null) {
        createJAXBContext();
      }
      return context.createUnmarshaller();
    } catch (JAXBException e) {
      throw new IOException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Reads the group from the input stream.
   *
   * @param xml
   *          the input stream
   * @return the deserialized group
   * @throws IOException
   */
  public static Group valueOf(InputStream xml, Unmarshaller unmarshaller) throws IOException {
    try {
      if (context == null) {
        createJAXBContext();
      }
      return unmarshaller.unmarshal(new StreamSource(xml), Group.class).getValue();
    } catch (JAXBException e) {
      throw new IOException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } finally {
      IoSupport.closeQuietly(xml);
    }
  }

  /**
   * Initialize the JAXBContext.
   */
  private static void createJAXBContext() throws JAXBException {
    context = JAXBContext.newInstance(Group.class);
  }

  /**
   * Serializes the group to an XML format.
   *
   * @return A String with this group' content as XML.
   */
  public String toXML() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      StringWriter writer = new StringWriter();
      Marshaller marshaller = Group.context.createMarshaller();
      marshaller.marshal(this, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
