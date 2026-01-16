/*
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

package org.opencastproject.job.api;

import static org.opencastproject.util.data.Collections.nullToNil;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.serviceregistry.api.IncidentServiceException;
import org.opencastproject.util.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/** 1:1 serialization of a {@link IncidentTreeImpl}. */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "incidentTree", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incidentTree", namespace = "http://job.opencastproject.org")
public final class JaxbIncidentTree {
  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
  private List<JaxbIncident> incidents;

  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_TREE)
  private List<JaxbIncidentTree> descendants;

  /** Constructor for JAXB */
  public JaxbIncidentTree() {
  }

  public JaxbIncidentTree(IncidentTree tree) throws IncidentServiceException, NotFoundException {
    this.incidents = tree.getIncidents().stream()
        .map(JaxbIncident::new)
        .collect(Collectors.toList());

    this.descendants = tree.getDescendants().stream()
        .map(t -> {
          try {
            return new JaxbIncidentTree(t);
          } catch (Exception e) {
            return chuck(e);
          }
        })
        .collect(Collectors.toList());
  }

  public IncidentTree toIncidentTree() {
    List<Incident> mappedIncidents = nullToNil(incidents).stream()
        .map(JaxbIncident::toIncident)
        .collect(Collectors.toList());

    List<IncidentTree> mappedDescendants = nullToNil(descendants).stream()
        .map(JaxbIncidentTree::toIncidentTree)
        .collect(Collectors.toList());

    return new IncidentTreeImpl(mappedIncidents, mappedDescendants);
  }
}
