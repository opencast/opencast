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

package org.opencastproject.adminui.endpoint;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.test.rest.RestServiceTestEnv;

import org.apache.http.HttpStatus;
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

import io.restassured.http.ContentType;

public class SeriesEndpointTest {

  private static final RestServiceTestEnv rt = testEnvForClasses(TestSeriesEndpoint.class);

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
    assertEquals("{\"1\":{\"name\":\"theme-1-name\",\"description\":\"theme-1-description\"}}", themes.toJSONString());
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
