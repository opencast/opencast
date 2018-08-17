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

package org.opencastproject.adminui.impl;

import org.opencastproject.adminui.api.LanguageService;
import org.opencastproject.adminui.exception.IllegalPathException;
import org.opencastproject.adminui.util.ClassPathInspector;
import org.opencastproject.adminui.util.Language;
import org.opencastproject.adminui.util.LanguageFileUtil;
import org.opencastproject.adminui.util.PathInspector;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Default implementation of {@link LanguageService}. */
public class LanguageServiceImpl implements LanguageService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(LanguageServiceImpl.class);

  private PathInspector pathInspector;
  private String languageFileFolder;

  /** OSGi component activation callback */
  void activate(BundleContext bundleContext) {
    logger.info("Activate language service");
    this.pathInspector = new ClassPathInspector(bundleContext.getBundle());
    setLanguageFileFolder(TRANSLATION_FILES_PATH);
  }

  @Override
  public List<Language> getAvailableLanguages() {
    List<Language> result = new ArrayList<Language>();
    try {
      List<String> files = pathInspector.listFiles(languageFileFolder);
      for (String file : files) {
        result.add(new Language(LanguageFileUtil.safelyStripLanguageFromFilename(file)));
      }
    } catch (IllegalPathException e) {
      logger.warn(
              "The provided path to the folder containing the language files ({}) does not exist! Returning an empty list...",
              languageFileFolder);
    }
    return result;
  }

  @Override
  public Language getBestLanguage(List<Locale> clientsAcceptableLanguages) {
    final List<Language> matches = getMatchingLanguages(clientsAcceptableLanguages, getAvailableLanguages());
    if (matches.size() > 0) {
      return matches.get(0);
    } else {
      return getFallbackLanguage(clientsAcceptableLanguages);
    }
  }

  @Override
  public Language getFallbackLanguage(List<Locale> clientsAcceptableLanguages) {
    // en_US is the master language and the only one known to be 100% translated.
    // In order to always have a functional fallback language we need to return en_US.
    return new Language(DEFAULT_LANGUAGE);
  }

  /**
   *
   * @param clientLocales
   * @param availableLanguages
   * @return
   */
  private static List<Language> getMatchingLanguages(final List<Locale> clientLocales,
          final List<Language> availableLanguages) {
    final List<Language> matches = new ArrayList<Language>();

    for (final Locale locale : clientLocales) {
      boolean foundPerfectMatch = false;

      // Check first, if there is a perfect locale match
      for (final Language language : availableLanguages) {
        if (locale.equals(language.getLocale())) {
          matches.add(language);
          foundPerfectMatch = true;
          break;
        }
      }

      // If no perfect match was found, check if there is a language match
      if (!foundPerfectMatch) {
        for (final Language language : availableLanguages) {
          if (locale.getLanguage().equals(language.getLocale().getLanguage())) {
            matches.add(language);
            break;
          }
        }
      }
    }

    return matches;
  }

  /**
   *
   * @param pathInspector
   */
  void setClassPathInspector(PathInspector pathInspector) {
    this.pathInspector = pathInspector;
  }

  void setLanguageFileFolder(String languageFileFolder) {
    this.languageFileFolder = languageFileFolder;
  }

}
