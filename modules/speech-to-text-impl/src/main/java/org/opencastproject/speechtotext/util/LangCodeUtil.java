/*
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

package org.opencastproject.speechtotext.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for language codes
 */
public final class LangCodeUtil {

  // Prevent instantiation because of Checkstyle: "HideUtilityClassConstructor"
  private LangCodeUtil() {
    throw new UnsupportedOperationException("Utility class should not have a public or default constructor");
  }

  /** Map to get ISO 639 language code for language name in English */
  private static final Map<String, String> iso3ToIso2Map = new HashMap<>();

  /** Map to get ISO 639 language code for 3-letter language code */
  private static final Map<String, String> langNameToIso2Map = new HashMap<>();

  static {
    for (String languageCode : Locale.getISOLanguages()) {
      Locale locale = Locale.of(languageCode);
      String languageName = locale.getDisplayLanguage(Locale.of("en"));
      langNameToIso2Map.put(languageName, languageCode);
      String languageISO3 = locale.getISO3Language();
      iso3ToIso2Map.put(languageISO3, languageCode);
    }
  }

  /**
   * Convert ISO 639-3 language code to ISO 639-2 language code.
   *
   * @param langIso3Code The ISO 639-3 language code to convert.
   * @param defaultValue The default value to return if the conversion fails.
   * @return The ISO 639-2 language code, or the default value if the conversion fails.
   */
  public static String iso3ToIso2(String langIso3Code, String defaultValue) {
    return iso3ToIso2Map.getOrDefault(langIso3Code, defaultValue);
  }

  /**
   * Get the ISO 639-2 language code for a given language name in English.
   *
   * @param langNameInEnglish The language name in English.
   * @param defaultValue      The default value to return if the conversion fails.
   * @return The ISO 639-2 language code, or the default value if the conversion fails.
   */
  public static String getIso2FromLang(String langNameInEnglish, String defaultValue) {
    return langNameToIso2Map.getOrDefault(langNameInEnglish, defaultValue);
  }

}
