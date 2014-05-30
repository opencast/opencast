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
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A list of {@link AccessControlEntry}s.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "acl", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "acl", namespace = "http://org.opencastproject.security")
public final class AccessControlList {

  /** The list of access control entries */
  @XmlElement(name = "ace")
  private List<AccessControlEntry> entries;

  /**
   * No-arg constructor needed by JAXB
   */
  public AccessControlList() {
    this.entries = new ArrayList<AccessControlEntry>();
  }

  public AccessControlList(AccessControlEntry... entries) {
    this.entries = new ArrayList<AccessControlEntry>(Arrays.asList(entries));
  }

  public AccessControlList(List<AccessControlEntry> entries) {
    this.entries = new ArrayList<AccessControlEntry>(entries);
  }

  /**
   * @return the entries
   */
  public List<AccessControlEntry> getEntries() {
    return entries;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return entries.toString();
  }

}
