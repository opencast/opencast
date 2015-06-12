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

package org.opencastproject.adminui.util;

import static org.junit.Assert.assertEquals;

import org.opencastproject.adminui.exception.IllegalLanguageFilenameException;

import org.junit.Test;

import java.util.Locale;

public class LanguageFileUtilTest {

  @Test
  public void testStripLanguageFromFilename() throws IllegalLanguageFilenameException {
    String input = "lang-de_DE.json";
    String expected = "de_DE";
    assertEquals(expected, LanguageFileUtil.stripLanguageFromFilename(input));
  }

  @Test
  public void testStripAnotherLanguageFromFilename() throws IllegalLanguageFilenameException {
    String input = "lang-tac_DI.json";
    String expected = "tac_DI";
    assertEquals(expected, LanguageFileUtil.stripLanguageFromFilename(input));
  }

  @Test(expected = IllegalLanguageFilenameException.class)
  public void testStripLanguageFromWrongFilename() throws IllegalLanguageFilenameException {
    String input = "wrong-de_DE.json";
    LanguageFileUtil.stripLanguageFromFilename(input);
  }

  @Test(expected = IllegalLanguageFilenameException.class)
  public void testStripLanguageFromWrongCapitalizationFilename() throws IllegalLanguageFilenameException {
    String input = "lang-DE_de.json";
    LanguageFileUtil.stripLanguageFromFilename(input);
  }

  @Test(expected = IllegalLanguageFilenameException.class)
  public void testStripLanguageFromFullFilename() throws IllegalLanguageFilenameException {
    LanguageFileUtil.stripLanguageFromFilename("lang-DE_de.json");
  }

  @Test
  public void testDisplayNameGerman() {
    String expected = Locale.GERMAN.getDisplayName();
    assertEquals(expected, LanguageFileUtil.getDisplayLanguageFromLanguageCode("de_DE"));
    assertEquals(expected, LanguageFileUtil.getDisplayLanguageFromLanguageCode("de-de"));
    assertEquals(expected, LanguageFileUtil.getDisplayLanguageFromLanguageCode("de"));

  }
}
