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
package org.opencastproject.metadata.dublincore

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThat
import org.opencastproject.metadata.dublincore.TestUtil.read

import org.hamcrest.CustomTypeSafeMatcher
import org.hamcrest.Matcher
import org.junit.Test

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Arrays
import java.util.HashSet

class DublinCoreUtilTest {
    @Test
    @Throws(Exception::class)
    fun testChecksumDistinct() {
        assertThat(
                Arrays.asList<String>(
                        checksum("/checksum/dublincore1-1.xml"),
                        checksum("/checksum/dublincore1-2.xml"),
                        checksum("/checksum/dublincore1-3.xml"),
                        checksum("/checksum/dublincore1-4.xml"),
                        checksum("/checksum/dublincore1-5.xml"),
                        checksum("/checksum/dublincore1-6.xml"),
                        checksum("/checksum/dublincore1-7.xml"),
                        checksum("/checksum/dublincore1-8.xml"),
                        checksum("/checksum/dublincore1-9.xml"),
                        checksum("/checksum/dublincore1-A.xml")
                ),
                this.isDistinct())
    }

    @Test
    @Throws(Exception::class)
    fun testChecksumEqual() {
        assertThat(
                Arrays.asList<String>(
                        checksum("/checksum/dublincore2-1.xml"),
                        checksum("/checksum/dublincore2-2.xml"),
                        checksum("/checksum/dublincore2-3.xml"),
                        checksum("/checksum/dublincore2-4.xml")
                ),
                this.allEqual())
    }

    /** Make sure no character contains a null byte, so that it is safe to use 0 as a separator.  */
    @Test
    @Throws(Exception::class)
    fun testUtf8CodePointsDoNotContainNullByte() {
        var i = Character.MIN_VALUE.toInt() + 1
        while (i <= Character.MAX_VALUE.toInt()) {
            for (b in Character.valueOf(i.toChar()).toString().toByteArray(StandardCharsets.UTF_8)) {
                assertNotEquals(0, b.toLong())
            }
            i++
        }
    }

    @Test
    @Throws(Exception::class)
    fun testDigestingSplitStrings() {
        val md1 = MessageDigest.getInstance("md5")
        md1.update("haus".toByteArray(StandardCharsets.UTF_8))
        md1.update("meister".toByteArray(StandardCharsets.UTF_8))
        val digest1 = md1.digest()
        val md2 = MessageDigest.getInstance("md5")
        md2.update("hausmeister".toByteArray(StandardCharsets.UTF_8))
        val digest2 = md2.digest()
        assertArrayEquals(digest1, digest2)
    }

    //
    //
    //

    @Throws(Exception::class)
    private fun checksum(dcFile: String): String? {
        return DublinCoreUtil.calculateChecksum(read(dcFile)).value
    }

    private fun <A> isDistinct(): Matcher<List<A>> {
        return object : CustomTypeSafeMatcher<List<A>>("a list containing distinct elements") {
            override fun matchesSafely(list: List<A>): Boolean {
                return list.size == HashSet(list).size
            }
        }
    }

    private fun <A> allEqual(): Matcher<List<A>> {
        return object : CustomTypeSafeMatcher<List<A>>("a list containing equal elements") {
            override fun matchesSafely(list: List<A>): Boolean {
                return HashSet(list).size <= 1
            }
        }
    }
}
