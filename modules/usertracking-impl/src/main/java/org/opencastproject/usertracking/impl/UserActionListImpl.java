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

package org.opencastproject.usertracking.impl;

import org.opencastproject.usertracking.api.UserAction;
import org.opencastproject.usertracking.api.UserActionList;

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
 * A {@link List} of {@link UserActionList}s
 */
@XmlType(name = "actions", namespace = "http://usertracking.opencastproject.org")
@XmlRootElement(name = "actions", namespace = "http://usertracking.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserActionListImpl implements UserActionList {

  @XmlAttribute(name = "total")
  private int total;

  @XmlAttribute(name = "offset")
  private int offset;

  @XmlAttribute(name = "limit")
  private int limit;

  @XmlElement(name = "action", namespace = "http://usertracking.opencastproject.org")
  private List<UserActionImpl> actions;

  public void add(UserAction annotation) {
    actions.add((UserActionImpl) annotation);
  }

  public void add(Collection<UserAction> userActions) {
    for (UserAction userAction : userActions) {
      add((UserActionImpl)userAction);
    }
  }

  /**
   * A no-arg constructor needed by JAXB
   */
  public UserActionListImpl() {
    this.actions = new ArrayList<UserActionImpl>();
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public int getTotal() {
    return total;
  }

  public int getLimit() {
    return limit;
  }

  public int getOffset() {
    return offset;
  }

  public List<UserAction> getUserActions() {
    List<UserAction> userActions = new LinkedList<UserAction>();
    for (UserActionImpl userActionImpl : actions) {
      userActions.add(userActionImpl);
    }
    return userActions;
  }
}
