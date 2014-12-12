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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A wrapper for user collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "users", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "users", namespace = "http://org.opencastproject.security")
public class JaxbUserList {

  /** A list of users. */
  @XmlElement(name = "user")
  protected List<JaxbUser> users = new ArrayList<JaxbUser>();

  public JaxbUserList() {
  }

  public JaxbUserList(JaxbUser user) {
    users.add(user);
  }

  public JaxbUserList(Collection<JaxbUser> users) {
    for (JaxbUser user : users)
      this.users.add(user);
  }

  /**
   * @return the users
   */
  public List<JaxbUser> getUsers() {
    return users;
  }

  /**
   * @param users
   *          the users to set
   */
  public void setUsers(List<JaxbUser> users) {
    this.users = users;
  }

  public void add(User user) {
    if (user instanceof JaxbUser) {
      users.add((JaxbUser) user);
    } else {
      users.add(JaxbUser.fromUser(user));
    }
  }

}
