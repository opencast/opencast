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
package org.opencastproject.job.api;

import static org.opencastproject.util.data.Collections.nullToNil;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.serviceregistry.api.IncidentServiceException;
import org.opencastproject.util.NotFoundException;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentList", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incidentList", namespace = "http://job.opencastproject.org")
public final class JaxbIncidentList {
  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
  private List<JaxbIncident> incidents;

  /** Default constructor needed by jaxb */
  public JaxbIncidentList() {
  }

  public JaxbIncidentList(List<Incident> incidents)
          throws IncidentServiceException, NotFoundException {
    this.incidents = mlist(incidents).map(JaxbIncident.mkFn).value();
  }

  public List<Incident> toIncidents() {
    return mlist(nullToNil(incidents)).map(JaxbIncident.toIncidentFn).value();
  }
}
