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
package org.opencastproject.userdirectory;

import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.util.EqualsUtil;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A simple user model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "group", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "group", namespace = "http://org.opencastproject.security")
public final class JaxbGroup implements Group {

  @XmlElement(name = "id")
  protected String groupId;

  @XmlElement(name = "organization")
  protected JaxbOrganization organization;

  @XmlElement(name = "name")
  protected String name;

  @XmlElement(name = "description")
  protected String description;

  @XmlElement(name = "role")
  protected String role;

  @XmlElement(name = "member")
  @XmlElementWrapper(name = "members")
  protected Set<String> members;

  @XmlElement(name = "role")
  @XmlElementWrapper(name = "roles")
  protected Set<JaxbRole> roles;

  /**
   * No-arg constructor needed by JAXB
   */
  public JaxbGroup() {
  }

  /**
   * Constructs a group with the specified groupId, name, description and group role.
   *
   * @param groupId
   *          the group id
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param description
   *          the description
   */
  public JaxbGroup(String groupId, JaxbOrganization organization, String name, String description) {
    super();
    this.groupId = groupId;
    this.organization = organization;
    this.name = name;
    this.description = description;
    this.role = ROLE_PREFIX + groupId.toUpperCase();
    this.roles = new HashSet<JaxbRole>();
  }

  /**
   * Constructs a group with the specified groupId, name, description, group role and roles.
   *
   * @param groupId
   *          the group id
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param description
   *          the description
   * @param roles
   *          the additional group roles
   */
  public JaxbGroup(String groupId, JaxbOrganization organization, String name, String description, Set<JaxbRole> roles) {
    this(groupId, organization, name, description);
    this.roles = roles;
  }

  /**
   * Constructs a group with the specified groupId, name, description, group role and roles.
   *
   * @param groupId
   *          the group id
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param description
   *          the description
   * @param roles
   *          the additional group roles
   * @param members
   *          the group members
   */
  public JaxbGroup(String groupId, JaxbOrganization organization, String name, String description, Set<JaxbRole> roles,
          Set<String> members) {
    this(groupId, organization, name, description, roles);
    this.members = members;
  }

  public static JaxbGroup fromGroup(Group group) {
    JaxbOrganization organization = JaxbOrganization.fromOrganization(group.getOrganization());
    Set<JaxbRole> roles = new HashSet<JaxbRole>();
    for (Role role : group.getRoles()) {
      if (role instanceof JaxbRole)
        roles.add((JaxbRole) role);
      roles.add(JaxbRole.fromRole(role));
    }
    return new JaxbGroup(group.getGroupId(), organization, group.getName(), group.getDescription(), roles,
            group.getMembers());
  }

  /**
   * @see org.opencastproject.security.api.Group#getGroupId()
   */
  @Override
  public String getGroupId() {
    return groupId;
  }

  /**
   * @see org.opencastproject.security.api.Group#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @see org.opencastproject.security.api.Group#getOrganization()
   */
  @Override
  public Organization getOrganization() {
    return organization;
  }

  /**
   * @see org.opencastproject.security.api.Group#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * @see org.opencastproject.security.api.Group#getRole()
   */
  @Override
  public String getRole() {
    return role;
  }

  /**
   * @see org.opencastproject.security.api.Group#getMembers()
   */
  @Override
  public Set<String> getMembers() {
    return members;
  }

  /**
   * @see org.opencastproject.security.api.Group#getRoles()
   */
  @Override
  public Set<Role> getRoles() {
    return new HashSet<Role>(roles);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return EqualsUtil.hash(groupId, organization);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Group))
      return false;
    Group other = (Group) obj;
    return groupId.equals(other.getGroupId()) && organization.equals(other.getOrganization());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuilder(groupId).append(":").append(organization).toString();
  }

}
