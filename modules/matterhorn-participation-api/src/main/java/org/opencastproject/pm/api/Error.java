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
package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notEmpty;

import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;

/**
 * Business object for synchronization error data.
 */
public class Error {

  /** The error identifier */
  private Long id;

  /** The error type */
  private String type;

  /** The error description */
  private String description;

  /** The error source */
  private String source;

  /**
   * Creates an error
   * 
   * @param type
   *          the error type
   * @param description
   *          the error description
   * @param source
   *          the error source
   */
  public Error(String type, String description, String source) {
    this.type = notEmpty(type, "type");
    this.description = notEmpty(description, "description");
    this.source = source;
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the error id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the error id
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the error type
   * 
   * @param type
   *          the type
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Returns the type
   * 
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the error description
   * 
   * @param description
   *          the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the description
   * 
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the error source
   * 
   * @param source
   *          the source
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Returns the source
   * 
   * @return the source
   */
  public String getSource() {
    return source;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Error error = (Error) o;
    return type.equals(error.getType()) && description.equals(error.getDescription())
            && source.equals(error.getSource());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, type, description, source);
  }

  public Obj toJson() {
    return Jsons.obj(Jsons.p("id", id), Jsons.p("type", type), Jsons.p("description", description),
            Jsons.p("source", source));
  }
}
