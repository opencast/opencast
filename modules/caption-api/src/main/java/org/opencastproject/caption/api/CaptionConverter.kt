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

package org.opencastproject.caption.api

import org.opencastproject.mediapackage.MediaPackageElement

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Imports caption catalogs to a list of caption objects and exports these objects to catalog presentations.
 */
interface CaptionConverter {

    /**
     * Get extension of specific caption format.
     *
     * @return caption format extension
     */
    val extension: String

    /**
     * Get type of specific caption element (Catalog, Attachment).
     *
     * @return type
     */
    val elementType: MediaPackageElement.Type

    /**
     * Imports captions to [List]. If caption format is capable of containing more than one language, language
     * parameter is used to define which captions are parsed.
     *
     * @param inputStream
     * stream from where captions are read
     * @param language
     * (optional) captions' language
     * @return [List] List of captions
     * @throws CaptionConverterException
     * if parser encounters an exception
     */
    @Throws(CaptionConverterException::class)
    fun importCaption(inputStream: InputStream, language: String): List<Caption>

    /**
     * Exports caption collection. Language parameter is used to set language of the captions for those caption format
     * that are capable of storing information about language.
     *
     * @param outputStream
     * stream to which captions are written
     * @param captions
     * collection to be exported
     * @param language
     * (optional) captions' language
     * @throws IOException
     * if exception occurs writing to output stream
     */
    @Throws(IOException::class)
    fun exportCaption(outputStream: OutputStream, captions: List<Caption>, language: String)

    /**
     * Reads captions and return information about language if such information is available. Returns empty list
     * otherwise.
     *
     * @param inputStream
     * stream from where captions are read
     * @return Array containing languages in captions
     * @throws CaptionConverterException
     * if parser encounters exception
     */
    @Throws(CaptionConverterException::class)
    fun getLanguageList(inputStream: InputStream): Array<String>

}
