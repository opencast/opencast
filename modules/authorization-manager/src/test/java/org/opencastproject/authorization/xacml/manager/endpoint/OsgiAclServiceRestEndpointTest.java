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

package org.opencastproject.authorization.xacml.manager.endpoint;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForCustomConfig;

import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.test.rest.RestServiceTestEnv;
import org.opencastproject.util.UrlSupport;

import org.glassfish.jersey.server.ResourceConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class OsgiAclServiceRestEndpointTest {

  private static final String NEW_ROLE = "NEW_ROLE";
  private static final String SERIES_10_INSTRUCTOR_ROLE = "SERIES_10_INSTRUCTOR";

  private static final int OK = Response.Status.OK.getStatusCode();
  private static final int NO_CONTENT = Response.Status.NO_CONTENT.getStatusCode();
  private static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
  private static final int CONFLICT = Response.Status.CONFLICT.getStatusCode();
  private static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();

  private static Long privateAclId;
  private static Long publicAclId;
  private String publicAcl
      = "{\"acl\": {\"ace\": {\"allow\":true, \"action\":\"read\", \"role\":\"SERIES_10_INSTRUCTOR\" }}}";
  private String privateAcl
      = "{\"acl\": {\"ace\": {\"allow\":false, \"action\":\"read\", \"role\":\"SERIES_10_INSTRUCTOR\" }}}";

  @Before
  public void setUpTest() throws Exception {
    publicAclId = extractAclId(given().formParam("name", "Public").formParam("acl", publicAcl).expect().statusCode(OK)
        .when().post(host("/acl")));

    privateAclId = extractAclId(given().formParam("name", "Private").formParam("acl", privateAcl).expect()
        .statusCode(OK).when().post(host("/acl")));
  }

  @After
  public void tearDownTest() {
    given().pathParam("aclId", publicAclId).when().delete(host("/acl/{aclId}"));
    given().pathParam("aclId", privateAclId).when().delete(host("/acl/{aclId}"));
  }

  @Test
  public void testAclExtendInputDifferentRoleExpectsAdded() {
    given()
        .formParam("acl", publicAcl)
        .formParam("action", Action.READ.toString())
        .formParam("role", NEW_ROLE)
        .formParam("allow", true)

        .expect()
        .body("ace[0].role", equalTo(SERIES_10_INSTRUCTOR_ROLE))
        .body("ace[0].action", equalTo(Action.READ.toString()))
        .body("ace[0].allow", equalTo(true))

        .body("ace[1].role", equalTo(NEW_ROLE))
        .body("ace[1].action", equalTo(Action.READ.toString()))
        .body("ace[1].allow", equalTo(true))

        .statusCode(OK).when().post(host("/acl/extend"));
  }

  @Test
  public void testAclExtendInputDifferentActionExpectsAdded() {
    given()
        .formParam("acl", publicAcl)
        .formParam("action", Action.WRITE.toString())
        .formParam("role", SERIES_10_INSTRUCTOR_ROLE)
        .formParam("allow", true)

        .expect()
        .body("ace[0].role", equalTo(SERIES_10_INSTRUCTOR_ROLE))
        .body("ace[0].action", equalTo(Action.READ.toString()))
        .body("ace[0].allow", equalTo(true))

        .body("ace[1].role", equalTo(SERIES_10_INSTRUCTOR_ROLE))
        .body("ace[1].action", equalTo(Action.WRITE.toString()))
        .body("ace[1].allow", equalTo(true))

        .statusCode(OK).when().post(host("/acl/extend"));
  }

  @Test
  public void testAclExtendInputDifferentAllowExpectsUpdated() {
    given()
        .formParam("acl", publicAcl)
        .formParam("action", Action.READ.toString())
        .formParam("role", SERIES_10_INSTRUCTOR_ROLE)
        .formParam("allow", false)
        .expect()
        .body("ace[0].role", equalTo(SERIES_10_INSTRUCTOR_ROLE))
        .body("ace[0].action", equalTo(Action.READ.toString()))
        .body("ace[0].allow", equalTo(false))
        .statusCode(OK).when().post(host("/acl/extend"));
  }

  @Test
  public void testAclExtendInputRoleAlreadyAddedExpectsSameAcl() {
    given()
        .formParam("acl", publicAcl)
        .formParam("action", Action.READ.toString())
        .formParam("role", SERIES_10_INSTRUCTOR_ROLE)
        .formParam("allow", true)
        .expect()
        .body("ace[0].role", equalTo(SERIES_10_INSTRUCTOR_ROLE))
        .body("ace[0].action", equalTo(Action.READ.toString()))
        .body("ace[0].allow", equalTo(true))
        .statusCode(OK).when().post(host("/acl/extend"));
  }

  @Test
  public void testAclExtendInputEmptyAclExpectsBadRequest() {
    given().formParam("acl", "").formParam("action", "write").formParam("role", NEW_ROLE).expect()
        .statusCode(BAD_REQUEST).when().post(host("/acl/extend"));
  }

  @Test
  public void testAclExtendInputEmptyRoleExpectsBadRequest() {
    given().formParam("acl", publicAcl).formParam("action", "write").formParam("role", "").expect()
        .statusCode(BAD_REQUEST).when().post(host("/acl/extend"));
  }

  @Test
  public void testAclExtendInputEmptyActionExpectsBadRequest() {
    given().formParam("acl", publicAcl).formParam("action", "").formParam("role", NEW_ROLE).expect()
        .statusCode(BAD_REQUEST).when().post(host("/acl/extend"));
  }

  @Test
  public void testAclReduceInputExistingAceExpectsAceGone() {
    given().formParam("acl", publicAcl).formParam("action", Action.READ.toString())
        .formParam("role", SERIES_10_INSTRUCTOR_ROLE).expect().body(containsString("{\"ace\":[]}")).statusCode(OK)
        .when().post(host("/acl/reduce"));
  }

  @Test
  public void testAclReduceInputWrongRoleExpectsSameAcl() {
    given().formParam("acl", publicAcl).formParam("action", Action.READ.toString()).formParam("role", NEW_ROLE)
        .expect()
        .body("ace[0].role", equalTo(SERIES_10_INSTRUCTOR_ROLE))
        .body("ace[0].action", equalTo(Action.READ.toString()))
        .body("ace[0].allow", equalTo(true))
        .statusCode(OK).when().post(host("/acl/reduce"));
  }

  @Test
  public void testAclReduceInputWrongActionExpectsSameAcl() {
    given()
        .formParam("acl", publicAcl)
        .formParam("action", Action.WRITE.toString())
        .formParam("role", SERIES_10_INSTRUCTOR_ROLE)
        .expect()
        .body("ace[0].role", equalTo(SERIES_10_INSTRUCTOR_ROLE))
        .body("ace[0].action", equalTo(Action.READ.toString()))
        .body("ace[0].allow", equalTo(true))
        .statusCode(OK).when().post(host("/acl/reduce"));
  }

  @Test
  public void testAclReduceInputEmptyAclExpectsBadRequest() {
    given().formParam("acl", "").formParam("action", "write").formParam("role", NEW_ROLE).expect()
        .statusCode(BAD_REQUEST).when().post(host("/acl/reduce"));
  }

  @Test
  public void testAclReduceInputEmptyRoleExpectsBadRequest() {
    given().formParam("acl", publicAcl).formParam("action", "write").formParam("role", "").expect()
        .statusCode(BAD_REQUEST).when().post(host("/acl/reduce"));
  }

  @Test
  public void testAclReduceInputEmptyActionExpectsBadRequest() {
    given().formParam("acl", publicAcl).formParam("action", "").formParam("role", NEW_ROLE).expect()
        .statusCode(BAD_REQUEST).when().post(host("/acl/reduce"));
  }

  @Test
  public void testAclEditor() throws Exception {
    String publicAclWrite
        = "{\"acl\": {\"ace\": {\"allow\":true, \"action\":\"write\", \"role\":\"SERIES_10_INSTRUCTOR\" }}}";
    String publicAclWrite2
        = "{\"acl\": {\"ace\": {\"allow\":false, \"action\":\"write\", \"role\":\"SERIES_10_INSTRUCTOR\" }}}";

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
          new ResourceConfig(TestRestService.class, NotFoundExceptionMapper.class));

  @BeforeClass
  public static void oneTimeSetUp() {
    env.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    env.tearDownServer();
  }

  public static String host(String path) {
    return env.host(UrlSupport.concat("test", path));
  }

  public static Long extractAclId(io.restassured.response.Response r) throws Exception {
    JSONObject json = (JSONObject) new JSONParser().parse(r.asString());
    return (Long) json.get("id");
  }

}
