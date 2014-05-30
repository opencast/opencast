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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * A JAXB-annotated list of organizations.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "organizations", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "organizations", namespace = "http://org.opencastproject.security")
public class OrganizationList {

  /** The list of organizations */
  @XmlElement(name = "organization")
  protected List<Organization> organizations;

  /**
   * No arg constructor needed by JAXB
   */
  public OrganizationList() {
  }

  /**
   * Constructs a new OrganizationList wrapper from a list of organizations.
   *
   * @param organizations
   *          the list or organizations
   */
  public OrganizationList(List<Organization> organizations) {
    this.organizations = organizations;
  }

  /**
   * @return the organizations
   */
  public List<Organization> getOrganizations() {
    return organizations;
  }

  /**
   * @param organizations
   *          the organizations to set
   */
  public void setOrganizations(List<Organization> organizations) {
    this.organizations = organizations;
  }
}
