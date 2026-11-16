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

package org.opencastproject.opensearch.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.opensearch.impl.AbstractOpenSearchQueryBuilder;
import org.opencastproject.opensearch.index.objects.event.EventIndexSchema;
import org.opencastproject.opensearch.index.objects.event.EventQueryBuilder;
import org.opencastproject.opensearch.index.objects.event.EventSearchQuery;
import org.opencastproject.opensearch.index.objects.series.SeriesIndexSchema;
import org.opencastproject.opensearch.index.objects.series.SeriesQueryBuilder;
import org.opencastproject.opensearch.index.objects.series.SeriesSearchQuery;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.User;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.QueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests how text searches are scored.
 */
public class TextSearchQueryTest {

  private static final String ORGANIZATION = "mh_default_org";

  private final JaxbOrganization organization = new JaxbOrganization(ORGANIZATION);
  private final User admin = new JaxbUser("admin", "test", organization,
      Set.of(new JaxbRole(SecurityConstants.GLOBAL_ADMIN_ROLE, organization)));

  /** The prefix query scores every match the same, so phrase queries have to rank the results. */
  @Test
  public void testSeriesTextQueryIsRanked() throws Exception {
    SeriesSearchQuery query = new SeriesSearchQuery(ORGANIZATION, admin);
    query.withText("biology");

    assertRankedTextQuery(toJson(new SeriesQueryBuilder(query)));
  }

  /** Some "*.search" fields of the event index are keyword fields, which reject phrase queries unless lenient. */
  @Test
  public void testEventTextQueryIsRanked() throws Exception {
    EventSearchQuery query = new EventSearchQuery(ORGANIZATION, admin);
    query.withText("biology");

    assertRankedTextQuery(toJson(new EventQueryBuilder(query)));
  }

  @Test
  public void testQueryWithoutTextHasNoTextClauses() throws Exception {
    SeriesSearchQuery series = new SeriesSearchQuery(ORGANIZATION, admin);
    EventSearchQuery events = new EventSearchQuery(ORGANIZATION, admin);

    assertTrue(collectMultiMatches(toJson(new SeriesQueryBuilder(series))).isEmpty());
    assertTrue(collectMultiMatches(toJson(new EventQueryBuilder(events))).isEmpty());
  }

  /** Long texts are only matched, not phrase ranked or matched with typos: the queries are slow. */
  @Test
  public void testLongTextIsNotPhraseRanked() throws Exception {
    String tenWords = "one two three four five six seven eight nine ten";
    EventSearchQuery events = new EventSearchQuery(ORGANIZATION, admin);
    events.withText(tenWords);
    assertEquals(4, collectMultiMatches(toJson(new EventQueryBuilder(events))).size());

    events = new EventSearchQuery(ORGANIZATION, admin);
    events.withText(tenWords + " eleven");
    List<JsonObject> matches = collectMultiMatches(toJson(new EventQueryBuilder(events)));
    assertEquals(1, matches.size());
    assertEquals("bool_prefix", matches.get(0).get("type").getAsString());
  }

  /** The prefix query does not tolerate typos in the last word, so the fuzzy query is an alternative to it. */
  @Test
  public void testLastWordMayContainTypos() throws Exception {
    SeriesSearchQuery query = new SeriesSearchQuery(ORGANIZATION, admin);
    query.withText("biolgy");
    JsonObject json = toJson(new SeriesQueryBuilder(query));
    List<JsonObject> matches = collectMultiMatches(json);

    List<JsonObject> fuzzy = byType(matches, "best_fields");
    assertEquals(1, fuzzy.size());
    assertEquals("biolgy", fuzzy.get(0).get("query").getAsString());
    assertEquals("AND", fuzzy.get(0).get("operator").getAsString());
    assertEquals("AUTO:4,8", fuzzy.get(0).get("fuzziness").getAsString());
    assertEquals(1, fuzzy.get(0).get("prefix_length").getAsInt());
    assertTrue(fuzzy.get(0).get("lenient").getAsBoolean());
    assertEquals("The fuzzy query must weight the title like the ranking queries",
        AbstractOpenSearchQueryBuilder.DEFAULT_FIELD_BOOSTS.get("title"), fieldWeight(fuzzy.get(0), "title"), 0.0f);

    // Either of the two is enough to match
    assertEquals(1, byType(matches, "bool_prefix").size());
    assertEquals("biolgy", byType(matches, "bool_prefix").get(0).get("query").getAsString());
    assertFalse("The prefix query is one of two alternatives", isRequired(json, fuzzy.get(0)));
  }

  /** Numbers and long texts do not get the fuzzy query: it would match unrelated words or be too slow. */
  @Test
  public void testFuzzyQueryIsLimited() throws Exception {
    assertEquals("AUTO:4,8", byType(fuzzyMatches("biolgy"), "best_fields").get(0).get("fuzziness").getAsString());
    String elevenWords = "one two three four five six seven eight nine ten eleven";
    for (String text : new String[] {"2024", "vorlesung 2024", "*", "   ", elevenWords}) {
      assertTrue("No fuzzy query expected for '" + text + "'", byType(fuzzyMatches(text), "best_fields").isEmpty());
    }
    assertEquals(1, byType(fuzzyMatches("2024 vorlesung"), "best_fields").size());
  }

  private List<JsonObject> fuzzyMatches(String text) throws Exception {
    SeriesSearchQuery query = new SeriesSearchQuery(ORGANIZATION, admin);
    query.withText(text);
    return collectMultiMatches(toJson(new SeriesQueryBuilder(query)));
  }

  /** Titles which start with the search text rank higher, with the words normalized like the indexed ones. */
  @Test
  public void testTitleStartIsBoosted() throws Exception {
    SeriesSearchQuery query = new SeriesSearchQuery(ORGANIZATION, admin);
    query.withText("Über Analy");
    String json = toJson(new SeriesQueryBuilder(query)).toString();

    assertTrue(json.contains("\"span_first\""));
    assertTrue(json.contains("\"end\":2"));
    assertTrue(json.contains("\"span_term\":{\"title.search\":{\"value\":\"uber\""));
    assertTrue(json.contains("\"prefix\":{\"title.search\":{\"value\":\"analy\""));
    assertTrue("The words have to be in the typed order", json.contains("\"in_order\":true"));

    // A single word needs no span_near
    query = new SeriesSearchQuery(ORGANIZATION, admin);
    query.withText("analysis");
    json = toJson(new SeriesQueryBuilder(query)).toString();
    assertTrue(json.contains("\"span_first\""));
    assertFalse(json.contains("\"span_near\""));

    // Long texts are not ranked this way, like they are not phrase ranked
    query = new SeriesSearchQuery(ORGANIZATION, admin);
    query.withText("one two three four five six seven eight nine ten eleven");
    assertFalse(toJson(new SeriesQueryBuilder(query)).toString().contains("\"span_first\""));
  }

  /** Without fuzzy search there is no fuzzy query. */
  @Test
  public void testNoFuzzyQueryWithoutFuzzySearch() throws Exception {
    SeriesSearchQuery query = new SeriesSearchQuery(ORGANIZATION, admin);
    query.withText(false, "biology");
    JsonObject json = toJson(new SeriesQueryBuilder(query));
    List<JsonObject> matches = collectMultiMatches(json);

    assertTrue(byType(matches, "best_fields").isEmpty());
    assertEquals(1, byType(matches, "bool_prefix").size());
    assertFalse(byType(matches, "bool_prefix").get(0).has("fuzziness"));
    assertTrue("The prefix query must be required", isRequired(json, byType(matches, "bool_prefix").get(0)));
  }

  /** The weights of fields can be configured, separately for series and events. */
  @Test
  public void testFieldBoostsAreConfigurable() throws Exception {
    SeriesSearchQuery series = new SeriesSearchQuery(ORGANIZATION, admin);
    series.withText("biology");
    EventSearchQuery events = new EventSearchQuery(ORGANIZATION, admin);
    events.withText("biology");

    List<JsonObject> seriesMatches = collectMultiMatches(
        toJson(new SeriesQueryBuilder(series, Map.of("title", 5.0f, "description", 0.5f))));
    for (String type : new String[] {"phrase", "phrase_prefix"}) {
      JsonObject match = byType(seriesMatches, type).get(0);
      assertEquals(5.0f, fieldWeight(match, "title"), 0.0f);
      assertEquals(0.5f, fieldWeight(match, "description"), 0.0f);
      assertEquals(1.0f, fieldWeight(match, "creator"), 0.0f);
    }

    // The configuration of events does not affect series and the other way around
    List<JsonObject> eventMatches = collectMultiMatches(
        toJson(new EventQueryBuilder(events, Map.of("series_name", 2.5f))));
    for (String type : new String[] {"phrase", "phrase_prefix"}) {
      JsonObject match = byType(eventMatches, type).get(0);
      assertEquals(2.5f, fieldWeight(match, "series_name"), 0.0f);
      assertEquals(1.0f, fieldWeight(match, "title"), 0.0f);
    }

    // Without configuration the title is weighted higher
    assertRankedTextQuery(toJson(new SeriesQueryBuilder(series)));
  }

  /** A boost of 1, like no configuration, leaves the field out of the query. */
  @Test
  public void testFieldsAreNotWeightedWithBoostOfOne() throws Exception {
    SeriesSearchQuery query = new SeriesSearchQuery(ORGANIZATION, admin);
    query.withText("biology");

    for (Map<String, Float> boosts : List.of(Map.of("title", 1.0f, "description", 1.0f), Map.<String, Float>of())) {
      for (JsonObject match : collectMultiMatches(toJson(new SeriesQueryBuilder(query, boosts)))) {
        for (JsonElement field : match.getAsJsonArray("fields")) {
          assertFalse("Unexpected field " + field, field.getAsString().startsWith("title.search"));
          assertFalse("Unexpected field " + field, field.getAsString().startsWith("description.search"));
        }
      }
    }
  }

  @Test
  public void testParseFieldBoosts() {
    Set<String> known = Set.of("title", "description", "series_name");
    Map<String, Float> defaults = AbstractOpenSearchQueryBuilder.DEFAULT_FIELD_BOOSTS;

    assertEquals(Map.of("title", 3.0f, "description", 1.0f),
        OpenSearchIndex.parseFieldBoosts("title^3.0,description^1.0", "test", known));
    assertEquals(Map.of("title", 4.5f, "series_name", 0.5f),
        OpenSearchIndex.parseFieldBoosts("  title^4.5 , series_name ^ 0.5 ,", "test", known));
    assertEquals(Map.of("title", 2.0f), OpenSearchIndex.parseFieldBoosts("title^2", "test", known));

    // Not configured: the default. Configured as empty: no boosts at all
    assertEquals(defaults, OpenSearchIndex.parseFieldBoosts(null, "test", known));
    assertEquals(Map.of(), OpenSearchIndex.parseFieldBoosts("", "test", known));
    assertEquals(Map.of(), OpenSearchIndex.parseFieldBoosts("  ", "test", known));

    // Invalid entries are ignored, but do not affect the valid ones
    assertEquals(Map.of("title", 2.0f),
        OpenSearchIndex.parseFieldBoosts("title^2,description,series_name^high,description^0,description^-1,"
            + "series_name^NaN,series_name^Infinity,^3,desc ription^2", "test", known));

    // The last boost of a field wins
    assertEquals(Map.of("title", 5.0f), OpenSearchIndex.parseFieldBoosts("title^2,title^5", "test", known));

    // Unknown fields are kept (they warn), as extended metadata fields depend on the configuration
    assertEquals(Map.of("title", 2.0f, "extended_metadata_tiss_courseNumber", 4.0f, "nothing", 1.5f),
        OpenSearchIndex.parseFieldBoosts("title^2,extended_metadata_tiss_courseNumber^4,nothing^1.5", "test", known));
  }

  @Test
  public void testSchemaFields() {
    Set<String> series = OpenSearchIndex.schemaFields(SeriesIndexSchema.class);
    assertTrue(series.containsAll(Set.of("title", "uid", "description", "creator")));
    assertFalse(series.contains("series_name"));

    Set<String> events = OpenSearchIndex.schemaFields(EventIndexSchema.class);
    assertTrue(events.containsAll(Set.of("title", "uid", "description", "series_name", "presenter", "location")));
  }

  private static void assertRankedTextQuery(JsonObject json) {
    List<JsonObject> matches = collectMultiMatches(json);

    // Matches by prefix ...
    List<JsonObject> prefix = byType(matches, "bool_prefix");
    assertEquals(1, prefix.size());
    assertEquals("biology", prefix.get(0).get("query").getAsString());

    // ... and ranks by phrase queries
    for (String type : new String[] {"phrase", "phrase_prefix"}) {
      List<JsonObject> ranking = byType(matches, type);
      assertEquals("Expected one " + type + " query", 1, ranking.size());
      JsonObject match = ranking.get(0);
      assertEquals("biology", match.get("query").getAsString());
      assertTrue(type + " query must be lenient", match.has("lenient") && match.get("lenient").getAsBoolean());
      assertTrue(type + " query must boost the score", match.has("boost") && match.get("boost").getAsFloat() > 0.0f);
      assertEquals(type + " query must weight the title higher than the other fields",
          AbstractOpenSearchQueryBuilder.DEFAULT_FIELD_BOOSTS.get("title"), fieldWeight(match, "title"), 0.0f);
    }
    // The fuzzy words query is the fourth one (see testLastWordMayContainTypos)
    assertEquals(4, matches.size());

    // Whole words must count more than a prefix, which favors long words
    float phraseBoost = byType(matches, "phrase").get(0).get("boost").getAsFloat();
    float prefixBoost = byType(matches, "phrase_prefix").get(0).get("boost").getAsFloat();
    assertTrue("A phrase must count more than a phrase prefix", phraseBoost > prefixBoost);

    // Only the better of the two counts
    List<JsonObject> disMax = collectDisMax(json);
    assertEquals(1, disMax.size());
    assertEquals(2, disMax.get(0).getAsJsonArray("queries").size());

    // Ranking is optional, matching is not
    assertFalse("Ranking queries must be optional", isRequired(json, byType(matches, "phrase").get(0)));
  }

  private static JsonObject toJson(QueryBuilder queryBuilder) throws IOException {
    XContentBuilder builder = XContentFactory.jsonBuilder();
    queryBuilder.toXContent(builder, ToXContent.EMPTY_PARAMS);
    return JsonParser.parseString(builder.toString()).getAsJsonObject();
  }

  /** Collects the content of all multi_match queries anywhere in the query. */
  private static List<JsonObject> collectMultiMatches(JsonElement element) {
    List<JsonObject> found = new ArrayList<>();
    if (element.isJsonObject()) {
      for (var entry : element.getAsJsonObject().entrySet()) {
        if ("multi_match".equals(entry.getKey())) {
          found.add(entry.getValue().getAsJsonObject());
        } else {
          found.addAll(collectMultiMatches(entry.getValue()));
        }
      }
    } else if (element.isJsonArray()) {
      for (JsonElement child : element.getAsJsonArray()) {
        found.addAll(collectMultiMatches(child));
      }
    }
    return found;
  }

  /** Collects the content of all dis_max queries anywhere in the query. */
  private static List<JsonObject> collectDisMax(JsonElement element) {
    List<JsonObject> found = new ArrayList<>();
    if (element.isJsonObject()) {
      for (var entry : element.getAsJsonObject().entrySet()) {
        if ("dis_max".equals(entry.getKey())) {
          found.add(entry.getValue().getAsJsonObject());
        }
        found.addAll(collectDisMax(entry.getValue()));
      }
    } else if (element.isJsonArray()) {
      for (JsonElement child : element.getAsJsonArray()) {
        found.addAll(collectDisMax(child));
      }
    }
    return found;
  }

  /** Returns the weight of a field relative to the other searchable fields, or 1 if it is not weighted. */
  private static float fieldWeight(JsonObject multiMatch, String field) {
    float others = 1.0f;
    float weight = 1.0f;
    for (JsonElement f : multiMatch.getAsJsonArray("fields")) {
      String[] parts = f.getAsString().split("\\^");
      float boost = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
      if (parts[0].equals(field + ".search")) {
        weight = boost;
      } else if (parts[0].equals("*.search")) {
        others = boost;
      }
    }
    return weight / others;
  }

  private static List<JsonObject> byType(List<JsonObject> matches, String type) {
    List<JsonObject> result = new ArrayList<>();
    for (JsonObject match : matches) {
      if (type.equals(match.get("type").getAsString())) {
        result.add(match);
      }
    }
    return result;
  }

  /** Tells whether the given multi_match is inside a "must" clause (as opposed to an optional "should"). */
  private static boolean isRequired(JsonElement root, JsonObject multiMatch) {
    return clauseOf(root, multiMatch, null).equals("must");
  }

  private static String clauseOf(JsonElement element, JsonObject multiMatch, String clause) {
    if (element.isJsonObject()) {
      for (var entry : element.getAsJsonObject().entrySet()) {
        if ("multi_match".equals(entry.getKey()) && entry.getValue() == multiMatch) {
          return clause == null ? "" : clause;
        }
        String found = clauseOf(entry.getValue(), multiMatch, nearestClause(entry.getKey(), clause));
        if (found != null) {
          return found;
        }
      }
    } else if (element.isJsonArray()) {
      JsonArray array = element.getAsJsonArray();
      for (JsonElement child : array) {
        String found = clauseOf(child, multiMatch, clause);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static String nearestClause(String key, String current) {
    return "must".equals(key) || "should".equals(key) || "filter".equals(key) ? key : current;
  }
}
