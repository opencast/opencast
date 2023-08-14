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


package org.opencastproject.mediapackage.identifier;

import java.util.UUID;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Simple and straightforward implementation of the {@link Id} interface.
 */
@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class IdImpl implements Id {

  private static final Pattern pattern = Pattern.compile("[\\w-_.:;()]+");

  /** The identifier */
  @XmlValue
  protected String id = null;

  /**
   * Needed for JAXB serialization
   */
  public IdImpl() {
  }

  /**
   * Creates a new identifier.
   *
   * @param id
   *          the identifier
   */
  public IdImpl(final String id) {
    if (!pattern.matcher(id).matches()) {
      throw new IllegalArgumentException("Id must match " + pattern);
    }
    this.id = id;
  }

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
  public boolean equals(Object o) {
    if (o instanceof IdImpl) {
      IdImpl other = (IdImpl) o;
      return id != null && other.id != null && id.equals(other.id);
    }
    return false;
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
   * Generate a new UUID-based Id.
   * @return New Id
   */
  public static Id fromUUID() {
    return new IdImpl(UUID.randomUUID().toString());
  }
}
