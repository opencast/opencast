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
package org.opencastproject.oaipmh.server

/** OAI-PMH server.  */
interface OaiPmhServerInfo {

    /**
     * Return the mount point of the server, e.g. /oaipmh. This is *not* the full
     * base url like http://localhost:8080/oaipmh but only the path.
     */
    val mountPoint: String

    /** Check if the requested repo exists.  */
    fun hasRepo(id: String): Boolean
}
