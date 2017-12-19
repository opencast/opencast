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
package org.opencastproject.util;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/** Contains general purpose {@link Properties} utility functions. */
public final class PropertiesUtil {

  private PropertiesUtil() {
  }

  /**
   * Convert the given {@link Properties} to a {@link Dictionary} of strings
   *
   * @param properties
   *          the properties
   * @return the {@link Dictionary} of strings
   */
  public static Dictionary<String, String> toDictionary(Properties properties) {
    Dictionary<String, String> dictionary = new Hashtable<>();
    for (Entry<Object, Object> entry : properties.entrySet()) {
      dictionary.put(entry.getKey().toString(), entry.getValue().toString());
    }
    return dictionary;
  }

  /**
   * Convert the given {@link Properties} to a {@link Map} of strings
   *
   * @param properties
   *          the properties
   * @return the {@link Map} of strings
   */
  public static Map<String, String> toMap(Properties properties) {
    Map<String, String> map = new HashMap<String, String>();
    for (final String name : properties.stringPropertyNames())
      map.put(name, properties.getProperty(name));
    return map;
  }

}
