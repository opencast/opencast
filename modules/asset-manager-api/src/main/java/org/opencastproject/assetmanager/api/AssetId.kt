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
package org.opencastproject.assetmanager.api

import com.entwinemedia.fn.Equality.eq
import java.lang.String.format

import com.entwinemedia.fn.Equality

import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class AssetId(val version: Version, val mediaPackageId: String, val mediaPackageElementId: String) {

    override fun hashCode(): Int {
        return Equality.hash(version, mediaPackageId, mediaPackageElementId)
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is AssetId && eqFields((that as AssetId?)!!)
    }

    private fun eqFields(that: AssetId): Boolean {
        return eq(mediaPackageId, that.mediaPackageId) && eq(mediaPackageElementId, that.mediaPackageElementId) && eq(version, that.version)
    }

    override fun toString(): String {
        return format("AssetId(mpId=%s, mpeId=%s, version=%s)", mediaPackageId, mediaPackageElementId, version)
    }

    companion object {

        fun mk(version: Version, mpId: String, mpeId: String): AssetId {
            return AssetId(version, mpId, mpeId)
        }
    }
}
