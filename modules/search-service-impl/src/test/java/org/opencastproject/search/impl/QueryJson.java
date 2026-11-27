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

package org.opencastproject.search.impl;

import static org.junit.Assert.assertEquals;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Compares OpenSearch search request bodies against expected JSON files ("golden files").
 * <p>
 * Different clients render the same query differently: they add default values, use different (but equivalent)
 * syntax for ranges or wildcards, and order clauses differently. Both the actual and the expected JSON are therefore
 * normalized into a canonical form before comparing them, so that only semantic differences make a test fail.
 * <p>
 * Expected files live in {@code src/test/resources/golden/}. Run the tests with {@code -DupdateGolden=true} to
 * (re-)generate them from the current implementation.
 */
public final class QueryJson {

  /** Keys with values that are defaults in OpenSearch and therefore carry no meaning. */
  private static final Map<String, JsonPrimitive> DEFAULTS = Map.of(
      "boost", new JsonPrimitive(1.0),
      "adjust_pure_negative", new JsonPrimitive(true),
      "slop", new JsonPrimitive(0),
      "prefix_length", new JsonPrimitive(0),
      "max_expansions", new JsonPrimitive(50),
      "zero_terms_query", new JsonPrimitive("NONE"),
      "auto_generate_synonyms_phrase_query", new JsonPrimitive(true),
      "fuzzy_transpositions", new JsonPrimitive(true),
      "lang", new JsonPrimitive("painless"));

  /** Boolean query clauses whose order is irrelevant. */
  private static final Set<String> UNORDERED_CLAUSES = Set.of("filter", "must", "must_not", "should");

  /** Top-level keys which define what is returned, not what matches. They are compared separately, if at all. */
  private static final Set<String> IGNORED_TOP_LEVEL = Set.of("stored_fields", "_source");

  private QueryJson() {
  }

  /**
   * Assert that the given request body is semantically equal to the expected golden file.
   *
   * @param name
   *          name of the golden file, without extension
   * @param actualJson
   *          the request body as rendered by the client
   */
  public static void assertMatchesGolden(String name, String actualJson) throws IOException {
    final String actual = normalize(actualJson);
    final String resource = "/golden/" + name + ".json";
    if (Boolean.getBoolean("updateGolden")) {
      FileUtils.writeStringToFile(new File("src/test/resources" + resource), actual, StandardCharsets.UTF_8);
      return;
    }
    try (InputStream in = QueryJson.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new AssertionError("Missing golden file " + resource + ". Actual:\n" + actual);
      }
      assertEquals("Query differs from " + resource, normalize(IOUtils.toString(in, StandardCharsets.UTF_8)),
          actual);
    }
  }

  /**
   * Bring a search request body into a canonical form.
   */
  public static String normalize(String json) {
    final JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    IGNORED_TOP_LEVEL.forEach(root::remove);
    // Tracking all hits can be requested as "true" or with the maximum number of hits to track
    final JsonElement trackTotalHits = root.get("track_total_hits");
    if (trackTotalHits != null && trackTotalHits.isJsonPrimitive() && trackTotalHits.getAsJsonPrimitive().isBoolean()
        && trackTotalHits.getAsBoolean()) {
      root.addProperty("track_total_hits", Integer.MAX_VALUE);
    }
    return new GsonBuilder().setPrettyPrinting().create().toJson(normalize(null, root)) + "\n";
  }

  private static JsonElement normalize(String key, JsonElement element) {
    if (element.isJsonArray()) {
      final List<JsonElement> items = new ArrayList<>();
      for (JsonElement item : element.getAsJsonArray()) {
        items.add(normalize(null, item));
      }
      if (key != null && UNORDERED_CLAUSES.contains(key)) {
        items.sort(Comparator.comparing(JsonElement::toString));
      }
      final JsonArray result = new JsonArray();
      items.forEach(result::add);
      return result;
    }
    if (!element.isJsonObject()) {
      return element;
    }

    final JsonObject object = element.getAsJsonObject();
    convertRange(object);
    final TreeMap<String, JsonElement> sorted = new TreeMap<>();
    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      final String name = entry.getKey();
      JsonElement value = entry.getValue();
      if (value.isJsonNull() || isDefault(name, value)) {
        continue;
      }
      if ("terms".equals(name) && value.isJsonObject()) {
        value = sortTermValues(value.getAsJsonObject());
      } else if ("wildcard".equals(name) && value.isJsonObject()) {
        value = renameWildcardValue(value.getAsJsonObject());
      } else if ("multi_match".equals(name)) {
        value = normalizeMultiMatch(value.getAsJsonObject());
      }
      sorted.put(name, normalize(name, value));
    }
    final JsonObject result = new JsonObject();
    sorted.forEach(result::add);
    return result;
  }

  private static boolean isDefault(String name, JsonElement value) {
    final JsonPrimitive defaultValue = DEFAULTS.get(name);
    if (defaultValue == null || !value.isJsonPrimitive()) {
      return false;
    }
    final JsonPrimitive primitive = value.getAsJsonPrimitive();
    if (defaultValue.isNumber() && primitive.isNumber()) {
      return defaultValue.getAsDouble() == primitive.getAsDouble();
    }
    return defaultValue.getAsString().equalsIgnoreCase(primitive.getAsString());
  }

  /** Convert the {@code from}/{@code to}/{@code include_*} range syntax into {@code gt}/{@code gte}/... */
  private static void convertRange(JsonObject object) {
    if (!object.has("include_lower") && !object.has("include_upper")) {
      return;
    }
    final boolean includeLower = !object.has("include_lower") || object.get("include_lower").getAsBoolean();
    final boolean includeUpper = !object.has("include_upper") || object.get("include_upper").getAsBoolean();
    final JsonElement from = object.remove("from");
    final JsonElement to = object.remove("to");
    object.remove("include_lower");
    object.remove("include_upper");
    if (from != null && !from.isJsonNull()) {
      object.add(includeLower ? "gte" : "gt", from);
    }
    if (to != null && !to.isJsonNull()) {
      object.add(includeUpper ? "lte" : "lt", to);
    }
  }

  private static JsonObject sortTermValues(JsonObject terms) {
    final JsonObject result = new JsonObject();
    for (Map.Entry<String, JsonElement> entry : terms.entrySet()) {
      if (entry.getValue().isJsonArray()) {
        final List<JsonElement> values = new ArrayList<>();
        entry.getValue().getAsJsonArray().forEach(values::add);
        values.sort(Comparator.comparing(JsonElement::getAsString));
        final JsonArray array = new JsonArray();
        values.forEach(array::add);
        result.add(entry.getKey(), array);
      } else {
        result.add(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  /** Wildcard patterns may be given as {@code wildcard} or as {@code value}. Use {@code value}. */
  private static JsonObject renameWildcardValue(JsonObject wildcard) {
    for (Map.Entry<String, JsonElement> entry : wildcard.entrySet()) {
      if (entry.getValue().isJsonObject()) {
        final JsonObject field = entry.getValue().getAsJsonObject();
        if (field.has("wildcard")) {
          field.add("value", field.remove("wildcard"));
        }
      }
    }
    return wildcard;
  }

  /** Field boosts of 1.0 are implicit, the order of fields is irrelevant, enum values are case-insensitive. */
  private static JsonObject normalizeMultiMatch(JsonObject multiMatch) {
    final JsonObject result = multiMatch.deepCopy();
    if (result.has("fields")) {
      final List<String> names = new ArrayList<>();
      for (JsonElement field : result.getAsJsonArray("fields")) {
        names.add(field.getAsString().replaceAll("\\^1\\.0$", ""));
      }
      names.sort(null);
      final JsonArray fields = new JsonArray();
      names.forEach(fields::add);
      result.add("fields", fields);
    }
    for (String name : new String[] { "type", "operator" }) {
      if (result.has(name)) {
        result.addProperty(name, result.get(name).getAsString().toLowerCase());
      }
    }
    return result;
  }
}
