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
import com.entwinemedia.fn.Equality.hash
import java.lang.String.format

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.P1
import com.entwinemedia.fn.Prelude

import java.util.Date
import javax.annotation.ParametersAreNonnullByDefault
import javax.annotation.concurrent.Immutable

/**
 * A property value.
 *
 *
 * The wrapped type is not exposed as a generic type parameter since [Value]s appear in contexts like lists where this type information cannot be preserved.
 * To access the wrapped type one can choose between two options.
 * If the type is known, use [.get].
 * If the type is not known, safely decompose the value with [.decompose].
 *
 * The value type is a sum type made up from
 *
 *  * [StringValue]
 *  * [DateValue]
 *  * [LongValue]
 *  * [BooleanValue]
 *
 *
 *
 * Use one of the various `mk(..)` constructors to create a new [Value].
 *
 * @see Property
 */
@ParametersAreNonnullByDefault
abstract class Value
//  public static final Class<UntypedValue> UNTYPED = UntypedValue.class;

private constructor() {

    val type: ValueType<*>
        get() = decompose(object : Fn<String, ValueType<*>>() {
            override fun apply(a: String): ValueType<*> {
                return STRING
            }
        }, object : Fn<Date, ValueType<*>>() {
            override fun apply(a: Date): ValueType<*> {
                return DATE
            }
        }, object : Fn<Long, ValueType<*>>() {
            override fun apply(a: Long): ValueType<*> {
                return LONG
            }
        }, object : Fn<Boolean, ValueType<*>>() {
            override fun apply(a: Boolean): ValueType<*> {
                return BOOLEAN
            }
        }, object : Fn<Version, ValueType<*>>() {
            override fun apply(a: Version): ValueType<*> {
                return VERSION
            }
        })

    /** Get the wrapped value.  */
    abstract fun get(): Any

    /**
     * Get the wrapped value in a type safe way. Use this method if you are
     * sure about the contained value type. Otherwise consider the use
     * of [.decompose].
     *
     * @param ev
     * Evidence type. The type parameter `A` of the evidence type
     * must match the type of the wrapped value. Any other value will result
     * in an exception being thrown.
     * @throws java.lang.RuntimeException
     * if the passed evidence `ev` does not match the type of the wrapped value
     */
    operator fun <A> get(ev: ValueType<A>): A {
        return if (type.javaClass == ev.javaClass) {
            get() as A
        } else {
            throw RuntimeException(this.toString() + " is not a " + ev.javaClass.simpleName)
        }
    }

    /**
     * Decompose (or pattern match) the value instance. Provide a function to handle each possible type.
     * Use [.doNotMatch] as a placeholder that yields an [Prelude.unexhaustiveMatch] error.
     */
    fun <A> decompose(
            stringValue: Fn<in String, out A>,
            dateValue: Fn<in Date, out A>,
            longValue: Fn<in Long, out A>,
            booleanValue: Fn<in Boolean, out A>,
            versionValue: Fn<in Version, out A>): A {
        return if (this is StringValue) {
            stringValue.apply(this.get())
        } else if (this is DateValue) {
            dateValue.apply(this.get())
        } else if (this is LongValue) {
            longValue.apply(this.get())
        } else if (this is BooleanValue) {
            booleanValue.apply(this.get())
        } else if (this is VersionValue) {
            versionValue.apply(this.get())
        } else {
            // catch bug
            Prelude.unexhaustiveMatch(this)
        }
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    //
    // Type evidence and factory classes
    //

    /**
     * ValueType gives evidence that type `A` is suitable for the creation
     * of a [Value].
     *
     *
     * This is a more advanced version of the usual `Class<A>` idiom.
     * A `ValueType` is also a constructor for [TypedValue]s of
     * the same type `A`.
     *
     * @param <A> the type to give evidence of
    </A> */
    abstract class ValueType<A>
    /** It should not be possible to inherit from outside class [Value].  */
    private constructor() {

        abstract fun mk(a: A): TypedValue<A>

        abstract fun <B> match(
                stringType: P1<out B>,
                dateType: P1<out B>,
                longType: P1<out B>,
                booleanType: P1<out B>,
                versionType: P1<out B>): B
    }

    class StringType : ValueType<String>() {
        override fun mk(a: String): TypedValue<String> {
            return Value.mk(a)
        }

        override fun <B> match(
                stringType: P1<out B>,
                dateType: P1<out B>,
                longType: P1<out B>,
                booleanType: P1<out B>,
                versionType: P1<out B>): B {
            return stringType.get1()
        }
    }

    class DateType : ValueType<Date>() {
        override fun mk(a: Date): TypedValue<Date> {
            return Value.mk(a)
        }

        override fun <B> match(
                stringType: P1<out B>,
                dateType: P1<out B>,
                longType: P1<out B>,
                booleanType: P1<out B>,
                versionType: P1<out B>): B {
            return dateType.get1()
        }
    }

    class LongType : ValueType<Long>() {
        override fun mk(a: Long): TypedValue<Long> {
            return Value.mk(a)
        }

        override fun <B> match(
                stringType: P1<out B>,
                dateType: P1<out B>,
                longType: P1<out B>,
                booleanType: P1<out B>,
                versionType: P1<out B>): B {
            return longType.get1()
        }
    }

    class BooleanType : ValueType<Boolean>() {
        override fun mk(a: Boolean): TypedValue<Boolean> {
            return Value.mk(a)
        }

        override fun <B> match(
                stringType: P1<out B>,
                dateType: P1<out B>,
                longType: P1<out B>,
                booleanType: P1<out B>,
                versionType: P1<out B>): B {
            return booleanType.get1()
        }
    }

    class VersionType : ValueType<Version>() {
        override fun mk(a: Version): TypedValue<Version> {
            return Value.mk(a)
        }

        override fun <B> match(
                stringType: P1<out B>,
                dateType: P1<out B>,
                longType: P1<out B>,
                booleanType: P1<out B>,
                versionType: P1<out B>): B {
            return versionType.get1()
        }
    }

    class UntypedType : ValueType<Any>() {
        override fun mk(a: Any): TypedValue<Any> {
            throw RuntimeException("Cannot create an untyped value")
        }

        override fun <B> match(
                stringType: P1<out B>,
                dateType: P1<out B>,
                longType: P1<out B>,
                booleanType: P1<out B>,
                versionType: P1<out B>): B {
            throw RuntimeException("Cannot match an untyped value type")
        }
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    //
    // Value classes
    //

    /** Helper type to reduce boilerplate code.  */
    // CHECKSTYLE:OFF -> class shall be public but not the constructor
    open class TypedValue<A>
    /** It should not be possible to inherit from outside class [Value].  */
    private constructor(private val value: A) : Value() {

        override fun get(): A {
            return value
        }

        override fun hashCode(): Int {
            return hash(value)
        }

        // generic implementation of equals
        // since all wrapped types cannot equal each other this is safe
        override fun equals(that: Any?): Boolean {
            return this === that || that is TypedValue<*> && eqFields((that as TypedValue<*>?)!!)
        }

        private fun eqFields(that: TypedValue<*>): Boolean {
            return eq(value, that.value)
        }

        override fun toString(): String {
            return format("%s(%s)", javaClass.simpleName, value)
        }
    }
    // CHECKSTYLE:ON

    /**
     * A value of type [String].
     */
    @Immutable
    class StringValue(value: String) : TypedValue<String>(value)

    /**
     * A value of type [java.util.Date].
     */
    class DateValue(value: Date) : TypedValue<Date>(value)

    /**
     * A value of type [java.lang.Long].
     */
    @Immutable
    class LongValue(value: Long) : TypedValue<Long>(value)

    /**
     * A value of type [java.lang.Boolean].
     */
    @Immutable
    class BooleanValue(value: Boolean) : TypedValue<Boolean>(value)

    /**
     * A value of type [Version].
     */
    @Immutable
    class VersionValue(value: Version) : TypedValue<Version>(value)

    companion object {
        val STRING = StringType()
        val DATE = DateType()
        val LONG = LongType()
        val BOOLEAN = BooleanType()
        val VERSION = VersionType()
        // TODO: rename to UNKNOWN
        val UNTYPED = UntypedType()

        //

        /**
         * Use as a placeholder that yields an [Prelude.unexhaustiveMatch] error in
         * value decomposition.
         *
         * @see .decompose
         */
        fun <B> doNotMatch(): Fn<Any, B> {
            return object : Fn<Any, B>() {
                override fun apply(a: Any): B {
                    return Prelude.unexhaustiveMatch(a)
                }
            }
        }

        /* ------------------------------------------------------------------------------------------------------------------ */

        //
        // constructor methods
        //

        /** Create a new value of type [String].  */
        fun mk(value: String): StringValue {
            return StringValue(value)
        }

        /** Create a new value of type [java.util.Date].  */
        fun mk(value: Date): DateValue {
            return DateValue(value)
        }

        /** Create a new value of type [java.lang.Long].  */
        fun mk(value: Long): LongValue {
            return LongValue(value)
        }

        /** Create a new value of type [java.lang.Boolean].  */
        fun mk(value: Boolean): BooleanValue {
            return BooleanValue(value)
        }

        /** Create a new value of type [Version].  */
        fun mk(value: Version): VersionValue {
            return VersionValue(value)
        }

        /** Generic constructor. Creates a value for any existing ValueType.  */
        fun <A> mk(mk: ValueType<A>, a: A): TypedValue<A> {
            return mk.mk(a)
        }
    }
}
