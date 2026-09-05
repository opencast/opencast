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
package org.opencastproject.external.endpoint;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_ACCEPTABLE;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.test.rest.RestServiceTestEnv;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.http.ContentType;
import uk.co.datumedge.hamcrest.json.SameJSONAs;

/** Test cases for {@link SeriesEndpoint} */
public class SeriesEndpointTest {

  private static final String APP_V1_0_0_JSON = "application/v1.0.0+json";
  private static final String APP_V1_0_0_XML = "application/v1.0.0+xml";

  /** The REST test environment */
  private static final RestServiceTestEnv env = testEnvForClasses(TestSeriesEndpoint.class);

  /** The json parser */

  @BeforeClass
  public static void oneTimeSetUp() {
    env.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    env.tearDownServer();
  }

  /** Unit test for {@link SeriesEndpoint#getSeriesList(String, String, String, String, int, int, Boolean, Boolean)} */
  @Test
  public void testGetSeriesListJson() throws Exception {
    final String response = given().log().all().expect().statusCode(SC_OK).when().get(env.host("/")).asString();

    final JsonArray json = JsonParser.parseString(response).getAsJsonArray();
    assertEquals(1, json.size());

    final JsonObject series1 = json.get(0).getAsJsonObject();
    assertEquals("4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f", series1.get("identifier").getAsString());
    assertEquals("Via API", series1.get("title").getAsString());
    assertEquals("2015-04-16T09:12:36Z", series1.get("created").getAsString());
    assertEquals("Gracie Walsh", series1.get("creator").getAsString());

    final JsonArray subjects = series1.getAsJsonArray("subjects");
    assertEquals(1, subjects.size());
    assertEquals("Topic", subjects.get(0).getAsString());
  }

  /** Unit test for {@link SeriesEndpoint#getSeries(String, String, Boolean)} */
  @Test
  public void testGetSeriesJson() throws Exception {
    final String response = given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f")
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_OK).when().get(env.host("/{seriesId}"))
            .asString();

    final JsonObject json = JsonParser.parseString(response).getAsJsonObject();
    assertEquals("4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f", json.get("identifier").getAsString());
    assertEquals("Via API", json.get("title").getAsString());
    assertEquals("A series created over the external API", json.get("description").getAsString());
    assertEquals("opencast", json.get("organization").getAsString());
    assertEquals("Gracie Walsh", json.get("creator").getAsString());
    assertEquals("2015-04-16T09:12:36Z", json.get("created").getAsString());

    JsonArray topics = json.getAsJsonArray("subjects");
    assertEquals(1, topics.size());
    assertEquals("Topic", topics.get(0).getAsString());

    JsonArray contributors = json.getAsJsonArray("contributors");
    assertTrue(contributors.contains(new JsonPrimitive("Nu'man Farooq Morcos")));
    assertTrue(contributors.contains(new JsonPrimitive("Alfie Gibbons")));
    assertEquals(2, contributors.size());

    JsonArray publishers = json.getAsJsonArray("publishers");
    assertTrue(publishers.contains(new JsonPrimitive("Sophie Chandler")));
    assertEquals(1, publishers.size());

    JsonArray organizers = json.getAsJsonArray("organizers");
    assertTrue(organizers.contains(new JsonPrimitive("Peter Feierabend")));
    assertTrue(organizers.contains(new JsonPrimitive("Florian Naumann")));
    assertTrue(organizers.contains(new JsonPrimitive("Niklas Vogler")));
    assertEquals(3, organizers.size());
  }

  @Test
  public void testCreateSeriesJson() throws Exception {
    String metadataJson = IOUtils.toString(getClass().getResource("/series/create-series-metadata.json"), UTF_8);
    given().formParam("metadata", metadataJson).formParam("acl", "[]").formParam("theme", "1234")
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_CREATED).when().post(env.host("/"));
  }

  @Test
  public void testCreateSeriesWithMissingMetadataJson() throws Exception {
    given().formParam("acl", "").formParam("theme", "1234").accept(APP_V1_0_0_JSON).log().all().expect()
            .statusCode(SC_BAD_REQUEST).when().post(env.host("/"));
  }

  @Test
  public void testNotImplementedSeriesXml() throws Exception {
    given().accept(APP_V1_0_0_XML).log().all().expect().statusCode(SC_NOT_ACCEPTABLE).when().post(env.host("/"));
  }

  @Test
  public void testDeleteSeriesJson() throws Exception {
    given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f").accept(APP_V1_0_0_JSON).log().all().expect()
            .statusCode(SC_NO_CONTENT).when().delete(env.host("/{seriesId}"));
  }

  /** Unit test for {@link SeriesEndpoint#getSeriesMetadata(String, String, String)} */
  @Test
  public void testMissingGetSeriesMetadataJson() throws Exception {
    given().pathParam("seriesId", "unknown-series-id").accept(APP_V1_0_0_JSON).log().all().expect()
            .statusCode(SC_NOT_FOUND).when().get(env.host("/{seriesId}/metadata"));
  }

  @Test
  public void testGetSeriesMetadataJson() throws Exception {
    String expected = IOUtils.toString(getClass().getResource("/series/metadata/series-metadata.json"), UTF_8);
    final String response = given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f")
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_OK).when().get(env.host("/{seriesId}/metadata"))
            .asString();
    assertThat(expected, SameJSONAs.sameJSONAs(response).allowingAnyArrayOrdering());
  }

  /** Unit test for {@link SeriesEndpoint#getSeriesMetadata(String, String, String)} */
  @Test
  public void testMissingSeriesGetSeriesMetadataByTypeJson() throws Exception {
    given().pathParam("seriesId", "unknown-series-id").pathParam("type", "dublincore").accept(APP_V1_0_0_JSON).log()
            .all().expect().statusCode(SC_NOT_FOUND).when().get(env.host("/{seriesId}/metadata/{type}"));
  }

  @Test
  public void testMissingCatalogGetSeriesMetadataByTypeJson() throws Exception {
    given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f").pathParam("type", "missing")
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_NOT_FOUND).when()
            .get(env.host("/{seriesId}/metadata/{type}"));
  }

  @Test
  public void testGetSeriesMetadataByTypeJson() throws Exception {
    String expected = IOUtils.toString(getClass().getResource("/series/metadata/series-metadata-dublincore.json"),
        UTF_8);
    final String response = given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f")
            .queryParam("type", "dublincore/series").accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_OK)
            .when().get(env.host("/{seriesId}/metadata")).asString();
    assertThat(expected, SameJSONAs.sameJSONAs(response).allowingAnyArrayOrdering());
  }

  /** Unit test for {@link SeriesEndpoint#updateSeriesMetadata(String, String, String, String)} */
  @Test
  public void testMissingMetadataUpdateSeriesMetadataJson() throws Exception {
    given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f").queryParam("type", "dublincore/series")
            .contentType(ContentType.URLENC)
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_BAD_REQUEST).when()
            .put(env.host("/{seriesId}/metadata"));
  }

  @Test
  public void testEmptyMetadataUpdateSeriesMetadataJson() throws Exception {
    given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f").queryParam("type", "dublincore/series")
            .formParam("metadata", "[]").accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_BAD_REQUEST).when()
            .put(env.host("/{seriesId}/metadata"));
  }

  @Test
  public void testMissingSeriesUpdateSeriesMetadataJson() throws Exception {
    String metadata = IOUtils.toString(getClass().getResource("/series/metadata/put-metadata.json"), UTF_8);
    given().pathParam("seriesId", "unknown-series-id").pathParam("type", "dublincore").formParam("metadata", metadata)
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_NOT_FOUND).when()
            .put(env.host("/{seriesId}/metadata/{type}"));
  }

  @Test
  public void testMissingCatalogUpdateSeriesMetadataJson() throws Exception {
    String metadata = IOUtils.toString(getClass().getResource("/series/metadata/put-metadata.json"), UTF_8);
    given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f").pathParam("type", "missing")
            .formParam("metadata", metadata).accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_NOT_FOUND)
            .when().put(env.host("/{seriesId}/metadata/{type}"));
  }

  @Test
  public void testUpdateSeriesMetadataJson() throws Exception {
    String metadata = IOUtils.toString(getClass().getResource("/series/metadata/put-metadata.json"), UTF_8);
    given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f").queryParam("type", "dublincore/series")
            .formParam("metadata", metadata).accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_OK).when()
            .put(env.host("/{seriesId}/metadata"));
  }

  /** Unit test for {@link SeriesEndpoint#deleteSeriesMetadataByType(String, String, String)} */
  @Test
  public void testMissingSeriesDeleteSeriesMetadataJson() throws Exception {
    given().pathParam("seriesId", "unknown-series-id").pathParam("type", "othercatalog").accept(APP_V1_0_0_JSON).log()
            .all().expect().statusCode(SC_NOT_FOUND).when().delete(env.host("/{seriesId}/metadata/{type}"));
  }

  @Test
  public void testMissingCatalogDeleteSeriesMetadataJson() throws Exception {
    given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f").pathParam("type", "missing")
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_NOT_FOUND).when()
            .delete(env.host("/{seriesId}/metadata/{type}"));
  }

  @Test
  public void testDeleteMainCatalogDeleteSeriesMetadataJson() throws Exception {
    given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f").queryParam("type", "dublincore/series")
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_FORBIDDEN).when()
            .delete(env.host("/{seriesId}/metadata"));
  }

  @Test
  public void testDeleteOtherSeriesMetadataJson() throws Exception {
    String metadata = IOUtils.toString(getClass().getResource("/series/metadata/put-metadata.json"), UTF_8);
    given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f").queryParam("type", "othercatalog/episode")
            .formParam("metadata", metadata).accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_NO_CONTENT)
            .when().delete(env.host("/{seriesId}/metadata"));
  }

  /** Unit test for {@link SeriesEndpoint#getSeriesAcl(String, String)} */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetSeriesAclJson() throws Exception {
    final String response = given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f")
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_OK).when().get(env.host("/{seriesId}/acl"))
            .asString();

    final JsonArray json = JsonParser.parseString(response).getAsJsonArray();
    assertEquals(3, json.size());

    JsonObject adminRead = new JsonObject();
    adminRead.addProperty("allow", true);
    adminRead.addProperty("action", "read");
    adminRead.addProperty("role", "ROLE_ADMIN");

    JsonObject adminWrite = new JsonObject();
    adminWrite.addProperty("allow", true);
    adminWrite.addProperty("action", "write");
    adminWrite.addProperty("role", "ROLE_ADMIN");

    JsonObject anonRead = new JsonObject();
    anonRead.addProperty("allow", true);
    anonRead.addProperty("action", "read");
    anonRead.addProperty("role", "ROLE_ANONYMOUS");

    assertTrue(json.contains(adminRead));
    assertTrue(json.contains(adminWrite));
    assertTrue(json.contains(anonRead));
  }

  /** Unit test for {@link SeriesEndpoint#getSeriesProperties(String, String)} */
  @Test
  public void testGetSeriesPropertiesJson() throws Exception {
    final String response = given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f")
            .accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_OK).when()
            .get(env.host("/{seriesId}/properties")).asString();

    final JsonObject json = JsonParser.parseString(response).getAsJsonObject();
    assertEquals(2, json.size());
    assertEquals("false", json.get("live").getAsString());
    assertEquals("true", json.get("ondemand").getAsString());
  }

  @Test
  public void testUpdateSeriesAclJson() throws Exception {
    final String updatedAcl = "[{\"allow\":true,\"action\":\"write\",\"role\": \"ROLE_ADMIN\"},"
        + "{\"allow\": true,\"action\": \"read\",\"role\": \"ROLE_USER\"}]";
    final String response = given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f")
            .formParam("acl", updatedAcl).accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_OK).when()
            .put(env.host("/{seriesId}/acl")).asString();

    final JsonArray json = JsonParser.parseString(response).getAsJsonArray();
    assertEquals(JsonParser.parseString(updatedAcl), json);
  }

  @Test
  public void testUpdateSeriesPropertiesJson() throws Exception {
    final String updatedProperties = "{\"live\":true,\"ondemand\":\"true\"}";
    final String response = given().pathParam("seriesId", "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f")
            .formParam("properties", updatedProperties).accept(APP_V1_0_0_JSON).log().all().expect().statusCode(SC_OK)
            .when().put(env.host("/{seriesId}/properties")).asString();

    final JsonObject json = JsonParser.parseString(response).getAsJsonObject();
    assertEquals(JsonParser.parseString(updatedProperties), json);
  }
}
