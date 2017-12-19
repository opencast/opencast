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

import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.EqualsUtil.hash;

import org.opencastproject.fun.juc.Immutables;

import java.util.List;

public final class IncidentTreeImpl implements IncidentTree {
  private final List<Incident> incidents;

  private final List<IncidentTree> descendants;

  public IncidentTreeImpl(List<Incident> incidents, List<IncidentTree> descendants) {
    this.incidents = Immutables.mk(incidents);
    if (descendants != null) {
      this.descendants = Immutables.mk(descendants);
    } else {
      this.descendants = Immutables.nil();
    }
  }

  @Override public List<Incident> getIncidents() {
    return incidents;
  }

  @Override public List<IncidentTree> getDescendants() {
    return descendants;
  }

  @Override public int hashCode() {
    return hash(incidents, descendants);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof IncidentTree && eqFields((IncidentTree) that));
  }

  private boolean eqFields(IncidentTree that) {
    return eq(incidents, that.getIncidents())
            && eq(descendants, that.getDescendants());
  }
}
