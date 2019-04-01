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

package org.opencastproject.fsresources

import org.easymock.EasyMock
import org.easymock.IAnswer
import org.junit.Before
import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

import javax.servlet.ServletOutputStream

/**
 * Test for StaticResourceServlet
 *
 * Sept 22, 2014 MH-10447, fix for files of size 2048*C bytes in copyRange()
 *
 */
class StaticResourceServletTest {

    private var servlet: StaticResourceServlet? = null
    private var ostream: ServletOutputStream? = null

    /**
     * @throws java.lang.Exception
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {

        servlet = StaticResourceServlet()

        // Mock the ostream
        // Specifically, throw exception if len is -1
        // http://docs.oracle.com/javase/1.5.0/docs/api/java/io/OutputStream.html#write%28byte[],%20int,%20int%29
        ostream = EasyMock.createNiceMock<ServletOutputStream>(ServletOutputStream::class.java)
        ostream!!.write(EasyMock.anyObject<Any>() as ByteArray, EasyMock.eq(0), EasyMock.anyInt())
        EasyMock.expectLastCall<Any>().andAnswer {
            //supply your mock implementation here...
            val len = EasyMock.getCurrentArguments()[2] as Int
            // negative len
            if (len == -1) {
                throw IndexOutOfBoundsException()
            }
            null
        }.anyTimes()
        EasyMock.replay(ostream!!)

    }

    /**
     * Helper utility for copyRange() method test
     *
     * @param byteArray
     * @param start
     * @param end
     */
    private fun testCopyRangeMethod(byteArray: ByteArray, start: Long, end: Long) {
        val instream = ByteArrayInputStream(byteArray)
        try {
            servlet!!.copyRange(instream, ostream, start, end)
        } catch (io: Exception) {
            throw AssertionError(io)
        } finally {
            try {
                instream.close()
            } catch (e: IOException) {
                // Ignore quietly
            }

        }
    }

    // Test copyRange with multiple size files
    @Test
    fun testCopyRange() {
        testCopyRangeMethod(ByteArray(2048), 0, 2048)
        testCopyRangeMethod(ByteArray(2049), 0, 2049)
        testCopyRangeMethod(ByteArray(2047), 0, 2047)
        testCopyRangeMethod(ByteArray(0), 0, 0)
    }
}
