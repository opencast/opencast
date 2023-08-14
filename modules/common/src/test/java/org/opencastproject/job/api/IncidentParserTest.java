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

package org.opencastproject.job.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.opencastproject.util.IoSupport.loadFileFromClassPathAsString;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.xmlmatchers.XmlMatchers.similarTo;
import static org.xmlmatchers.transform.XmlConverters.the;

import org.opencastproject.job.api.Incident.Severity;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class IncidentParserTest {

  private static final Logger logger = LoggerFactory.getLogger(IncidentParserTest.class);

  @Test
  public void testSerializationOfJaxbIncident() throws Exception {
    final Incident incident = new IncidentImpl(1, 2, "service", "localhost", new Date(0), Severity.FAILURE, "code",
            Arrays.asList(tuple("title", "content"), tuple("Another title", "...and even more content")),
            Collections.singletonMap("key", "value"));
    final String marshaled = IncidentParser.I.toXml(new JaxbIncident(incident));
    logger.info(marshaled);
    assertThat(the(marshaled),
            similarTo(the(loadFileFromClassPathAsString("/org/opencastproject/job/api/expected-incident-1.xml").get())));
    final Incident unmarshaled = IncidentParser.I.parseIncidentFromXml(IOUtils.toInputStream(marshaled)).toIncident();
    assertEquals(incident, unmarshaled);
  }

  @Test
  public void testSerializationOfJaxbIncidentTree() throws Exception {
    final IncidentTree tree = new IncidentTreeImpl(
            Arrays.asList(incident(1), incident(2)),
            Collections.singletonList(new IncidentTreeImpl(
                    Collections.singletonList(incident(3)),
                    Collections.singletonList(new IncidentTreeImpl(
                            Arrays.asList(incident(4), incident(5)),
                            Collections.emptyList())))));
    final String marshaled = IncidentParser.I.toXml(new JaxbIncidentTree(tree));
    logger.info(marshaled);
    assertThat(the(marshaled),
            similarTo(the(loadFileFromClassPathAsString("/org/opencastproject/job/api/expected-incident-tree-1.xml")
                    .get())));
    final IncidentTree unmarshaled = IncidentParser.I.parseIncidentTreeFromXml(IOUtils.toInputStream(marshaled))
            .toIncidentTree();
    assertEquals(tree, unmarshaled);
  }

  public Incident incident(long id) {
    return new IncidentImpl(id, 2, "service", "localhost", new Date(id), Severity.FAILURE, "code",
            Collections.singletonList(tuple("title", "content")), Collections.singletonMap("key", "value"));
  }
}
