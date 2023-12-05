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

package org.opencastproject.kernel.bundleinfo;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.kernel.bundleinfo.BundleInfoImpl.bundleInfo;
import static org.opencastproject.kernel.bundleinfo.BundleInfoRestEndpoint.bundleInfoJson;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;
import static org.opencastproject.util.ReflectionUtil.run;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBTestEnv;
import org.opencastproject.test.rest.RestServiceTestEnv;
import org.opencastproject.util.data.Option;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.path.json.JsonPath;

/**
 * Note that the Jersey implementation serializes number values as strings in JSON so the respective tests test for both
 * the number and the string.
 */
public class BundleInfoRestEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(TestBundleInfoRestEndpoint.class);

  private static DBSession db;

  // shared with the test endpoint implementation
  static final BundleInfoDb bundleInfo = new AbstractBundleInfoDb() {
    @Override
    protected DBSession getDBSession() {
      return db;
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
    db = DBTestEnv.newDBSession(OsgiBundleInfoDb.PERSISTENCE_UNIT);
  }

  @After
  public void after() {
    db.close();
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
    expect().body("count", equalTo(0)).when().get(rt.host("/bundles/list"));
  }

  @Test
  public void testBundlesNonEmptyResponse1() {
    bundleInfo.store(bundleInfo("localhost", "bundle-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "bundle-2", 2L, "1.4.0", some("5e34af")));
    expect()
            // number is expected but jersey returns a string
            .body("count", equalTo(2)).body("bundleInfos[0].bundleId", equalTo(1))
            .body("bundleInfos[1].bundleSymbolicName", equalTo("bundle-2")).when().get(rt.host("/bundles/list"));
  }

  @Test
  public void testBundlesNonEmptyResponse2() {
    bundleInfo.store(bundleInfo("localhost", "bundle-2", 2L, "1.4.0", none("")));
    expect().body("count", equalTo(1)).body("bundleInfos[0].buildNumber", equalTo(null)).when()
            .get(rt.host("/bundles/list"));
  }

  @Test
  public void testBundlesCheck1() {
    // no opencast bundles
    bundleInfo.store(bundleInfo("localhost", "bundle-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "bundle-2", 2L, "1.4.0", some("5e34af")));
    // default bundle name prefix is "opencast"
    expect().statusCode(404).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck2() {
    // all opencast bundles have the same version
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("otherhost", "opencast-2", 2L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.1", some("5e34af")));
    expect().body(equalTo("true")).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck3() {
    // not all opencast bundles have the same version
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("otherhost", "opencast-2", 2L, "1.4.0", some("5e0000")));
    bundleInfo.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.0", some("5e34af")));
    expect().body(equalTo("false")).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck4() {
    // not all opencast bundles have the same version
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("otherhost", "opencast-2", 2L, "1.4.1", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.0", some("5e34af")));
    expect().body(equalTo("false")).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck5() {
    // not all opencast bundles have the same version
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("otherhost", "opencast-2", 2L, "1.4.0", none("")));
    bundleInfo.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.0", some("5e34af")));
    expect().body(equalTo("false")).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck6() {
    // all opencast bundles have the same version
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("otherhost", "opencast-2", 2L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.0", some("5e34af")));
    given().param("prefix", "opencast", "bundle").expect().body(equalTo("true")).when()
            .get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck7() {
    // not all opencast bundles have the same version
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("otherhost", "opencast-2", 2L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.1", some("5e34af")));
    given().param("prefix", "opencast", "bundle").expect().body(equalTo("false")).when()
            .get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundlesCheck8() {
    // not all opencast bundles have the same version
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("otherhost", "opencast-2", 2L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "bundle-1", 2L, "1.4.1", some("5e34af")));
    given().param("prefix", "bla", "blubb").expect().statusCode(404).when().get(rt.host("/bundles/check"));
  }

  @Test
  public void testBundleVersionsConsistent1() {
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "opencast-2", 2L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "opencast-3", 3L, "1.4.0", some("5e34af")));
    expect().body("consistent", equalTo(true)).body("version", equalTo("1.4.0"))
            .body("buildNumber", equalTo("5e34af")).when().get(rt.host("/bundles/version"));
  }

  @Test
  public void testBundleVersionsInconsistent1() {
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "opencast-2", 2L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "opencast-3", 3L, "1.4.1", some("5e34af")));
    expect().body("consistent", equalTo(false)).body("", not(hasKey("version")))
            .body("", not(hasKey("buildNumber"))).body("versions.buildNumber", hasItems("5e34af"))
            .body("versions.version", hasItems("1.4.0", "1.4.1")).when().get(rt.host("/bundles/version"));
  }

  @Test
  public void testBundleVersionsInconsistent2() {
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "opencast-2", 2L, "1.4.0", some("5e34a")));
    bundleInfo.store(bundleInfo("localhost", "opencast-3", 3L, "1.4.0", some("5e34af")));
    expect().body("consistent", equalTo(false)).body("", not(hasKey("version")))
            .body("", not(hasKey("buildNumber"))).body("versions.buildNumber", hasItems("5e34af", "5e34a"))
            .body("versions.version", hasItems("1.4.0")).when().get(rt.host("/bundles/version"));
  }

  @Test
  public void testBundleVersionsInconsistent3() {
    bundleInfo.store(bundleInfo("localhost", "opencast-1", 1L, "1.4.0", some("5e34af")));
    bundleInfo.store(bundleInfo("localhost", "opencast-2", 2L, "1.4.0", none("")));
    bundleInfo.store(bundleInfo("localhost", "opencast-3", 3L, "1.4.0", some("5e34af")));
    expect().body("consistent", equalTo(false)).body("", not(hasKey("version")))
            .body("", not(hasKey("buildNumber"))).body("versions.buildNumber", iterableWithSize(2))
            .body("versions.buildNumber", hasItems(null, "5e34af")).body("versions.version", hasItems("1.4.0"))
            .body("versions.buildNumber", hasItems("5e34af")).when().get(rt.host("/bundles/version"));
  }

}
