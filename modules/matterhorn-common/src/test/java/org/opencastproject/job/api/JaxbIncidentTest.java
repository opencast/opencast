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
import static org.opencastproject.util.ReflectionUtil.run;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.fun.juc.Immutables;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.util.data.Tuple;

import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class JaxbIncidentTest {
  /** This test ensures full mapping of all Incident properties to its accompanying JAXB DTO. */
  @Test
  public void testEquivalence() throws Exception {
    final Date now = new Date();
    final JaxbIncident dto = new JaxbIncident(new IncidentImpl(
            1, 2, "service", "localhost", now, Severity.FAILURE, "code",
            Immutables.list(tuple("detail-1", "value-1"), tuple("detail-2", "detail-2")),
            Immutables.map(tuple("param", "value"), tuple("param-2", "value-2"))));
    final Incident incident = dto.toIncident();
    run(Incident.class, new Incident() {
      @Override public long getId() {
        assertEquals("id transferred", 1L, incident.getId());
        return 0;
      }

      @Override public long getJobId() {
        assertEquals("jobId transferred", 2L, incident.getJobId());
        return 0;
      }

      @Override public String getServiceType() {
        assertEquals("serviceType transferred", "service", incident.getServiceType());
        return null;
      }

      @Override public String getProcessingHost() {
        assertEquals("processingHost transferred", "localhost", incident.getProcessingHost());
        return null;
      }

      @Override public Date getTimestamp() {
        assertEquals("timestamp transferred", now, incident.getTimestamp());
        return null;
      }

      @Override public Severity getSeverity() {
        assertEquals("severity transferred", Severity.FAILURE, incident.getSeverity());
        return null;
      }

      @Override public String getCode() {
        assertEquals("code transferred", "code", incident.getCode());
        return null;
      }

      @Override public List<Tuple<String, String>> getDetails() {
        assertEquals("details transferred",
                     Immutables.list(tuple("detail-1", "value-1"), tuple("detail-2", "detail-2")),
                     incident.getDetails());
        return null;
      }

      @Override public Map<String, String> getDescriptionParameters() {
        assertEquals("decriptionParameters transferred",
                     Immutables.map(tuple("param", "value"), tuple("param-2", "value-2")),
                     incident.getDescriptionParameters());
        return null;
      }
    });
  }
}
