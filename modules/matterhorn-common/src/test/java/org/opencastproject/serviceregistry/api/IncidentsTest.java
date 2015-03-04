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
package org.opencastproject.serviceregistry.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.fun.juc.Immutables;
import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.job.api.IncidentImpl;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.job.api.IncidentTreeImpl;

import org.junit.Test;

import java.util.Date;

public class IncidentsTest {
  @Test
  public void testFindFailure1() {
    final IncidentTree r = new IncidentTreeImpl(
            Immutables.list(mkIncident(Severity.INFO), mkIncident(Severity.INFO), mkIncident(Severity.INFO)),
            Immutables.<IncidentTree>list(
                    new IncidentTreeImpl(
                            Immutables.list(mkIncident(Severity.INFO), mkIncident(Severity.WARNING)),
                            Immutables.<IncidentTree>list(
                                    new IncidentTreeImpl(
                                            Immutables.list(mkIncident(Severity.WARNING), mkIncident(Severity.FAILURE)),
                                            Immutables.<IncidentTree>nil())))));
    assertTrue(Incidents.findFailure(r));
  }

  @Test
  public void testFindFailure2() {
    final IncidentTree r = new IncidentTreeImpl(
            Immutables.list(mkIncident(Severity.INFO), mkIncident(Severity.INFO), mkIncident(Severity.INFO)),
            Immutables.<IncidentTree>list(
                    new IncidentTreeImpl(
                            Immutables.list(mkIncident(Severity.INFO), mkIncident(Severity.WARNING)),
                            Immutables.<IncidentTree>list(
                                    new IncidentTreeImpl(
                                            Immutables.list(mkIncident(Severity.WARNING), mkIncident(Severity.INFO)),
                                            Immutables.<IncidentTree>nil())))));
    assertFalse(Incidents.findFailure(r));
  }

  @Test
  public void testFindFailure3() {
    final IncidentTree r = new IncidentTreeImpl(
            Immutables.list(mkIncident(Severity.FAILURE), mkIncident(Severity.INFO), mkIncident(Severity.INFO)),
            Immutables.<IncidentTree>list(
                    new IncidentTreeImpl(
                            Immutables.list(mkIncident(Severity.INFO), mkIncident(Severity.WARNING)),
                            Immutables.<IncidentTree>list(
                                    new IncidentTreeImpl(
                                            Immutables.list(mkIncident(Severity.WARNING), mkIncident(Severity.INFO)),
                                            Immutables.<IncidentTree>nil())))));
    assertTrue(Incidents.findFailure(r));
  }

  private Incident mkIncident(Severity s) {
    return new IncidentImpl(
            0, 0, "servicetype", "host", new Date(), s, "code", Incidents.NO_DETAILS, Incidents.NO_PARAMS);
  }
}
