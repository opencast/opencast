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

package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notEmpty;

import org.opencastproject.util.EqualsUtil;

/**
 * Business object for an action.
 */
public class Action {

  /** The action id */
  private Long id;

  /** The action name */
  private String name;

  /** The Matterhorn operation handler */
  private String handler;

  /**
   * Creates an action
   * 
   * @param name
   *          the name
   * @param handler
   *          the handler
   */
  public Action(String name, String handler) {
    this.handler = notEmpty(handler, "name");
    this.name = notEmpty(handler, "handler");
  }

  /**
   * Creates an action
   * 
   * @param id
   *          the id
   * @param name
   *          the name
   * @param handler
   *          the handler
   */
  public Action(Long id, String name, String handler) {
    this(name, handler);
    this.id = id;
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the action id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the action id
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the name
   * 
   * @param name
   *          the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the name
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the handler
   * 
   * @param handler
   *          the handler
   */
  public void setHandler(String handler) {
    this.handler = handler;
  }

  /**
   * Returns the handler
   * 
   * @return the handler
   */
  public String getHandler() {
    return handler;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Action action = (Action) o;
    return name.equals(action.getName()) && handler.equals(action.getHandler());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, name, handler);
  }

  @Override
  public String toString() {
    return "Action:" + id + "|" + name;
  }

}
