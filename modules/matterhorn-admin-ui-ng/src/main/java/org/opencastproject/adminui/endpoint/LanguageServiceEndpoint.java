/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.adminui.endpoint;

import org.opencastproject.adminui.api.LanguageService;
import org.opencastproject.adminui.util.Language;
import org.opencastproject.adminui.util.LocaleFormattingStringProvider;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

@Path("/")
@RestService(name = "LanguageService", title = "Language Information", notes = "", abstractText = "This service provides information about the currently available translations.")
public class LanguageServiceEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(LanguageServiceEndpoint.class);

  /** Reference to the {@link LanguageService} instance. */
  private LanguageService languageSrv;

  /** OSGi callback to bind a {@link LanguageService} instance. */
  void setLanguageService(LanguageService languageSrv) {
    this.languageSrv = languageSrv;
  }

  @GET
  @Path("languages.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "languages", description = "Information about the user locale and the available languages", reponses = { @RestResponse(description = "Returns information about the current user's locale and the available translations", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  @SuppressWarnings("unchecked")
  public String getLanguagesInfo(@Context HttpHeaders headers) {

    final List<Locale> clientsAcceptableLanguages = headers.getAcceptableLanguages();
    final List<Language> serversAvailableLanguages = languageSrv.getAvailableLanguages();

    logger.debug("Get languages for the following locale(s): '{}'.", clientsAcceptableLanguages.toArray());

    JSONObject json = new JSONObject();
    JSONArray availableLanguages = new JSONArray();
    for (Language serverLang : serversAvailableLanguages) {
      availableLanguages.add(languageToJson(serverLang));
    }
    json.put("availableLanguages", availableLanguages);

    logger.debug("Available languages: '{}'.", availableLanguages);

    Language bestLanguage = languageSrv.getBestLanguage(clientsAcceptableLanguages);
    json.put("bestLanguage", languageToJson(bestLanguage));
    Language fallbackLanguage = languageSrv.getFallbackLanguage(clientsAcceptableLanguages);
    json.put("fallbackLanguage", languageToJson(fallbackLanguage));

    logger.debug("Returns the following languages for the locale(s) '{}': '{}'.", clientsAcceptableLanguages.toArray(),
            availableLanguages);

    return json.toJSONString();
  }

  private JSONObject languageToJson(Language language) {
    JSONObject json = new JSONObject();
    if (language != null) {
      json.put("code", language.getCode());
      json.put("displayLanguage", language.getDisplayName());
      addDateTimeFormatsTo(json, language);
    }
    return json;
  }

  @SuppressWarnings("unchecked")
  private void addDateTimeFormatsTo(JSONObject json, Language language) {

    LocaleFormattingStringProvider localeFormattingStringProvider = new LocaleFormattingStringProvider(
            language.getLocale());

    final JSONObject dateFormatsJson = new JSONObject();
    final JSONObject dateTimeFormat = new JSONObject();
    final JSONObject timeFormat = new JSONObject();
    final JSONObject dateFormat = new JSONObject();

    for (int i = 0; i < LanguageService.DATEPATTERN_STYLES.length; i++) {
      dateTimeFormat.put(LanguageService.DATEPATTERN_STYLENAMES[i],
              localeFormattingStringProvider.getDateTimeFormat(LanguageService.DATEPATTERN_STYLES[i]));

      timeFormat.put(LanguageService.DATEPATTERN_STYLENAMES[i],
              localeFormattingStringProvider.getTimeFormat(LanguageService.DATEPATTERN_STYLES[i]));

      dateFormat.put(LanguageService.DATEPATTERN_STYLENAMES[i],
              localeFormattingStringProvider.getDateFormat(LanguageService.DATEPATTERN_STYLES[i]));
    }
    dateFormatsJson.put("dateTime", dateTimeFormat);
    dateFormatsJson.put("time", timeFormat);
    dateFormatsJson.put("date", dateFormat);
    json.put("dateFormats", dateFormatsJson);
  }

}
