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
package org.opencastproject.transcription.amberscript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages the available Amberscript languages and helps with finding the
 * right code for a given language string.
 */
public final class AmberscriptLangUtil {

  private static final Logger logger = LoggerFactory.getLogger(AmberscriptLangUtil.class);

  /** Contains the available amberscript lang codes */
  private List<String> availableAmberscriptLangs;

  /** Contains different locale names and codes, that maps to the corresponding amberscript language */
  private Map<String, String> langMap;

  /** Singleton instance */
  private static AmberscriptLangUtil amberscriptLangUtil;

  /** Singleton method */
  public static AmberscriptLangUtil getInstance() {
    if (amberscriptLangUtil == null) {
      amberscriptLangUtil = new AmberscriptLangUtil();
    }
    return amberscriptLangUtil;
  }

  /** Singleton constructor */
  private AmberscriptLangUtil() {
    populateLangMap();
  }

  /**
   * Iterates through all available amberscript languages and finds the corresponding
   * java locale object. After that a map will be created, that maps all possible
   * spellings to the amberscript language code.
   */
  private void populateLangMap() {
    langMap = new HashMap<>();
    availableAmberscriptLangs = new LinkedList<>();
    availableAmberscriptLangs.add("af-za");
    availableAmberscriptLangs.add("sq-al");
    availableAmberscriptLangs.add("am-et");
    availableAmberscriptLangs.add("ar");
    availableAmberscriptLangs.add("hy-am");
    availableAmberscriptLangs.add("az-az");
    availableAmberscriptLangs.add("id-id");
    availableAmberscriptLangs.add("eu-es");
    availableAmberscriptLangs.add("bn-bd");
    availableAmberscriptLangs.add("bn-in");
    availableAmberscriptLangs.add("bs-ba");
    availableAmberscriptLangs.add("bg");
    availableAmberscriptLangs.add("my-mm");
    availableAmberscriptLangs.add("ca");
    availableAmberscriptLangs.add("cmn");
    availableAmberscriptLangs.add("hr");
    availableAmberscriptLangs.add("cs");
    availableAmberscriptLangs.add("da");
    availableAmberscriptLangs.add("nl");
    availableAmberscriptLangs.add("en-au");
    availableAmberscriptLangs.add("en-uk");
    availableAmberscriptLangs.add("en");
    availableAmberscriptLangs.add("et-ee");
    availableAmberscriptLangs.add("fa-ir");
    availableAmberscriptLangs.add("fil-ph");
    availableAmberscriptLangs.add("fi");
    availableAmberscriptLangs.add("nl-be");
    availableAmberscriptLangs.add("fr-ca");
    availableAmberscriptLangs.add("fr");
    availableAmberscriptLangs.add("gl-es");
    availableAmberscriptLangs.add("ka-ge");
    availableAmberscriptLangs.add("de-at");
    availableAmberscriptLangs.add("de-ch");
    availableAmberscriptLangs.add("de");
    availableAmberscriptLangs.add("el");
    availableAmberscriptLangs.add("gu-in");
    availableAmberscriptLangs.add("iw-il");
    availableAmberscriptLangs.add("hi");
    availableAmberscriptLangs.add("hu");
    availableAmberscriptLangs.add("is-is");
    availableAmberscriptLangs.add("it");
    availableAmberscriptLangs.add("ja");
    availableAmberscriptLangs.add("jv-id");
    availableAmberscriptLangs.add("kn-in");
    availableAmberscriptLangs.add("km-kh");
    availableAmberscriptLangs.add("ko");
    availableAmberscriptLangs.add("lo-la");
    availableAmberscriptLangs.add("lv");
    availableAmberscriptLangs.add("lt");
    availableAmberscriptLangs.add("mk-mk");
    availableAmberscriptLangs.add("ms");
    availableAmberscriptLangs.add("ml-in");
    availableAmberscriptLangs.add("mr-in");
    availableAmberscriptLangs.add("mn-mn");
    availableAmberscriptLangs.add("ne-np");
    availableAmberscriptLangs.add("no");
    availableAmberscriptLangs.add("pl");
    availableAmberscriptLangs.add("pt-br");
    availableAmberscriptLangs.add("pt");
    availableAmberscriptLangs.add("pa-guru-in");
    availableAmberscriptLangs.add("ro");
    availableAmberscriptLangs.add("ru");
    availableAmberscriptLangs.add("sr-rs");
    availableAmberscriptLangs.add("si-lk");
    availableAmberscriptLangs.add("sk");
    availableAmberscriptLangs.add("sl");
    availableAmberscriptLangs.add("es");
    availableAmberscriptLangs.add("su-id");
    availableAmberscriptLangs.add("sw-ke");
    availableAmberscriptLangs.add("sw-tz");
    availableAmberscriptLangs.add("sv");
    availableAmberscriptLangs.add("ta-in");
    availableAmberscriptLangs.add("ta-my");
    availableAmberscriptLangs.add("ta-sg");
    availableAmberscriptLangs.add("ta-lk");
    availableAmberscriptLangs.add("te-in");
    availableAmberscriptLangs.add("th-th");
    availableAmberscriptLangs.add("tr");
    availableAmberscriptLangs.add("uk-ua");
    availableAmberscriptLangs.add("ur-in");
    availableAmberscriptLangs.add("ur-pk");
    availableAmberscriptLangs.add("uz-uz");
    availableAmberscriptLangs.add("vi-vn");
    availableAmberscriptLangs.add("zu-za");

    // it's important that we first put in the '-' languages,
    // so the more general languages will overwrite the first ones
    for (String amberLang : availableAmberscriptLangs)  {
      if (amberLang.contains("-")) {
        addAmberLangToMap(amberLang);
      }
    }
    for (String amberLang : availableAmberscriptLangs)  {
      if (!amberLang.contains("-")) {
        addAmberLangToMap(amberLang);
      }
    }
  }

  /**
   * Puts some different spellings of the java Local class into a map, with
   * the corresponding Amberscript language code.
   * @param amberLang the amberscript language code
   */
  private void addAmberLangToMap(String amberLang) {
    Locale locale;
    if (amberLang.contains("-")) {
      locale = Locale.forLanguageTag(amberLang);
    } else {
      locale = Locale.forLanguageTag(amberLang + "-" + amberLang);
    }
    if (locale.getDisplayCountry().equals("")) {
      logger.warn("Locale not found for code '{}'", amberLang);
      return;
    }
    langMap.put(amberLang, amberLang);
    langMap.put(locale.toString().toLowerCase(), amberLang);
    langMap.put(locale.getDisplayLanguage().toLowerCase(), amberLang);
    langMap.put(locale.getDisplayName().toLowerCase(), amberLang);
    langMap.put(locale.getLanguage().toLowerCase(), amberLang);
    langMap.put(locale.getISO3Language().toLowerCase(), amberLang);
  }

  /**
   * Tries to determine which amberscript language code should be taken for a
   * given language name or code.
   * @return the amberscript language code or null if nothing was found.
   */
  public String getLanguageCodeOrNull(String languageWithRandomFormat) {
    return langMap.get(languageWithRandomFormat.toLowerCase());
  }

  /**
   * Adds a custom mapping entry. Can be configured in the amberscript config file.
   * @param customKey The key
   * @param amberscriptLangCode Amberscript Language Code
   */
  public void addCustomMapping(String customKey, String amberscriptLangCode) {
    langMap.put(customKey, amberscriptLangCode);
  }
}
