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
package org.opencastproject.adminui.endpoint;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase.SortType;
import org.opencastproject.rest.BulkOperationResult;
import org.opencastproject.rest.RestServiceTestEnv;

import com.jayway.restassured.http.ContentType;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class SeriesEndpointTest {

  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestSeriesEndpoint.class);

  private JSONParser parser;

  @Test
  public void testSeriesEndpointResult() throws ParseException, IOException {
    InputStream stream = SeriesEndpointTest.class.getResourceAsStream("/series.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);
    JSONObject actual = (JSONObject) parser.parse(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/series.json")).asString());
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testSortOrder() throws ParseException, IOException {
    // Test Sort by Contributor
    InputStream stream = SeriesEndpointTest.class.getResourceAsStream("/series_desc.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);

    JSONObject actual = (JSONObject) parser.parse(given().queryParam("sort", "contributors:DESC").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when().get(rt.host("/series.json")).asString());
    Assert.assertEquals(expected, actual);

    stream = SeriesEndpointTest.class.getResourceAsStream("/series_asc.json");
    reader = new InputStreamReader(stream);
    expected = (JSONObject) new JSONParser().parse(reader);

    actual = (JSONObject) parser.parse(given().queryParam("sort", "contributors:ASC").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when().get(rt.host("/series.json")).asString());
    Assert.assertEquals(expected, actual);
    // Test Sort by Created Date
    stream = SeriesEndpointTest.class.getResourceAsStream("/series_desc.json");
    reader = new InputStreamReader(stream);
    expected = (JSONObject) new JSONParser().parse(reader);

    actual = (JSONObject) parser.parse(given().queryParam("sort", "createdDateTime:DESC").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when().get(rt.host("/series.json")).asString());
    Assert.assertEquals(expected, actual);

    stream = SeriesEndpointTest.class.getResourceAsStream("/series_asc.json");
    reader = new InputStreamReader(stream);
    expected = (JSONObject) new JSONParser().parse(reader);

    actual = (JSONObject) parser.parse(given().queryParam("sort", "createdDateTime:ASC").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when().get(rt.host("/series.json")).asString());
    Assert.assertEquals(expected, actual);
    // Test Sort by Organizer
    stream = SeriesEndpointTest.class.getResourceAsStream("/series_desc.json");
    reader = new InputStreamReader(stream);
    expected = (JSONObject) new JSONParser().parse(reader);

    actual = (JSONObject) parser.parse(given().queryParam("sort", "creator:DESC").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/series.json")).asString());
    Assert.assertEquals(expected, actual);

    stream = SeriesEndpointTest.class.getResourceAsStream("/series_asc.json");
    reader = new InputStreamReader(stream);
    expected = (JSONObject) new JSONParser().parse(reader);

    actual = (JSONObject) parser.parse(given().queryParam("sort", "creator:ASC").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/series.json")).asString());
    Assert.assertEquals(expected, actual);
    // Test Sort by Title
    stream = SeriesEndpointTest.class.getResourceAsStream("/series_desc.json");
    reader = new InputStreamReader(stream);
    expected = (JSONObject) new JSONParser().parse(reader);

    actual = (JSONObject) parser.parse(given().queryParam("sort", "title:DESC").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/series.json")).asString());
    Assert.assertEquals(expected, actual);

    stream = SeriesEndpointTest.class.getResourceAsStream("/series_asc.json");
    reader = new InputStreamReader(stream);
    expected = (JSONObject) new JSONParser().parse(reader);

    actual = (JSONObject) parser.parse(given().queryParam("sort", "title:ASC").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/series.json")).asString());
    Assert.assertEquals(expected, actual);

    given().queryParam("sort", "managed_acl:ASC").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .when().get(rt.host("/series.json"));
  }

  @Test
  public void testSeriesMessages() throws ParseException, IOException {
    JSONArray actual = (JSONArray) parser.parse(given().pathParam("seriesId", "uuid").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when().get(rt.host("/{seriesId}/messages"))
            .asString());
    Assert.assertEquals(3, actual.size());
    JSONObject message = (JSONObject) actual.get(0);
    JSONObject person = (JSONObject) message.get("person");
    Assert.assertEquals("test3@email.ch", person.get("email"));
    JSONArray errors = (JSONArray) message.get("errors");
    Assert.assertEquals(2, errors.size());
    JSONObject error = (JSONObject) errors.get(0);
    Assert.assertEquals("source", error.get("source"));
  }

  @Test
  public void testSeriesMessagesDateSortedAsc() throws ParseException, IOException {
    JSONArray actual = (JSONArray) parser.parse(given().pathParam("seriesId", "uuid").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when()
            .get(rt.host("/{seriesId}/messages?sort=" + SortType.DATE)).asString());
    Assert.assertEquals(3, actual.size());
    JSONObject message = (JSONObject) actual.get(0);
    JSONObject person = (JSONObject) message.get("person");
    Assert.assertEquals("test1@email.ch", person.get("email"));
    JSONArray errors = (JSONArray) message.get("errors");
    Assert.assertEquals(2, errors.size());
    JSONObject error = (JSONObject) errors.get(0);
    Assert.assertEquals("source", error.get("source"));
  }

  @Test
  public void testSeriesMessagesDateSortedDesc() throws ParseException, IOException {
    JSONArray actual = (JSONArray) parser.parse(given().pathParam("seriesId", "uuid").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when()
            .get(rt.host("/{seriesId}/messages?sort=" + SortType.DATE_DESC)).asString());
    Assert.assertEquals(3, actual.size());
    JSONObject message = (JSONObject) actual.get(0);
    JSONObject person = (JSONObject) message.get("person");
    Assert.assertEquals("test3@email.ch", person.get("email"));
    JSONArray errors = (JSONArray) message.get("errors");
    Assert.assertEquals(2, errors.size());
    JSONObject error = (JSONObject) errors.get(0);
    Assert.assertEquals("source", error.get("source"));
  }

  @Test
  public void testSeriesMessagesSenderSortedAsc() throws ParseException, IOException {
    JSONArray actual = (JSONArray) parser.parse(given().pathParam("seriesId", "uuid").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when()
            .get(rt.host("/{seriesId}/messages?sort=" + SortType.SENDER)).asString());
    Assert.assertEquals(3, actual.size());
    JSONObject message = (JSONObject) actual.get(0);
    JSONObject person = (JSONObject) message.get("person");
    Assert.assertEquals("test1@email.ch", person.get("email"));
    JSONArray errors = (JSONArray) message.get("errors");
    Assert.assertEquals(2, errors.size());
    JSONObject error = (JSONObject) errors.get(0);
    Assert.assertEquals("source", error.get("source"));
  }

  @Test
  public void testSeriesMessagesSenderSortedDesc() throws ParseException, IOException {
    JSONArray actual = (JSONArray) parser.parse(given().pathParam("seriesId", "uuid").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when()
            .get(rt.host("/{seriesId}/messages?sort=" + SortType.SENDER_DESC)).asString());
    Assert.assertEquals(3, actual.size());
    JSONObject message = (JSONObject) actual.get(0);
    JSONObject person = (JSONObject) message.get("person");
    Assert.assertEquals("test3@email.ch", person.get("email"));
    JSONArray errors = (JSONArray) message.get("errors");
    Assert.assertEquals(2, errors.size());
    JSONObject error = (JSONObject) errors.get(0);
    Assert.assertEquals("source", error.get("source"));
  }

  @Test
  public void testCreateNewSeries() throws Exception {
    String seriesMetadataString = IOUtils.toString(getClass().getResource("/postNewSeriesMetadata.json"), "UTF-8");

    given().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("new"));
    given().formParam("metadata", "adsd").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("new"));

    String result = given().formParam("metadata", seriesMetadataString).expect().statusCode(HttpStatus.SC_CREATED)
            .when().post(rt.host("new")).asString();
    Assert.assertEquals("23", result);
  }

  @Test
  public void testDelete() throws Exception {
    BulkOperationResult emptyResult = new BulkOperationResult();
    BulkOperationResult foundResult = new BulkOperationResult();
    foundResult.addOk("1");
    foundResult.addOk("2");
    foundResult.addOk("3");
    foundResult.addNotFound("4");
    foundResult.addServerError("5");
    given().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/deleteSeries"));
    given().body("{}").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/deleteSeries"));
    String result = given().body("[]").expect().statusCode(HttpStatus.SC_OK).when().post(rt.host("/deleteSeries"))
            .asString();
    assertEquals(emptyResult.toJson(), result);
    result = given().body("[1,2,3,4,5]").expect().statusCode(HttpStatus.SC_OK).when().post(rt.host("/deleteSeries"))
            .asString();
    assertEquals(foundResult.toJson(), result);
  }

  @Test
  public void testGetTheme() throws ParseException {
    String foundId = "1";
    String seriesNotFound = "11";
    String themeNotFound = "2";

    String result = given().pathParam("seriesId", foundId).expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/{seriesId}/theme.json")).asString();
    JSONObject theme = ((JSONObject) parser.parse(result));
    assertEquals("theme-1-name", theme.get(foundId));

    given().pathParam("seriesId", seriesNotFound).expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{seriesId}/theme.json")).asString();

    result = given().pathParam("seriesId", themeNotFound).expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/{seriesId}/theme.json")).asString();
    assertEquals("{}", result);
  }

  @Test
  public void testPutTheme() throws ParseException {
    String validSeriesId = "1";
    String validThemeId = "1";
    String result = given().pathParam("seriesId", validSeriesId).formParam("themeId", validThemeId).expect()
            .statusCode(HttpStatus.SC_OK).when().put(rt.host("/{seriesId}/theme")).asString();
    JSONObject theme = ((JSONObject) parser.parse(result));
    assertEquals("theme-1-name", theme.get(validSeriesId));
  }

  @Test
  public void testRemoveTheme() throws ParseException {
    given().pathParam("seriesId", "1").expect().statusCode(HttpStatus.SC_NO_CONTENT).when()
            .delete(rt.host("/{seriesId}/theme"));
  }

  @Test
  public void testGetNewTheme() throws ParseException {
    String result = given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("/new/themes")).asString();
    JSONObject themes = ((JSONObject) parser.parse(result));
    assertEquals("{\"1\":\"theme-1-name\"}", themes.toJSONString());
  }

  @Test
  public void testUpdateAcl() throws ParseException {
    String validSeriesId = "1";
    String validAcl = "{\"acl\":{\"ace\":[]}}";
    given().pathParam("seriesId", validSeriesId).formParam("acl", "bla").formParam("override", "true").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/{seriesId}/access"));

    given().pathParam("seriesId", "bla").formParam("acl", validAcl).formParam("override", "true").expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().post(rt.host("/{seriesId}/access"));

    given().pathParam("seriesId", validSeriesId).formParam("acl", validAcl).formParam("override", "true").expect()
            .statusCode(HttpStatus.SC_OK).when().post(rt.host("/{seriesId}/access"));

    given().pathParam("seriesId", "2").formParam("acl", validAcl).formParam("override", "true").expect()
            .statusCode(HttpStatus.SC_CONFLICT).when().post(rt.host("/{seriesId}/access"));
  }

  @Before
  public void setUp() {
    parser = new JSONParser();
  }

  @BeforeClass
  public static void oneTimeSetUp() {
    Locale.setDefault(Locale.ENGLISH);
    rt.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    rt.tearDownServer();
  }

}
