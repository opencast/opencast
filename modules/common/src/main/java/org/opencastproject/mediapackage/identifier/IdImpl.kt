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

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.XmlValue

/**
 * Simple and straightforward implementation of the [Id] interface.
 */
@XmlType
@XmlAccessorType(XmlAccessType.NONE)
class IdImpl : Id {

    /** The identifier  */
    @XmlValue
    protected var id: String? = null

    /**
     * Needed for JAXB serialization
     */
    constructor() {}

    /**
     * Creates a new identifier.
     *
     * @param id
     * the identifier
     */
    constructor(id: String) {
        this.id = id
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.identifier.Id.compact
     */
    override fun compact(): String {
        return id!!.replace("/".toRegex(), "-").replace("\\\\".toRegex(), "-")
    }

    override fun toString(): String? {
        return id
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.equals
     */
    override fun equals(o: Any?): Boolean {
        if (o is IdImpl) {
            val other = o as IdImpl?
            return id != null && other!!.id != null && id == other.id
        }
        return false
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return id!!.hashCode()
    }
}
