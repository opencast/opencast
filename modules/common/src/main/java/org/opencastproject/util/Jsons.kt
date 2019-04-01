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

import org.opencastproject.util.data.Collections
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Function2
import org.opencastproject.util.data.Monadics
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Prelude

import org.json.simple.JSONArray
import org.json.simple.JSONObject

import java.util.ArrayList
import java.util.Date

/** JSON builder based on json-simple.  */
object Jsons {

    /** Check if a value is not [.ZERO_VAL].  */
    val notZero: Function<Val, Boolean> = object : Function<Val, Boolean>() {
        override fun apply(`val`: Val): Boolean? {
            return ZERO_VAL != `val`
        }
    }

    /** Get the value from a property.  */
    val getVal: Function<Prop, Val> = object : Function<Prop, Val>() {
        override fun apply(prop: Prop): Val {
            return prop.`val`
        }
    }

    /** [.toJson] as a function.  */
    val toJson: Function<Obj, String> = object : Function<Obj, String>() {
        override fun apply(obj: Obj): String {
            return obj.toJson()
        }
    }

    /** JSON null.  */
    val NULL: Val = object : Val() {

    }

    /** Identity for [values][Val].  */
    val ZERO_VAL: Val = object : Val() {

    }

    /** Identity for [objects][Obj].  */
    val ZERO_OBJ = obj()

    /** Identity for [arrays][Arr].  */
    val ZERO_ARR = arr()

    val stringVal: Function<String, Val> = object : Function<String, Val>() {
        override fun apply(s: String): Val {
            return v(s)
        }
    }

    class Prop private constructor(val name: String, val `val`: Val)

    // sum type
    abstract class Val

    private class SVal private constructor(val `val`: Any) : Val()

    class Obj private constructor(val props: List<Prop>) : Val() {

        fun append(o: Obj): Obj {
            return if (ZERO_OBJ != o)
                Obj(Collections.concat<Prop, List<*>>(props, o.props))
            else
                o
        }

        fun toJson(): String {
            return Jsons.toJson(this)
        }
    }

    class Arr(val vals: List<Val>) : Val() {

        fun append(a: Arr): Arr {
            return if (ZERO_ARR != a)
                Arr(Collections.concat<Val, List<*>>(vals, a.vals))
            else
                a
        }

        fun toJson(): String {
            return Jsons.toJson(this)
        }
    }

    //

    fun toJson(obj: Obj): String {
        return toJsonSimple(obj).toString()
    }

    fun toJson(arr: Arr): String {
        return toJsonSimple(arr).toString()
    }

    private fun toJsonSimple(obj: Obj): JSONObject {
        return mlist(obj.props).foldl(JSONObject(), object : Function2<JSONObject, Prop, JSONObject>() {
            override fun apply(jo: JSONObject, prop: Prop): JSONObject {
                jo[prop.name] = toJsonSimple(prop.`val`)
                return jo
            }
        })
    }

    private fun toJsonSimple(arr: Arr): JSONArray {
        return mlist(arr.vals).foldl(JSONArray(), object : Function2<JSONArray, Val, JSONArray>() {
            override fun apply(ja: JSONArray, `val`: Val): JSONArray {
                ja.add(toJsonSimple(`val`))
                return ja
            }
        })
    }

    private fun toJsonSimple(`val`: Val): Any? {
        if (`val` is SVal) {
            return `val`.`val`
        }
        if (`val` is Obj) {
            return toJsonSimple(`val`)
        }
        if (`val` is Arr) {
            return toJsonSimple(`val`)
        }
        return if (`val` == NULL) {
            null
        } else Prelude.unexhaustiveMatch<Any>()
    }

    /** Create an object.  */
    fun obj(vararg ps: Prop): Obj {
        return Obj(mlist(*ps).filter(notZero.o(getVal)).value())
    }

    /** Create an array.  */
    fun arr(vararg vs: Val): Arr {
        return Arr(mlist(*vs).filter(notZero).value())
    }

    /** Create an array.  */
    fun arr(vs: List<Val>): Arr {
        return Arr(mlist(vs).filter(notZero).value())
    }

    /** Create an array.  */
    fun arr(vs: Monadics.ListMonadic<Val>): Arr {
        return Arr(vs.filter(notZero).value())
    }

    fun v(v: Number): Val {
        return SVal(v)
    }

    fun v(v: String): Val {
        return SVal(v)
    }

    fun v(v: Boolean?): Val {
        return SVal(v)
    }

    fun v(v: Date): Val {
        return SVal(DateTimeSupport.toUTC(v.time))
    }

    /** Create a property.  */
    fun p(key: String, `val`: Val): Prop {
        return Prop(key, `val`)
    }

    /** Create a property. Passing none is like setting [.ZERO_VAL] which erases the property.  */
    fun p(key: String, `val`: Option<Val>): Prop {
        return Prop(key, `val`.getOrElse(ZERO_VAL))
    }

    /** Create a property. Convenience.  */
    fun p(key: String, value: Number): Prop {
        return Prop(key, v(value))
    }

    /** Create a property. Convenience.  */
    fun p(key: String, value: String): Prop {
        return Prop(key, v(value))
    }

    /** Create a property. Convenience.  */
    fun p(key: String, value: Boolean?): Prop {
        return Prop(key, v(value))
    }

    /** Create a property. Convenience.  */
    fun p(key: String, value: Date): Prop {
        return Prop(key, v(value))
    }

    /** Merge a list of objects into one (last one wins).  */
    fun append(vararg os: Obj): Obj {
        val props = mlist(*os).foldl(ArrayList(), object : Function2<ArrayList<Prop>, Obj, ArrayList<Prop>>() {
            override fun apply(props: ArrayList<Prop>, obj: Obj): ArrayList<Prop> {
                props.addAll(obj.props)
                return props
            }
        })
        return Obj(props)
    }

    /** Append a list of arrays into one.  */
    fun append(vararg `as`: Arr): Arr {
        val vals = mlist(*`as`).foldl(ArrayList(), object : Function2<ArrayList<Val>, Arr, ArrayList<Val>>() {
            override fun apply(vals: ArrayList<Val>, arr: Arr): ArrayList<Val> {
                vals.addAll(arr.vals)
                return vals
            }
        })
        return Arr(vals)
    }
}
