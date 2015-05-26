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

package org.opencastproject.metadata.dublincore;

import static java.lang.String.format;
import static org.opencastproject.util.EqualsUtil.eq;

import com.entwinemedia.fn.data.Opt;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.RequireUtil;

import java.io.Serializable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Representation of a DublinCore conforming property value.
 * <p/>
 * See <a
 * href="http://dublincore.org/documents/dc-xml-guidelines/">http://dublincore.org/documents/dc-xml-guidelines/</a> for
 * further details.
 */
@Immutable
@ParametersAreNonnullByDefault
public final class DublinCoreValue implements Serializable {
  private static final long serialVersionUID = 7660583858714438266L;

  private final String value;
  private final String language;
  private final Opt<EName> encodingScheme;

  /**
   * Create a new Dublin Core value.
   *
   * @param value
   *          the value
   * @param language
   *          the language (two letter ISO 639)
   * @param encodingScheme
   *          the encoding scheme used to encode the value
   */
  public DublinCoreValue(String value, String language, Opt<EName> encodingScheme) {
    this.value = RequireUtil.notNull(value, "value");
    this.language = RequireUtil.notNull(language, "language");
    this.encodingScheme = RequireUtil.notNull(encodingScheme, "encodingScheme");
  }

  /**
   * Create a new Dublin Core value.
   *
   * @param value
   *         the value
   * @param language
   *         the language (two letter ISO 639)
   * @param encodingScheme
   *         the encoding scheme used to encode the value
   */
  public static DublinCoreValue mk(String value, String language, Opt<EName> encodingScheme) {
    return new DublinCoreValue(value, language, encodingScheme);
  }

  /**
   * Create a new Dublin Core value.
   *
   * @param value
   *         the value
   * @param language
   *         the language (two letter ISO 639)
   * @param encodingScheme
   *         the encoding scheme used to encode the value
   */
  public static DublinCoreValue mk(String value, String language, EName encodingScheme) {
    return new DublinCoreValue(value, language, Opt.some(encodingScheme));
  }

  /**
   * Creates a new Dublin Core value without an encoding scheme.
   *
   * @param value
   *          the value
   * @param language
   *          the language (two letter ISO 639)
   */
  public static DublinCoreValue mk(String value, String language) {
    return new DublinCoreValue(value, language, Opt.<EName>none());
  }

  /**
   * Create a new Dublin Core value with the language set to undefined and no particular encoding scheme.
   *
   * @param value
   *          the value
   * @see org.opencastproject.metadata.dublincore.DublinCore#LANGUAGE_UNDEFINED
   */
  public static DublinCoreValue mk(String value) {
    return new DublinCoreValue(value, DublinCore.LANGUAGE_UNDEFINED, Opt.<EName>none());
  }

  /**
   * Return the value of the property.
   */
  public String getValue() {
    return value;
  }

  /**
   * Return the language.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Return the encoding scheme.
   */
  public Opt<EName> getEncodingScheme() {
    return encodingScheme;
  }

  public boolean hasEncodingScheme() {
    return encodingScheme.isSome();
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof DublinCoreValue && eqFields((DublinCoreValue) that));
  }

  private boolean eqFields(DublinCoreValue that) {
    return eq(value, that.value) && eq(language, that.language) && eq(encodingScheme, that.encodingScheme);
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(value, language, encodingScheme);
  }

  @Override public String toString() {
    return format("DublinCoreValue(%s,%s,%s)", value, language, encodingScheme);
  }
}
