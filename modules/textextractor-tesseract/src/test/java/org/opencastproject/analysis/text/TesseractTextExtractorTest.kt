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
import org.junit.Assert.assertTrue

import org.opencastproject.textextractor.api.TextFrame
import org.opencastproject.textextractor.tesseract.TesseractTextExtractor
import org.opencastproject.util.IoSupport
import org.opencastproject.util.StreamHelper

import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URL

/**
 * Test case for class [TesseractTextExtractor].
 */
class TesseractTextExtractorTest {

    /** Path to the test image  */
    protected var testPath = "/image.tiff"

    /** Test image  */
    protected var testFile: File? = null

    /** The tesseract text analyzer  */
    protected var analyzer: TesseractTextExtractor? = null

    /** The text without punctuation  */
    protected var text = "Land and Vegetation Key players on the"

    /** Additional options for tesseract  */
    protected var addopts = "--psm 3"

    /**
     * @throws java.lang.Exception
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val imageUrl = this.javaClass.getResource(testPath)
        testFile = File.createTempFile("ocrtest", ".jpg")
        FileUtils.copyURLToFile(imageUrl, testFile!!)
        analyzer = TesseractTextExtractor(tesseractbinary)
        analyzer!!.additionalOptions = addopts
    }

    /**
     * @throws java.io.File.IOException
     */
    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.deleteQuietly(testFile)
    }

    /**
     * Test method for [org.opencastproject.textextractor.tesseract.TesseractTextExtractor.getBinary].
     */
    @Test
    fun testGetBinary() {
        assertEquals(tesseractbinary, analyzer!!.binary)
    }

    /**
     * Test method for [org.opencastproject.textextractor.tesseract.TesseractTextExtractor.getAdditionalOptions].
     */
    @Test
    fun testGetAdditionalOptions() {
        assertEquals(addopts, analyzer!!.additionalOptions)
    }

    /**
     * Test method for [org.opencastproject.textextractor.tesseract.TesseractTextExtractor.analyze].
     */
    @Test
    @Throws(Exception::class)
    fun testAnalyze() {
        if (!tesseractInstalled)
            return

        val frame = analyzer!!.extract(testFile!!)
        assertTrue(frame.hasText())
    }

    companion object {

        /** Path to the tesseract binary  */
        protected var tesseractbinary = TesseractTextExtractor.TESSERACT_BINARY_DEFAULT

        /** True to run the tests  */
        private var tesseractInstalled = true

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(TesseractTextExtractorTest::class.java)

        @BeforeClass
        fun testTesseract() {
            var stdout: StreamHelper? = null
            var stderr: StreamHelper? = null
            val errorBuffer = StringBuffer()
            var p: Process? = null
            try {
                val command = arrayOf(tesseractbinary, "-v")
                p = ProcessBuilder(*command).start()
                stdout = StreamHelper(p!!.inputStream)
                stderr = StreamHelper(p.errorStream, errorBuffer)
                val status = p.waitFor()
                stdout.stopReading()
                stderr.stopReading()
                if (status != 0)
                    throw IllegalStateException()
            } catch (t: Throwable) {
                logger.warn("Skipping text analysis tests due to unsatisifed tesseract installation")
                logger.warn(t.message, t)
                logger.warn(errorBuffer.toString())
                tesseractInstalled = false
            } finally {
                IoSupport.closeQuietly(stdout)
                IoSupport.closeQuietly(stderr)
                IoSupport.closeQuietly(p)
            }
        }
    }

}
