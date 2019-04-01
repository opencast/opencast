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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodeDate
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodeDuration
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodePeriod
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodeTemporal
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.encodeDate
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.encodeDuration
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils.encodePeriod
import org.opencastproject.metadata.dublincore.TestUtil.createDate
import org.opencastproject.metadata.dublincore.TestUtil.precisionDay
import org.opencastproject.metadata.dublincore.TestUtil.precisionSecond

import com.entwinemedia.fn.data.Opt

import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * Test cases for [org.opencastproject.metadata.dublincore.EncodingSchemeUtils].
 */
class EncodingSchemeUtilsTest {

    private val logger = LoggerFactory.getLogger(EncodingSchemeUtilsTest::class.java)

    @Test
    fun printTimeZone() {
        // Not a test case...
        logger.info("Time zone = " + TimeZone.getDefault())
    }

    @Test
    fun testEncodeDate() {
        val now = Date()
        assertEquals(4, encodeDate(now, Precision.Year).value.length.toLong())
        assertEquals(3, encodeDate(now, Precision.Day).value.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size.toLong())
        assertEquals("2009-01-01T00:00:00Z".length.toLong(), encodeDate(now, Precision.Second).value.length.toLong())
        assertEquals(Opt.some<EName>(DublinCore.ENC_SCHEME_W3CDTF), encodeDate(now, Precision.Year).encodingScheme)
        // Test symmetry
        assertEquals(decodeDate(encodeDate(now, Precision.Second)), precisionSecond(now))
        assertEquals(decodeDate(encodeDate(createDate(1999, 3, 21, 14, 0, 0), Precision.Day)), precisionDay(createDate(
                1999, 3, 21, 14, 0, 0)))
        assertEquals("1724-04-22", encodeDate(createDate(1724, 4, 22, 18, 30, 0), Precision.Day).value)
        assertEquals("1724-04-22T18:30:00Z", encodeDate(createDate(1724, 4, 22, 18, 30, 0, "UTC"), Precision.Second)
                .value)
        assertEquals("1724-04-22T17:30:10Z", encodeDate(createDate(1724, 4, 22, 17, 30, 10, "UTC"), Precision.Second)
                .value)
        assertEquals("1724-04-22T17:30Z", encodeDate(createDate(1724, 4, 22, 17, 30, 25, "UTC"), Precision.Minute)
                .value)
        assertEquals("1999-03-21", encodeDate(createDate(1999, 3, 21, 18, 30, 25), Precision.Day).value)
        //
        logger.info(encodeDate(now, Precision.Day).value)
        logger.info(encodeDate(now, Precision.Second).value)
    }

    @Test
    fun testEncodeFraction() {
        val a = Date(1)
        val b = Date(125)
        val c = Date(100)
        assertEquals("1970-01-01T00:00:00.001Z", encodeDate(a, Precision.Fraction).value)
        assertEquals("1970-01-01T00:00:00.125Z", encodeDate(b, Precision.Fraction).value)
        assertEquals("1970-01-01T00:00:00.100Z", encodeDate(c, Precision.Fraction).value)
    }

    @Test
    fun testEncodePeriod() {
        val a = encodePeriod(DCMIPeriod(createDate(2007, 2, 10, 12, 0, 0), createDate(2009, 12, 24, 10, 0,
                0), "long time"), Precision.Day)
        assertEquals("start=2007-02-10; end=2009-12-24; name=long time; scheme=W3C-DTF;", a.value)
        assertEquals(Opt.some<EName>(DublinCore.ENC_SCHEME_PERIOD), a.encodingScheme)
        val b = encodePeriod(DCMIPeriod(createDate(2007, 2, 10, 12, 0, 0), null), Precision.Day)
        assertEquals("start=2007-02-10; scheme=W3C-DTF;", b.value)
    }

    @Test
    fun testDecodeDate() {
        assertEquals(createDate(2008, 10, 1, 0, 0, 0), decodeDate(DublinCoreValue.mk("2008-10-01")))
        assertEquals(createDate(1999, 3, 21, 14, 30, 0, "UTC"), decodeDate(DublinCoreValue.mk("1999-03-21T14:30Z")))
        assertEquals(createDate(1999, 3, 21, 14, 30, 0, "UTC"), decodeDate(DublinCoreValue.mk("1999-03-21T14:30:00Z")))
        assertEquals(createDate(1999, 3, 21, 14, 30, 15, "UTC"), decodeDate(DublinCoreValue.mk("1999-03-21T14:30:15Z")))
        assertEquals(createDate(2001, 9, 11, 0, 0, 0), decodeDate(DublinCoreValue.mk("2001-09-11")))
        assertEquals(createDate(2011, 2, 24, 7, 0, 0, "UTC"), decodeDate(DublinCoreValue.mk("1298530800000")))
        assertEquals(createDate(2011, 2, 24, 7, 0, 0, "UTC"), decodeDate("1298530800000"))
        logger.info(decodeDate(DublinCoreValue.mk("2009-03-31"))!!.toString())
        logger.info(decodeDate(DublinCoreValue.mk("2009-09-11"))!!.toString())
        logger.info(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(decodeDate(DublinCoreValue.mk(
                "2009-03-31"))))
    }

    @Test
    fun testDecodePeriod() {
        val a = decodePeriod(DublinCoreValue.mk("start=2008-10-01; end=2009-01-01;"))
        assertEquals(createDate(2008, 10, 1, 0, 0, 0), a!!.start)
        assertEquals(createDate(2009, 1, 1, 0, 0, 0), a.end)
        val b = decodePeriod(DublinCoreValue.mk("start=2008-10-01; end=2009-01-01"))
        assertEquals(createDate(2008, 10, 1, 0, 0, 0), b!!.start)
        assertEquals(createDate(2009, 1, 1, 0, 0, 0), b.end)
        val c = decodePeriod(DublinCoreValue.mk("start=2008-10-01"))
        assertEquals(createDate(2008, 10, 1, 0, 0, 0), c!!.start)
        assertNull(c.end)
        val d = decodePeriod(DublinCoreValue.mk("start=2008-10-01T10:20Z; end=2009-01-01; scheme=UNKNOWN"))
        assertNull(d)
        val e = decodePeriod(DublinCoreValue.mk("start=2008-10-01T10:20Z; end=2009-01-01; scheme=W3C-DTF"))
        assertNotNull(e)
        val f = decodePeriod(DublinCoreValue.mk("start=2008-10-01ERR; end=2009-01-01; scheme=W3C-DTF"))
        assertNull(f)
    }

    @Test
    fun testDecodeTemporal() {
        val match = object : Temporal.Match<Int> {
            override fun period(period: DCMIPeriod?): Int {
                return 1
            }

            override fun instant(instant: Date?): Int {
                return 2
            }

            override fun duration(duration: Long): Int {
                return 3
            }
        }
        val durationMatch = object : Temporal.Match<Long> {
            override fun period(period: DCMIPeriod?): Long? {
                throw RuntimeException()
            }

            override fun instant(instant: Date?): Long? {
                throw RuntimeException()
            }

            override fun duration(duration: Long): Long? {
                return duration
            }
        }
        assertSame(1, decodeTemporal(DublinCoreValue.mk("start=2008-10-01; end=2009-01-01;"))!!.fold(match))
        assertSame(2, decodeTemporal(DublinCoreValue.mk("2008-10-01"))!!.fold(match))
        assertSame(2, decodeTemporal(DublinCoreValue.mk("2008-10-01T10:30:05Z"))!!.fold(match))
        assertSame(1, decodeTemporal(DublinCoreValue.mk("start=2008-10-01T10:20Z; end=2009-01-01; scheme=W3C-DTF"))!!.fold(match))
        assertSame(3, decodeTemporal(DublinCoreValue.mk("PT10H5M"))!!.fold(match))
        assertEquals(10L * 60 * 60 * 1000 + 5 * 60 * 1000,
                decodeTemporal(DublinCoreValue.mk("PT10H5M"))!!.fold(durationMatch) as Long)
        assertEquals(10L * 60 * 60 * 1000 + (5 * 60 * 1000).toLong() + (28 * 1000).toLong(),
                decodeTemporal(DublinCoreValue.mk("PT10H5M28S"))!!.fold(durationMatch) as Long)
    }

    @Test
    fun testEncodeDuration() {
        val d1 = 2743414L
        assertEquals(d1, decodeDuration(encodeDuration(d1).value))
        val d2 = 78534795325L
        assertEquals(d2, decodeDuration(encodeDuration(d2).value))
        val d3 = 234L
        assertEquals(d3, decodeDuration(encodeDuration(d3).value))
        assertEquals(1 * 1000 * 60 * 60 + 10 * 1000 * 60 + 5 * 1000, decodeDuration("01:10:05"))

        assertEquals(Opt.some<EName>(DublinCore.ENC_SCHEME_ISO8601), encodeDuration(d3).encodingScheme)

        assertNull(decodeDuration(DublinCoreValue.mk("bla")))
        assertNull(decodeDuration(DublinCoreValue.mk(encodeDuration(d1).value, DublinCore.LANGUAGE_UNDEFINED,
                DublinCore.ENC_SCHEME_BOX)))
    }

}
