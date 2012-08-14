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
package org.opencastproject.serviceregistry.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A wrapper for host registration collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "hosts", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "hosts", namespace = "http://serviceregistry.opencastproject.org")
public class JaxbHostRegistrationList {

  /** A list of search items. */
  @XmlElement(name = "host")
  protected List<JaxbHostRegistration> registrations = new ArrayList<JaxbHostRegistration>();

  public JaxbHostRegistrationList() {
  }

  public JaxbHostRegistrationList(JaxbHostRegistration registration) {
    this.registrations.add(registration);
  }

  public JaxbHostRegistrationList(Collection<JaxbHostRegistration> registrations) {
    for (JaxbHostRegistration reg : registrations)
      this.registrations.add(reg);
  }

  /**
   * @return the registrations
   */
  public List<JaxbHostRegistration> getRegistrations() {
    return registrations;
  }

  /**
   * @param registrations
   *          the registrations to set
   */
  public void setRegistrations(List<JaxbHostRegistration> registrations) {
    this.registrations = registrations;
  }

  public void add(HostRegistration registration) {
    if (registration instanceof JaxbHostRegistration) {
      registrations.add((JaxbHostRegistration) registration);
    } else {
      throw new IllegalArgumentException("Service registrations must be an instance of JaxbHostRegistration");
    }
  }

}
