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

import org.opencastproject.fun.juc.Mutables;
import org.opencastproject.util.data.Function2;

import java.util.List;

public final class IncidentUtil {
  private IncidentUtil() {
  }

  /** Concat a tree of incidents into a list. */
  public static List<Incident> concat(IncidentTree tree) {
    return mlist(tree.getDescendants()).foldl(
            Mutables.list(tree.getIncidents()),
            new Function2<List<Incident>, IncidentTree, List<Incident>>() {
              @Override public List<Incident> apply(List<Incident> sum, IncidentTree tree) {
                sum.addAll(concat(tree));
                return sum;
              }
            });
  }
}
