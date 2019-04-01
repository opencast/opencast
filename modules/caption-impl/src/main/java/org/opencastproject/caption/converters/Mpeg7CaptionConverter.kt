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
import org.opencastproject.metadata.mpeg7.Audio
import org.opencastproject.metadata.mpeg7.AudioSegment
import org.opencastproject.metadata.mpeg7.FreeTextAnnotation
import org.opencastproject.metadata.mpeg7.FreeTextAnnotationImpl
import org.opencastproject.metadata.mpeg7.MediaDuration
import org.opencastproject.metadata.mpeg7.MediaTime
import org.opencastproject.metadata.mpeg7.MediaTimeImpl
import org.opencastproject.metadata.mpeg7.MediaTimePoint
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl
import org.opencastproject.metadata.mpeg7.TemporalDecomposition
import org.opencastproject.metadata.mpeg7.TextAnnotation

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.Calendar
import java.util.HashSet
import java.util.TimeZone

import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.TransformerFactoryConfigurationError
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * This is converter for Mpeg7 caption format.
 */
class Mpeg7CaptionConverter : CaptionConverter {

    /**
     * @see org.opencastproject.caption.api.CaptionConverter.getExtension
     */
    override val extension: String
        get() = EXTENSION

    override val elementType: Type
        get() = MediaPackageElement.Type.Catalog

    /**
     * @see org.opencastproject.caption.api.CaptionConverter.importCaption
     */
    @Throws(CaptionConverterException::class)
    override fun importCaption(inputStream: InputStream, language: String): List<Caption> {
        val captions = ArrayList<Caption>()
        val catalog = Mpeg7CatalogImpl(inputStream)
        val audioContentIterator = catalog.audioContent() ?: return captions
        content@ while (audioContentIterator.hasNext()) {
            val audioContent = audioContentIterator.next()
            val audioSegments = audioContent
                    .temporalDecomposition as TemporalDecomposition<AudioSegment>
            val audioSegmentIterator = audioSegments.segments() ?: continue@content
            while (audioSegmentIterator.hasNext()) {
                val segment = audioSegmentIterator.next()
                val annotationIterator = segment.textAnnotations() ?: continue@content
                while (annotationIterator.hasNext()) {
                    val annotation = annotationIterator.next()
                    if (annotation.language != language) {
                        logger.debug("Skipping audio content '{}' because of language mismatch", audioContent.id)
                        continue@content
                    }

                    val captionLines = ArrayList<String>()
                    val freeTextAnnotationIterator = annotation.freeTextAnnotations() ?: continue

                    while (freeTextAnnotationIterator.hasNext()) {
                        val freeTextAnnotation = freeTextAnnotationIterator.next()
                        captionLines.add(freeTextAnnotation.text)
                    }

                    val segmentTime = segment.mediaTime
                    val stp = segmentTime.mediaTimePoint
                    val d = segmentTime.mediaDuration

                    val startCalendar = Calendar.getInstance()
                    val millisAtStart = (stp.timeInMilliseconds - ((stp.hour * 60 + stp.minutes) * 60 + stp
                            .seconds) * 1000).toInt()
                    val millisAtEnd = (d.durationInMilliseconds - ((d.hours * 60 + d.minutes) * 60 + d
                            .seconds) * 1000).toInt()

                    startCalendar.set(Calendar.HOUR, stp.hour)
                    startCalendar.set(Calendar.MINUTE, stp.minutes)
                    startCalendar.set(Calendar.SECOND, stp.seconds)
                    startCalendar.set(Calendar.MILLISECOND, millisAtStart)

                    startCalendar.add(Calendar.HOUR, d.hours)
                    startCalendar.add(Calendar.MINUTE, d.minutes)
                    startCalendar.add(Calendar.SECOND, d.seconds)
                    startCalendar.set(Calendar.MILLISECOND, millisAtEnd)

                    try {
                        val startTime = TimeImpl(stp.hour, stp.minutes, stp.seconds, millisAtStart)
                        val endTime = TimeImpl(startCalendar.get(Calendar.HOUR), startCalendar.get(Calendar.MINUTE),
                                startCalendar.get(Calendar.SECOND), startCalendar.get(Calendar.MILLISECOND))
                        val caption = CaptionImpl(startTime, endTime, captionLines.toTypedArray())
                        captions.add(caption)
                    } catch (e: IllegalTimeFormatException) {
                        logger.warn("Error setting caption time: {}", e.message)
                    }

                }
            }
        }

        return captions
    }

    @Throws(IOException::class)
    fun exportCaption(outputStream: OutputStream, captions: List<Caption>, language: String) {

        val mpeg7 = Mpeg7CatalogImpl.newInstance()

        val mediaTime = MediaTimeImpl(0, 0)
        val audioContent = mpeg7.addAudioContent("captions", mediaTime, null)
        val captionDecomposition = audioContent
                .temporalDecomposition as TemporalDecomposition<AudioSegment>

        var segmentCount = 0
        for (caption in captions) {

            // Get all the words/parts for the transcript
            val words = caption.caption
            if (words.size == 0)
                continue

            // Create a new segment
            val segment = captionDecomposition.createSegment("segment-" + segmentCount++)

            val captionST = caption.startTime
            val captionET = caption.stopTime

            // Calculate start time
            val startTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            startTime.timeInMillis = 0
            startTime.add(Calendar.HOUR_OF_DAY, captionST.hours)
            startTime.add(Calendar.MINUTE, captionST.minutes)
            startTime.add(Calendar.SECOND, captionST.seconds)
            startTime.add(Calendar.MILLISECOND, captionST.milliseconds)

            // Calculate end time
            val endTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            endTime.timeInMillis = 0
            endTime.add(Calendar.HOUR_OF_DAY, captionET.hours)
            endTime.add(Calendar.MINUTE, captionET.minutes)
            endTime.add(Calendar.SECOND, captionET.seconds)
            endTime.add(Calendar.MILLISECOND, captionET.milliseconds)

            val startTimeInMillis = startTime.timeInMillis
            val endTimeInMillis = endTime.timeInMillis

            val duration = endTimeInMillis - startTimeInMillis

            segment.mediaTime = MediaTimeImpl(startTimeInMillis, duration)
            val textAnnotation = segment.createTextAnnotation(0f, 0f, language)

            // Collect all the words in the segment
            val captionLine = StringBuffer()

            // Add each words/parts as segment to the catalog
            for (word in words) {
                if (captionLine.length > 0)
                    captionLine.append(' ')
                captionLine.append(word)
            }

            // Append the text to the annotation
            textAnnotation.addFreeTextAnnotation(FreeTextAnnotationImpl(captionLine.toString()))

        }

        var tf: Transformer? = null
        try {
            tf = TransformerFactory.newInstance().newTransformer()
            val xmlSource = DOMSource(mpeg7.toXml())
            tf!!.transform(xmlSource, StreamResult(outputStream))
        } catch (e: TransformerConfigurationException) {
            logger.warn("Error serializing mpeg7 captions catalog: {}", e.message)
            throw IOException(e)
        } catch (e: TransformerFactoryConfigurationError) {
            logger.warn("Error serializing mpeg7 captions catalog: {}", e.message)
            throw IOException(e)
        } catch (e: TransformerException) {
            logger.warn("Error serializing mpeg7 captions catalog: {}", e.message)
            throw IOException(e)
        } catch (e: ParserConfigurationException) {
            logger.warn("Error serializing mpeg7 captions catalog: {}", e.message)
            throw IOException(e)
        }

    }

    /**
     * @see org.opencastproject.caption.api.CaptionConverter.getLanguageList
     */
    @Throws(CaptionConverterException::class)
    override fun getLanguageList(inputStream: InputStream): Array<String> {
        val languages = HashSet<String>()

        val catalog = Mpeg7CatalogImpl(inputStream)
        val audioContentIterator = catalog.audioContent() ?: return languages.toTypedArray()
        content@ while (audioContentIterator.hasNext()) {
            val audioContent = audioContentIterator.next()
            val audioSegments = audioContent
                    .temporalDecomposition as TemporalDecomposition<AudioSegment>
            val audioSegmentIterator = audioSegments.segments() ?: continue@content
            while (audioSegmentIterator.hasNext()) {
                val segment = audioSegmentIterator.next()
                val annotationIterator = segment.textAnnotations() ?: continue@content
                while (annotationIterator.hasNext()) {
                    val annotation = annotationIterator.next()
                    val language = annotation.language
                    if (language != null)
                        languages.add(language)
                }
            }
        }

        return languages.toTypedArray()
    }

    companion object {

        /** File extension for mpeg 7 catalogs  */
        private val EXTENSION = "xml"

        /** The logger  */
        private val logger = LoggerFactory.getLogger(Mpeg7CaptionConverter::class.java)
    }
}
