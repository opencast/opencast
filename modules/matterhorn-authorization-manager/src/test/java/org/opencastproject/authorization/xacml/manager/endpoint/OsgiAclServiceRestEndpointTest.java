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
package org.opencastproject.authorization.xacml.manager.endpoint;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForCustomConfig;

import org.opencastproject.rest.RestServiceTestEnv;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.UrlSupport;

import com.sun.jersey.api.core.ClassNamesResourceConfig;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;

//import static com.jayway.restassured.RestAssured.*;
//import static com.jayway.restassured.matcher.RestAssuredMatchers.*;

public class OsgiAclServiceRestEndpointTest {

  private static final int OK = Response.Status.OK.getStatusCode();
  private static final int NO_CONTENT = Response.Status.NO_CONTENT.getStatusCode();
  private static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
  private static final int CONFLICT = Response.Status.CONFLICT.getStatusCode();
  private static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
  private static final int INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

  private String acl;

  private static Long privateAclId;
  private static Long publicAclId;

  @Before
  public void setUpTest() throws Exception {
    String publicAcl = "{\"acl\": {\"ace\": {\"allow\":true, \"action\":\"read\", \"role\":\"SERIES_10_INSTRUCTOR\" }}}";
    String privateAcl = "{\"acl\": {\"ace\": {\"allow\":false, \"action\":\"read\", \"role\":\"SERIES_10_INSTRUCTOR\" }}}";

    publicAclId = extractAclId(given().formParam("name", "Public").formParam("acl", publicAcl).expect().statusCode(OK)
            .when().post(host("/acl")));

    privateAclId = extractAclId(given().formParam("name", "Private").formParam("acl", privateAcl).expect()
            .statusCode(OK).when().post(host("/acl")));
  }

  @After
  public void tearDownTest() throws Exception {
    given().pathParam("aclId", publicAclId).when().delete(host("/acl/{aclId}"));
    given().pathParam("aclId", privateAclId).when().delete(host("/acl/{aclId}"));
  }

  @Test
  public void testSeries() throws Exception {
    final String applicationDate = DateTimeSupport.toUTC(new Date().getTime());

    // Store
    given().pathParam("seriesId", "SERIES_1").formParam("applicationDate", applicationDate)
            .formParam("managedAclId", "asdfasdf").expect().statusCode(BAD_REQUEST).when()
            .post(host("/series/{seriesId}"));

    given().pathParam("seriesId", "SERIES_1").formParam("applicationDate", "asdfasdfsadf")
            .formParam("managedAclId", publicAclId).expect().statusCode(INTERNAL_SERVER_ERROR).when()
            .post(host("/series/{seriesId}"));

    long transitionId = extractTransitionId(given().pathParam("seriesId", "SERIES_1")
            .formParam("applicationDate", applicationDate).formParam("managedAclId", publicAclId).expect()
            .statusCode(OK).body("applicationDate", equalTo(applicationDate)).body("seriesId", equalTo("SERIES_1"))
            .when().post(host("/series/{seriesId}")));

    given().pathParam("seriesId", "SERIES_1").formParam("applicationDate", applicationDate)
            .formParam("managedAclId", publicAclId).log().all().expect().statusCode(CONFLICT).when()
            .post(host("/series/{seriesId}"));

    given().expect().statusCode(OK).log().all()
            .body("series[\"SERIES_1\"].transitions[0].applicationDate", equalTo(applicationDate))
            .body("series[\"SERIES_1\"].transitions[0].seriesId", equalTo("SERIES_1")).when()
            .get(host("/transitions.json"));

    // Update
    String newApplicationDate = DateTimeSupport.toUTC(new Date().getTime() + 100000L);

    given().pathParam("transitionId", transitionId).formParam("applicationDate", applicationDate)
            .formParam("managedAclId", "aadfasdf").expect().statusCode(BAD_REQUEST).when()
            .put(host("/series/{transitionId}"));

    given().pathParam("transitionId", "asdfadsf").formParam("applicationDate", applicationDate)
            .formParam("managedAclId", acl).expect().statusCode(NOT_FOUND).when().put(host("/series/{transitionId}"));

    transitionId = extractTransitionId(given().pathParam("transitionId", transitionId)
            .formParam("applicationDate", newApplicationDate).formParam("managedAclId", publicAclId).expect()
            .statusCode(OK).body("applicationDate", equalTo(newApplicationDate)).body("seriesId", equalTo("SERIES_1"))
            .when().put(host("/series/{transitionId}")));

    given().expect().statusCode(OK)
            .body("series[\"SERIES_1\"].transitions[0].applicationDate", equalTo(newApplicationDate))
            .body("series[\"SERIES_1\"].transitions[0].seriesId", equalTo("SERIES_1")).when()
            .get(host("/transitions.json"));

    // Delete
    given().pathParam("transitionId", "asdfasdfk").expect().statusCode(NOT_FOUND).when()
            .delete(host("/series/{transitionId}"));

    given().pathParam("transitionId", transitionId).expect().statusCode(NO_CONTENT).when()
            .delete(host("/series/{transitionId}"));

    given().expect().log().all().statusCode(OK).body("series[\"SERIES_1\"]", nullValue()).when()
            .get(host("/transitions.json"));
  }

  @Test
  public void testEpisode() throws Exception {
    String applicationDate = DateTimeSupport.toUTC(new Date().getTime());

    String episodeId = "22d026a7-e311-4f4a-9241-111d5cda7d33";

    // Store
    given().pathParam("episodeId", episodeId).formParam("applicationDate", applicationDate)
            .formParam("managedAclId", "asdfasdf").expect().statusCode(BAD_REQUEST).when()
            .post(host("/episode/{episodeId}"));

    given().pathParam("episodeId", episodeId).formParam("applicationDate", "asdfasdfsadf")
            .formParam("managedAclId", publicAclId).expect().statusCode(INTERNAL_SERVER_ERROR).when()
            .post(host("/episode/{episodeId}"));

    long transitionId = extractTransitionId(given().pathParam("episodeId", episodeId)
            .formParam("applicationDate", applicationDate).formParam("managedAclId", publicAclId).expect()
            .statusCode(OK).body("applicationDate", equalTo(applicationDate)).body("episodeId", equalTo(episodeId))
            .when().post(host("/episode/{episodeId}")));

    given().pathParam("episodeId", episodeId).formParam("applicationDate", applicationDate)
            .formParam("managedAclId", publicAclId).expect().statusCode(CONFLICT).when()
            .post(host("/episode/{episodeId}"));

    given().expect()
            .statusCode(OK)
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].applicationDate",
                    equalTo(applicationDate))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].episodeId", equalTo(episodeId))
            .when().get(host("/transitions.json"));

    // Update
    String newApplicationDate = DateTimeSupport.toUTC(new Date().getTime() + 100000L);

    given().pathParam("transitionId", transitionId).formParam("applicationDate", newApplicationDate)
            .formParam("managedAclId", "aadfasdf").expect().statusCode(BAD_REQUEST).when()
            .put(host("/episode/{transitionId}"));

    given().pathParam("transitionId", "asdfadsf").formParam("applicationDate", newApplicationDate)
            .formParam("acl", acl).expect().statusCode(NOT_FOUND).when().put(host("/episode/{transitionId}"));

    transitionId = extractTransitionId(given().pathParam("transitionId", transitionId)
            .formParam("applicationDate", newApplicationDate).formParam("managedAclId", publicAclId).expect()
            .statusCode(OK).body("applicationDate", equalTo(newApplicationDate)).body("episodeId", equalTo(episodeId))
            .when().put(host("/episode/{transitionId}")));

    given().expect()
            .statusCode(OK)
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].applicationDate",
                    equalTo(newApplicationDate))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].episodeId", equalTo(episodeId))
            .when().get(host("/transitions.json"));

    // Delete
    given().pathParam("transitionId", "asdfasdfkd").expect().statusCode(NOT_FOUND).when()
            .delete(host("/episode/{transitionId}"));

    given().pathParam("transitionId", transitionId).expect().statusCode(NO_CONTENT).when()
            .delete(host("/episode/{transitionId}"));

    given().expect().statusCode(OK).body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"]", nullValue()).when()
            .get(host("/transitions.json"));
  }

  @Test
  public void testGetByQuery() throws Exception {
    String from = DateTimeSupport.toUTC(new Date().getTime());
    String to = DateTimeSupport.toUTC(new Date().getTime() + 50000L);

    // Test wrong scope
    given().queryParam("scope", "asdf").expect().statusCode(BAD_REQUEST).when().get(host("/transitions.json"));
    given().queryParam("scope", "EPISODE").expect().statusCode(OK).when().get(host("/transitions.json"));

    // Test wrong date
    given().queryParam("after", "asdfa").expect().statusCode(INTERNAL_SERVER_ERROR).when()
            .get(host("/transitions.json"));
    given().queryParam("before", "asdfasdf").expect().statusCode(INTERNAL_SERVER_ERROR).when()
            .get(host("/transitions.json"));

    // Test json
    given().expect().statusCode(OK).when().get(host("/transitions.json"));

    // Test all params
    given().queryParam("after", from).queryParam("before", to).queryParam("scope", "SERIES")
            .queryParam("id", "SERIES_1").queryParam("managedAclId", 323).queryParam("transitionId", 435)
            .queryParam("done", false).expect().statusCode(OK).body("series[\"SERIES_+\"]", nullValue()).when()
            .get(host("/transitions.json"));
  }

  @Test
  public void testGetTransitions() throws Exception {
    String episodeId = "22d026a7-e311-4f4a-9241-111d5cda7d33";

    String workflowParams = "{\"videoPreview\":\"true\",\"distribution\":\"Matterhorn Media Module\",\"archiveOp\":\"true\",\"trimHold\":\"true\"}";

    String applicationDate = DateTimeSupport.toUTC(new Date().getTime());
    String applicationDate2 = DateTimeSupport.toUTC(new Date().getTime() + 50000L);

    long seriesTransitionId = extractTransitionId(given().pathParam("seriesId", "SERIES_1")
            .formParam("applicationDate", applicationDate).formParam("managedAclId", publicAclId)
            .formParam("workflowDefinitionId", "full").formParam("workflowParams", workflowParams).expect()
            .statusCode(OK).body("applicationDate", equalTo(applicationDate)).body("seriesId", equalTo("SERIES_1"))
            .when().post(host("/series/{seriesId}")));

    long episodeTransitionId = extractTransitionId(given().pathParam("episodeId", episodeId)
            .formParam("applicationDate", applicationDate2).formParam("managedAclId", privateAclId)
            .formParam("workflowDefinitionId", "full").formParam("workflowParams", workflowParams).expect()
            .statusCode(OK).body("applicationDate", equalTo(applicationDate2)).body("episodeId", equalTo(episodeId))
            .when().post(host("/episode/{episodeId}")));

    // Test json
    given().expect()
            .statusCode(OK)
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].applicationDate",
                    equalTo(applicationDate2))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].transitionId",
                    equalTo((int) episodeTransitionId))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].done", equalTo(false))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].episodeId",
                    equalTo("22d026a7-e311-4f4a-9241-111d5cda7d33"))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].organizationId",
                    equalTo("mh_default_org"))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].workflowId", equalTo("full"))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].workflowParams",
                    equalTo(workflowParams))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].acl.id",
                    equalTo(privateAclId.intValue()))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].transitions[0].acl.name", equalTo("Private"))
            .body("episodes[\"22d026a7-e311-4f4a-9241-111d5cda7d33\"].activeAcl.unmanagedAcl", notNullValue())
            .body("series[\"SERIES_1\"].transitions[0].applicationDate", equalTo(applicationDate))
            .body("series[\"SERIES_1\"].transitions[0].transitionId", equalTo((int) seriesTransitionId))
            .body("series[\"SERIES_1\"].transitions[0].done", equalTo(false))
            .body("series[\"SERIES_1\"].transitions[0].seriesId", equalTo("SERIES_1"))
            .body("series[\"SERIES_1\"].transitions[0].organizationId", equalTo("mh_default_org"))
            .body("series[\"SERIES_1\"].transitions[0].override", equalTo(false))
            .body("series[\"SERIES_1\"].transitions[0].workflowId", equalTo("full"))
            .body("series[\"SERIES_1\"].transitions[0].workflowParams", equalTo(workflowParams))
            .body("series[\"SERIES_1\"].transitions[0].acl.id", equalTo(publicAclId.intValue()))
            .body("series[\"SERIES_1\"].transitions[0].acl.name", equalTo("Public"))
            .body("series[\"SERIES_1\"].activeAcl.unmanagedAcl", notNullValue()).when().get(host("/transitions.json"));

    given().pathParam("transitionId", episodeTransitionId).expect().statusCode(NO_CONTENT).when()
            .delete(host("/episode/{transitionId}"));

    given().pathParam("transitionId", seriesTransitionId).expect().statusCode(NO_CONTENT).when()
            .delete(host("/series/{transitionId}"));
  }

  @Test
  public void testGetTransitionsFor() throws Exception {
    String episodeId = "12dd16a7-e321-4f4a-9241-111d53457d33";

    String workflowParams = "{\"videoPreview\":\"true\",\"distribution\":\"Matterhorn Media Module\",\"archiveOp\":\"true\",\"trimHold\":\"true\"}";

    String applicationDate = DateTimeSupport.toUTC(new Date().getTime() + 50000L);
    String applicationDate2 = DateTimeSupport.toUTC(new Date().getTime() + 150000L);

    long seriesTransitionId = extractTransitionId(given().pathParam("seriesId", "SERIES_2")
            .formParam("applicationDate", applicationDate).formParam("managedAclId", privateAclId)
            .formParam("workflowDefinitionId", "full").formParam("workflowParams", workflowParams).expect()
            .statusCode(OK).body("applicationDate", equalTo(applicationDate)).body("seriesId", equalTo("SERIES_2"))
            .when().post(host("/series/{seriesId}")));

    long episodeTransitionId = extractTransitionId(given().pathParam("episodeId", episodeId)
            .formParam("applicationDate", applicationDate2).formParam("managedAclId", privateAclId)
            .formParam("workflowDefinitionId", "full").formParam("workflowParams", workflowParams).expect()
            .statusCode(OK).body("applicationDate", equalTo(applicationDate2)).body("episodeId", equalTo(episodeId))
            .when().post(host("/episode/{episodeId}")));

    // Test json
    given().queryParam("episodeIds", episodeId)
            .queryParam("seriesIds", "SERIES_2")
            .queryParam("done", false)
            .log()
            .all()
            .expect()
            .statusCode(OK)
            .log()
            .all()
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0].applicationDate",
                    equalTo(applicationDate2))
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0].transitionId",
                    equalTo((int) episodeTransitionId))
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0].done", equalTo(false))
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0].episodeId",
                    equalTo("12dd16a7-e321-4f4a-9241-111d53457d33"))
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0].organizationId",
                    equalTo("mh_default_org"))
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0].workflowId", equalTo("full"))
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0].workflowParams",
                    equalTo(workflowParams))
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0].acl.id",
                    equalTo(privateAclId.intValue()))
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0].acl.name", equalTo("Private"))
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].activeAcl.unmanagedAcl", notNullValue())
            .body("series[\"SERIES_2\"].transitions[0].applicationDate", equalTo(applicationDate))
            .body("series[\"SERIES_2\"].transitions[0].transitionId", equalTo((int) seriesTransitionId))
            .body("series[\"SERIES_2\"].transitions[0].done", equalTo(false))
            .body("series[\"SERIES_2\"].transitions[0].seriesId", equalTo("SERIES_2"))
            .body("series[\"SERIES_2\"].transitions[0].organizationId", equalTo("mh_default_org"))
            .body("series[\"SERIES_2\"].transitions[0].override", equalTo(false))
            .body("series[\"SERIES_2\"].transitions[0].workflowId", equalTo("full"))
            .body("series[\"SERIES_2\"].transitions[0].workflowParams", equalTo(workflowParams))
            .body("series[\"SERIES_2\"].transitions[0].acl.id", equalTo(privateAclId.intValue()))
            .body("series[\"SERIES_2\"].transitions[0].acl.name", equalTo("Private"))
            .body("series[\"SERIES_2\"].activeAcl.unmanagedAcl", notNullValue()).when()
            .get(host("/transitionsfor.json"));

    given().queryParam("episodeIds", episodeId).queryParam("seriesIds", "SERIES_2").queryParam("done", true).log()
            .all().expect().statusCode(OK).log().all()
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].transitions[0]", nullValue())
            .body("episodes[\"12dd16a7-e321-4f4a-9241-111d53457d33\"].activeAcl.unmanagedAcl", notNullValue())
            .body("series[\"SERIES_2\"].transitions[0]", nullValue())
            .body("series[\"SERIES_2\"].activeAcl.unmanagedAcl", notNullValue()).when()
            .get(host("/transitionsfor.json"));

    given().pathParam("transitionId", episodeTransitionId).expect().statusCode(NO_CONTENT).when()
            .delete(host("/episode/{transitionId}"));

    given().pathParam("transitionId", seriesTransitionId).expect().statusCode(NO_CONTENT).when()
            .delete(host("/series/{transitionId}"));
  }

  @Test
  public void testApplyEpisode() throws Exception {
    // Test with wrong aclId
    given().pathParams("episodeId", "episodeid").formParam("aclId", 34242).expect().statusCode(NOT_FOUND).when()
            .post(host("/apply/episode/{episodeId}"));

    // Test with wrong episode Id
    given().pathParams("episodeId", "episodeid").queryParam("aclId", publicAclId).expect().statusCode(OK).when()
            .post(host("/apply/episode/{episodeId}"));
  }

  @Test
  public void testApplySeries() throws Exception {
    // Test with wrong aclId
    given().pathParams("seriesId", "SERIES_1").formParam("aclId", 34242).expect().statusCode(NOT_FOUND).when()
            .post(host("/apply/series/{seriesId}"));
    // Test with wrong series id
    given().pathParams("seriesId", "asdfasdf").formParam("aclId", privateAclId).expect().statusCode(NOT_FOUND).when()
            .post(host("/apply/series/{seriesId}"));

    given().pathParams("seriesId", "SERIES_1").formParam("aclId", privateAclId).expect().statusCode(OK).when()
            .post(host("/apply/series/{seriesId}"));
  }

  @Test
  public void testAclEditor() throws Exception {
    String publicAclWrite = "{\"acl\": {\"ace\": {\"allow\":true, \"action\":\"write\", \"role\":\"SERIES_10_INSTRUCTOR\" }}}";
    String publicAclWrite2 = "{\"acl\": {\"ace\": {\"allow\":false, \"action\":\"write\", \"role\":\"SERIES_10_INSTRUCTOR\" }}}";

    // GET
    // Test with existing acl Id
    given().pathParams("aclId", publicAclId).expect().statusCode(OK).body("acl.ace[0].allow", equalTo(true))
            .body("acl.ace[0].action", equalTo("read")).body("acl.ace[0].role", equalTo("SERIES_10_INSTRUCTOR")).when()
            .get(host("/acl/{aclId}"));

    // Test with false acl Id
    given().pathParams("aclId", "reddfsdffsd").expect().statusCode(NOT_FOUND).when().get(host("/acl/{aclId}"));
    // Get all acls
    given().log().all().expect().statusCode(OK).log().all().body("[0].name", equalTo("Public"))
            .body("[0].acl.ace[0].action", equalTo("read")).body("[0].acl.ace[0].allow", equalTo(true))
            .body("[0].acl.ace[0].role", equalTo("SERIES_10_INSTRUCTOR")).body("[1].name", equalTo("Private"))
            .body("[1].acl.ace[0].action", equalTo("read")).body("[1].acl.ace[0].allow", equalTo(false))
            .body("[1].acl.ace[0].role", equalTo("SERIES_10_INSTRUCTOR")).when().get(host("/acl/acls.json"));

    // POST
    // With a valid ACL
    String aclName = "PublicWrite";
    Long publicAclWriteId = extractAclId(given().formParam("name", aclName).formParam("acl", publicAclWrite).expect()
            .body("name", equalTo(aclName)).body("acl.ace[0].action", equalTo("write"))
            .body("acl.ace[0].allow", equalTo(true)).body("acl.ace[0].role", equalTo("SERIES_10_INSTRUCTOR"))
            .statusCode(OK).when().post(host("/acl")));
    // Try to publish one with the same name
    given().formParam("name", aclName).formParam("acl", publicAclWrite).expect().statusCode(CONFLICT).when()
            .post(host("/acl"));
    // Post one with a wrong acl
    given().formParam("name", "Wrong").formParam("acl", "test").expect().statusCode(BAD_REQUEST).when()
            .post(host("/acl"));

    // PUT
    given().pathParam("aclId", publicAclWriteId).formParam("name", aclName).formParam("acl", publicAclWrite2).expect()
            .body("name", equalTo(aclName)).body("acl.ace[0].action", equalTo("write"))
            .body("acl.ace[0].allow", equalTo(false)).body("acl.ace[0].role", equalTo("SERIES_10_INSTRUCTOR"))
            .statusCode(OK).when().put(host("/acl/{aclId}"));
    given().pathParam("aclId", publicAclWriteId).formParam("name", aclName).formParam("acl", "test").expect()
            .statusCode(BAD_REQUEST).when().put(host("/acl/{aclId}"));
    given().pathParam("aclId", "wrong_id").formParam("name", aclName).formParam("acl", "test").expect()
            .statusCode(NOT_FOUND).when().put(host("/acl/{aclId}"));

    // DELETE
    given().pathParam("aclId", "wrong_id").expect().statusCode(NOT_FOUND).when().delete(host("/acl/{aclId}"));
    given().pathParam("aclId", publicAclWriteId).expect().statusCode(NO_CONTENT).when().delete(host("/acl/{aclId}"));
    given().pathParams("aclId", publicAclWriteId).expect().statusCode(NOT_FOUND).when().get(host("/acl/{aclId}"));
  }

  // --

  private static final RestServiceTestEnv env = testEnvForCustomConfig(TestRestService.BASE_URL,
          new ClassNamesResourceConfig(TestRestService.class, NotFoundExceptionMapper.class));

  // CHECKSTYLE:OFF
  @BeforeClass
  public static void setUp() throws Exception {
    env.setUpServer();
  }

  @AfterClass
  public static void tearDownAfterClass() {
    env.tearDownServer();
  }

  public static String host(String path) {
    return env.host(UrlSupport.concat("test", path));
  }

  // CHECKSTYLE:ON

  public static class RegexMatcher extends BaseMatcher<String> {
    private final Pattern p;

    public RegexMatcher(String pattern) {
      p = Pattern.compile(pattern);
    }

    public static RegexMatcher regex(String pattern) {
      return new RegexMatcher(pattern);
    }

    @Override
    public boolean matches(Object item) {
      if (item != null) {
        return p.matcher(item.toString()).matches();
      } else {
        return false;
      }
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("regex [" + p.pattern() + "]");
    }
  }

  public static Long extractTransitionId(com.jayway.restassured.response.Response r) throws Exception {
    JSONObject json = (JSONObject) new JSONParser().parse(r.asString());
    return (Long) json.get("transitionId");
  }

  public static Long extractAclId(com.jayway.restassured.response.Response r) throws Exception {
    JSONObject json = (JSONObject) new JSONParser().parse(r.asString());
    return (Long) json.get("id");
  }

}