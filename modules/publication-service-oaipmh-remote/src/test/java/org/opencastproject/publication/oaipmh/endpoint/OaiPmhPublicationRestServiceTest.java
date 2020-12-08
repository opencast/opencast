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
package org.opencastproject.publication.oaipmh.endpoint;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;
import static org.opencastproject.util.UrlSupport.uri;

import org.opencastproject.job.api.Job;
import org.opencastproject.kernel.security.TrustedHttpClientImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.publication.oaipmh.remote.OaiPmhPublicationServiceRemoteImpl;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.test.rest.RestServiceTestEnv;

import com.entwinemedia.fn.data.ListBuilders;

import org.apache.http.client.methods.HttpRequestBase;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.HashSet;

/**
 * These tests are tightly coupled to {@link TestOaiPmhPublicationRestService}.
 */
public class OaiPmhPublicationRestServiceTest {
  public static final String CREATOR = "Tshiyoyo, Dieudonné";
  public static final URI JOB_URI = uri("http://localhost/job");


  @Test
  public void testPublishBrokenMediaPackage() throws Exception {
    // this should yield an error
    given().formParam("mediapackage", "bla").expect().statusCode(500).when().post(host("/"));
  }

  @Test
  public void testPublish() throws Exception {
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.addCreator(CREATOR);
    final String mpXml = MediaPackageParser.getAsXml(mp);
    given().formParam("mediapackage", mpXml).expect().statusCode(200).when().post(host("/"));
  }

  @Test
  public void testPublishUsingRemoteService() throws Exception {
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.addCreator(CREATOR);
    //
    final ServiceRegistry registry = EasyMock.createNiceMock(ServiceRegistry.class);
    final ServiceRegistration registration = EasyMock.createNiceMock(ServiceRegistration.class);
    EasyMock.expect(registration.getHost()).andReturn(url.getProtocol() + "://" + url.getHost() + ":" + url.getPort()).anyTimes();
    EasyMock.expect(registration.getPath()).andReturn(url.getPath()).anyTimes();
    EasyMock.expect(registry.getServiceRegistrationsByLoad(EasyMock.anyString())).andReturn(ListBuilders.SIA.mk(registration)).anyTimes();
    EasyMock.replay(registry, registration);
    final OaiPmhPublicationServiceRemoteImpl remote = new OaiPmhPublicationServiceRemoteImpl();
    remote.setTrustedHttpClient(new TestHttpClient());
    remote.setRemoteServiceManager(registry);
    //
    final Job job = remote.publish(mp, "mmp", new HashSet<String>(), new HashSet<String>(), false);
    assertEquals(job.getUri(), JOB_URI);
  }

  //
  // setup
  //

  private static final class TestHttpClient extends TrustedHttpClientImpl {
    TestHttpClient() {
      super("user", "pass");
      setSecurityService(EasyMock.createNiceMock(SecurityService.class));
    }

    /**
     * Override method with a no-op. In a test environment where the Opencast servlet filter chain is not in place
     * this will result in an inadvertent call to the REST endpoint which will most likely cause exceptions.
     */
    @Override protected String[] getRealmAndNonce(HttpRequestBase request) throws TrustedHttpClientException {
      return null;
    }
  }

  private static final URL url = localhostRandomPort();
  private static final RestServiceTestEnv rt = testEnvForClasses(url, TestOaiPmhPublicationRestService.class);

  // Great. Checkstyle: "This method should no be static". JUnit: "Method setUp() should be static." ;)
  // CHECKSTYLE:OFF
  @BeforeClass
  public static void setUp() throws Exception {
    rt.setUpServer();
  }

  @AfterClass
  public static void tearDownAfterClass() {
    rt.tearDownServer();
  }

  // CHECKSTYLE:ON

  // shortcut to testEnv.host
  public static String host(String path) {
    return rt.host(path);
  }
}
