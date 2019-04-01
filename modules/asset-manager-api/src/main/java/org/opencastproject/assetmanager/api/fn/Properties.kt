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
package org.opencastproject.assetmanager.api.fn

import com.entwinemedia.fn.Equality.eq
import java.lang.String.format

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.Property
import org.opencastproject.assetmanager.api.PropertyId
import org.opencastproject.assetmanager.api.PropertyName
import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.Value
import org.opencastproject.assetmanager.api.Value.ValueType
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.assetmanager.api.query.AResult
import org.opencastproject.assetmanager.api.query.PropertyField
import org.opencastproject.mediapackage.MediaPackage

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Pred
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.StreamFold
import com.entwinemedia.fn.StreamOp
import com.entwinemedia.fn.data.Opt

import java.util.Date

/**
 * Utility functions for dealing with single [properties][Property] and property streams.
 */
object Properties {

    /**
     * [Property.getValue] as a function.
     */
    val getValue: Fn<Property, Value> = object : Fn<Property, Value>() {
        override fun apply(p: Property): Value {
            return p.value
        }
    }

    /**
     * Extract all properties contained in a result.
     * They'll appear in the order of the returned [records][org.opencastproject.assetmanager.api.query.ARecord].
     */
    fun getProperties(result: AResult): Stream<Property> {
        return result.records.bind(ARecords.getProperties)
    }

    /**
     * Create a predicate to query a property by its media package ID.
     *
     * @see PropertyId.getMediaPackageId
     */
    fun byMediaPackageId(id: String): Pred<Property> {
        return object : Pred<Property>() {
            override fun apply(p: Property): Boolean? {
                return eq(p.id.mediaPackageId, id)
            }
        }
    }

    /**
     * Create a predicate to query a property by its namespace.
     *
     * @see PropertyId.getNamespace
     */
    fun byNamespace(namespace: String): Pred<Property> {
        return object : Pred<Property>() {
            override fun apply(p: Property): Boolean? {
                return eq(p.id.namespace, namespace)
            }
        }
    }

    /**
     * Create a predicate to query a property by its name.
     *
     * @see PropertyId.getName
     */
    fun byPropertyName(propertyName: String): Pred<Property> {
        return object : Pred<Property>() {
            override fun apply(p: Property): Boolean? {
                return eq(p.id.name, propertyName)
            }
        }
    }

    /**
     * Create a predicate to query a property by its full qualified name which is the tuple of namespace and name.
     *
     * @see PropertyId.getFqn
     */
    fun byFqnName(name: PropertyName): Pred<Property> {
        return byNamespace(name.namespace).and(byPropertyName(name.name))
    }

    /**
     * Set a string property on a media package.
     *
     */
    @Deprecated("make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema} instead of creating property IDs manually")
    fun setProperty(am: AssetManager, mpId: String, namespace: String, propertyName: String, value: String): Boolean {
        return setProperty(am, mpId, namespace, propertyName, Value.mk(value))
    }

    /**
     * Set a date property on a media package.
     *
     */
    @Deprecated("make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema} instead of creating property IDs manually")
    fun setProperty(am: AssetManager, mpId: String, namespace: String, propertyName: String, value: Date): Boolean {
        return setProperty(am, mpId, namespace, propertyName, Value.mk(value))
    }

    /**
     * Set a long property on a media package.
     *
     */
    @Deprecated("make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema} instead of creating property IDs manually")
    fun setProperty(am: AssetManager, mpId: String, namespace: String, propertyName: String, value: Long?): Boolean {
        return setProperty(am, mpId, namespace, propertyName, Value.mk(value!!))
    }

    /**
     * Set a boolean property on a media package.
     *
     */
    @Deprecated("make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema} instead of creating property IDs manually")
    fun setProperty(am: AssetManager, mpId: String, namespace: String, propertyName: String, value: Boolean): Boolean {
        return setProperty(am, mpId, namespace, propertyName, Value.mk(value))
    }

    /**
     * Set a property on a media package.
     *
     */
    @Deprecated("make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema} instead of creating property IDs manually")
    fun setProperty(am: AssetManager, mpId: String, namespace: String, propertyName: String, value: Value): Boolean {
        return am.setProperty(Property.mk(PropertyId.mk(mpId, namespace, propertyName), value))
    }

    fun removeProperties(am: AssetManager, owner: String, orgId: String, mpId: String, namespace: String): Long {
        val q = am.createQuery()
        return q.delete(owner, q.propertiesOf(namespace)).where(q.organizationId(orgId).and(q.mediaPackageId(mpId))).run()
    }

    fun getProperty(am: AssetManager, mpId: String, namespace: String, propertyName: String): Opt<Property> {
        val q = am.createQuery()
        return q.select(q.properties(PropertyName.mk(namespace, propertyName)))
                .where(q.mediaPackageId(mpId).and(q.property(Value.UNTYPED, namespace, propertyName).exists()))
                .run()
                .records.bind(ARecords.getProperties).head()
    }

    /**
     * Create a function to get a value from a property.
     *
     * @param ev the expected value type
     */
    fun <A> getValue(ev: ValueType<A>): Fn<Property, A> {
        return object : Fn<Property, A>() {
            override fun apply(p: Property): A {
                return p.value.get(ev)
            }
        }
    }

    /**
     * Create a stream fold to find the first property whose [name][PropertyId.getName] matches the given one
     * and extract its value.
     *
     * @param ev the expected value type
     * @param propertyName the name of the property
     * @throws RuntimeException if the property cannot be found or its type does not match
     */
    fun <A> getValue(ev: ValueType<A>, propertyName: String): StreamFold<Property, A> {
        return StreamFold.find(byPropertyName(propertyName)).fmap(get(propertyName)).fmap(getValue(ev))
    }

    /**
     * Create a stream fold to find the first property whose [full qualified name][PropertyId.getFqn] matches the given one
     * and extract its value.
     *
     * @param ev the expected value type
     * @param name the full qualified name of the property
     * @throws RuntimeException if the property cannot be found or its type does not match
     */
    fun <A> getValue(ev: ValueType<A>, name: PropertyName): StreamFold<Property, A> {
        return StreamFold.find(byFqnName(name)).fmap(get(name)).fmap(getValue(ev))
    }

    /**
     * Create a stream fold to find the first property whose [name][PropertyId.getName] matches the given one
     * and extract its value, wrapped in an [Opt].
     *
     * @param ev the expected value type
     * @param propertyName the name of the property
     */
    fun <A> getValueOpt(ev: ValueType<A>, propertyName: String): StreamFold<Property, Opt<A>> {
        return StreamFold.find(byPropertyName(propertyName)).fmap(lift(getValue(ev)))
    }

    /**
     * Create a stream fold to find the first property whose [full qualified name][PropertyId.getFqn] matches the given one
     * and extract their values, wrapped in an [Opt].
     *
     * @param ev the expected value type
     * @param name the full qualified name of the property
     */
    fun <A> getValueOpt(ev: ValueType<A>, name: PropertyName): StreamFold<Property, Opt<A>> {
        return StreamFold.find(byFqnName(name)).fmap(lift(getValue(ev)))
    }

    /**
     * Apply #get() to the given Opt or throw a RuntimeException if none.
     * Use `propertyName` for the exception message.
     */
    operator fun get(propertyName: String): Fn<Opt<Property>, Property> {
        return object : Fn<Opt<Property>, Property>() {
            override fun apply(p: Opt<Property>): Property {
                for (pp in p) {
                    return pp
                }
                throw RuntimeException(format("Property [%s] does not exist", propertyName))
            }
        }
    }

    /**
     * Apply #get() to the given Opt or throw a RuntimeException if none.
     * Use `name` for the exception message.
     */
    operator fun get(name: PropertyName): Fn<Opt<Property>, Property> {
        return get(name.toString())
    }

    /** Create a property.  */
    fun <A> mkProperty(f: PropertyField<A>, mp: MediaPackage, value: A): Property {
        return f.mk(mp.identifier.toString(), value)
    }

    /** Create a property.  */
    fun <A> mkProperty(f: PropertyField<A>, e: Snapshot, value: A): Property {
        return f.mk(e.mediaPackage.identifier.toString(), value)
    }

    /** Create a property.  */
    fun mkProperty(mpId: String, namespace: String, name: String, value: Value): Property {
        return Property.mk(PropertyId.mk(mpId, namespace, name), value)
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    /**
     * Get a boolean value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property does not exist or if the property is not a boolean
     */
    fun getBoolean(propertyName: String): StreamFold<Property, Boolean> {
        return getValue(Value.BOOLEAN, propertyName)
    }

    /**
     * Get a boolean value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property does not exist or if the property is not a boolean
     */
    fun getBoolean(name: PropertyName): StreamFold<Property, Boolean> {
        return getValue(Value.BOOLEAN, name)
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    /**
     * Get a string value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property does not exist or if the property is not a string
     */
    fun getString(propertyName: String): StreamFold<Property, String> {
        return getValue(Value.STRING, propertyName)
    }

    /**
     * Get a string value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property does not exist or if the property is not a string
     */
    fun getString(name: PropertyName): StreamFold<Property, String> {
        return getValue(Value.STRING, name)
    }

    /**
     * Get string values from all properties.
     *
     * @throws java.lang.RuntimeException
     * if at least one property is not a string
     */
    fun getStrings(propertyName: String): StreamOp<Property, String> {
        return object : StreamOp<Property, String>() {
            override fun apply(s: Stream<out Property>): Stream<String> {
                return s.filter(byPropertyName(propertyName)).map(getValue(Value.STRING))
            }
        }
    }

    /**
     * Get string values from all properties.
     *
     * @throws java.lang.RuntimeException
     * if at least one property is not a string
     */
    fun getStrings(name: PropertyName): StreamOp<Property, String> {
        return object : StreamOp<Property, String>() {
            override fun apply(s: Stream<out Property>): Stream<String> {
                return s.filter(byFqnName(name)).map(getValue(Value.STRING))
            }
        }
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    /**
     * Get a date value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property does not exist or if the property is not a date
     */
    fun getDate(propertyName: String): StreamFold<Property, Date> {
        return getValue(Value.DATE, propertyName)
    }

    /**
     * Get a date value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property does not exist or if the property is not a date
     */
    fun getDate(name: PropertyName): StreamFold<Property, Date> {
        return getValue(Value.DATE, name)
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    /**
     * Get a long value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property does not exist or if the property is not a long
     */
    fun getLong(propertyName: String): StreamFold<Property, Long> {
        return getValue(Value.LONG, propertyName)
    }

    /**
     * Get a long value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property does not exist or if the property is not a long
     */
    fun getLong(name: PropertyName): StreamFold<Property, Long> {
        return getValue(Value.LONG, name)
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    /**
     * Get a string value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property is not a string
     */
    fun getStringOpt(propertyName: String): StreamFold<Property, Opt<String>> {
        return getValueOpt(Value.STRING, propertyName)
    }

    /**
     * Get a string value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property is not a string
     */
    fun getStringOpt(name: PropertyName): StreamFold<Property, Opt<String>> {
        return getValueOpt(Value.STRING, name)
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    /**
     * Get a date value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property is not a date
     */
    fun getDateOpt(propertyName: String): StreamFold<Property, Opt<Date>> {
        return getValueOpt(Value.DATE, propertyName)
    }

    /**
     * Get a date value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property is not a date
     */
    fun getDateOpt(name: PropertyName): StreamFold<Property, Opt<Date>> {
        return getValueOpt(Value.DATE, name)
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    /**
     * Get a long value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property is not a long
     */
    fun getLongOpt(propertyName: String): StreamFold<Property, Opt<Long>> {
        return getValueOpt(Value.LONG, propertyName)
    }

    /**
     * Get a long value. Uses the first property with the given name.
     *
     * @throws java.lang.RuntimeException
     * if the property is not a long
     */
    fun getLongOpt(name: PropertyName): StreamFold<Property, Opt<Long>> {
        return getValueOpt(Value.LONG, name)
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    private fun <A, B> lift(f: Fn<in A, out B>): Fn<Opt<A>, Opt<B>> {
        return object : Fn<Opt<A>, Opt<B>>() {
            override fun apply(`as`: Opt<A>): Opt<B> {
                return `as`.map(f)
            }
        }
    }
}
