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

import java.lang.String.format
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.data.functions.Misc.cast
import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import org.apache.commons.io.IOUtils
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

import java.io.IOException
import java.io.InputStream

/** Accessor for JSON objects aka maps.  */
// todo -- think about using specialized Exception (JsonExcpetion ?); handle parse exception in jsonObj(String)
class JsonObj
/** Create a wrapper for a map.  */
(private val json: Map<*, *>) {

    fun keySet(): Set<*> {
        return json.keys
    }

    fun `val`(key: String): JsonVal {
        return JsonVal(get(Any::class.java, key))
    }

    fun valOpt(key: String): JsonVal {
        return JsonVal(get(Any::class.java, key))
    }

    fun obj(key: String): JsonObj {
        return jsonObj(get<Map>(Map<*, *>::class.java, key))
    }

    fun arr(key: String): JsonArr {
        return JsonArr(get<List>(List<*>::class.java, key))
    }

    fun has(key: String): Boolean {
        return json.containsKey(key)
    }

    /**
     * Get mandatory value of type `ev`.
     *
     * @return the requested value if it exists and has the required type
     */
    @Deprecated("")
    operator fun <A> get(ev: Class<A>, key: String): A {
        val v = json.get(key)
        return if (v != null) {
            try {
                cast(v, ev)
            } catch (e: ClassCastException) {
                throw RuntimeException(format("Key %s has not required type %s but %s", key, ev.name, v.javaClass
                        .getName()))
            }

        } else {
            throw RuntimeException(format("Key %s does not exist", key))
        }
    }

    /**
     * Get optional value of type `ev`.
     *
     * @return some if the value exists and has the required type, none otherwise
     */
    @Deprecated("")
    fun <A> opt(ev: Class<A>, key: String): Option<A> {
        val v = json.get(key)
        return if (v != null) {
            try {
                some(cast(v, ev))
            } catch (e: ClassCastException) {
                none()
            }

        } else {
            none()
        }
    }

    /**
     * Get mandatory JSON object.
     *
     */
    @Deprecated("")
    fun getObj(key: String): JsonObj {
        return jsonObj(get<Map>(Map<*, *>::class.java, key))
    }

    /**
     * Get an optional JSON object.
     *
     */
    @Deprecated("")
    fun optObj(key: String): Option<JsonObj> {
        return opt<Map<*, *>>(Map<*, *>::class.java, key).map(jsonObj)
    }

    companion object {

        /** Constructor function.  */
        fun jsonObj(json: Map<*, *>): JsonObj {
            return JsonObj(json)
        }

        /** Create a JsonObj from a JSON string.  */
        fun jsonObj(json: String): JsonObj {
            return JsonObj(parse(json))
        }

        @Throws(IOException::class)
        fun mk(`in`: InputStream): JsonObj {
            return JsonObj(parse(IOUtils.toString(`in`)))
        }

        val fromInputStream: Function<InputStream, JsonObj> = object : Function.X<InputStream, JsonObj>() {
            @Throws(Exception::class)
            public override fun xapply(`in`: InputStream): JsonObj {
                return mk(`in`)
            }
        }

        /** [.jsonObj] as a function.  */
        val jsonObj: Function<Map<*, *>, JsonObj> = object : Function<Map<*, *>, JsonObj>() {
            override fun apply(json: Map<*, *>): JsonObj {
                return jsonObj(json)
            }
        }

        private fun parse(json: String): Map<*, *> {
            try {
                return JSONParser().parse(json) as Map<*, *>
            } catch (e: ParseException) {
                return chuck(e)
            }

        }
    }
}
