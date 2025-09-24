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

import org.opencastproject.serviceregistry.api.IncidentServiceException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;

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
        .map(JaxbIncident.mkFn::apply)
        .collect(Collectors.toList());

    this.descendants = tree.getDescendants().stream()
        .map(mkFn::apply)
        .collect(Collectors.toList());
  }

  public static final Function<IncidentTree, JaxbIncidentTree> mkFn = new Function.X<IncidentTree, JaxbIncidentTree>() {
    @Override public JaxbIncidentTree xapply(IncidentTree tree) throws Exception {
      return new JaxbIncidentTree(tree);
    }
  };

  public IncidentTree toIncidentTree() {
    List<Incident> mappedIncidents = nullToNil(incidents).stream()
        .map(JaxbIncident.toIncidentFn::apply)
        .collect(Collectors.toList());

    List<IncidentTree> mappedDescendants = nullToNil(descendants).stream()
        .map(toIncidentTreeFn::apply)
        .collect(Collectors.toList());

    return new IncidentTreeImpl(mappedIncidents, mappedDescendants);
  }

  public static final Function<JaxbIncidentTree, IncidentTree> toIncidentTreeFn = new Function<JaxbIncidentTree, IncidentTree>() {
    @Override public IncidentTree apply(JaxbIncidentTree dto) {
      return dto.toIncidentTree();
    }
  };
}
