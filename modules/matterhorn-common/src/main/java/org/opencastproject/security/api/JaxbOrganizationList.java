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

package org.opencastproject.security.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A JAXB-annotated list of organizations.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "organizations", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "organizations", namespace = "http://org.opencastproject.security")
public class JaxbOrganizationList {

  /** The list of organizations */
  @XmlElement(name = "organization")
  protected List<JaxbOrganization> organizations = new ArrayList<JaxbOrganization>();

  /**
   * No arg constructor needed by JAXB
   */
  public JaxbOrganizationList() {
  }

  /**
   * Constructs a new OrganizationList wrapper from a list of organizations.
   *
   * @param organizations
   *          the list or organizations
   */
  public JaxbOrganizationList(List<JaxbOrganization> organizations) {
    this.organizations = organizations;
  }

  /**
   * @return the organizations
   */
  public List<JaxbOrganization> getOrganizations() {
    return organizations;
  }

  /**
   * @param organizations
   *          the organizations to set
   */
  public void setOrganizations(List<JaxbOrganization> organizations) {
    this.organizations = organizations;
  }

  public void add(Organization org) {
    if (org instanceof JaxbOrganization) {
      organizations.add((JaxbOrganization) org);
    } else {
      organizations.add(JaxbOrganization.fromOrganization(org));
    }
  }

}
