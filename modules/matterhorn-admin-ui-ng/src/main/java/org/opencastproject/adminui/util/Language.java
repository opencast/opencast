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

import org.opencastproject.adminui.api.LanguageService;

import java.util.Locale;

/**
 * Represents a language with its properties.
 * 
 * @author ademasi
 * 
 */
public class Language {
  private String code;
  private String displayName;
  private Locale locale;

  public Language(String languageCode) {
    this.code = languageCode;
    this.displayName = LanguageFileUtil.getDisplayLanguageFromLanguageCode(languageCode);

    String[] localCodes = languageCode.split("_");
    if (localCodes.length > 1)
      this.locale = new Locale(localCodes[0], localCodes[1]);
    else
      this.locale = new Locale(localCodes[0]);
  }

  public static Language defaultLanguage() {
    return new Language(LanguageService.DEFAULT_LANGUAGE);
  }

  /**
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * @param code
   *          the code to set
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * @return the displayName
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * @param displayName
   *          the displayName to set
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((code == null) ? 0 : code.hashCode());
    result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Language other = (Language) obj;
    if (code == null) {
      if (other.code != null)
        return false;
    } else if (!code.equals(other.code))
      return false;
    if (displayName == null) {
      if (other.displayName != null)
        return false;
    } else if (!displayName.equals(other.displayName))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return String.format("code: %s / displayName: %s", code, displayName);
  }

  /**
   * @return the locale
   */
  public Locale getLocale() {
    return locale;
  }
}
