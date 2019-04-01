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

package org.opencastproject.assetmanager.api.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThat

import org.opencastproject.assetmanager.api.Value
import org.opencastproject.assetmanager.api.Version

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Fns

import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test

import java.util.Date

/**
 * Test the [org.opencastproject.assetmanager.api.Value] type.
 *
 *
 * Not in the same package as [org.opencastproject.assetmanager.api.Value]
 * on purpose to showcase method visibility.
 */
class ValueTest {
    @Test
    @Throws(Exception::class)
    fun testGet() {
        Assert.assertEquals("a value", Value.mk("a value").get(Value.STRING))
        assertThat(Value.mk(1511L).get(Value.LONG), Matchers.instanceOf(Long::class.java))
        assertEquals(Value.mk(10L).get(), 10L)
    }

    @Test(expected = java.lang.RuntimeException::class)
    @Throws(Exception::class)
    fun testGetTypesDoNotMatch() {
        Value.mk(1511L).get(Value.STRING)
    }

    @Test
    fun testDecompose() {
        val value = Value.mk(1511L).decompose(Fns.id(), Fns.id(), Fns.id(), Fns.id(), Fns.id())
        assertEquals(1511L, value)
        val valueAsString = Value.mk(1511L).decompose(
                Value.doNotMatch(),
                asString.o(Fns.id()),
                asString.o(Fns.id()),
                Value.doNotMatch(),
                Value.doNotMatch())
        assertEquals("1511", valueAsString)
    }

    @Test(expected = java.lang.Error::class)
    fun testDecomposeNoMatch() {
        Value.mk(Date()).decompose(
                Value.doNotMatch(),
                Value.doNotMatch<Any>(),
                Value.doNotMatch(),
                Value.doNotMatch(),
                Value.doNotMatch())
    }

    @Test
    fun testMkGeneric() {
        assertEquals("sl", Value.mk(Value.STRING, "sl").get())
        Value.StringValue("23")
    }

    @Test
    fun testEquality() {
        assertEquals(Value.mk(true), Value.mk(true))
        assertEquals(Value.mk(12L), Value.mk(12L))
        assertEquals(Value.mk("test"), Value.mk("test"))
        val now = Date()
        assertEquals(Value.mk(now), Value.mk(now))
        assertNotEquals(Value.mk(true), Value.mk(false))
        assertNotEquals(Value.mk(11L), Value.mk(15L))
        assertNotEquals(Value.mk("test"), Value.mk("teest"))
        assertNotEquals(Value.mk(now), Value.mk(Date(0)))
    }

    companion object {

        private val asString = object : Fn<Any, String>() {
            override fun apply(o: Any): String {
                return o.toString()
            }
        }
    }
}
