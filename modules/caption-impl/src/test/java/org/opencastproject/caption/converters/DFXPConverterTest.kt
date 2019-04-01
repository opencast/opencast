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


package org.opencastproject.caption.converters

import org.opencastproject.caption.api.Caption

import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

import junit.framework.Assert

/**
 *
 * Test class for DFXP converter.
 *
 */
class DFXPConverterTest {

    // converter
    private var dfxpConverter: DFXPCaptionConverter? = null
    // sample
    private var inputStream: InputStream? = null
    // resulting output stream
    private var outputStream: ByteArrayOutputStream? = null
    // expected output

    private val expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\""

    @Before
    @Throws(IOException::class)
    fun setUp() {
        dfxpConverter = DFXPCaptionConverter()
        inputStream = DFXPConverterTest::class.java.getResourceAsStream("/sample.dfxp.xml")
        outputStream = ByteArrayOutputStream()
    }

    @Test
    fun testDFXPConversion() {
        try {
            // verify conversion parsing and exporting without exception
            val collection = dfxpConverter!!.importCaption(inputStream!!, "en")
            dfxpConverter!!.exportCaption(outputStream, collection, "en")
            Assert.assertTrue(outputStream!!.toString("UTF-8").startsWith(expectedOutput))
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail(e.message)
        }

    }

    @After
    @Throws(IOException::class)
    fun tear() {
        IOUtils.closeQuietly(inputStream)
        IOUtils.closeQuietly(outputStream)
    }

}
