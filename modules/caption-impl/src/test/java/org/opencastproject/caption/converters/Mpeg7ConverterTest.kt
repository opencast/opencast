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
import org.opencastproject.caption.api.CaptionConverterException
import org.opencastproject.caption.api.IllegalTimeFormatException
import org.opencastproject.caption.api.Time
import org.opencastproject.caption.impl.TimeImpl

import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

import junit.framework.Assert

/**
 * Test class for Mpeg7 format.
 *
 */
class Mpeg7ConverterTest {

    // SubRip converter
    private var converter: Mpeg7CaptionConverter? = null
    // mpeg7 sample
    private var inputStream: InputStream? = null
    // output stream
    private var outputStream: ByteArrayOutputStream? = null
    // expected second segment start time
    private var time: Time? = null

    @Before
    @Throws(IOException::class, IllegalTimeFormatException::class)
    fun setUp() {
        converter = Mpeg7CaptionConverter()
        inputStream = Mpeg7ConverterTest::class.java.getResourceAsStream(FILE)
        outputStream = ByteArrayOutputStream()
        time = TimeImpl(0, 0, 5, 89)
    }

    @Test
    fun testImportAndExport() {
        try {
            // Test import from example file
            val collection = testImport(inputStream)

            IOUtils.closeQuietly(inputStream)

            converter!!.exportCaption(outputStream, collection, LANGUAGE)

            inputStream = ByteArrayInputStream(outputStream!!.toByteArray())

            testImport(inputStream)

        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail(e.message)
        }

    }

    fun testImport(inputStream: InputStream?): List<Caption> {
        var collection: List<Caption>? = null

        try {
            collection = converter!!.importCaption(inputStream!!, LANGUAGE)

            val nbCaption = collection.size

            // Check size
            Assert.assertEquals(25, nbCaption)

            // Check the last caption value
            Assert.assertEquals(EXPECTED_LAST_CAPTION, collection[nbCaption - 1].caption[0])

            // Check start time from second segment
            Assert.assertEquals(0, collection[1].startTime.compareTo(time!!))

        } catch (e: CaptionConverterException) {
            e.printStackTrace()
            Assert.fail(e.message)
        }

        return collection
    }

    @After
    @Throws(IOException::class)
    fun tear() {
        IOUtils.closeQuietly(inputStream)
        IOUtils.closeQuietly(outputStream)
    }

    companion object {
        // expected output
        private val EXPECTED_LAST_CAPTION = "ENJOYS GETTING QUESTIONS"
        // Captions language
        private val LANGUAGE = "en"
        // Sample file
        private val FILE = "/sample.mpeg7.xml"
    }

}
