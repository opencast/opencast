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

package org.opencastproject.analysis.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import org.opencastproject.textextractor.api.TextFrame
import org.opencastproject.textextractor.tesseract.TesseractTextFrame

import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.Test

import java.io.InputStream

/**
 * Test case for class [TesseractTextFrame].
 */
class TesseractTextFrameTest {

    /** Path to the test frame  */
    protected var testFile = "/image.txt"

    /** The test frame  */
    protected var textFrame: TextFrame? = null

    /** The text used for the tests  */
    protected var text = "Land and Vegetation: Key players on the"

    /** Number of lines on the frame  */
    protected var linesOnFrame = 2

    /**
     * @throws java.lang.Exception
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        var `is`: InputStream? = null
        try {
            `is` = javaClass.getResourceAsStream(testFile)
            textFrame = TesseractTextFrame.parse(`is`)
        } finally {
            IOUtils.closeQuietly(`is`)
        }
    }

    /**
     * Test method for [org.opencastproject.textextractor.tesseract.TesseractTextFrame.getLines].
     */
    @Test
    fun testGetText() {
        assertEquals(linesOnFrame.toLong(), textFrame!!.lines.size.toLong())
        assertEquals(text, textFrame!!.lines[0].text)
    }

    /**
     * Test method for [org.opencastproject.textextractor.tesseract.TesseractTextFrame.hasText].
     */
    @Test
    fun testHasText() {
        assertTrue(textFrame!!.hasText())
        assertFalse(TesseractTextFrame().hasText())
    }

}
