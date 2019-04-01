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

package org.opencastproject.workspace.impl.jmx

import org.opencastproject.util.data.Option.Match
import org.opencastproject.workspace.api.Workspace

class WorkspaceBean(private val workspace: Workspace) : WorkspaceMXBean {

    /**
     * @see org.opencastproject.workspace.impl.jmx.WorkspaceMXBean.getFreeSpace
     */
    override val freeSpace: Long
        get() = workspace.usableSpace.fold(object : Match<Long, Long> {
            override fun some(a: Long?): Long? {
                return a
            }

            override fun none(): Long? {
                return -1L
            }
        })

    /**
     * @see org.opencastproject.workspace.impl.jmx.WorkspaceMXBean.getUsedSpace
     */
    override val usedSpace: Long
        get() = workspace.usedSpace.fold(object : Match<Long, Long> {
            override fun some(a: Long?): Long? {
                return a
            }

            override fun none(): Long? {
                return -1L
            }
        })

    /**
     * @see org.opencastproject.workspace.impl.jmx.WorkspaceMXBean.getTotalSpace
     */
    override val totalSpace: Long
        get() = workspace.totalSpace.fold(object : Match<Long, Long> {
            override fun some(a: Long?): Long? {
                return a
            }

            override fun none(): Long? {
                return -1L
            }
        })

}
