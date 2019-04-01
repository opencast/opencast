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
import org.opencastproject.caption.api.CaptionConverter
import org.opencastproject.caption.api.CaptionConverterException
import org.opencastproject.caption.api.IllegalTimeFormatException
import org.opencastproject.caption.api.Time
import org.opencastproject.caption.impl.CaptionImpl
import org.opencastproject.caption.impl.TimeImpl
import org.opencastproject.caption.util.TimeUtil
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.ArrayList
import java.util.Scanner

/**
 * Converter engine for SubRip srt caption format. It does not support advanced SubRip format (SubRip format with
 * annotations). Advanced format will be parsed but all annotations will be stripped off.
 *
 */
class SubRipCaptionConverter : CaptionConverter {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.caption.api.CaptionConverter.getExtension
     */
    override val extension: String
        get() = EXTENSION

    override val elementType: Type
        get() = MediaPackageElement.Type.Attachment

    /**
     * {@inheritDoc} Since srt does not store information about language, language parameter is ignored.
     *
     * @see org.opencastproject.caption.api.CaptionConverter.importCaption
     */
    @Throws(CaptionConverterException::class)
    override fun importCaption(`in`: InputStream, language: String): List<Caption> {

        val collection = ArrayList<Caption>()

        // initialize scanner object
        val scanner = Scanner(`in`, "UTF-8")
        scanner.useDelimiter("[\n(\r\n)]{2}")

        // create initial time
        var time: Time? = null
        try {
            time = TimeImpl(0, 0, 0, 0)
        } catch (e1: IllegalTimeFormatException) {
        }

        while (scanner.hasNext()) {
            var captionString = scanner.next()
            // convert line endings to \n
            captionString = captionString.replace("\r\n", "\n")

            // split to number, time and caption
            val captionParts = captionString.split("\n".toRegex(), 3).toTypedArray()
            // check for table length
            if (captionParts.size != 3) {
                throw CaptionConverterException("Invalid caption for SubRip format: $captionString")
            }

            // get time part
            val timePart = captionParts[1].split("-->".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            // parse time
            val inTime: Time
            val outTime: Time
            try {
                inTime = TimeUtil.importSrt(timePart[0].trim { it <= ' ' })
                outTime = TimeUtil.importSrt(timePart[1].trim { it <= ' ' })
            } catch (e: IllegalTimeFormatException) {
                throw CaptionConverterException(e.message)
            }

            // check for time validity
            if (inTime.compareTo(time!!) < 0 || outTime.compareTo(inTime) <= 0) {
                logger.warn("Caption with invalid time encountered. Skipping...")
                continue
            }
            time = outTime

            // get text captions
            val captionLines = createCaptionLines(captionParts[2])
                    ?: throw CaptionConverterException("Caption does not contain any caption text: $captionString")

            // create caption object and add to caption collection
            val caption = CaptionImpl(inTime, outTime, captionLines)
            collection.add(caption)
        }

        return collection
    }

    /**
     * {@inheritDoc} Since srt does not store information about language, language parameter is ignored.
     */
    @Throws(IOException::class)
    fun exportCaption(outputStream: OutputStream, captions: List<Caption>, language: String?) {

        if (language != null) {
            logger.debug("SubRip format does not include language information. Ignoring language attribute.")
        }

        // initialize stream writer
        val osw = OutputStreamWriter(outputStream, "UTF-8")
        val bw = BufferedWriter(osw)

        // initialize counter
        var counter = 1
        for (caption in captions) {
            val captionString = String.format("%2\$d%1\$s%3\$s --> %4\$s%1\$s%5\$s%1\$s%1\$s", LINE_ENDING, counter,
                    TimeUtil.exportToSrt(caption.startTime), TimeUtil.exportToSrt(caption.stopTime),
                    createCaptionText(caption.caption))
            bw.append(captionString)
            counter++
        }

        bw.flush()
        bw.close()
        osw.close()
    }

    /**
     * Helper function that creates caption text.
     *
     * @param captionLines
     * array containing caption lines
     * @return string representation of caption text
     */
    private fun createCaptionText(captionLines: Array<String>): String {
        val builder = StringBuilder(captionLines[0])
        for (i in 1 until captionLines.size) {
            builder.append(LINE_ENDING)
            builder.append(captionLines[i])
        }
        return builder.toString()
    }

    /**
     * Helper function that splits text into lines and remove any style annotation
     *
     * @param captionText
     * @return array of caption's text lines
     */
    private fun createCaptionLines(captionText: String): Array<String>? {
        val captionLines = captionText.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (captionLines.size == 0) {
            return null
        }
        for (i in captionLines.indices) {
            captionLines[i] = captionLines[i].replace("(<\\s*.\\s*>)|(</\\s*.\\s*>)".toRegex(), "").trim { it <= ' ' }
        }
        return captionLines
    }

    /**
     * {@inheritDoc} Returns empty list since srt format does not store any information about language.
     *
     * @see org.opencastproject.caption.api.CaptionConverter.getLanguageList
     */
    @Throws(CaptionConverterException::class)
    override fun getLanguageList(input: InputStream): Array<String> {
        return arrayOfNulls(0)
    }

    companion object {

        /** Logging utility  */
        private val logger = LoggerFactory.getLogger(SubRipCaptionConverter::class.java)

        private val EXTENSION = "srt"

        /** line ending used in srt - windows native in specification  */
        private val LINE_ENDING = "\r\n"
    }
}
