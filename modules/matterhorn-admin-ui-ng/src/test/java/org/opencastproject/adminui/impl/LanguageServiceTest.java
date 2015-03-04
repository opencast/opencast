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
package org.opencastproject.adminui.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.adminui.api.LanguageService;
import org.opencastproject.adminui.util.Language;
import org.opencastproject.adminui.util.TestClassPathInspector;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageServiceTest {

  private LanguageServiceImpl languageService;

  @Before
  public void setUp() {
    TestClassPathInspector classPathInspector = new TestClassPathInspector();
    languageService = new LanguageServiceImpl();
    languageService.setClassPathInspector(classPathInspector);
    languageService.setLanguageFileFolder(LanguageService.TRANSLATION_FILES_PATH);
  }

  @Test
  public void testIllegalPath() {
    languageService.setLanguageFileFolder("quark");
    testNumberOfLanguages(0);
  }

  @Test
  public void testActivatorPath() {
    testNumberOfLanguages(2);
  }

  @Test
  public void testLanguageFormat() {
    List<Language> availableLanguages = listAvailableLanguages();
    for (Language lang : availableLanguages) {
      assertNotNull(lang.getCode());
      assertNotNull(lang.getDisplayName());
    }
  }

  private void testNumberOfLanguages(int expectedNumber) {
    List<Language> availableLanguages = listAvailableLanguages();
    assertEquals(expectedNumber, availableLanguages.size());
  }

  private List<Language> listAvailableLanguages() {
    List<Language> availableLanguages = languageService.getAvailableLanguages();
    return availableLanguages;
  }

  @Test
  public void testLanguageRegex() {
    testPattern("lang-de_DE.json", "de");
  }

  @Test
  public void testLanguageRegexFrench() {
    testPattern("lang-fr_FR.json", "fr");
  }

  @Test
  public void testIsoLanguage() {
    testPattern("lang-aa.json", "aa");
  }

  protected void testPattern(String filename, String expected) {
    Pattern p = Pattern.compile(LanguageService.LANGUAGE_PATTERN);
    Matcher matcher = p.matcher(filename);
    assertTrue(matcher.matches());
    assertEquals(1, matcher.groupCount());
    assertEquals(expected, matcher.group(1));
  }

  @Test
  public void testLocaleExtraction() {
    testLocale("de", "lang-de_DE.json");
  }

  @Test
  public void testDefaultLanguage() {
    testLocale("en", "quark");
  }

  @Test
  public void testFrench() {
    testLocale("fr", "lang-fr_FR.json");
  }

  @Test
  public void testItalian() {
    testLocale("it", "lang-it_IT.json");
  }

  @Test
  public void testAllLanguages() {
    Locale[] locales = Locale.getAvailableLocales();
    for (Locale expected : locales) {
      if (!"".equals(expected.getLanguage())) {
        testLocale(expected.getLanguage(), "lang-" + expected.getLanguage() + ".json");
      }
    }
  }

  private void testLocale(String expectedLanguageName, String filename) {
    String localeFromFilename = localeFromFilename(filename);
    assertEquals(expectedLanguageName, localeFromFilename);
  }

  private String localeFromFilename(String translationFileName) {
    Pattern p = Pattern.compile(LanguageService.LANGUAGE_PATTERN);
    Matcher matcher = p.matcher(translationFileName);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return "en";
  }

  @Test
  public void simpleTestFindBestLanguage() {
    LanguageServiceTestExecutor test = new LanguageServiceTestExecutor(languageService);
    test.setClientsAcceptableLanguages(Locale.GERMAN);
    test.setServersAvailableLanguages("lang-en.json", "lang-de_DE.json");
    test.execute("de_DE", "en_US");
  }

  @Test
  public void testFallbackLanguageIsEnglish() {
    LanguageServiceTestExecutor test = new LanguageServiceTestExecutor(languageService);
    test.setClientsAcceptableLanguages(Locale.GERMAN);
    test.setServersAvailableLanguages("lang-en_US.json", "lang-de_DE.json");
    test.execute("de_DE", "en_US");
  }

  @Test
  public void testBasilsBrowser() {
    LanguageServiceTestExecutor test = new LanguageServiceTestExecutor(languageService);
    test.setClientsAcceptableLanguages(Locale.GERMAN, Locale.US, Locale.ENGLISH);
    test.setServersAvailableLanguages("lang-en_US.json", "lang-de_DE.json", "lang-en.json", "lang-jp_JP.json");
    test.execute("de_DE", "en_US");
  }

  @Test
  public void moreChoiceTestFindBestLanguage() {
    LanguageServiceTestExecutor test = new LanguageServiceTestExecutor(languageService);
    test.setClientsAcceptableLanguages(Locale.GERMAN, Locale.FRENCH, Locale.ITALIAN);
    test.setServersAvailableLanguages("lang-en.json", "lang-de_CH.json");
    test.execute("de_CH", "en_US");
  }

  @Test
  public void shortLanguageNameTestFindBestLanguage() {
    LanguageServiceTestExecutor test = new LanguageServiceTestExecutor(languageService);
    test.setClientsAcceptableLanguages(Locale.GERMAN, Locale.FRENCH, Locale.ITALIAN);
    test.setServersAvailableLanguages("lang-en.json", "lang-de.json");
    test.execute("de", "en_US");
  }

  @Test
  public void findDefaultBestLanguageTest() {
    LanguageServiceTestExecutor test = new LanguageServiceTestExecutor(languageService);
    test.setClientsAcceptableLanguages(Locale.GERMAN, Locale.FRENCH, Locale.ITALIAN);
    test.setServersAvailableLanguages("tr", "cz", "jp");
    test.execute("en_US", "en_US");
  }

  @Test
  public void testFallbackLanguage() {
    LanguageServiceTestExecutor test = new LanguageServiceTestExecutor(languageService);
    test.setClientsAcceptableLanguages(Locale.GERMAN, Locale.FRENCH, Locale.ITALIAN);
    test.setServersAvailableLanguages("de", "it", "jp");
    test.execute("de", "en_US");
  }

}
