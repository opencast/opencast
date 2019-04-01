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

package org.opencastproject.adminui.impl

import org.opencastproject.adminui.api.LanguageService
import org.opencastproject.adminui.exception.IllegalPathException
import org.opencastproject.adminui.util.ClassPathInspector
import org.opencastproject.adminui.util.Language
import org.opencastproject.adminui.util.LanguageFileUtil
import org.opencastproject.adminui.util.PathInspector

import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Locale

/** Default implementation of [LanguageService].  */
class LanguageServiceImpl : LanguageService {

    private var pathInspector: PathInspector? = null
    private var languageFileFolder: String? = null

    override val availableLanguages: List<Language>
        get() {
            val result = ArrayList<Language>()
            try {
                val files = pathInspector!!.listFiles(languageFileFolder)
                for (file in files) {
                    result.add(Language(LanguageFileUtil.safelyStripLanguageFromFilename(file)))
                }
            } catch (e: IllegalPathException) {
                logger.warn(
                        "The provided path to the folder containing the language files ({}) does not exist! Returning an empty list...",
                        languageFileFolder)
            }

            return result
        }

    /** OSGi component activation callback  */
    internal fun activate(bundleContext: BundleContext) {
        logger.info("Activate language service")
        this.pathInspector = ClassPathInspector(bundleContext.bundle)
        setLanguageFileFolder(LanguageService.TRANSLATION_FILES_PATH)
    }

    override fun getBestLanguage(clientsAcceptableLanguages: List<Locale>): Language {
        val matches = getMatchingLanguages(clientsAcceptableLanguages, availableLanguages)
        return if (matches.size > 0) {
            matches[0]
        } else {
            getFallbackLanguage(clientsAcceptableLanguages)
        }
    }

    override fun getFallbackLanguage(clientsAcceptableLanguages: List<Locale>): Language {
        // en_US is the master language and the only one known to be 100% translated.
        // In order to always have a functional fallback language we need to return en_US.
        return Language(LanguageService.DEFAULT_LANGUAGE)
    }

    /**
     *
     * @param pathInspector
     */
    internal fun setClassPathInspector(pathInspector: PathInspector) {
        this.pathInspector = pathInspector
    }

    internal fun setLanguageFileFolder(languageFileFolder: String) {
        this.languageFileFolder = languageFileFolder
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(LanguageServiceImpl::class.java)

        /**
         *
         * @param clientLocales
         * @param availableLanguages
         * @return
         */
        private fun getMatchingLanguages(clientLocales: List<Locale>,
                                         availableLanguages: List<Language>): List<Language> {
            val matches = ArrayList<Language>()

            for (locale in clientLocales) {
                var foundPerfectMatch = false

                // Check first, if there is a perfect locale match
                for (language in availableLanguages) {
                    if (locale == language.locale) {
                        matches.add(language)
                        foundPerfectMatch = true
                        break
                    }
                }

                // If no perfect match was found, check if there is a language match
                if (!foundPerfectMatch) {
                    for (language in availableLanguages) {
                        if (locale.language == language.locale!!.language) {
                            matches.add(language)
                            break
                        }
                    }
                }
            }

            return matches
        }
    }

}
