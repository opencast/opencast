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

import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.functions.Misc.cast

import org.opencastproject.util.data.Function

import java.util.ArrayList

class JsonArr(arr: List<*>) : Iterable<JsonVal> {
    private val `val`: List<Any>

    init {
        this.`val` = ArrayList<Any>(arr)
    }

    fun `val`(index: Int): JsonVal {
        return JsonVal(`val`[index])
    }

    fun obj(index: Int): JsonObj {
        return JsonObj(`val`[index] as Map<*, *>)
    }

    fun arr(index: Int): JsonArr {
        return JsonArr(`val`[index] as List<*>)
    }

    fun <A> `as`(converter: Function<Any, A>): List<A> {
        return mlist(`val`).map(converter).value()
    }

    fun get(): List<JsonVal> {
        return mlist(`val`).map(JsonVal.asJsonVal).value()
    }

    override fun iterator(): Iterator<JsonVal> {
        return mlist(`val`).map(JsonVal.asJsonVal).iterator()
    }

    private fun <A> caster(ev: Class<A>): Function<Any, A> {
        return object : Function<Any, A>() {
            override fun apply(o: Any): A {
                return cast(o, ev)
            }
        }
    }
}
