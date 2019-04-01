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
package org.opencastproject.assetmanager.api.query

import org.opencastproject.assetmanager.api.Value
import org.opencastproject.assetmanager.api.Value.ValueType
import org.opencastproject.assetmanager.api.Version

import java.util.Date

/**
 * The schema class helps to build type safe and easy to use property schemas.
 * It makes code using properties more readable and reliable.
 */
abstract class PropertySchema
/**
 * Create a new property schema.
 *
 * @param q a query builder
 * @param namespace
 */
(protected val q: AQueryBuilder, protected val namespace: String) {

    /** Get the namespace of the schema.  */
    fun namespace(): String {
        return namespace
    }

    /** Get a predicate that matches if a property of the schema's namespace exists.  */
    fun hasPropertiesOfNamespace(): Predicate {
        return q.hasPropertiesOf(namespace)
    }

    /** Get a target to select all properties of the schema's namespace.  */
    fun allProperties(): Target {
        return q.propertiesOf(namespace)
    }

    /** Generic property field constructor.  */
    protected fun <A> prop(ev: ValueType<A>, name: String): PropertyField<A> {
        return q.property(ev, namespace, name)
    }

    /** Create a property field for Strings.  */
    protected fun stringProp(name: String): PropertyField<String> {
        return prop(Value.STRING, name)
    }

    /** Create a property field for Longs.  */
    protected fun longProp(name: String): PropertyField<Long> {
        return prop(Value.LONG, name)
    }

    /** Create a property field for Booleans.  */
    protected fun booleanProp(name: String): PropertyField<Boolean> {
        return prop(Value.BOOLEAN, name)
    }

    /** Create a property field for Dates.  */
    protected fun dateProp(name: String): PropertyField<Date> {
        return prop(Value.DATE, name)
    }

    /** Create a property field for Versions.  */
    protected fun versionProp(name: String): PropertyField<Version> {
        return prop(Value.VERSION, name)
    }
}
