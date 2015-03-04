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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.opencastproject.fun.juc.Immutables.list;
import static org.opencastproject.fun.juc.Immutables.map;
import static org.opencastproject.util.IoSupport.loadFileFromClassPathAsString;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.xmlmatchers.XmlMatchers.similarTo;
import static org.xmlmatchers.transform.XmlConverters.the;

import org.opencastproject.fun.juc.Immutables;
import org.opencastproject.job.api.Incident.Severity;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.util.Date;

public class IncidentParserTest {
  @Test
  public void testSerializationOfJaxbIncident() throws Exception {
    final Incident incident = new IncidentImpl(1, 2, "service", "localhost", new Date(0), Severity.FAILURE, "code",
            list(tuple("title", "content"), tuple("Another title", "...and even more content")), map(tuple("key",
                    "value")));
    final String marshaled = IncidentParser.I.toXml(new JaxbIncident(incident));
    System.out.println(marshaled);
    assertThat(the(marshaled),
            similarTo(the(loadFileFromClassPathAsString("/org/opencastproject/job/api/expected-incident-1.xml").get())));
    final Incident unmarshaled = IncidentParser.I.parseIncidentFromXml(IOUtils.toInputStream(marshaled)).toIncident();
    assertEquals(incident, unmarshaled);
  }

  @Test
  public void testSerializationOfJaxbIncidentTree() throws Exception {
    final IncidentTree tree = new IncidentTreeImpl(list(incident(1), incident(2)),
            Immutables.<IncidentTree> list(new IncidentTreeImpl(list(incident(3)), Immutables
                    .<IncidentTree> list(new IncidentTreeImpl(list(incident(4), incident(5)), Immutables
                            .<IncidentTree> nil())))));
    final String marshaled = IncidentParser.I.toXml(new JaxbIncidentTree(tree));
    System.out.println(marshaled);
    assertThat(the(marshaled),
            similarTo(the(loadFileFromClassPathAsString("/org/opencastproject/job/api/expected-incident-tree-1.xml")
                    .get())));
    final IncidentTree unmarshaled = IncidentParser.I.parseIncidentTreeFromXml(IOUtils.toInputStream(marshaled))
            .toIncidentTree();
    assertEquals(tree, unmarshaled);
  }

  public Incident incident(long id) {
    return new IncidentImpl(id, 2, "service", "localhost", new Date(id), Severity.FAILURE, "code", list(tuple("title",
            "content")), map(tuple("key", "value")));
  }
}
