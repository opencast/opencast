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

package org.opencastproject.adminui.endpoint;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.test.rest.NotFoundExceptionMapper;
import org.opencastproject.test.rest.RestServiceTestEnv;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class ThemesEndpointTest {
  private static final Logger logger = LoggerFactory.getLogger(ThemesEndpointTest.class);
  private static final RestServiceTestEnv rt = testEnvForClasses(
          TestThemesEndpoint.class,
          NotFoundExceptionMapper.class);
  /** A parser for handling JSON documents inside the body of a request. */
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
  public void testCreateTheme() throws IOException {
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

    JsonObject theme = (JsonParser.parseString(result).getAsJsonObject());
    // Make sure the creationDate property exists
    assertTrue(StringUtils.trimToNull(theme.get("creationDate").getAsString()) != null);
    // Remove it from the results
    theme.remove("creationDate");
    logger.info("Expected" + themesString);
    logger.info("Result: " + theme.toString());
    assertThat(themesString, SameJSONAs.sameJSONAs(theme.toString()).allowingAnyArrayOrdering());
  }

  @Test
  public void testUpdateTheme() throws IOException {
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

    JsonObject theme = (JsonParser.parseString(result).getAsJsonObject());
    // Make sure the creationDate property exists
    assertTrue(StringUtils.trimToNull(theme.get("creationDate").getAsString()) != null);
    // Remove it from the results
    theme.remove("creationDate");
    logger.info("Expected" + themesString);
    logger.info("Result: " + theme.toString());
    assertThat(themesString, SameJSONAs.sameJSONAs(theme.toString()).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetThemes() throws IOException {
    String themesString = IOUtils.toString(getClass().getResource("/themes.json"), "UTF-8");
    String result = given().expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("/themes.json")).asString();
    logger.info("Expected" + themesString);
    logger.info("Result" + result);
    assertThat(themesString, SameJSONAs.sameJSONAs(result).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetTheme() {
    // Test invalid id
    given().pathParam("themeId", "asdasd").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{themeId}.json")).asString();
    // Test unknown id
    given().pathParam("themeId", notFoundId).expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{themeId}.json")).asString();
    // Test correct id
    String result = given().pathParam("themeId", foundId).expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/{themeId}.json")).asString();
    JsonObject theme = (JsonParser.parseString(result).getAsJsonObject());
    assertEquals(foundId.toString(), theme.get("id").getAsString());
    assertEquals("test.mp4", theme.get("bumperFileName").getAsString());
    assertEquals("http://localhost:8080/staticfiles/uuid1", theme.get("bumperFileUrl").getAsString());
  }

  @Test
  public void testGetThemeUsage() {
    // Test invalid id
    given().pathParam("themeId", "asdasd").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{themeId}/usage.json")).asString();
    // Test unknown id
    given().pathParam("themeId", notFoundId).expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{themeId}/usage.json")).asString();
    // Test correct id
    String result = given().pathParam("themeId", foundId).expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/{themeId}/usage.json")).asString();
    JsonObject series = (JsonParser.parseString(result).getAsJsonObject());
    JsonArray seriesArr = series.getAsJsonArray("series");
    Assert.assertEquals(3, seriesArr.size());
    JsonObject series1 = seriesArr.get(0).getAsJsonObject();
    Assert.assertEquals("Series1Id", series1.get("id").getAsString());
    Assert.assertEquals("Series 1 Title", series1.get("title").getAsString());
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
