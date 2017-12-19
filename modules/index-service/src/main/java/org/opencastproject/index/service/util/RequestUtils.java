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
package org.opencastproject.index.service.util;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

public final class RequestUtils {
  public static final String ID_JSON_KEY = "id";
  public static final String VALUE_JSON_KEY = "value";
  public static final String REQUIRED_JSON_KEY = "required";

  private RequestUtils() {
  }

  /**
   * Get a {@link Map} of metadata fields from a JSON array.
   *
   * @param json
   *          The json input.
   * @return A {@link Map} of the metadata fields ids and values.
   * @throws ParseException
   *           Thrown if the json is malformed.
   */
  public static Map<String, String> getKeyValueMap(String json) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONArray updatedFields = (JSONArray) parser.parse(json);
    Map<String, String> fieldMap = new TreeMap<String, String>();
    JSONObject field;
    @SuppressWarnings("unchecked")
    ListIterator<Object> iterator = updatedFields.listIterator();
    while (iterator.hasNext()) {
      field = (JSONObject) iterator.next();
      String id = field.get(ID_JSON_KEY) != null ? field.get(ID_JSON_KEY).toString() : "";
      String value = field.get(VALUE_JSON_KEY) != null ? field.get(VALUE_JSON_KEY).toString() : "";
      String requiredStr = field.get(REQUIRED_JSON_KEY) != null ? field.get(REQUIRED_JSON_KEY).toString() : "false";
      boolean required = Boolean.parseBoolean(requiredStr);

      if (StringUtils.trimToNull(id) != null && (StringUtils.trimToNull(value) != null || !required)) {
        fieldMap.put(id, value);
      } else {
        throw new IllegalArgumentException(String.format(
                "One of the metadata fields is missing an id or value. The id was '%s' and the value was '%s'.", id,
                value));
      }
    }
    return fieldMap;
  }

}
