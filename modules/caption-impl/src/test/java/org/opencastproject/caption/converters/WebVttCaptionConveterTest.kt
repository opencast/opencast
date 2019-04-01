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
import org.opencastproject.caption.impl.CaptionImpl
import org.opencastproject.caption.impl.TimeImpl

import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList

/**
 * Test class for WebVTT format.
 *
 */
class WebVttCaptionConveterTest {

    private var format: WebVttCaptionConverter? = null
    private var outputStream: ByteArrayOutputStream? = null
    // Expected output
    private val expectedOutput = ("WEBVTT\n\n00:00:49.520 --> 00:00:52.961\n" + CAPTION_LINE
            + "1.\n\n00:00:54.123 --> 00:00:56.456\n" + CAPTION_LINE + "2.\n\n")

    @Before
    @Throws(IOException::class)
    fun setUp() {
        format = WebVttCaptionConverter()
        outputStream = ByteArrayOutputStream()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        IOUtils.closeQuietly(outputStream)
    }

    @Test
    @Throws(Exception::class)
    fun testExport() {
        val captionList = ArrayList<Caption>()
        val captionLines1 = arrayOfNulls<String>(1)
        captionLines1[0] = CAPTION_LINE + "1."
        captionList.add(CaptionImpl(TimeImpl(0, 0, 49, 520), TimeImpl(0, 0, 52, 961), captionLines1))
        val captionLines2 = arrayOfNulls<String>(1)
        captionLines2[0] = CAPTION_LINE + "2."
        captionList.add(CaptionImpl(TimeImpl(0, 0, 54, 123), TimeImpl(0, 0, 56, 456), captionLines2))

        format!!.exportCaption(outputStream, captionList, null)
        Assert.assertTrue(outputStream!!.toString("UTF-8") == expectedOutput)
    }

    companion object {

        private val CAPTION_LINE = "This is caption testing line "
    }
}
