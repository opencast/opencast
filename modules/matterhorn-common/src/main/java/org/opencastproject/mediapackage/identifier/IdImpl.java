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

package org.opencastproject.mediapackage.identifier;

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

  /** The identifier */
  @XmlValue
  protected String id = null;

  /**
   * Needed for JAXB serialization
   */
  public IdImpl() {
  }

  /**
   * Creates a new serial identifier as created by {@link SerialIdBuilder}.
   *
   * @param id
   *          the identifier
   */
  public IdImpl(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.identifier.Id#compact()
   */
  public String compact() {
    return id.replaceAll("/", "-").replaceAll("\\\\", "-");
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
}
