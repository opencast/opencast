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
 * A wrapper for service registration collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "services", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "services", namespace = "http://serviceregistry.opencastproject.org")
public class JaxbServiceRegistrationList {
  /** A list of search items. */
  @XmlElement(name = "service")
  protected List<JaxbServiceRegistration> registrations = new ArrayList<JaxbServiceRegistration>();

  public JaxbServiceRegistrationList() {
  }

  public JaxbServiceRegistrationList(JaxbServiceRegistration registration) {
    this.registrations.add(registration);
  }

  public JaxbServiceRegistrationList(Collection<JaxbServiceRegistration> registrations) {
    for (JaxbServiceRegistration stat : registrations)
      this.registrations.add((JaxbServiceRegistration) stat);
  }

  /**
   * @return the registrations
   */
  public List<JaxbServiceRegistration> getRegistrations() {
    return registrations;
  }

  /**
   * @param registrations
   *          the registrations to set
   */
  public void setStats(List<JaxbServiceRegistration> registrations) {
    this.registrations = registrations;
  }

  public void add(ServiceRegistration registration) {
    if (registration instanceof JaxbServiceRegistration) {
      registrations.add((JaxbServiceRegistration) registration);
    } else {
      throw new IllegalArgumentException("Service registrations must be an instance of JaxbServiceRegistration");
    }
  }
}
