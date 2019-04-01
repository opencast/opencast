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
import com.entwinemedia.fn.Equality.hash
import java.lang.String.format

import javax.annotation.ParametersAreNonnullByDefault

/**
 * A property of a media package.
 * Properties can be defined and associated to a media package's version history.
 */
@ParametersAreNonnullByDefault
class Property(val id: PropertyId, val value: Value) {

    override fun hashCode(): Int {
        return hash(id, value)
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is Property && eqFields((that as Property?)!!)
    }

    private fun eqFields(that: Property): Boolean {
        return eq(id, that.id) && eq(value, that.value)
    }

    override fun toString(): String {
        return format("Property(%s=%s)", id, value)
    }

    companion object {

        fun mk(id: PropertyId, value: Value): Property {
            return Property(id, value)
        }
    }
}
