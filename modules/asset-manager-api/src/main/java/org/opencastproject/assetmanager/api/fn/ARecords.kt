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
package org.opencastproject.assetmanager.api.fn

import org.opencastproject.assetmanager.api.Property
import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.query.ARecord

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Pred
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt

/**
 * Functions to deal with [ARecord]s.
 */
object ARecords {
    val getMediaPackageId: Fn<ARecord, String> = object : Fn<ARecord, String>() {
        override fun apply(item: ARecord): String {
            return item.mediaPackageId
        }
    }

    val getProperties: Fn<ARecord, Stream<Property>> = object : Fn<ARecord, Stream<Property>>() {
        override fun apply(item: ARecord): Stream<Property> {
            return item.properties
        }
    }

    val hasProperties: Pred<ARecord> = object : Pred<ARecord>() {
        override fun apply(item: ARecord): Boolean? {
            return !item.properties.isEmpty
        }
    }

    /**
     * Get the snapshot from a record.
     *
     * @see ARecord.getSnapshot
     */
    val getSnapshot: Fn<ARecord, Opt<Snapshot>> = object : Fn<ARecord, Opt<Snapshot>>() {
        override fun apply(a: ARecord): Opt<Snapshot> {
            return a.snapshot
        }
    }
}
