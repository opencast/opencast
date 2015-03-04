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

import org.opencastproject.adminui.util.Language;
import org.opencastproject.adminui.util.MockPathInspector;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LanguageServiceTestExecutor {
  private List<Locale> clientsAcceptableLanguages;
  private LanguageServiceImpl languageService;

  public LanguageServiceTestExecutor(LanguageServiceImpl languageService) {
    this.languageService = languageService;
  }

  public void execute(String expectedBestLanguage, String expectedFallbackLanguage) {
    Language bestLanguage = languageService.getBestLanguage(clientsAcceptableLanguages);
    Language fallbackLanguage = languageService.getFallbackLanguage(clientsAcceptableLanguages);
    assertEquals(new Language(expectedBestLanguage), bestLanguage);
    assertEquals(new Language(expectedFallbackLanguage), fallbackLanguage);
  }

  /**
   * @param clientsAcceptableLanguages
   *          the clientsAcceptableLanguages to set
   */
  public void setClientsAcceptableLanguages(Locale... clientsAcceptableLanguages) {
    this.clientsAcceptableLanguages = Arrays.asList(clientsAcceptableLanguages);
  }

  /**
   * @param serversAvailableLanguages
   *          the serversAvailableLanguages to set
   */
  public void setServersAvailableLanguages(String... serversAvailableLanguages) {
    MockPathInspector pathInspector = new MockPathInspector();
    pathInspector.setServerAvailableLanguages(Arrays.asList(serversAvailableLanguages));
    languageService.setClassPathInspector(pathInspector);
  }

}
