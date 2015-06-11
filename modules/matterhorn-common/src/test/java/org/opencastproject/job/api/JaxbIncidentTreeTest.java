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
import static org.opencastproject.util.ReflectionUtil.run;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.fun.juc.Immutables;
import org.opencastproject.job.api.Incident.Severity;

import org.junit.Test;

import java.util.Date;
import java.util.List;

public class JaxbIncidentTreeTest {
  /** This test ensures full mapping of all IncidentTree properties to its accompanying JAXB DTO. */
  @Test
  public void testEquivalence() throws Exception {
    final List<Incident> incidents = Immutables.list(mkIncident(1), mkIncident(2));
    final List<IncidentTree> subTrees = Immutables.<IncidentTree>list(new IncidentTreeImpl(
            Immutables.list(mkIncident(3), mkIncident(4)),
            Immutables.<IncidentTree>nil()));
    final JaxbIncidentTree dto = new JaxbIncidentTree(new IncidentTreeImpl(incidents, subTrees));
    final IncidentTree tree = dto.toIncidentTree();
    run(IncidentTree.class, new IncidentTree() {
      @Override public List<Incident> getIncidents() {
        assertEquals("incidents transferred", incidents, tree.getIncidents());
        return null;
      }

      @Override public List<IncidentTree> getDescendants() {
        assertEquals("childIncidents transferred", subTrees, tree.getDescendants());
        return null;
      }
    });
  }

  private static Incident mkIncident(long id) {
    return new IncidentImpl(
            id, id, "service-" + id, "host-" + id, new Date(id),
            Severity.FAILURE,
            "code-" + id,
            Immutables.list(tuple("detail-" + id, "value-" + id)),
            Immutables.map(tuple("key-" + id, "value-" + id)));
  }
}
