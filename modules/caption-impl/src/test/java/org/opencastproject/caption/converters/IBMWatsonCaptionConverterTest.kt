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
import org.opencastproject.caption.api.Time

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.InputStream

class IBMWatsonCaptionConverterTest {
    private var converter: IBMWatsonCaptionConverter? = null
    private var inputStream: InputStream? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        converter = IBMWatsonCaptionConverter()
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    @Throws(Exception::class)
    fun testImportCaptionPush() {
        inputStream = IBMWatsonCaptionConverterTest::class.java.getResourceAsStream("/pulled_transcription.json")
        importCaption()
    }

    @Test
    @Throws(Exception::class)
    fun testImportCaptionPull() {
        inputStream = IBMWatsonCaptionConverterTest::class.java.getResourceAsStream("/pushed_transcription.json")
        importCaption()
    }

    @Throws(Exception::class)
    private fun importCaption() {
        val captionList = converter!!.importCaption(inputStream!!, "")
        Assert.assertEquals(7, captionList.size.toLong())
        var caption = captionList[0]
        var text = caption.caption
        Assert.assertEquals(1, text.size.toLong())
        Assert.assertEquals("in the earliest days it was a style of programming called imperative programming language ",
                text[0])
        var time = caption.startTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(0, time.seconds.toLong())
        Assert.assertEquals(750, time.milliseconds.toLong())

        time = caption.stopTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(5, time.seconds.toLong())
        Assert.assertEquals(240, time.milliseconds.toLong())

        caption = captionList[1]
        text = caption.caption
        Assert.assertEquals(1, text.size.toLong())
        Assert.assertEquals("principal example of that is the language see ", text[0])
        time = caption.startTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(7, time.seconds.toLong())
        Assert.assertEquals(460, time.milliseconds.toLong())

        time = caption.stopTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(10, time.seconds.toLong())
        Assert.assertEquals(150, time.milliseconds.toLong())

        caption = captionList[2]
        text = caption.caption
        Assert.assertEquals(1, text.size.toLong())
        Assert.assertEquals(
                "it is rather old because Sarah is fact stems from the late 19 seventies but he still use a great deal ",
                text[0])
        time = caption.startTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(10, time.seconds.toLong())
        Assert.assertEquals(620, time.milliseconds.toLong())

        time = caption.stopTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(18, time.seconds.toLong())
        Assert.assertEquals(110, time.milliseconds.toLong())

        caption = captionList[3]
        text = caption.caption
        Assert.assertEquals(1, text.size.toLong())
        Assert.assertEquals("in fact is the principal programming language that's taught ", text[0])
        time = caption.startTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(18, time.seconds.toLong())
        Assert.assertEquals(110, time.milliseconds.toLong())

        time = caption.stopTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(20, time.seconds.toLong())
        Assert.assertEquals(960, time.milliseconds.toLong())

        caption = captionList[4]
        text = caption.caption
        Assert.assertEquals(1, text.size.toLong())
        Assert.assertEquals("in a very popular ", text[0])
        time = caption.startTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(21, time.seconds.toLong())
        Assert.assertEquals(490, time.milliseconds.toLong())

        time = caption.stopTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(22, time.seconds.toLong())
        Assert.assertEquals(580, time.milliseconds.toLong())

        caption = captionList[5]
        text = caption.caption
        Assert.assertEquals(1, text.size.toLong())
        Assert.assertEquals(
                "a computer science course called CS 15 see if it is up to become the largest undergraduate course herpetological ",
                text[0])
        time = caption.startTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(23, time.seconds.toLong())
        Assert.assertEquals(320, time.milliseconds.toLong())

        time = caption.stopTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(28, time.seconds.toLong())
        Assert.assertEquals(900, time.milliseconds.toLong())

        caption = captionList[6]
        text = caption.caption
        Assert.assertEquals(1, text.size.toLong())
        Assert.assertEquals("thing office who are extension ", text[0])
        time = caption.startTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(28, time.seconds.toLong())
        Assert.assertEquals(900, time.milliseconds.toLong())

        time = caption.stopTime
        Assert.assertEquals(0, time.hours.toLong())
        Assert.assertEquals(0, time.minutes.toLong())
        Assert.assertEquals(30, time.seconds.toLong())
        Assert.assertEquals(0, time.milliseconds.toLong())
    }
}
