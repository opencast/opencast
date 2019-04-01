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

@Immutable
@ParametersAreNonnullByDefault
class PropertyId
/**
 * Create a new property ID.
 */
(mpId: String, namespace: String, name: String) : Serializable {

    val mediaPackageId: String
    val namespace: String
    val name: String

    val fqn: PropertyName
        get() = PropertyName.mk(namespace, name)

    init {
        this.mediaPackageId = RequireUtil.notEmpty(mpId, "mpId")
        this.namespace = RequireUtil.notEmpty(namespace, "namespace")
        this.name = RequireUtil.notEmpty(name, "name")
    }

    //

    override fun hashCode(): Int {
        return Equality.hash(mediaPackageId, namespace, name)
    }

    override fun equals(that: Any?): Boolean {
        return this === that || that is PropertyId && eqFields((that as PropertyId?)!!)
    }

    private fun eqFields(that: PropertyId): Boolean {
        return eq(mediaPackageId, that.mediaPackageId) && eq(namespace, that.namespace) && eq(name, that.name)
    }

    override fun toString(): String {
        return format("PropertyId(%s, %s, %s)", mediaPackageId, namespace, name)
    }

    companion object {
        private const val serialVersionUID = -2614578081057869958L

        /**
         * Create a new property ID from the given parameters.
         */
        fun mk(mpId: String, namespace: String, propertyName: String): PropertyId {
            return PropertyId(mpId, namespace, propertyName)
        }

        /**
         * Create a new property ID from the given parameters.
         */
        fun mk(mpId: String, fqn: PropertyName): PropertyId {
            return PropertyId(mpId, fqn.namespace, fqn.name)
        }
    }
}
