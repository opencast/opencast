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
package org.opencastproject.assetmanager.api.query

import org.opencastproject.assetmanager.api.Property
import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.Version
import org.opencastproject.assetmanager.api.fn.ARecords
import org.opencastproject.assetmanager.api.fn.Snapshots

import com.entwinemedia.fn.Stream

/**
 * Extensions for [AResult].
 */
class RichAResult(private val result: AResult) : AResult {

    val properties: Stream<Property>
        get() = result.records.bind(ARecords.getProperties)

    /** Get all selected snapshots.  */
    val snapshots: Stream<Snapshot>
        get() = result.records.bind(ARecords.getSnapshot)

    /** Get all selected versions.  */
    val versions: Stream<Version>
        get() = snapshots.map(Snapshots.getVersion)

    override val records: Stream<ARecord>
        get() = result.records

    override val size: Long
        get() = result.size

    override val query: String
        get() = result.query

    override val totalSize: Long
        get() = result.totalSize

    override val limit: Long
        get() = result.limit

    override val offset: Long
        get() = result.offset

    override val searchTime: Long
        get() = result.searchTime

    /** Count all properties contained in the result.  */
    fun countProperties(): Int {
        return sizeOf(properties)
    }

    /** Count all snapshots contained in the result.  */
    fun countSnapshots(): Int {
        return sizeOf(snapshots)
    }

    private fun <A> sizeOf(stream: Stream<A>): Int {
        var count = 0
        for (ignore in stream) {
            count++
        }
        return count
    }

    //
    // delegates
    //

    override fun iterator(): Iterator<ARecord> {
        return result.iterator()
    }
}
