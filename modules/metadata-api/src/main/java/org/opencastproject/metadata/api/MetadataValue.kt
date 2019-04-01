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


package org.opencastproject.metadata.api

import org.opencastproject.util.RequireUtil.notNull

/**
 * A metadata value.
 *
 * @param <A>
 * type of the encapsulated data
</A> */
class MetadataValue<A>
/**
 * Create a new value. None of the parameters must be null.
 */
@JvmOverloads constructor(value: A, name: String, language: String = MetadataValues.LANGUAGE_UNDEFINED) {

    val value: A
    val name: String
    val language: String

    init {
        this.value = notNull(value, "value")
        this.name = notNull(name, "name")
        this.language = notNull(language, "language")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("MetadataValue")
        sb.append("{value=").append(value)
        sb.append(", name=").append(name)
        sb.append(", language=").append(language)
        sb.append('}')
        return sb.toString()
    }
}
/**
 * Create a value with language set to [MetadataValues.LANGUAGE_UNDEFINED].
 */
