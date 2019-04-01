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


package org.opencastproject.metadata.dublincore

import java.util.Date

/**
 * A time interval, representing a DCMI period. They may be open.
 *
 *
 * For further information on DCMI periods please refer to [http://dublincore.org/documents/dcmi-period/](http://dublincore.org/documents/dcmi-period/).
 */
class DCMIPeriod
/**
 * Create a new period with an optional name. To create an open interval you may set one of the bounbaries null.
 */
@JvmOverloads constructor(
        /**
         * Returns the start date of the period or null, if it has only an upper bound.
         */
        val start: Date?,
        /**
         * Returns the end date of the period or null, if it has only a lower bound.
         */
        val end: Date?,
        /**
         * Returns the optional name of the period.
         *
         * @return the name of the period or null
         */
        val name: String? = null) {

    /**
     * Checks if the interval is closed.
     */
    val isClosed: Boolean
        get() = start != null && end != null

    init {
        if (start == null && end == null)
            throw IllegalStateException("A period must be bounded at least at one end")
        if (start != null && end != null && end.before(start))
            throw IllegalStateException("The end date is before the start date")
    }

    /**
     * Checks if the interval has a start boundary.
     */
    fun hasStart(): Boolean {
        return start != null
    }

    /**
     * Checks if the interval has an end boundary.
     */
    fun hasEnd(): Boolean {
        return end != null
    }

    /**
     * Checks if the interval has a name.
     */
    fun hasName(): Boolean {
        return name != null
    }

    override fun toString(): String {
        return ("DCMIPeriod{" + "start=" + (start ?: "]") + ", end=" + (end ?: "[")
                + (if (name != null) ", name='$name'" else "") + '}'.toString())
    }

}
/**
 * Create a new period. To create an open interval you may set one of the boundaries null.
 */
