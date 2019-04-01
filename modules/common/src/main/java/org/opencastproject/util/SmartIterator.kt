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

import java.util.ArrayList
import java.util.LinkedHashMap
import kotlin.collections.Map.Entry

/**
 * Utility class for applying limit and offset to a map or collection
 */
class SmartIterator<A>(private val limit: Int, private val offset: Int) {
    private var iterator: Iterator<*>? = null

    /**
     * Apply limit and offset to a map of value type [A]
     *
     * @param map
     * the map
     * @return the filtered map
     */
    fun applyLimitAndOffset(map: Map<String, A>): Map<String, A> {
        iterator = map.entries.iterator()

        val filteredMap = LinkedHashMap<String, A>()
        var i = 0
        while (isRecordRequired(filteredMap.size)) {
            val item = iterator!!.next() as Entry<String, A>
            if (i++ >= offset) {
                filteredMap[item.key] = item.value
            }
        }
        return filteredMap
    }

    private fun isRecordRequired(filteredMapSize: Int): Boolean {
        return (filteredMapSize < limit || limit == 0) && iterator!!.hasNext()
    }

    /**
     * Apply limit and offset to a collection of type [A]
     *
     * @param unfilteredCollection
     * the collection
     * @return the filtered list
     */
    fun applyLimitAndOffset(unfilteredCollection: Collection<A>): List<A> {
        iterator = unfilteredCollection.iterator()
        val filteredList = ArrayList<A>()
        var i = 0
        while (isRecordRequired(filteredList.size)) {
            val nextItem = iterator!!.next() as A
            if (i++ >= offset) {
                filteredList.add(nextItem)
            }
        }
        return filteredList
    }
}
