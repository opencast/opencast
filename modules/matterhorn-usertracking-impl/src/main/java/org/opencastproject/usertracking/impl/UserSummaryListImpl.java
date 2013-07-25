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
package org.opencastproject.usertracking.impl;

import org.opencastproject.usertracking.api.UserSummary;
import org.opencastproject.usertracking.api.UserSummaryList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A {@link List} of {@link UserSummaryList}s
 */
@XmlType(name = "summaries", namespace = "http://usertracking.opencastproject.org")
@XmlRootElement(name = "summaries", namespace = "http://usertracking.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserSummaryListImpl implements UserSummaryList {

  @XmlAttribute(name = "total")
  protected int total;

  @XmlElement(name = "summary", namespace = "http://usertracking.opencastproject.org")
  protected List<UserSummaryImpl> userSummaries;

  public void add(UserSummary userSummary) {
    boolean existingUser = false;
    for (UserSummary current : userSummaries) {
      if (userSummary.getUserId().equals(current.getUserId())) {
        current.combine(userSummary);
        existingUser = true;
      }
    }
    if (!existingUser) {
      userSummaries.add((UserSummaryImpl) userSummary);
      total++;
    }
  }

  public void add(Collection<UserSummary> newUserSummaries) {
    for (UserSummary userSummary : newUserSummaries) {
      add((UserSummaryImpl)userSummary);
    }
  }
  
  /**
   * A no-arg constructor needed by JAXB
   */
  public UserSummaryListImpl() {
    this.userSummaries = new ArrayList<UserSummaryImpl>();
  }

  public int getTotal() {
    return total;
  }
  
  public List<UserSummary> getUserSummaries() {
    List<UserSummary> result = new LinkedList<UserSummary>();
    for (UserSummaryImpl userActionImpl : userSummaries) {
      result.add(userActionImpl);
    }
    return result;
  }
}
