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

import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Room;
import org.opencastproject.rest.NotFoundExceptionMapper;
import org.opencastproject.rest.RestServiceTestEnv;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.httpclient.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class BlacklistsEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestBlacklistEndpoint.class,
          NotFoundExceptionMapper.class);

  private static JSONParser parser;
  private String validRoomBlackListedId = "13";
  private String validPersonBlackListedId = "8";

  @Test
  public void testGetPeriod() throws ParseException {
    given().pathParam("periodId", 0).log().all().expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{periodId}"));

    String periodResponse = given().pathParam("periodId", 1).log().all().expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/{periodId}")).asString();
    JSONObject period = (JSONObject) parser.parse(periodResponse);
    long id = Long.parseLong(period.get("id").toString());
    Assert.assertEquals(1L, id);
    Assert.assertEquals("2014-05-11T13:24:31Z", period.get("start"));
    Assert.assertEquals("2014-05-11T13:30:00Z", period.get("end"));
    Assert.assertEquals("Sick", period.get("purpose"));
    Assert.assertEquals("Flu", period.get("comment"));
  }

  @Test
  public void testGetBlacklists() throws ParseException, IOException {
    given().log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("/blacklists.json"));
    given().queryParam("type", "test").log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .get(rt.host("/blacklists.json"));

    // Test type
    String responseString = given().queryParam("type", "person").log().all().expect().statusCode(HttpStatus.SC_OK)
            .when().get(rt.host("/blacklists.json")).asString();

    JSONArray responseJson = (JSONArray) parser.parse(responseString);
    Assert.assertEquals(2, responseJson.size());
    JSONObject blacklist = (JSONObject) responseJson.get(1);
    long id = Long.parseLong(blacklist.get("id").toString());
    Assert.assertEquals(1L, id);
    Assert.assertEquals("2014-05-11T13:24:31Z", blacklist.get("start"));
    Assert.assertEquals("2014-05-11T13:30:00Z", blacklist.get("end"));

    // Test filter by name
    responseString = given().queryParam("type", "person").queryParam("name", "Peter").log().all().expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("/blacklists.json")).asString();
    responseJson = (JSONArray) parser.parse(responseString);
    Assert.assertEquals(0, responseJson.size());

    responseString = given().queryParam("type", "person").queryParam("name", "Hans").log().all().expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("/blacklists.json")).asString();
    responseJson = (JSONArray) parser.parse(responseString);
    Assert.assertEquals(2, responseJson.size());

    // Test limit offset
    responseString = given().queryParam("type", "person").queryParam("name", "Hans").queryParam("limit", 1)
            .queryParam("offset", 1).log().all().expect().statusCode(HttpStatus.SC_OK).when()
            .get(rt.host("/blacklists.json")).asString();
    responseJson = (JSONArray) parser.parse(responseString);
    Assert.assertEquals(1, responseJson.size());
    blacklist = (JSONObject) responseJson.get(0);
    id = Long.parseLong(blacklist.get("id").toString());
    Assert.assertEquals(1L, id);

    // Test order
    responseString = given().queryParam("type", "person").queryParam("name", "Hans").queryParam("limit", 1)
            .queryParam("offset", 1).queryParam("sort", "PERIOD_DESC").log().all().expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("/blacklists.json")).asString();
    responseJson = (JSONArray) parser.parse(responseString);
    Assert.assertEquals(1, responseJson.size());
    blacklist = (JSONObject) responseJson.get(0);
    id = Long.parseLong(blacklist.get("id").toString());
    Assert.assertEquals(2L, id);

    responseString = given().queryParam("type", "person").queryParam("name", "Hans").queryParam("limit", 0)
            .queryParam("offset", 0).queryParam("sort", "PERIOD_DESC").log().all().expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("/blacklists.json")).asString();
    responseJson = (JSONArray) parser.parse(responseString);
    Assert.assertEquals(2, responseJson.size());
    blacklist = (JSONObject) responseJson.get(0);
    id = Long.parseLong(blacklist.get("id").toString());
    Assert.assertEquals(1L, id);
  }

  @Test
  public void testGetEventsCountInputNoParamsExpectsBadRequest() {
    given().log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("/blacklistCount"));
  }

  @Test
  public void testGetEventsCountInputInvalidBlacklistIdExpectsNotFound() {
    given().log().all().queryParam("blacklistedId", "Not a valid id").queryParam("type", Person.TYPE)
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime()))
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime())).expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host("/blacklistCount"));
  }

  @Test
  public void testGetEventsCountInputInvalidTypeExpectsBadRequest() {
    given().log().all().queryParam("blacklistedId", validRoomBlackListedId).queryParam("type", "Invalid Type")
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime()))
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime())).expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("/blacklistCount"));
  }

  @Test
  public void testGetEventsCountInputInvalidStartDateExpectsBadRequest() {
    given().log().all().queryParam("blacklistedId", validRoomBlackListedId).queryParam("type", Room.TYPE)
            .queryParam("start", "Invalid start date")
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime())).expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("/blacklistCount"));
  }

  @Test
  public void testGetEventsCountInputInvalidEndDateExpectsBadRequest() {
    given().log().all().queryParam("blacklistedId", validRoomBlackListedId).queryParam("type", Room.TYPE)
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime()))
            .queryParam("end", "Not a valid end date").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .get(rt.host("/blacklistCount"));
  }

  @Test
  public void testGetEventsCountInputRoomTypeExpectsOkValidJson() throws ParseException {
    String responseString = given().log().all().queryParam("blacklistedId", validRoomBlackListedId)
            .queryParam("type", Room.TYPE)
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime() - 30000))
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime() + 30000)).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("/blacklistCount")).asString();
    JSONObject responseJson = (JSONObject) parser.parse(responseString);
    assertEquals("There should be 3 events with the room to black list in this period.", 3L,
            responseJson.get("eventsCount"));
    assertEquals("There should be 2 courses with the room to black list in this period.", 2L,
            responseJson.get("coursesCount"));
  }

  @Test
  public void testGetEventsCountInputPersonTypeExpectsOkValidJson() throws ParseException {
    String responseString = given().log().all().queryParam("blacklistedId", validPersonBlackListedId)
            .queryParam("type", Person.TYPE)
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime() - 30000))
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime() + 30000)).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("/blacklistCount")).asString();
    JSONObject responseJson = (JSONObject) parser.parse(responseString);
    assertEquals("There should be 3 events with the room to black list in this period.", 4L,
            responseJson.get("eventsCount"));
    assertEquals("There should be 2 courses with the room to black list in this period.", 3L,
            responseJson.get("coursesCount"));
  }

  @Test
  public void testGetEventsCountsInputNoParamsExpectsBadRequest() {
    given().log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("/blacklistCounts"));
  }

  @Test
  public void testGetEventsCountsInputInvalidBlacklistIdExpectsBadRequest() {
    given().log().all().queryParam("blacklistedId", "Not a valid id").queryParam("type", Person.TYPE)
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime()))
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime())).expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().get(rt.host("/blacklistCount"));
  }

  @Test
  public void testGetEventsCountsInputInvalidTypeExpectsBadRequest() {
    given().log().all().queryParam("blacklistedId", validRoomBlackListedId).queryParam("type", "Invalid Type")
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime()))
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime())).expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("/blacklistCounts"));
  }

  @Test
  public void testGetEventsCountsInputInvalidStartDateExpectsBadRequest() {
    given().log().all().queryParam("blacklistedId", validRoomBlackListedId).queryParam("type", Room.TYPE)
            .queryParam("start", "Invalid start date")
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime())).expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().get(rt.host("/blacklistCounts"));
  }

  @Test
  public void testGetEventsCountsInputInvalidEndDateExpectsBadRequest() {
    given().log().all().queryParam("blacklistedId", validRoomBlackListedId).queryParam("type", Room.TYPE)
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime()))
            .queryParam("end", "Not a valid end date").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .get(rt.host("/blacklistCounts"));
  }

  @Test
  public void testGetEventsCountsInputRoomTypeExpectsOkValidJson() throws ParseException {
    String responseString = given().log().all().queryParam("blacklistedIds", "[" + validRoomBlackListedId + "]")
            .queryParam("type", Room.TYPE)
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime() - 30000))
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime() + 30000)).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("/blacklistCounts")).asString();
    JSONObject responseJson = (JSONObject) parser.parse(responseString);

    assertEquals("There should be 3 events with the room to black list in this period.", 3L,
            responseJson.get("eventsTotal"));
    assertEquals("There should be 2 courses with the room to black list in this period.", 2L,
            responseJson.get("coursesTotal"));
    assertEquals(3L, ((JSONObject) ((JSONObject) responseJson.get("results")).get("13")).get("eventsCount"));
    assertEquals(2L, ((JSONObject) ((JSONObject) responseJson.get("results")).get("13")).get("coursesCount"));
  }

  @Test
  public void testGetEventsCountsInputPersonTypeExpectsOkValidJson() throws ParseException {
    String responseString = given().log().all().queryParam("blacklistedIds", "[" + validPersonBlackListedId + "]")
            .queryParam("type", Person.TYPE)
            .queryParam("start", DateTimeSupport.toUTC(TestBlacklistEndpoint.START_DATE.getTime() - 30000))
            .queryParam("end", DateTimeSupport.toUTC(TestBlacklistEndpoint.END_DATE.getTime() + 30000)).expect()
            .statusCode(HttpStatus.SC_OK).when().get(rt.host("/blacklistCounts")).asString();
    JSONObject responseJson = (JSONObject) parser.parse(responseString);
    assertEquals("There should be 4 events with the room to black list in this period.", 4L,
            responseJson.get("eventsTotal"));
    assertEquals("There should be 3 courses with the room to black list in this period.", 3L,
            responseJson.get("coursesTotal"));
    assertEquals(4L, ((JSONObject) ((JSONObject) responseJson.get("results")).get("8")).get("eventsCount"));
    assertEquals(3L, ((JSONObject) ((JSONObject) responseJson.get("results")).get("8")).get("coursesCount"));
  }

  @Test
  public void testPostBlacklist() throws ParseException, IOException {
    given().log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/"));
    given().formParam("start", "123").log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host("/"));
    given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "test").log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host("/"));
    given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "person").formParam("blacklistedId", 0).log().all().expect()
            .statusCode(HttpStatus.SC_NOT_FOUND).when().post(rt.host("/"));
    // Start time after end time should be a bad request.
    given().formParam("start", "2016-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "person").formParam("blacklistedId", 5).log().all().expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/")).asString();
    String responseString = given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "person").formParam("blacklistedId", 5).log().all().expect()
            .statusCode(HttpStatus.SC_CREATED).when().post(rt.host("/")).asString();

    JSONObject responseJson = (JSONObject) parser.parse(responseString);
    Assert.assertNotNull(responseJson);
    long id = Long.parseLong(responseJson.get("id").toString());
    Assert.assertEquals(27L, id);
    Assert.assertEquals("2014-05-11T13:35:20Z", responseJson.get("start"));
    Assert.assertEquals("2014-05-11T13:40:00Z", responseJson.get("end"));

    responseString = given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "person").formParam("blacklistedId", 7).log().all().expect()
            .statusCode(HttpStatus.SC_CREATED).when().post(rt.host("/")).asString();

    responseJson = (JSONObject) parser.parse(responseString);
    Assert.assertNotNull(responseJson);
    id = Long.parseLong(responseJson.get("id").toString());
    Assert.assertEquals(27L, id);
    Assert.assertEquals("2014-05-11T13:35:20Z", responseJson.get("start"));
    Assert.assertEquals("2014-05-11T13:40:00Z", responseJson.get("end"));
  }

  @Test
  public void testPostBlacklists() throws ParseException, IOException {
    String path = "/blacklists";
    // Empty parameters
    given().log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host(path));
    // Bad start
    given().formParam("start", "123").log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host(path));
    // Bad end
    given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "123").log().all().expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host(path));
    // Bad type
    given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "test").log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host(path));
    // Just a number, not a JSON array
    given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "person").formParam("blacklistedIds", 0).log().all().expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host(path));
    // A JSON Object instead of a JSON Array
    given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "person").formParam("blacklistedIds", "{'1' : '1'}").log().all().expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host(path));
    // Start time after end time results in bad request
    given().formParam("start", "2016-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "person").formParam("blacklistedIds", "[0, 5]").log().all().expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host(path)).asString();

    String responseString = given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "person").formParam("blacklistedIds", "[0, 5]").log().all().expect()
            .statusCode(HttpStatus.SC_OK).when().post(rt.host(path)).asString();

    System.out.println(responseString);

    JSONObject responseJson = (JSONObject) parser.parse(responseString);
    Assert.assertNotNull(responseJson);
    JSONArray ok = (JSONArray) responseJson.get("ok");
    Assert.assertEquals(1, ok.size());
    Assert.assertEquals("5", ok.get(0));

    JSONArray notFound = (JSONArray) responseJson.get("notFound");
    Assert.assertEquals(1, notFound.size());
    Assert.assertEquals("0", notFound.get(0));

    responseString = given().formParam("start", "2014-05-11T13:35:20Z").formParam("end", "2014-05-11T13:40:00Z")
            .formParam("type", "person").formParam("blacklistedIds", "[0,7]").log().all().expect()
            .statusCode(HttpStatus.SC_OK).when().post(rt.host(path)).asString();

    responseJson = (JSONObject) parser.parse(responseString);
    Assert.assertNotNull(responseJson);

    ok = (JSONArray) responseJson.get("ok");
    Assert.assertEquals(1, ok.size());
    Assert.assertEquals("7", ok.get(0));

    notFound = (JSONArray) responseJson.get("notFound");
    Assert.assertEquals(1, notFound.size());
    Assert.assertEquals("0", notFound.get(0));
  }

  @Test
  public void testPutPeriod() throws ParseException {
    given().pathParam("periodId", 0).log().all().expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .put(rt.host("/{periodId}"));

    String start = "2000-05-11T13:35:20Z";
    String end = "2014-05-11T13:35:20Z";
    String purpose = "Under construction";
    String comment = "A comment about the current period.";

    // Make sure that you cannot update a period with a later start time than the end time.
    given().pathParam("periodId", 1).formParam("start", end).formParam("end", start).formParam("purpose", purpose)
            .formParam("comment", comment).log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .put(rt.host("/{periodId}")).asString();

    String periodResponse = given().pathParam("periodId", 1).formParam("start", start).formParam("end", end)
            .formParam("purpose", purpose).formParam("comment", comment).log().all().expect()
            .statusCode(HttpStatus.SC_OK).when().put(rt.host("/{periodId}")).asString();
    JSONObject period = (JSONObject) parser.parse(periodResponse);
    long id = (Long) period.get("id");
    Assert.assertEquals(1L, id);
    Assert.assertEquals(start, period.get("start"));
    Assert.assertEquals(end, period.get("end"));
    Assert.assertEquals(purpose, period.get("purpose"));
    Assert.assertEquals(comment, period.get("comment"));
  }

  @Test
  public void testDeletePeriod() throws ParseException {
    given().pathParam("periodId", 0).log().all().expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .delete(rt.host("/{periodId}"));
    given().pathParam("periodId", 1).log().all().expect().statusCode(HttpStatus.SC_NO_CONTENT).when()
            .delete(rt.host("/{periodId}")).asString();
  }

  @BeforeClass
  public static void oneTimeSetUp() {
    rt.setUpServer();
    parser = new JSONParser();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    rt.tearDownServer();
  }

}
