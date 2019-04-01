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


package org.opencastproject.mediapackage.identifier

/**
 * Interface that describes the methods of an id builder.
 */
interface IdBuilder {

    /**
     * Creates a new identifier. The identifier is supposed to be unique within a running system.
     *
     *
     * The default implementation will return a uuid-style identifier.
     *
     *
     * @return the new identifier
     */
    fun createNew(): Id

    /**
     * This method can be used to determine if `id` is in fact a vaild identifier as expected by this id
     * builder. If this is not the case, an [IllegalArgumentException] is thrown.
     *
     * @return the id
     * @throws IllegalArgumentException
     * if the identifier is malformed
     */
    @Throws(IllegalArgumentException::class)
    fun fromString(id: String): Id

}
