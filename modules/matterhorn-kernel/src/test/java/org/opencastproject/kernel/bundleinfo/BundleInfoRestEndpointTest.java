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
package org.opencastproject.kernel.bundleinfo;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.kernel.bundleinfo.BundleInfoImpl.bundleInfo;
import static org.opencastproject.kernel.bundleinfo.BundleInfoRestEndpoint.bundleInfoJson;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;
import static org.opencastproject.util.ReflectionUtil.run;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestPersistenceEnv;

import org.opencastproject.rest.RestServiceTestEnv;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceEnv;

import com.jayway.restassured.path.json.JsonPath;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Note that the Jersey implementation serializes number values as strings in JSON so the respective tests test for both
 * the number and the string.
 */
public class BundleInfoRestEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(),
          TestBundleInfoRestEndpoint.class);

  private static PersistenceEnv penv;

  // shared with the test endpoint implementation
  static final BundleInfoDb db = new AbstractBundleInfoDb() {
    @Override
    protected PersistenceEnv getPersistenceEnv() {
      return penv;
    }
  };

  @BeforeClass
  public static void oneTimeSetUp() {
    rt.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    rt.tearDownServer();
  }

  @Before
  public void before() {
    penv = newTestPersistenceEnv(OsgiBundleInfoDb.PERSISTENCE_UNIT);
  }

  @After
  public void after() {
    penv.close();
  }

  @Test
  public void testBundleInfoJsonSerialization() {
    final JsonPath p = JsonPath.from(bundleInfoJson(bundleInfo("host", "bundle", 1L, "version", some("sha"))).toJson());
    run(BundleInfo.class, new BundleInfo() {
      @Override
      public String getHost() {
        assertEquals("host", p.getString("host"));
        return null;
      }

      @Override
      public String getBundleSymbolicName() {
        assertEquals("bundle", p.getString("bundleSymbolicName"));
        return null;
      }

      @Override
      public long getBundleId() {
        assertEquals(1L, p.getLong("bundleId"));
        return 0;
      }

      @Override
      public String getBundleVersion() {
        assertEquals("bundle", p.getString("bundleSymbolicName"));
        return null;
      }

      @Override
      public Option<String> getBuildNumber() {
        assertEquals("sha", p.getString("buildNumber"));
        return null;
      }

      @Override
      public BundleVersion getVersion() {
        assertEquals("sha", p.getString("buildNumber"));
        return null;
      }
    });
  }

  @Test
  public void testBundlesEmptyResponse() {
    expect().log().all().body("count", equalTo(0)).when().get(rt.host("/bundles/list"));
  }

  @Test
  public void testBundlesNonEmptyResponse1() {
    db.store(bundleInfo("localhost", "bundle-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "bundle-2", 2L, "1.4.0", some("5e34af")));
    expect().log().all()
            // number is expected but jersey returns a string
            .body("count", equalTo(2)).body("bundleInfos[0].bundleId", equalTo(1))
            .body("bundleInfos[1].bundleSymbolicName", equalTo("bundle-2")).when().get(rt.host("/bundles/list"));
  }

  @Test
  public void testBundlesNonEmptyResponse2() {
    db.store(bundleInfo("localhost", "bundle-2", 2L, "1.4.0", none("")));
    expect().log().all().body("count", equalTo(1)).body("bundleInfos[0].buildNumber", equalTo(null)).when()
            .get(rt.host("/bundles/list"));
  }

  @Test
  public void testBundlesCheck1() {
    // no matterhorn bundles
    db.store(bundleInfo("localhost", "bundle-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "bundle-2", 2L, "1.4.0", some("5e34af")));
    // default bundle name prefix is "matterhorn"
    expect().log().all().statusCode(404).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck2() {
    // all matterhorn bundles have the same version
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("otherhost", "matterhorn-2", 2L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.1", some("5e34af")));
    expect().log().all().body(equalTo("true")).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck3() {
    // not all matterhorn bundles have the same version
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("otherhost", "matterhorn-2", 2L, "1.4.0", some("5e0000")));
    db.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.0", some("5e34af")));
    expect().log().all().body(equalTo("false")).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck4() {
    // not all matterhorn bundles have the same version
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("otherhost", "matterhorn-2", 2L, "1.4.1", some("5e34af")));
    db.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.0", some("5e34af")));
    expect().log().all().body(equalTo("false")).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck5() {
    // not all matterhorn bundles have the same version
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("otherhost", "matterhorn-2", 2L, "1.4.0", none("")));
    db.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.0", some("5e34af")));
    expect().log().all().body(equalTo("false")).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck6() {
    // all matterhorn bundles have the same version
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("otherhost", "matterhorn-2", 2L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.0", some("5e34af")));
    given().param("prefix", "matterhorn", "bundle").expect().log().all().body(equalTo("true")).when()
            .get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck7() {
    // not all matterhorn bundles have the same version
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("otherhost", "matterhorn-2", 2L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.1", some("5e34af")));
    given().param("prefix", "matterhorn", "bundle").expect().log().all().body(equalTo("false")).when()
            .get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck8() {
    // not all matterhorn bundles have the same version
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("otherhost", "matterhorn-2", 2L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.1", some("5e34af")));
    given().param("prefix", "bla", "blubb").expect().log().all().statusCode(404).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundleVersionsConsistent1() {
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "matterhorn-2", 2L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "matterhorn-3", 3L, "1.4.0", some("5e34af")));
    expect().log().all().body("consistent", equalTo(true)).body("version", equalTo("1.4.0"))
            .body("buildNumber", equalTo("5e34af")).when().get(rt.host("/bundles/version"));
  }

  @Test
  public void testBundleVersionsInconsistent1() {
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "matterhorn-2", 2L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "matterhorn-3", 3L, "1.4.1", some("5e34af")));
    expect().log().all().body("consistent", equalTo(false)).body("", not(hasKey("version")))
            .body("", not(hasKey("buildNumber"))).body("versions.buildNumber", hasItems("5e34af"))
            .body("versions.version", hasItems("1.4.0", "1.4.1")).when().get(rt.host("/bundles/version"));
  }

  @Test
  public void testBundleVersionsInconsistent2() {
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "matterhorn-2", 2L, "1.4.0", some("5e34a")));
    db.store(bundleInfo("localhost", "matterhorn-3", 3L, "1.4.0", some("5e34af")));
    expect().log().all().body("consistent", equalTo(false)).body("", not(hasKey("version")))
            .body("", not(hasKey("buildNumber"))).body("versions.buildNumber", hasItems("5e34af", "5e34a"))
            .body("versions.version", hasItems("1.4.0")).when().get(rt.host("/bundles/version"));
  }

  @Test
  public void testBundleVersionsInconsistent3() {
    db.store(bundleInfo("localhost", "matterhorn-1", 1L, "1.4.0", some("5e34af")));
    db.store(bundleInfo("localhost", "matterhorn-2", 2L, "1.4.0", none("")));
    db.store(bundleInfo("localhost", "matterhorn-3", 3L, "1.4.0", some("5e34af")));
    expect().log().all().body("consistent", equalTo(false)).body("", not(hasKey("version")))
            .body("", not(hasKey("buildNumber"))).body("versions.buildNumber", iterableWithSize(2))
            .body("versions.buildNumber", hasItems(null, "5e34af")).body("versions.version", hasItems("1.4.0"))
            .body("versions.buildNumber", hasItems("5e34af")).when().get(rt.host("/bundles/version"));
  }

}
