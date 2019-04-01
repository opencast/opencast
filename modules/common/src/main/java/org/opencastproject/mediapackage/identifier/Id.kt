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

import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/**
 * Interface for an identifier.
 */
@XmlJavaTypeAdapter(Id.Adapter::class)
interface Id {

    /**
     * Returns the local identifier of this [Id]. The local identifier is defined to be free of separator characters
     * that could potentially get into the way when creating file or directory names from the identifier.
     *
     * For example, given that the interface is implemented by a class representing CNRI handles, the identifier would
     * then look something like `10.3930/ETHZ/abcd`, whith `10.3930` being the handle prefix,
     * `ETH` the authority and `abcd` the local part. `toURI()` would then return
     * `10.3930-ETH-abcd` or any other suitable form.
     *
     * @return a path separator-free representation of the identifier
     */
    fun compact(): String

    class Adapter : XmlAdapter<IdImpl, Id>() {
        @Throws(Exception::class)
        override fun marshal(id: Id): IdImpl {
            return id as? IdImpl ?: throw IllegalStateException("an unknown ID is un use: $id")
        }

        @Throws(Exception::class)
        override fun unmarshal(id: IdImpl): Id {
            return id
        }
    }

    /**
     * Return a string representation of the identifier from which an object of type Id should
     * be reconstructable.
     */
    override fun toString(): String
}
