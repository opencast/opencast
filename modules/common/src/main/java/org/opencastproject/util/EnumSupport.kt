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

import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Enum utility methods.
 */
object EnumSupport {

    /**
     * Support method to help enums implement an enhanced `valueOf(String)` method, that does not throw an
     * IllegalArgumentException in case of incoming values, that do not match any of the enum's values.
     *
     * @param enumClass
     * the enum's class
     * @param value
     * the value to look up
     * @return the matching enum value or null if none matches
     */
    fun <E : Enum<*>> fromString(enumClass: Class<E>, value: String?): E? {
        var value: String? = value ?: return null
        value = value!!.trim { it <= ' ' }
        if (value.length == 0)
            return null
        var m: Method? = null
        try {
            m = enumClass.getDeclaredMethod("valueOf", String::class.java)
        } catch (ignore: NoSuchMethodException) {
        }

        try {
            m!!.isAccessible = true
        } catch (ignore: SecurityException) {
        }

        try {
            return m!!.invoke(null, value) as E
        } catch (ignore: IllegalAccessException) {
        } catch (ignore: InvocationTargetException) {
        }

        return null
    }

    /** Create a function to parse a string into an Enum value.  */
    fun <A : Enum<*>> parseEnum(e: A): Function<String, Option<A>> {
        return object : Function<String, Option<A>>() {
            override fun apply(s: String): Option<A> {
                try {
                    return some(Enum.valueOf<out Enum>(e.javaClass, s) as A)
                } catch (ex: Exception) {
                    return none()
                }

            }
        }
    }
}
