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

package org.opencastproject.metadata.dublincore;

import org.opencastproject.mediapackage.EName;

import java.io.Serializable;

/**
 * Representation of a Dublin Core conforming property value.
 * <p/>
 * See <a
 * href="http://dublincore.org/documents/dc-xml-guidelines/">http://dublincore.org/documents/dc-xml-guidelines/</a> for
 * further details.
 */
public class DublinCoreValue implements Serializable {

  /**
   * Serial version UID
   */
  private static final long serialVersionUID = 1L;

  private final String value;
  private final String language;
  private final EName encodingScheme;

  /**
   * Creates a new Dublin Core value.
   *
   * @param value
   *          the value
   * @param language
   *          the language (two letter ISO 639)
   * @param encodingScheme
   *          the encoding scheme used to encode the value or null
   */
  public DublinCoreValue(String value, String language, EName encodingScheme) {
    if (value == null)
      throw new IllegalArgumentException("Value must not be null");
    if (language == null)
      throw new IllegalArgumentException("Language must not be null");

    this.value = value;
    this.language = language;
    this.encodingScheme = encodingScheme;
  }

  /**
   * Creates a new Dublin Core value without an encoding scheme.
   *
   * @param value
   *          the value
   * @param language
   *          the language (two letter ISO 639)
   */
  public DublinCoreValue(String value, String language) {
    this(value, language, null);
  }

  /**
   * Creates a new Dublin Core value with the language set to undefined and no particular encoding scheme.
   *
   * @param value
   *          the value
   * @see org.opencastproject.metadata.dublincore.DublinCore#LANGUAGE_UNDEFINED
   */
  public DublinCoreValue(String value) {
    this(value, DublinCore.LANGUAGE_UNDEFINED, null);
  }

  /**
   * Returns the value of the property.
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns the language.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Returns the encoding scheme or null if no encoding scheme is specified.
   */
  public EName getEncodingScheme() {
    return encodingScheme;
  }

  public boolean hasValue() {
    return value != null && value.length() > 0;
  }

  public boolean hasLanguage() {
    return language != null && language.length() > 0;
  }

  public boolean hasEncodingScheme() {
    return encodingScheme != null;
  }

  /**
   * Two values are considered equal if their value und language property are equal. The encoding scheme is only taken
   * into account when both values have a scheme set.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    DublinCoreValue that = (DublinCoreValue) o;

    if (!value.equals(that.value))
      return false;
    if (!language.equals(that.language))
      return false;
    if (encodingScheme != null && that.encodingScheme != null && !encodingScheme.equals(that.encodingScheme))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + language.hashCode();
    return result;
  }
}
