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

import org.opencastproject.util.jaxb.JaxbParser;

import java.io.IOException;
import java.io.InputStream;

/** JAXB parser for JAXB DTOs of {@link Incident}. */
public final class IncidentParser extends JaxbParser {
  /** Instance of IncidentParser */
  public static final IncidentParser I =
          new IncidentParser("org.opencastproject.job.api:org.opencastproject.serviceregistry.api");

  private IncidentParser(String contextPath) {
    super(contextPath);
  }

  public JaxbIncidentDigestList parseDigestFromXml(InputStream xml) throws IOException {
    return unmarshal(JaxbIncidentDigestList.class, xml);
  }

  public JaxbIncident parseIncidentFromXml(InputStream xml) throws IOException {
    return unmarshal(JaxbIncident.class, xml);
  }

  public JaxbIncidentList parseIncidentsFromXml(InputStream xml) throws IOException {
    return unmarshal(JaxbIncidentList.class, xml);
  }

  public JaxbIncidentTree parseIncidentTreeFromXml(InputStream xml) throws IOException {
    return unmarshal(JaxbIncidentTree.class, xml);
  }

  public String toXml(JaxbIncident incident) throws IOException {
    return marshal(incident);
  }

  public String toXml(JaxbIncidentTree tree) throws IOException {
    return marshal(tree);
  }
}
