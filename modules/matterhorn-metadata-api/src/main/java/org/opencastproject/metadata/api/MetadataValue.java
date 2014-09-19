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

package org.opencastproject.metadata.api;

import static org.opencastproject.util.RequireUtil.notNull;

/**
 * A metadata value.
 *
 * @param <A>
 *          type of the encapsulated data
 */
public final class MetadataValue<A> {

  private final A value;
  private final String name;
  private final String language;

  /**
   * Create a new value. None of the parameters must be null.
   */
  public MetadataValue(A value, String name, String language) {
    this.value = notNull(value, "value");
    this.name = notNull(name, "name");
    this.language = notNull(language, "language");
  }

  /**
   * Create a value with language set to {@link MetadataValues#LANGUAGE_UNDEFINED}.
   */
  public MetadataValue(A value, String name) {
    this(value, name, MetadataValues.LANGUAGE_UNDEFINED);
  }

  public A getValue() {
    return value;
  }

  public String getName() {
    return name;
  }

  public String getLanguage() {
    return language;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("MetadataValue");
    sb.append("{value=").append(value);
    sb.append(", name=").append(name);
    sb.append(", language=").append(language);
    sb.append('}');
    return sb.toString();
  }
}
