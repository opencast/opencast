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

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.rest.NotFoundExceptionMapper;
import org.opencastproject.rest.RestServiceTestEnv;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

import java.io.IOException;

public class ThemesEndpointTest {

  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestThemesEndpoint.class,
          NotFoundExceptionMapper.class);
  /** A parser for handling JSON documents inside the body of a request. */
  private final JSONParser parser = new JSONParser();
  private Long foundId = 1L;
  private Long notFoundId = 10L;

  @BeforeClass
  public static void oneTimeSetUp() {
    rt.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    rt.tearDownServer();
  }

  @Test
  public void testCreateTheme() throws ParseException, IOException {
    String themesString = IOUtils.toString(getClass().getResource("/theme-create.json"), "UTF-8");
    Boolean isDefault = true;
    String name = "New Theme Name";
    String description = "New Theme Description";

    Boolean bumperActive = true;
    String bumperFile = "bumper-file";

    Boolean trailerActive = true;
    String trailerFile = "trailer-file";

    Boolean titleSlideActive = true;
    String titleSlideBackground = "title-background";
    String titleSlideMetadata = "title-metadata";

    Boolean licenseSlideActive = true;
    String licenseSlideBackground = "license-background";
    String licenseSlideDescription = "license-description";

    Boolean watermarkActive = true;
    String watermarkPosition = "watermark-position";
    String watermarkFile = "watermark-file";

    String result = given().formParam("default", isDefault.toString()).formParam("name", name)
            .formParam("description", description).formParam("bumperActive", bumperActive.toString())
            .formParam("bumperFile", bumperFile).formParam("trailerActive", trailerActive.toString())
            .formParam("trailerFile", trailerFile).formParam("titleSlideActive", titleSlideActive.toString())
            .formParam("titleSlideBackground", titleSlideBackground)
            .formParam("titleSlideMetadata", titleSlideMetadata).formParam("licenseSlideActive", licenseSlideActive)
            .formParam("licenseSlideBackground", licenseSlideBackground)
            .formParam("licenseSlideDescription", licenseSlideDescription)
            .formParam("watermarkActive", watermarkActive).formParam("watermarkPosition", watermarkPosition)
            .formParam("watermarkFile", watermarkFile).expect().statusCode(HttpStatus.SC_OK).when().post(rt.host("/"))
            .asString();

    JSONObject theme = ((JSONObject) parser.parse(result));
    // Make sure the creationDate property exists
    assertTrue(StringUtils.trimToNull(theme.get("creationDate").toString()) != null);
    // Remove it from the results
    theme.remove("creationDate");
    System.out.println("Expected" + themesString);
    System.out.println("Result: " + theme.toJSONString());
    assertThat(themesString, SameJSONAs.sameJSONAs(theme.toJSONString()).allowingAnyArrayOrdering());
  }

  @Test
  public void testUpdateTheme() throws ParseException, IOException {
    String themesString = IOUtils.toString(getClass().getResource("/theme-update.json"), "UTF-8");
    String result = given().pathParam("themeId", 1).formParam("default", true).formParam("name", "new-name")
            .formParam("description", "new-description").formParam("bumperActive", true)
            .formParam("bumperFile", "new-bumper-file").formParam("trailerActive", true)
            .formParam("trailerFile", "new-trailer-file").formParam("titleSlideActive", true)
            .formParam("titleSlideBackground", "new-title-background")
            .formParam("titleSlideMetadata", "new-title-metadata").formParam("licenseSlideActive", true)
            .formParam("licenseSlideBackground", "new-license-background")
            .formParam("licenseSlideDescription", "new-license-description").formParam("watermarkActive", true)
            .formParam("watermarkPosition", "new-watermark-position").formParam("watermarkFile", "new-watermark-file")
            .expect().statusCode(HttpStatus.SC_OK).when().put(rt.host("/{themeId}")).asString();

    JSONObject theme = ((JSONObject) parser.parse(result));
    // Make sure the creationDate property exists
    assertTrue(StringUtils.trimToNull(theme.get("creationDate").toString()) != null);
    // Remove it from the results
    theme.remove("creationDate");
    System.out.println("Expected" + themesString);
    System.out.println("Result: " + theme.toJSONString());
    assertThat(themesString, SameJSONAs.sameJSONAs(theme.toJSONString()).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetThemes() throws ParseException, IOException {
    String themesString = IOUtils.toString(getClass().getResource("/themes.json"), "UTF-8");
    String result = given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("/themes.json")).asString();
    System.out.println("Expected" + themesString);
    System.out.println("Result" + result);
    assertThat(themesString, SameJSONAs.sameJSONAs(result).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetTheme() throws ParseException {
    // Test invalid id
    given().pathParam("themeId", "asdasd").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{themeId}.json")).asString();
    // Test unknown id
    given().pathParam("themeId", notFoundId).expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{themeId}.json")).asString();
    // Test correct id
    String result = given().pathParam("themeId", foundId).expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/{themeId}.json")).asString();
    JSONObject theme = ((JSONObject) parser.parse(result));
    assertEquals(foundId.toString(), theme.get("id").toString());
    assertEquals("test.mp4", theme.get("bumperFileName").toString());
    assertEquals("http://localhost:8080/staticfiles/uuid1", theme.get("bumperFileUrl").toString());
  }

  @Test
  public void testGetThemeUsage() throws ParseException {
    // Test invalid id
    given().pathParam("themeId", "asdasd").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{themeId}/usage.json")).asString();
    // Test unknown id
    given().pathParam("themeId", notFoundId).expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{themeId}/usage.json")).asString();
    // Test correct id
    String result = given().pathParam("themeId", foundId).expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/{themeId}/usage.json")).asString();
    JSONObject series = ((JSONObject) parser.parse(result));
    JSONArray seriesArr = (JSONArray) series.get("series");
    Assert.assertEquals(3, seriesArr.size());
    JSONObject series1 = (JSONObject) seriesArr.get(0);
    Assert.assertEquals("Series1Id", series1.get("id").toString());
    Assert.assertEquals("Series 1 Title", series1.get("title").toString());
  }

  @Test
  public void testDeleteThemes() {
    // Test invalid id
    given().pathParam("themeId", "asdasd").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .delete(rt.host("/{themeId}")).asString();
    // Test unknown id
    given().pathParam("themeId", notFoundId).expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .delete(rt.host("/{themeId}")).asString();
    // Test correct id
    given().pathParam("themeId", foundId).expect().statusCode(HttpStatus.SC_NO_CONTENT).when()
            .delete(rt.host("/{themeId}")).asString();
  }
}
