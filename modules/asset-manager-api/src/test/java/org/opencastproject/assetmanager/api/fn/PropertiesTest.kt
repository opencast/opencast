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

import com.entwinemedia.fn.Stream.`$`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.opencastproject.assetmanager.api.fn.Properties.mkProperty

import org.opencastproject.assetmanager.api.Property
import org.opencastproject.assetmanager.api.PropertyName
import org.opencastproject.assetmanager.api.Value

import com.entwinemedia.fn.Pred
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt

import org.junit.Test
import org.junit.runner.RunWith

import java.util.Date

import junitparams.JUnitParamsRunner
import junitparams.Parameters

@RunWith(JUnitParamsRunner::class)
class PropertiesTest {

    @Test
    @Parameters("unknown.namespace | 0", "org.opencastproject.approval | 3", "org.opencastproject.comment | 2")
    @Throws(Exception::class)
    fun testByNamespace(namespace: String, expectedCount: Int) {
        assertEquals(expectedCount.toLong(), filterCount(Properties.byNamespace(namespace)).toLong())
    }

    @Test
    @Parameters("unknown.name | 0", "approved | 1", "date | 1", "comment | 2")
    @Throws(Exception::class)
    fun testByPropertyName(propertyName: String, expectedCount: Int) {
        assertEquals(expectedCount.toLong(), filterCount(Properties.byPropertyName(propertyName)).toLong())
    }

    @Test
    @Parameters("unknown.name | approved | 0", "org.opencastproject.approval | approved | 1", "org.opencastproject.approval | date | 1", "org.opencastproject.approval | comment | 1")
    @Throws(Exception::class)
    fun testByFqnName(namespace: String, propertyName: String, expectedCount: Int) {
        assertEquals(expectedCount.toLong(), filterCount(Properties.byFqnName(PropertyName.mk(namespace, propertyName))).toLong())
    }

    @Test
    @Parameters("mp-x | 0", "mp-1 | 2", "mp-2 | 1", "mp-3 | 2")
    @Throws(Exception::class)
    fun testByMediaPackageId(mpId: String, expectedCount: Int) {
        assertEquals(expectedCount.toLong(), filterCount(Properties.byMediaPackageId(mpId)).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testGetValue() {
        assertEquals(
                `$`<TypedValue<out Serializable>>(Value.mk(false), Value.mk("Bad audio"), Value.mk(Date(0)), Value.mk("Hello world"), Value.mk(1L)).toList(),
                ps.map(Properties.getValue).toList())
    }

    @Test(expected = java.lang.RuntimeException::class)
    @Throws(Exception::class)
    fun testGetValueWithExpectedTypeTypesDoNotMatch() {
        ps.map(Properties.getValue(Value.STRING)).toList()
    }

    @Test
    @Throws(Exception::class)
    fun testGetValueWithExpectedType() {
        assertEquals(
                `$`("Bad audio", "Hello world").toList(),
                ps.filter(Properties.byPropertyName("comment")).map(Properties.getValue(Value.STRING)).toList())
    }

    @Test
    @Throws(Exception::class)
    fun testGetValueFold() {
        assertEquals("Bad audio", ps.apply(Properties.getValue(Value.STRING, "comment")))
        assertEquals(false, ps.apply(Properties.getValue(Value.BOOLEAN, "approved")))
        assertEquals(Date(0), ps.apply(Properties.getValue(Value.DATE, "date")))
    }

    @Test(expected = java.lang.RuntimeException::class)
    @Throws(Exception::class)
    fun testGetValueFoldNotFound() {
        ps.apply(Properties.getValue(Value.STRING, "unknown"))
    }

    @Test(expected = java.lang.RuntimeException::class)
    @Throws(Exception::class)
    fun testGetValueFoldTypeDoesNotMatch() {
        ps.apply(Properties.getValue(Value.BOOLEAN, "comment"))
    }

    @Test
    @Throws(Exception::class)
    fun testGetValueFoldOpt() {
        assertEquals(Opt.some("Bad audio"), ps.apply(Properties.getValueOpt(Value.STRING, "comment")))
        assertEquals(Opt.some(false), ps.apply(Properties.getValueOpt(Value.BOOLEAN, "approved")))
        assertEquals(Opt.some(Date(0)), ps.apply(Properties.getValueOpt(Value.DATE, "date")))
    }

    @Test
    @Throws(Exception::class)
    fun testGetValueFoldOptNotFound() {
        assertEquals(Opt.none<Any>(), ps.apply(Properties.getValueOpt(Value.STRING, "unknown")))
    }

    @Test(expected = java.lang.RuntimeException::class)
    @Throws(Exception::class)
    fun testGetValueFoldOptTypeDoesNotMatch() {
        assertEquals(Opt.none<Any>(), ps.apply(Properties.getValueOpt(Value.BOOLEAN, "comment")))
    }

    @Test
    @Throws(Exception::class)
    fun testGetStrings() {
        assertEquals(`$`("Bad audio", "Hello world").toList(), ps.apply(Properties.getStrings("comment")).toList())
        assertEquals(`$`("Hello world").toList(), ps.apply(Properties.getStrings(PropertyName.mk("org.opencastproject.comment", "comment"))).toList())
    }

    @Test
    @Throws(Exception::class)
    fun testGetStringsNotFound() {
        assertTrue(ps.apply(Properties.getStrings("unknown")).isEmpty)
        assertTrue(ps.apply(Properties.getStrings(PropertyName.mk("org.opencastproject.approval", "unknown"))).isEmpty)
    }

    @Test
    @Throws(Exception::class)
    fun testGetBoolean() {
        assertEquals(false, ps.apply(Properties.getBoolean("approved")))
    }

    @Test(expected = java.lang.RuntimeException::class)
    @Throws(Exception::class)
    fun testGetBooleanNotFound() {
        ps.apply(Properties.getBoolean("unknown"))
    }

    @Test
    @Throws(Exception::class)
    fun testGetString() {
        assertEquals("Bad audio", ps.apply(Properties.getString("comment")))
    }

    @Test(expected = java.lang.RuntimeException::class)
    @Throws(Exception::class)
    fun testGetStringNotFound() {
        ps.apply(Properties.getString("unknown"))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDate() {
        assertEquals(Date(0), ps.apply(Properties.getDate("date")))
    }

    @Test(expected = java.lang.RuntimeException::class)
    @Throws(Exception::class)
    fun testGetDateNotFound() {
        ps.apply(Properties.getDate("unknown"))
    }

    @Test
    @Throws(Exception::class)
    fun testGetLong() {
        assertEquals(1L, ps.apply(Properties.getLong("count")))
    }

    @Test(expected = java.lang.RuntimeException::class)
    @Throws(Exception::class)
    fun testGetNotFoundLong() {
        ps.apply(Properties.getLong("unknown"))
    }

    @Test
    @Throws(Exception::class)
    fun testGetStringOpt() {
        assertEquals(Opt.some("Bad audio"), ps.apply(Properties.getStringOpt("comment")))
        assertEquals(Opt.none<Any>(), ps.apply(Properties.getStringOpt("unknown")))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDateOpt() {
        assertEquals(Opt.some(Date(0)), ps.apply(Properties.getDateOpt("date")))
        assertEquals(Opt.none<Any>(), ps.apply(Properties.getDateOpt("unknown")))
    }

    @Test
    @Throws(Exception::class)
    fun testGetLongOpt() {
        assertEquals(Opt.some(1L), ps.apply(Properties.getLongOpt("count")))
        assertEquals(Opt.none<Any>(), ps.apply(Properties.getLongOpt("unknown")))
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    private fun filterCount(p: Pred<Property>): Int {
        return ps.filter(p).toList().size
    }

    companion object {
        internal val pb1 = mkProperty("mp-1", "org.opencastproject.approval", "approved", Value.mk(false))
        internal val ps1 = mkProperty("mp-1", "org.opencastproject.approval", "comment", Value.mk("Bad audio"))
        internal val pd1 = mkProperty("mp-2", "org.opencastproject.approval", "date", Value.mk(Date(0)))
        internal val ps2 = mkProperty("mp-3", "org.opencastproject.comment", "comment", Value.mk("Hello world"))
        internal val pl1 = mkProperty("mp-3", "org.opencastproject.comment", "count", Value.mk(1L))

        internal val ps = `$`(pb1, ps1, pd1, ps2, pl1)
    }
}
