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

package org.opencastproject.adminui.api;

import org.opencastproject.adminui.util.Language;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * A service which provides information about translations for the matterhorn-admin-ui-ng bundle. Usage: Configure the
 * directory which contains the translation files by invoking {@link #setLanguageFileFolder(String)} before
 * {@link #getAvailableLanguages()}.
 */
public interface LanguageService {

  /** The styles for which formatting strings are going to be available. */
  int[] DATEPATTERN_STYLES = new int[] { SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM, SimpleDateFormat.FULL };

  /** The corresponding names to the DATEPATTERN_STYLES. */
  String[] DATEPATTERN_STYLENAMES = new String[] { "short", "medium", "full" };

  /** The path to the compiled translation files, located in this bundle's src/main/resources */
  String TRANSLATION_FILES_PATH = "public/org/opencastproject/adminui/languages/";

  /** The pattern for identifying the language in the filename. */
  String LANGUAGE_PATTERN = "lang-([a-z]{2,}).*\\.json";

  /** The default language is US English. */
  String DEFAULT_LANGUAGE = "en_US";

  /**
   * A list of the locales for which translations of the matterhorn-admin-ui-ng are available.
   *
   * @return All Languages for which translations are available.
   */
  List<Language> getAvailableLanguages();

  /**
   * Finds the first language in <code>serversAvailableLanguages</code> which matches one of the
   * clientsAcceptableLanguages.
   *
   * @param clientsAcceptableLanguages
   * @return The best Language
   */
  Language getBestLanguage(List<Locale> clientsAcceptableLanguages);

  /**
   * Gets the second match between acceptable and available languages.
   *
   * @param serversAvailableLanguages
   * @param clientsAcceptableLanguages
   *
   * @return The second best language.
   */
  Language getFallbackLanguage(List<Locale> clientsAcceptableLanguages);

}
