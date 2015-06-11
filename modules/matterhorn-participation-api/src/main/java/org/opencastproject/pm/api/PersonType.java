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
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Val;

/**
 * Business object for a person type.
 */
public class PersonType {

  /** The person type identifier */
  private Long id;

  /** The name */
  private String name;

  /** The function */
  private String function;

  /**
   * Creates a person type
   * 
   * @param name
   *          the name
   * @param function
   *          the function
   */
  public PersonType(String name, String function) {
    this.name = notEmpty(name, "name");
    this.function = notEmpty(function, "function");
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the person type id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the person type id
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
   * Sets the function
   * 
   * @param function
   *          the function
   */
  public void setFunction(String function) {
    this.function = function;
  }

  /**
   * Returns the function
   * 
   * @return the function
   */
  public String getFunction() {
    return function;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    PersonType type = (PersonType) o;
    return name.equals(type.getName()) && function.equals(type.getFunction());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, name, function);
  }

  @Override
  public String toString() {
    return "PersonType: " + name;
  }

  public Val toJson() {
    return Jsons.obj(Jsons.p("id", id), Jsons.p("name", name), Jsons.p("function", function));
  }

}
