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

import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.util.data.Function

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Reflection utils.  */
object ReflectionUtil {

    /**
     * Simple helper to avoid unnecessary `return null;` statements when using [.run],
     * [.run] or [.xfer].
     * Just wrap your expression like this `return call(expr)`.
     */
    fun <A> call(expression: Any): A? {
        return null
    }

    fun bcall(expression: Any): Boolean {
        return false
    }

    fun lcall(expression: Any): Long {
        return 0L
    }

    fun icall(expression: Any): Int {
        return 0
    }

    fun dcall(expression: Any): Double {
        return 0.0
    }

    /**
     * Call all parameterless methods of `c` on object `o`.
     *
     *
     * Does not call any bridge, synthetic or native methods and also no methods declared in Object.
     */
    fun <A, B : A> run(c: Class<A>, o: B) {
        runInternal(c.methods, o)
    }

    /**
     * Call all parameterless methods of object `a`.
     *
     *
     * Does not call any bridge, synthetic or native methods and also no methods declared in Object.
     */
    fun <A> run(a: A) {
        runInternal(a.javaClass.getMethods(), a)
    }

    private fun <A> runInternal(ms: Array<Method>, a: A) {
        try {
            for (m in ms) {
                if (m.parameterTypes.size == 0
                        && m.declaringClass != Any::class.java
                        && !m.isBridge && !m.isSynthetic
                        && !Modifier.isNative(m.modifiers)) {
                    m.invoke(a)
                }
            }
        } catch (e: Exception) {
            chuck<Any>(e)
        }

    }

    /**
     * Helper method for the transfer idiom.
     *
     *
     * Take `target` and apply it to `f`. Take the result and call all
     * parameterless methods on it that are described by class `source`. This way `f`
     * is able to create a transfer object that sets all properties of `target`.
     *
     *
     * The advantage of this style is that each time the `source` class changes the compiler
     * will yield an error if a field is missed in the transfer. The common getter/setter or constructor idiom
     * does not detect errors of this kind at compile time.
     */
    fun <A, B> xfer(target: A, source: Class<B>, f: Function<A, out B>): A {
        val b = f.apply(target)
        run(source, b)
        return target
    }
}
