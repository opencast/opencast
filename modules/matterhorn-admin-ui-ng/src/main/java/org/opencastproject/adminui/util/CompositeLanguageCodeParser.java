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
package org.opencastproject.adminui.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompositeLanguageCodeParser {

  /**
   * Some language strings are composed of language code and country, this pattern is the basis for finding the language
   * part.
   */
  public static final String COMPOSITE_LANGUAGE_NAME = "(.*)[_|-].*";

  private static final Pattern COMPOSITE_LANGUAGE_PATTERN = Pattern.compile(COMPOSITE_LANGUAGE_NAME);
  private Matcher matcher;

  public CompositeLanguageCodeParser(String compositedLanguageCode) {
    matcher = COMPOSITE_LANGUAGE_PATTERN.matcher(compositedLanguageCode);
  }

  public boolean isComposite() {
    return matcher.matches();
  }

  public String getSimpleLanguage() {
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return "";
  }

}
