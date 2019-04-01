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

import java.util.Objects
import java.util.stream.Collectors

/** Utility function helping to implement equality.  */
object EqualsUtil {

    /** Check if `a` and `b` are equal. Each of them may be null.  */
    @Deprecated("")
    fun eqObj(a: Any?, b: Any?): Boolean {
        return a == b
    }

    /** Check if `a` and `b` are equal. Each of them may be null.  */
    @Deprecated("")
    fun eq(a: Any, b: Any): Boolean {
        return a == b
    }

    /** Check if `a` and `b` are not equal. Each of them may be null.  */
    fun ne(a: Any, b: Any): Boolean {
        return !eq(a, b)
    }

    /** Check if `a` and `b` have the same class ([Object.getClass]). Each may be null.  */
    fun eqClasses(a: Any, b: Any): Boolean {
        return bothNotNull(a, b) && a.javaClass == b.javaClass
    }

    /**
     * Compare the (distinct) elements of two lists for equality treating the lists as sets.
     *
     *
     * Sets by definition do not allow multiplicity of elements; a set is a (possibly empty) collection of distinct elements.
     * As Lists may contain non-unique entries, this method removes duplicates before continuing with the comparison check.
     *
     * Examples of
     * 1. equality: {1, 2} = {2, 1} = {1, 1, 2} = {1, 2, 2, 1, 2}, null = null
     * 2. unequal: {1, 2, 2} != {1, 2, 3}, null != {}
     */
    fun eqListUnsorted(`as`: List<*>?, bs: List<*>?): Boolean {
        var `as` = `as`
        var bs = bs
        if (`as` == null || bs == null) {
            return eqObj(`as`, bs)
        }

        `as` = `as`.stream().distinct().collect<List<*>, Any>(Collectors.toList<*>())
        bs = bs.stream().distinct().collect<List<*>, Any>(Collectors.toList<*>())

        if (`as`!!.size != bs!!.size) {
            return false
        }
        for (a in `as`) {
            if (!bs.contains(a)) {
                return false
            }
        }

        return true
    }

    /**
     * Compare the elements of two lists one by one.
     *
     */
    @Deprecated("use {@link #eqObj(Object, Object)} or {@link java.util.List#equals(Object)}")
    fun eqListSorted(`as`: List<*>?, bs: List<*>?): Boolean {
        if (`as` != null && bs != null && `as`.size == bs.size) {
            val asi = `as`.iterator()
            val bsi = bs.iterator()
            while (asi.hasNext() && bsi.hasNext()) {
                if (asi.next() != bsi.next())
                    return false
            }
            return true
        } else {
            return eqObj(`as`, bs)
        }
    }

    /**
     * Compare two maps.
     *
     */
    @Deprecated("use {@link #eqObj(Object, Object)} or {@link java.util.Map#equals(Object)}")
    fun eqMap(`as`: Map<*, *>, bs: Map<*, *>): Boolean {
        for ((key, value) in `as`) {
            val bv = bs[key]
            if (bv == null || !eqObj(value, bv))
                return false
        }
        return true
    }

    /** Check if both objects are either null or not null.  */
    fun bothNullOrNot(a: Any?, b: Any?): Boolean {
        return !((a == null) xor (b == null))
    }

    fun bothNotNull(a: Any?, b: Any?): Boolean {
        return a != null && b != null
    }

    fun bothNull(a: Any?, b: Any?): Boolean {
        return a == null && b == null
    }

    /**
     * Create a hash code for a list of objects. Each of them may be null.
     * Algorithm adapted from "Programming in Scala, Second Edition", p670.
     */
    @Deprecated("")
    fun hash(vararg `as`: Any): Int {
        return Objects.hash(*`as`)
    }

}
