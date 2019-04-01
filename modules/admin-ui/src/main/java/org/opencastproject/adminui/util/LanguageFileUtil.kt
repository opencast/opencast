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

package org.opencastproject.adminui.util

import org.opencastproject.adminui.exception.IllegalLanguageFilenameException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Locale

/**
 * A utility class which provides methods related to translation files, it
 * operates on its [.LANGUAGE_PATTERN].
 *
 * @author ademasi
 */
object LanguageFileUtil {

    /**
     * The convention used when naming translated json files.
     */
    val LANGUAGE_PATTERN = "^lang-(([a-z])+[_[A-Z]+]?).*.json$"

    private val logger = LoggerFactory
            .getLogger(LanguageFileUtil::class.java)

    /**
     * Executes [.stripLanguageFromFilename] on each of the
     * provided `filenames`.
     *
     * @param filenames
     * @return
     */
    fun extractLanguagenamesFromFilenames(
            filenames: List<String>): List<String> {
        val result = ArrayList<String>()
        for (filename in filenames) {
            try {
                result.add(stripLanguageFromFilename(filename))
            } catch (e: IllegalLanguageFilenameException) {
                logger.warn(
                        "There is an illegal language filename lurking around. Excluding it from the available languages list.",
                        e)
            }

        }
        return result
    }

    /**
     * Finds the language substring in a translation file (e.g. lang-de_DE.json
     * will result in de_DE). Is gracefule for non-language-file strings, such
     * as de or de_DE.
     *
     * @param filename
     * a language code according to the patterns lang-xy_AB.json,
     * yy_XX, yy-QQ, yy.
     * @return The language substring, e.g. de_DE.
     * @throws IllegalLanguageFilenameException
     * if none of the compliant patterns are met by filename
     */
    @Throws(IllegalLanguageFilenameException::class)
    fun stripLanguageFromFilename(filename: String): String {
        var result: String? = null
        if (filename.matches("[a-z]{1,2}".toRegex())) {
            result = filename
        } else if (filename.matches(LANGUAGE_PATTERN.toRegex())) {
            result = filename.replace("lang-".toRegex(), "").replace(".json".toRegex(), "")
        } else if (filename.matches(CompositeLanguageCodeParser.COMPOSITE_LANGUAGE_NAME.toRegex()) && filename.length < 4) {
            result = filename
        }
        if (result != null) {
            return result
        }
        throw IllegalLanguageFilenameException(
                String.format("The filename %s does not comply with the expected pattern lang-xy_AB.json", filename))
    }

    fun safelyStripLanguageFromFilename(filename: String): String {
        try {
            return stripLanguageFromFilename(filename)
        } catch (e: IllegalLanguageFilenameException) {
            logger.warn("Could not strip the language name from the filename {}. This indicates that the filename on the " + "server is not compliant with the naming convention.", filename, e)
            return filename
        }

    }

    /**
     * Finds the part before the - or _ of a composited language code like de_DE
     * (as it is returned by [.stripLanguageFromFilename] or
     * [.safelyStripLanguageFromFilename]. If the languageCode is
     * not composited, it will be returned as is.
     *
     * @param languageCode
     * @return The ISO part of a composited language code, if not composited,
     * the languageCode.
     */
    fun getIsoLanguagePart(languageCode: String): String {
        val parser = CompositeLanguageCodeParser(
                languageCode)
        return if (parser.isComposite) {
            parser.simpleLanguage
        } else {
            languageCode
        }
    }

    /**
     * Returns the displayName from the Locale that belongs to the language
     * code.
     *
     * @param languageCode
     * @return
     */
    fun getDisplayLanguageFromLanguageCode(languageCode: String): String {
        val parser = CompositeLanguageCodeParser(languageCode)
        val locale = if (parser.isComposite) Locale(parser.simpleLanguage) else Locale(languageCode)
        val displayLanguage = locale.getDisplayLanguage(locale)
        return Character.toUpperCase(displayLanguage[0]) + displayLanguage.substring(1)
    }

}// No default constructor for utility class.
