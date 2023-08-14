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

package org.opencastproject.serviceregistry.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.job.api.IncidentImpl;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.job.api.IncidentTreeImpl;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class IncidentsTest {
  @Test
  public void testFindFailure1() {
    final IncidentTree r = new IncidentTreeImpl(
            Arrays.asList(mkIncident(Severity.INFO), mkIncident(Severity.INFO), mkIncident(Severity.INFO)),
            Collections.singletonList(new IncidentTreeImpl(
                    Arrays.asList(mkIncident(Severity.INFO), mkIncident(Severity.WARNING)),
                    Collections.singletonList(new IncidentTreeImpl(
                            Arrays.asList(mkIncident(Severity.WARNING), mkIncident(Severity.FAILURE)),
                            Collections.emptyList())))));
    assertTrue(Incidents.findFailure(r));
  }

  @Test
  public void testFindFailure2() {
    final IncidentTree r = new IncidentTreeImpl(
            Arrays.asList(mkIncident(Severity.INFO), mkIncident(Severity.INFO), mkIncident(Severity.INFO)),
            Collections.singletonList(new IncidentTreeImpl(
                    Arrays.asList(mkIncident(Severity.INFO), mkIncident(Severity.WARNING)),
                    Collections.singletonList(new IncidentTreeImpl(
                            Arrays.asList(mkIncident(Severity.WARNING), mkIncident(Severity.INFO)),
                            Collections.emptyList())))));
    assertFalse(Incidents.findFailure(r));
  }

  @Test
  public void testFindFailure3() {
    final IncidentTree r = new IncidentTreeImpl(
            Arrays.asList(mkIncident(Severity.FAILURE), mkIncident(Severity.INFO), mkIncident(Severity.INFO)),
            Collections.singletonList(new IncidentTreeImpl(
                    Arrays.asList(mkIncident(Severity.INFO), mkIncident(Severity.WARNING)),
                    Collections.singletonList(new IncidentTreeImpl(
                            Arrays.asList(mkIncident(Severity.WARNING), mkIncident(Severity.INFO)),
                            Collections.emptyList())))));
    assertTrue(Incidents.findFailure(r));
  }

  private Incident mkIncident(Severity s) {
    return new IncidentImpl(
            0, 0, "servicetype", "host", new Date(), s, "code", Incidents.NO_DETAILS, Incidents.NO_PARAMS);
  }
}
