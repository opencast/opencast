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

/**
 * Provides the {@link Gson} instance Opencast uses for reading and writing JSON.
 * <p>
 * Gson's defaults are unsuitable for Opencast in four ways, which is why this shared instance exists instead of
 * every call site building its own:
 * <ul>
 * <li>By default Gson escapes <code>&lt;</code>, <code>&gt;</code>, <code>&amp;</code>, <code>=</code> and
 * <code>'</code> as <code>\\uXXXX</code>. Opencast serializes URLs almost everywhere, so this would litter the
 * output with escapes.</li>
 * <li>By default Gson omits object members whose value is <code>null</code>. The External API explicitly requires
 * empty fields to be present, so they have to be serialized.</li>
 * <li>By default Gson reads every JSON number into a <code>Double</code> when the target type is unknown, which
 * silently loses precision for large integers. <code>LONG_OR_DOUBLE</code> keeps integral values as
 * <code>Long</code>.</li>
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
   * json-simple returned <code>null</code> from <code>get()</code> for an absent key and for an explicit JSON null
   * alike, and the surrounding code was written against that. Gson distinguishes the two and answers with a
   * {@link com.google.gson.JsonNull} in the second case, which {@link JsonElement#getAsString()} would then fail on.
   * This collapses both back to <code>null</code>.
   *
   * @param json
   *          the object to read from
   * @param key
   *          the member name
   * @return the member as a string, or <code>null</code> if it is absent or JSON null
   */
  public static String getStringOrNull(JsonObject json, String key) {
    JsonElement value = json.get(key);
    return value == null || value.isJsonNull() ? null : value.getAsString();
  }

  /**
   * Renders a JSON value as plain text.
   * <p>
   * json-simple handed back bare Java objects, so <code>toString()</code> on a parsed member gave the string itself.
   * A {@link JsonElement}'s <code>toString()</code> is its JSON text, which would wrap a string in quotes. This
   * returns the raw string for primitives and the JSON text for objects and arrays, matching the old behaviour.
   *
   * @param value
   *          the value to render
   * @return the value as plain text
   */
  public static String asText(JsonElement value) {
    return value.isJsonPrimitive() ? value.getAsString() : value.toString();
  }
}
