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
package org.opencastproject.util

import java.util.UUID

/**
 * Holds information about a file upload.
 */
class UploadJob {

    /**
     * @return the id
     */
    var id: String? = null
        private set

    /**
     * @return the filename
     */
    /**
     * @param filename
     * the filename to set
     */
    var filename = ""

    /**
     * @return the bytesTotal
     */
    /**
     * @param bytesTotal
     * the bytesTotal to set
     */
    var bytesTotal = -1L

    /**
     * @return the bytesReceived
     */
    /**
     * @param bytesReceived
     * the bytesReceived to set
     */
    var bytesReceived = -1L

    constructor() {
        this.id = UUID.randomUUID().toString()
    }

    constructor(filename: String, bytesTotal: Long) {
        this.id = UUID.randomUUID().toString()
        this.filename = filename
        this.bytesTotal = bytesTotal
    }
}
