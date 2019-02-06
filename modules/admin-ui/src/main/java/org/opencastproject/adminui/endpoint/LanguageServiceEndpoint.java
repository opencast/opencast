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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.adminui.endpoint;

import org.opencastproject.adminui.api.LanguageService;
import org.opencastproject.adminui.util.Language;
import org.opencastproject.adminui.util.LocaleFormattingStringProvider;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

@Path("/")
@RestService(name = "LanguageService", title = "Language Information",
  abstractText = "This service provides information about the currently available translations.",
  notes = { "This service offers information about the user locale and available languages for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class LanguageServiceEndpoint implements ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(LanguageServiceEndpoint.class);

  /** Reference to the {@link LanguageService} instance. */
  private LanguageService languageSrv;

  /** For Serialization */
  private static final Gson gson = new Gson();

  /** OSGi callback to bind a {@link LanguageService} instance. */
  void setLanguageService(LanguageService languageSrv) {
    this.languageSrv = languageSrv;
  }

  private static Set<String> excludedLocales;

  private static final String EXCLUDE_CONFIG_KEY = "org.opencastproject.adminui.languages.exclude";

  /** OSGi callback if properties file is present */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    excludedLocales = new HashSet<>();
    if (properties == null) {
      logger.info("No configuration available, using defaults");
      return;
    }

    String excludes = StringUtils.trimToEmpty((String) properties.get(EXCLUDE_CONFIG_KEY));
    excludedLocales.addAll(Arrays.asList(StringUtils.split(excludes, ", ")));
  }

  @GET
  @Path("languages.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "languages", description = "Information about the user locale and the available languages", reponses = { @RestResponse(description = "Returns information about the current user's locale and the available translations", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public String getLanguagesInfo(@Context HttpHeaders headers) {

    final Map<String, Object> json = new HashMap<>();
    json.put("availableLanguages",
      languageSrv.getAvailableLanguages()
        .parallelStream()
        .filter(l -> !excludedLocales.contains(l.getCode()))
        .map(this::languageToJson)
        .collect(Collectors.toList()));

    final List<Locale> acceptableLanguages = headers.getAcceptableLanguages();
    json.put("bestLanguage", languageToJson(languageSrv.getBestLanguage(acceptableLanguages)));
    json.put("fallbackLanguage", languageToJson(languageSrv.getFallbackLanguage(acceptableLanguages)));

    logger.debug("Language data: {}", json);

    return gson.toJson(json);
  }

  /**
   * Prepares a language for JSON serialization.
   *
   * @param language
   *          Language to prepare
   * @return Map with prepared data
   */
  private Map<String, Object> languageToJson(Language language) {
    if (language == null) {
      return null;
    }
    Map<String, Object> jsonData = new HashMap<>();
    jsonData.put("code", language.getCode());
    jsonData.put("displayLanguage", language.getDisplayName());

    final LocaleFormattingStringProvider formattingProvider = new LocaleFormattingStringProvider(language.getLocale());

    final Map<String, Object> dateFormatsJson = new HashMap<>();
    final Map<String, String> dateTimeFormat = new HashMap<>();
    final Map<String, String> timeFormat = new HashMap<>();
    final Map<String, String> dateFormat = new HashMap<>();

    for (int i = 0; i < LanguageService.DATEPATTERN_STYLES.length; i++) {
      dateTimeFormat.put(LanguageService.DATEPATTERN_STYLENAMES[i],
              formattingProvider.getDateTimeFormat(LanguageService.DATEPATTERN_STYLES[i]));

      timeFormat.put(LanguageService.DATEPATTERN_STYLENAMES[i],
              formattingProvider.getTimeFormat(LanguageService.DATEPATTERN_STYLES[i]));

      dateFormat.put(LanguageService.DATEPATTERN_STYLENAMES[i],
              formattingProvider.getDateFormat(LanguageService.DATEPATTERN_STYLES[i]));
    }
    dateFormatsJson.put("dateTime", dateTimeFormat);
    dateFormatsJson.put("time", timeFormat);
    dateFormatsJson.put("date", dateFormat);
    jsonData.put("dateFormats", dateFormatsJson);

    return jsonData;
  }

}
