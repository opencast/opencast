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
package org.opencastproject.assetmanager.api

import com.entwinemedia.fn.Equality.eq
import java.lang.String.format

import org.opencastproject.util.RequireUtil

import com.entwinemedia.fn.Equality

import java.io.Serializable

import javax.annotation.ParametersAreNonnullByDefault
import javax.annotation.concurrent.Immutable

/**
 * A full qualified property name.
 */
@Immutable
@ParametersAreNonnullByDefault
class PropertyName
/**
 * Create a new full qualified property name.
 */
(namespace: String, name: String) : Serializable {

    /** Return the namespace.  */
    val namespace: String
    /** Return the namespace local name.  */
    val name: String

    init {
        this.namespace = RequireUtil.notEmpty(namespace, "namespace")
        this.name = RequireUtil.notEmpty(name, "name")
    }

    //

    override fun hashCode(): Int {
        return Equality.hash(namespace, name)
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is PropertyName && eqFields((that as PropertyName?)!!)
    }

    private fun eqFields(that: PropertyName): Boolean {
        return eq(namespace, that.namespace) && eq(name, that.name)
    }

    override fun toString(): String {
        return format("PropertyFqn(%s, %s)", namespace, name)
    }

    companion object {
        private const val serialVersionUID = -7542245565083273994L

        /**
         * Create a new full qualified property name.
         */
        fun mk(namespace: String, name: String): PropertyName {
            return PropertyName(namespace, name)
        }
    }
}
