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

package org.opencastproject.util.query

/**
 * A general purpose result set.
 *
 * @param <A> the type of result items
</A> */
abstract class ResultSetBase<A> {

    /** Return a string representation of the query.  */
    abstract val query: String

    /** Return the number of items the query could potentially yield.  */
    abstract val totalSize: Long

    /** Return the set size limit.  */
    abstract val limit: Long

    /** Return the offset within the total result set.  */
    abstract val offset: Long

    /** Return the page number of this slice.  */
    val page: Long
        get() = if (limit > 0) offset / limit else 0

    abstract val searchTime: Long
    /** Return the retrieved slice.  */
    abstract fun <X : A> getItems(): List<X>

    /** Return the size of retrieved [slice][.getItems].  */
    fun size(): Long {
        return getItems<A>().size.toLong()
    }
}
