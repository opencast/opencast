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

package org.opencastproject.inspection.api.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This utility class provides some utility functions to handle the options of the media inspection service.
 * This class is thread-safe.
 */
public final class Options {

  /* Empty map of options indicating no options set */
  public static final Map<String, String> NO_OPTION = Collections.unmodifiableMap(new HashMap<String, String>());

  /* Used to parse JSON. Gson is thread-safe */
  private static final Gson gson = new Gson();

  /* Hide utility class constructor */
  private Options() { };

  /**
   * Parse the media inspection service options JSON string
   *
   * @param options
   *          Options in form of a JSON string
   * @return
   *          Options as Java map data structure
   */
  public static Map<String, String> fromJson(String options) {
    Map<String, String> result = null;
    if (options != null) {
      result = gson.fromJson(options, new TypeToken<Map<String, String>>() { }.getType());
    } else {
      result = NO_OPTION;
    }
    return result;
  }

  /**
   * Transform media inspection service options to a JSON string
   *
   * @param options
   *          Media inspection service options
   * @return
   *          Media inspection service options represented by a JSON string
   */
  public static String toJson(Map<String, String> options) {
    String result = null;
    if (options != null) {
      result = gson.toJson(options);
    } else {
      result = gson.toJson(NO_OPTION);
    }
    return result;
  }

}
