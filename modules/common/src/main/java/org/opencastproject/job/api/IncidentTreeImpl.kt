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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.job.api

import org.opencastproject.util.EqualsUtil.eq
import org.opencastproject.util.EqualsUtil.hash

import java.util.Collections

class IncidentTreeImpl(incidents: List<Incident>, descendants: List<IncidentTree>?) : IncidentTree {
    override val incidents: List<Incident>

    override val descendants: List<IncidentTree>

    init {
        this.incidents = Collections.unmodifiableList(incidents)
        if (descendants != null) {
            this.descendants = Collections.unmodifiableList(descendants)
        } else {
            this.descendants = emptyList<IncidentTree>()
        }
    }

    override fun hashCode(): Int {
        return hash(incidents, descendants)
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is IncidentTree && eqFields((that as IncidentTree?)!!)
    }

    private fun eqFields(that: IncidentTree): Boolean {
        return eq(incidents, that.incidents) && eq(descendants, that.descendants)
    }
}
