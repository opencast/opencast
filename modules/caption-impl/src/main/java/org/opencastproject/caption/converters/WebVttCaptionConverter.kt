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

class WebVttCaptionConverter : CaptionConverter {
    override val extension: String
        get() = EXTENSION

    override val elementType: Type
        get() = MediaPackageElement.Type.Attachment

    /**
     * {@inheritDoc} Language parameter is ignored.
     *
     * @see org.opencastproject.caption.api.CaptionConverter.importCaption
     */
    @Throws(CaptionConverterException::class)
    override fun importCaption(`in`: InputStream, language: String): List<Caption> {
        // TODO
        throw UnsupportedOperationException()
    }

    /**
     * {@inheritDoc} Language parameter is ignored.
     */
    @Throws(IOException::class)
    fun exportCaption(outputStream: OutputStream, captions: List<Caption>, language: String) {

        val osw = OutputStreamWriter(outputStream, "UTF-8")
        val bw = BufferedWriter(osw)

        bw.append("WEBVTT\n\n")

        for (caption in captions) {
            val captionString = String.format("%s --> %s\n%s\n\n", TimeUtil.exportToVtt(caption.startTime),
                    TimeUtil.exportToVtt(caption.stopTime), createCaptionText(caption.caption))
            bw.append(captionString)
            logger.trace(captionString)
        }

        bw.flush()
        bw.close()
        osw.close()
    }

    private fun createCaptionText(captionLines: Array<String>): String {
        val builder = StringBuilder(captionLines[0])
        for (i in 1 until captionLines.size) {
            builder.append("\n")
            builder.append(captionLines[i])
        }
        return builder.toString()
    }

    @Throws(CaptionConverterException::class)
    override fun getLanguageList(input: InputStream): Array<String> {
        return arrayOfNulls(0)
    }

    companion object {

        /** Logging utility  */
        private val logger = LoggerFactory.getLogger(WebVttCaptionConverter::class.java)

        private val EXTENSION = "vtt"
    }

}
