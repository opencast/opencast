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

package org.opencastproject.adminui.api

import org.opencastproject.adminui.util.Language

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A service which provides information about translations for the admin-ui bundle. Usage: Configure the
 * directory which contains the translation files by invoking setLanguageFileFolder before
 * [.getAvailableLanguages].
 */
interface LanguageService {

    /**
     * A list of the locales for which translations of the admin-ui are available.
     *
     * @return All Languages for which translations are available.
     */
    val availableLanguages: List<Language>

    /**
     * Finds the first language in `serversAvailableLanguages` which matches one of the
     * clientsAcceptableLanguages.
     *
     * @param clientsAcceptableLanguages
     * @return The best Language
     */
    fun getBestLanguage(clientsAcceptableLanguages: List<Locale>): Language

    /**
     * Gets the second match between acceptable and available languages.
     *
     * @param clientsAcceptableLanguages
     *
     * @return The second best language.
     */
    fun getFallbackLanguage(clientsAcceptableLanguages: List<Locale>): Language

    companion object {

        /** The styles for which formatting strings are going to be available.  */
        val DATEPATTERN_STYLES = intArrayOf(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM, SimpleDateFormat.FULL)

        /** The corresponding names to the DATEPATTERN_STYLES.  */
        val DATEPATTERN_STYLENAMES = arrayOf("short", "medium", "full")

        /** The path to the compiled translation files, located in this bundle's src/main/resources  */
        val TRANSLATION_FILES_PATH = "public/org/opencastproject/adminui/languages/"

        /** The pattern for identifying the language in the filename.  */
        val LANGUAGE_PATTERN = "lang-([a-z]{2,}).*\\.json"

        /** The default language is US English.  */
        val DEFAULT_LANGUAGE = "en_US"
    }

}
