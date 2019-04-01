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

import org.opencastproject.util.data.Monadics.mlist

import org.opencastproject.util.data.Function2

import java.util.LinkedList

object IncidentUtil {

    /** Concat a tree of incidents into a list.  */
    fun concat(tree: IncidentTree): List<Incident> {
        return mlist(tree.descendants).foldl(
                LinkedList(tree.incidents),
                object : Function2<List<Incident>, IncidentTree, List<Incident>>() {
                    override fun apply(sum: List<Incident>, tree: IncidentTree): List<Incident> {
                        sum.addAll(concat(tree))
                        return sum
                    }
                })
    }
}
