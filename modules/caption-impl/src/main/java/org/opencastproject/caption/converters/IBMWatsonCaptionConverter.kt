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
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.ArrayList

class IBMWatsonCaptionConverter : CaptionConverter {

    override val extension: String
        get() = "json"

    override val elementType: Type
        get() = MediaPackageElement.Type.Attachment

    @Throws(CaptionConverterException::class)
    override fun importCaption(inputStream: InputStream, language: String): List<Caption> {
        val captionList = ArrayList<Caption>()
        val jsonParser = JSONParser()

        try {
            val resultsObj = jsonParser.parse(InputStreamReader(inputStream)) as JSONObject
            var jobId = "Unknown"
            if (resultsObj["id"] != null)
                jobId = resultsObj["id"] as String

            // Log warnings
            if (resultsObj["warnings"] != null) {
                val warningsArray = resultsObj["warnings"] as JSONArray
                if (warningsArray != null) {
                    for (w in warningsArray)
                        logger.warn("Warning from Speech-To-Text service: {}$w")
                }
            }

            val outerResultsArray = resultsObj["results"] as JSONArray
            val obj = outerResultsArray[0] as JSONObject
            val resultsArray = obj["results"] as JSONArray

            resultsLoop@ for (i in resultsArray.indices) {
                val resultElement = resultsArray[i] as JSONObject
                // Ignore results that are not final
                if (!(resultElement["final"] as Boolean))
                    continue

                val alternativesArray = resultElement["alternatives"] as JSONArray
                if (alternativesArray != null && alternativesArray.size > 0) {
                    val alternativeElement = alternativesArray[0] as JSONObject
                    val transcript = alternativeElement["transcript"] as String
                    if (transcript != null) {
                        val timestampsArray = alternativeElement["timestamps"] as JSONArray
                        if (timestampsArray == null || timestampsArray.size == 0) {
                            logger.warn("Could not build caption object for job {}, result index {}: timestamp data not found",
                                    jobId, i)
                            continue
                        }
                        // Force a maximum line size of LINE_SIZE + one word
                        val words = transcript.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val line = StringBuffer()
                        var indexFirst = -1
                        var indexLast = -1
                        for (j in words.indices) {
                            if (indexFirst == -1)
                                indexFirst = j
                            line.append(words[j])
                            line.append(" ")
                            if (line.length >= LINE_SIZE || j == words.size - 1) {
                                indexLast = j
                                // Create a caption
                                var start = -1.0
                                var end = -1.0
                                if (indexLast < timestampsArray.size) {
                                    // Get start time of first element
                                    var wordTsArray = timestampsArray[indexFirst] as JSONArray
                                    if (wordTsArray.size == 3)
                                        start = (wordTsArray[1] as Number).toDouble()
                                    // Get end time of last element
                                    wordTsArray = timestampsArray[indexLast] as JSONArray
                                    if (wordTsArray.size == 3)
                                        end = (wordTsArray[2] as Number).toDouble()
                                }
                                if (start == -1.0 || end == -1.0) {
                                    logger.warn("Could not build caption object for job {}, result index {}: start/end times not found",
                                            jobId, i)
                                    continue@resultsLoop
                                }

                                val captionLines = arrayOfNulls<String>(1)
                                captionLines[0] = line.toString().replace("%HESITATION", "...")
                                captionList.add(CaptionImpl(buildTime((start * 1000).toLong()), buildTime((end * 1000).toLong()),
                                        captionLines))
                                indexFirst = -1
                                indexLast = -1
                                line.setLength(0)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Error when parsing IBM Watson transcriptions result: {}" + e.message)
            throw CaptionConverterException(e)
        }

        return captionList
    }

    @Throws(IOException::class)
    fun exportCaption(outputStream: OutputStream, captions: List<Caption>, language: String) {
        throw UnsupportedOperationException()
    }

    @Throws(CaptionConverterException::class)
    override fun getLanguageList(inputStream: InputStream): Array<String> {
        throw UnsupportedOperationException()
    }

    @Throws(IllegalTimeFormatException::class)
    private fun buildTime(ms: Long): Time {
        var ms = ms
        val h = (ms / 3600000L).toInt()
        val m = (ms % 3600000L / 60000L).toInt()
        val s = (ms % 60000L / 1000L).toInt()
        ms = (ms % 1000).toInt().toLong()

        return TimeImpl(h, m, s, ms.toInt())
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(IBMWatsonCaptionConverter::class.java)

        private val LINE_SIZE = 100
    }

}
