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

package org.opencastproject.adminui.endpoint

import org.opencastproject.adminui.api.LanguageService
import org.opencastproject.adminui.util.Language
import org.opencastproject.adminui.util.LocaleFormattingStringProvider
import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.google.gson.Gson

import org.apache.commons.lang3.StringUtils
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Arrays
import java.util.Dictionary
import java.util.HashMap
import java.util.HashSet
import java.util.Locale
import java.util.stream.Collectors

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

@Path("/")
@RestService(name = "LanguageService", title = "Language Information", abstractText = "This service provides information about the currently available translations.", notes = ["This service offers information about the user locale and available languages for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
class LanguageServiceEndpoint : ManagedService {

    /** Reference to the [LanguageService] instance.  */
    private var languageSrv: LanguageService? = null

    /** OSGi callback to bind a [LanguageService] instance.  */
    internal fun setLanguageService(languageSrv: LanguageService) {
        this.languageSrv = languageSrv
    }

    /** OSGi callback if properties file is present  */
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        excludedLocales = HashSet()
        if (properties == null) {
            logger.info("No configuration available, using defaults")
            return
        }

        val excludes = StringUtils.trimToEmpty(properties.get(EXCLUDE_CONFIG_KEY) as String)
        excludedLocales!!.addAll(Arrays.asList(*StringUtils.split(excludes, ", ")))
    }

    @GET
    @Path("languages.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "languages", description = "Information about the user locale and the available languages", reponses = [RestResponse(description = "Returns information about the current user's locale and the available translations", responseCode = HttpServletResponse.SC_OK)], returnDescription = "")
    fun getLanguagesInfo(@Context headers: HttpHeaders): String {

        val json = HashMap<String, Any>()
        json["availableLanguages"] = languageSrv!!.availableLanguages
                .parallelStream()
                .filter { l -> !excludedLocales!!.contains(l.code) }
                .map<Map<String, Any>>(Function<Language, Map<String, Any>> { this.languageToJson(it) })
                .collect<List<Map<String, Any>>, Any>(Collectors.toList())

        val acceptableLanguages = headers.acceptableLanguages
        json["bestLanguage"] = languageToJson(languageSrv!!.getBestLanguage(acceptableLanguages))
        json["fallbackLanguage"] = languageToJson(languageSrv!!.getFallbackLanguage(acceptableLanguages))

        logger.debug("Language data: {}", json)

        return gson.toJson(json)
    }

    /**
     * Prepares a language for JSON serialization.
     *
     * @param language
     * Language to prepare
     * @return Map with prepared data
     */
    private fun languageToJson(language: Language?): Map<String, Any>? {
        if (language == null) {
            return null
        }
        val jsonData = HashMap<String, Any>()
        jsonData["code"] = language.code
        jsonData["displayLanguage"] = language.displayName

        val formattingProvider = LocaleFormattingStringProvider(language.locale)

        val dateFormatsJson = HashMap<String, Any>()
        val dateTimeFormat = HashMap<String, String>()
        val timeFormat = HashMap<String, String>()
        val dateFormat = HashMap<String, String>()

        for (i in LanguageService.DATEPATTERN_STYLES.indices) {
            dateTimeFormat[LanguageService.DATEPATTERN_STYLENAMES[i]] = formattingProvider.getDateTimeFormat(LanguageService.DATEPATTERN_STYLES[i])

            timeFormat[LanguageService.DATEPATTERN_STYLENAMES[i]] = formattingProvider.getTimeFormat(LanguageService.DATEPATTERN_STYLES[i])

            dateFormat[LanguageService.DATEPATTERN_STYLENAMES[i]] = formattingProvider.getDateFormat(LanguageService.DATEPATTERN_STYLES[i])
        }
        dateFormatsJson["dateTime"] = dateTimeFormat
        dateFormatsJson["time"] = timeFormat
        dateFormatsJson["date"] = dateFormat
        jsonData["dateFormats"] = dateFormatsJson

        return jsonData
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(LanguageServiceEndpoint::class.java)

        /** For Serialization  */
        private val gson = Gson()

        private var excludedLocales: MutableSet<String>? = null

        private val EXCLUDE_CONFIG_KEY = "org.opencastproject.adminui.languages.exclude"
    }

}
