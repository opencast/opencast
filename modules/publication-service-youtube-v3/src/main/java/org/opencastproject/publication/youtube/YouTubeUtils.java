/*
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

package org.opencastproject.publication.youtube;

import org.opencastproject.util.XProperties;

import org.apache.commons.lang3.StringUtils;

import java.util.Dictionary;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Supports YouTube property management.
 */
public final class YouTubeUtils {

  public static final String keyPrefix = "org.opencastproject.publication.youtube.";

  private YouTubeUtils() {
  }

  /**
   * Disciplined way of getting properties.
   *
   * @param dictionary may not be {@code null}
   * @param key  may not be {@code null}
   * @return associated value or null
   */
  static String get(final XProperties dictionary, final YouTubeKey key) {
    return get(dictionary, key, true);
  }

  /**
   * Disciplined way of getting required properties.
   *
   * @param dictionary may not be {@code null}
   * @param key  may not be {@code null}
   * @param required when true, and property result is null, we throw {@link java.lang.IllegalArgumentException}
   * @return associated value or null
   */
  static String get(final XProperties dictionary, final YouTubeKey key, final boolean required) {
    final String value = (String) dictionary.getProperty(keyPrefix + key.name());
    final String trimmed = StringUtils.trimToNull(value);
    if (required && trimmed == null) {
      throw new IllegalArgumentException("Null or blank value for YouTube-related property: " + keyPrefix + key.name());
    }
    return trimmed;
  }

  /**
   * Disciplined way of getting a list of properties with a common prefix
   *
   * @param dictionary may not be {@code null}
   * @param key  may not be {@code null}
   * @return associated values
   */
  public static Map<String, String> getAll(XProperties dictionary, YouTubeKey key) {
    final String prefix = keyPrefix + key.name() + ".";
    return dictionary.entrySet().stream()
        .filter(e -> e.getKey() instanceof String)
        .filter(e -> ((String) e.getKey()).startsWith(prefix))
        .collect(Collectors.toMap(
            e -> ((String) e.getKey()).substring(prefix.length()),
            e -> StringUtils.trimToNull((String) e.getValue())
        ));
  }

  /**
   * Disciplined way of setting properties.
   *
   * @param dictionary may not be {@code null}
   * @param key  may not be {@code null}
   * @param value may not be {@code null}
   */
  static void put(final Dictionary dictionary, final YouTubeKey key, final Object value) {
    dictionary.put(keyPrefix + key.name(), value);
  }
}
