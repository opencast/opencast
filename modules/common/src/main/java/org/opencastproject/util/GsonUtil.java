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

package org.opencastproject.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.ToNumberPolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides the {@link Gson} instance Opencast uses for reading and writing JSON.
 * <p>
 * Gson's defaults are unsuitable for Opencast in four ways, which is why this shared instance exists instead of
 * every call site building its own:
 * <ul>
 * <li>By default Gson escapes {@code <}, {@code >}, {@code &}, {@code =} and {@code '} as {@code \\uXXXX}.
 * Opencast serializes URLs almost everywhere, so this would litter the output with escapes.</li>
 * <li>By default Gson omits object members whose value is {@code null}. The External API explicitly requires empty
 * fields to be present, so they have to be serialized.</li>
 * <li>By default Gson reads every JSON number into a {@code Double} when the target type is unknown, which silently
 * loses precision for large integers. {@code LONG_OR_DOUBLE} keeps integral values as {@code Long}.</li>
 * <li>By default Gson parses leniently and accepts input the JSON specification does not allow, such as unquoted
 * keys. Requests carrying such input used to be rejected, so parsing is set to strict.</li>
 * </ul>
 * <p>
 * {@link Gson} is thread safe, so the single instance can be shared freely.
 */
public final class GsonUtil {

  private static final Gson GSON = new GsonBuilder()
      .disableHtmlEscaping()
      .serializeNulls()
      .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
      .setStrictness(Strictness.STRICT)
      .create();

  private GsonUtil() {
  }

  /**
   * Returns the shared, pre-configured {@link Gson} instance.
   *
   * @return the Gson instance to use for all JSON reading and writing
   */
  public static Gson gson() {
    return GSON;
  }

  /**
   * Reads a string member from a JSON object.
   * <p>
   * json-simple returned {@code null} from {@code get()} for an absent key and for an explicit JSON null
   * alike, and the surrounding code was written against that. Gson distinguishes the two and answers with a
   * {@link com.google.gson.JsonNull} in the second case, which {@link JsonElement#getAsString()} would then fail on.
   * This collapses both back to {@code null}.
   *
   * @param json
   *          the object to read from
   * @param key
   *          the member name
   * @return the member as a string, or {@code null} if it is absent or JSON null
   */
  public static String getStringOrNull(JsonObject json, String key) {
    JsonElement value = json.get(key);
    return value == null || value.isJsonNull() ? null : value.getAsString();
  }

  /**
   * Renders a JSON value as plain text.
   * <p>
   * json-simple handed back bare Java objects, so {@code toString()} on a parsed member gave the string itself.
   * A {@link JsonElement}'s {@code toString()} is its JSON text, which would wrap a string in quotes. This
   * returns the raw string for primitives and the JSON text for objects and arrays, matching the old behaviour.
   *
   * @param value
   *          the value to render
   * @return the value as plain text
   */
  public static String asText(JsonElement value) {
    return value.isJsonPrimitive() ? value.getAsString() : value.toString();
  }

  /**
   * Flattens a JSON object into a map of strings.
   * <p>
   * json-simple's object was a {@link Map} already, so callers used to cast it and read the values as strings. This
   * renders each member with {@link #asText(JsonElement)} and maps JSON null to {@code null}, as the cast did.
   *
   * @param json
   *          the object to flatten
   * @return the members of the object as plain text
   */
  public static Map<String, String> toStringMap(JsonObject json) {
    Map<String, String> map = new HashMap<>();
    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
      JsonElement value = entry.getValue();
      map.put(entry.getKey(), value.isJsonNull() ? null : asText(value));
    }
    return map;
  }
}
