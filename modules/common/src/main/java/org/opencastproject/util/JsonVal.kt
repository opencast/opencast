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

import org.opencastproject.util.data.functions.Misc.cast

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.functions.Options
import org.opencastproject.util.data.functions.Strings

class JsonVal(private val `val`: Any) {

    val isObj: Boolean
        get() = `val` is Map<*, *>

    val isArr: Boolean
        get() = `val` is List<*>

    fun <A> `as`(converter: Function<Any, out A>): A {
        return converter.apply(`val`)
    }

    fun get(): Any {
        return `val`
    }

    companion object {

        val asString = caster<String>(String::class.java)
        val asInteger = caster<Int>(Int::class.java)
        val asLong = caster<Long>(Long::class.java)
        val asFloat = caster<Float>(Float::class.java)
        val asDouble = caster<Double>(Double::class.java)
        val asBoolean = caster<Boolean>(Boolean::class.java)
        val asJsonObj: Function<Any, JsonObj> = object : Function<Any, JsonObj>() {
            override fun apply(o: Any): JsonObj {
                return JsonObj.jsonObj(o as Map<*, *>)
            }
        }
        val asJsonArr: Function<Any, JsonArr> = object : Function<Any, JsonArr>() {
            override fun apply(o: Any): JsonArr {
                return JsonArr(o as List<*>)
            }
        }
        val asJsonVal: Function<Any, JsonVal> = object : Function<Any, JsonVal>() {
            override fun apply(o: Any): JsonVal {
                return JsonVal(o)
            }
        }
        val stringAsInteger = Options.getF<Int>().o(Strings.toInt.o(asString))

        private fun <A> caster(ev: Class<A>): Function<Any, A> {
            return object : Function<Any, A>() {
                override fun apply(o: Any): A {
                    return cast(o, ev)
                }
            }
        }
    }
}
