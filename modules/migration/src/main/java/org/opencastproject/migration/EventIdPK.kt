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
package org.opencastproject.migration

import java.io.Serializable
import java.util.Objects

class EventIdPK : Serializable {

    val mediaPackageId: String
    val organization: String

    constructor() {}

    constructor(mediaPackageId: String, organization: String) {
        this.mediaPackageId = mediaPackageId
        this.organization = organization
    }

    override fun hashCode(): Int {
        return Objects.hash(mediaPackageId, organization)
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this)
            return true
        if (obj !is EventIdPK)
            return false
        val pk = obj as EventIdPK?
        return pk!!.mediaPackageId == mediaPackageId && pk.organization == organization
    }

    companion object {

        /**
         * Serial UUID
         */
        private const val serialVersionUID = 5277574531617973229L
    }

}
