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
 * Business object for sources.
 */
public class SchedulingSource {

  /** The source id */
  private Long id;

  /** The source source */
  private String source;

  /**
   * Creates a source
   * 
   * @param source
   *          the source
   */
  public SchedulingSource(String source) {
    this.source = notEmpty(source, "source");
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the source id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the source id
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the source
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
    SchedulingSource source = (SchedulingSource) o;
    return source.equals(source.getSource());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, source);
  }

  @Override
  public String toString() {
    return "scheduling source:" + id + "|" + source;
  }

}
