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

import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.IncidentServiceException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;

import java.util.List;
import java.util.Locale;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentDigestTree", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incidentDigestTree", namespace = "http://job.opencastproject.org")
public final class JaxbIncidentDigestTree {
  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
  private List<JaxbIncidentDigest> incidents;

  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_TREE)
  private List<JaxbIncidentDigestTree> descendants;

  /** Constructor for JAXB */
  public JaxbIncidentDigestTree() {
  }

  public JaxbIncidentDigestTree(IncidentService svc, Locale locale, IncidentTree tree) throws IncidentServiceException,
          NotFoundException {
    this.incidents = mlist(tree.getIncidents()).map(JaxbIncidentDigest.mkFn(svc, locale)).value();
    this.descendants = mlist(tree.getDescendants()).map(mkFn(svc, locale)).value();
  }

  public static Function<IncidentTree, JaxbIncidentDigestTree> mkFn(final IncidentService svc, final Locale locale) {
    return new Function.X<IncidentTree, JaxbIncidentDigestTree>() {
      @Override
      public JaxbIncidentDigestTree xapply(IncidentTree tree) throws Exception {
        return new JaxbIncidentDigestTree(svc, locale, tree);
      }
    };
  }
}
