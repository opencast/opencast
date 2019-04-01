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

import org.apache.commons.lang3.StringUtils

/**
 * Utility functions for handling common requirements.
 */
object RequireUtil {

    /**
     * Require an expression to hold true.
     *
     * @param expr
     * the expression
     * @param exprName
     * the name of the expression used to create an error message in case `expr` evaluates to false
     * @throws IllegalArgumentException
     * in case `expr` evaluates to false
     */
    fun requireTrue(expr: Boolean, exprName: String) {
        if (!expr) {
            throw IllegalArgumentException("Requirement '$exprName' must hold true")
        }
    }

    /**
     * Require an expression to be false.
     *
     * @param expr
     * the expression
     * @param exprName
     * the name of the expression used to create an error message in case `expr` evaluates to false
     * @throws IllegalArgumentException
     * in case `expr` evaluates to true
     */
    fun requireFalse(expr: Boolean, exprName: String) {
        if (expr) {
            throw IllegalArgumentException("Requirement '$exprName' must be false")
        }
    }

    /**
     * The given value must not be null.
     *
     *
     * Example:
     *
     * <pre>
     * class A {
     * private String a;
     *
     * A(String a) {
     * this.a = notNull(a, &quot;a&quot;);
     * }
     * }
    </pre> *
     *
     * @param value
     * the value to check for null
     * @param valueName
     * the name of the value; used in error message
     * @return the value, if it is not null
     * @throws IllegalArgumentException
     * in case of `value` being null
     */
    fun <A> notNull(value: A?, valueName: String): A {
        if (value == null)
            throw IllegalArgumentException("$valueName must not be null")
        return value
    }

    /**
     * The given value must not be null or empty.
     *
     *
     * Example:
     *
     * <pre>
     * class A {
     * private String a;
     *
     * A(String a) {
     * this.a = notEmpty(a, &quot;a&quot;);
     * }
     * }
    </pre> *
     *
     * @param value
     * the value to check for emptiness
     * @param valueName
     * the name of the value; used in error message
     * @return the value, if it is not empty
     * @throws IllegalArgumentException
     * in case of `value` being empty
     */
    fun notEmpty(value: String, valueName: String): String {
        if (StringUtils.isEmpty(value))
            throw IllegalArgumentException("$valueName must not be null or empty")
        return value
    }

    /**
     * The given string value must not be blank, empty nor `null`. Otherwise, an [IllegalArgumentException] is
     * thrown.
     *
     * @param value
     * the value to check for not being empty
     * @param valueName
     * the name of the value
     * @return the value, if not blank
     */
    fun requireNotBlank(value: String, valueName: String): String {
        if (StringUtils.isBlank(value))
            throw IllegalArgumentException("$valueName must not be null or blank")
        return value
    }

    /** The value may be null but if it is not null it must not be of size 0.  */
    fun nullOrNotEmpty(value: String?, valueName: String): String? {
        if (value != null && value.length == 0)
            throw IllegalArgumentException("$valueName must either be null or not empty")
        return value
    }

    fun between(value: Double, min: Double, max: Double): Double {
        if (min <= value && value <= max)
            return value
        throw IllegalArgumentException("$value must be between $min and $max")
    }

    fun min(value: Int, min: Int): Int {
        if (min <= value)
            return value
        throw IllegalArgumentException("$value must not be smaller than $min")
    }

    fun min(value: Long, min: Long): Long {
        if (min <= value)
            return value
        throw IllegalArgumentException("$value must not be smaller than $min")
    }
}
