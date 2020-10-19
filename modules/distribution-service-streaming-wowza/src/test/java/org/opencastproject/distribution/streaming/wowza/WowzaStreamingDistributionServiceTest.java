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
package org.opencastproject.distribution.streaming.wowza;

import static java.lang.String.format;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class WowzaStreamingDistributionServiceTest {

  private static WowzaStreamingDistributionService streamingService = null;
  private static ServiceRegistry serviceRegistry = null;
  private static BundleContext bundleContext = null;
  private static SecurityService securityService = null;
  private static OrganizationDirectoryService orgDirectoryService = null;
  private static UserDirectoryService userDirectoryService = null;

  private static final String defaultTenant = "mh_default_org";
  private static final String defaultUrlProperty = format(WowzaStreamingDistributionService.WOWZA_URL_KEY, defaultTenant);
  private static final String defaultPortProperty = format(WowzaStreamingDistributionService.WOWZA_PORT_KEY, defaultTenant);

  private static final String tenant1 = "tenant_1";
  private static final String tenant2 = "tenant_2";
  private static final String tenant3 = "tenant_3";

  private static JaxbOrganization org1 = null;
  private static JaxbOrganization org2 = null;
  private static JaxbOrganization org3 = null;

  private static String property1 = format(WowzaStreamingDistributionService.WOWZA_URL_KEY, tenant1);
  private static String property2 = format(WowzaStreamingDistributionService.WOWZA_URL_KEY, tenant2);

  private static String tenant1Url = "http://example.com/path";
  private static String tenant2Url = "https://example.com/path2";
  private static Map map = null;

  private static final String streamingURL = "http://example.com/path";
  private static final String port = "10";
  private static final String completeStreamingUrl = "http://example.com:10/path";

  @Before
  public void before() throws Exception {
    map = new HashMap();

    bundleContext = createNiceMock(BundleContext.class);
    expect(bundleContext.getProperty(WowzaStreamingDistributionService.STREAMING_DIRECTORY_KEY)).andReturn("/")
            .anyTimes();
    replay(bundleContext);

    streamingService = new WowzaStreamingDistributionService();

//    serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
//    List<HostRegistration> hosts = new ArrayList<>();
//    hosts.add(new JaxbHostRegistration("host1", "1.1.1.1", "node1", 400000, 8, 8, true, false));
//    expect(serviceRegistry.getHostRegistrations()).andReturn(hosts).anyTimes();
//    replay(serviceRegistry);

    User anonymous = new JaxbUser("anonymous", "test", new DefaultOrganization(),
            new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, new DefaultOrganization()));

    userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);
    streamingService.setUserDirectoryService(userDirectoryService);

    securityService = EasyMock.createNiceMock(SecurityService.class);
    expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    streamingService.setSecurityService(securityService);

    orgDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    streamingService.setOrganizationDirectoryService(orgDirectoryService);

    StatusLine statusLine = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(statusLine.getStatusCode()).andReturn(HttpServletResponse.SC_OK).anyTimes();
    EasyMock.replay(statusLine);

    HttpResponse response = EasyMock.createNiceMock(HttpResponse.class);
    EasyMock.expect(response.getStatusLine()).andReturn(statusLine).anyTimes();
    EasyMock.replay(response);

    final TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    EasyMock.expect(httpClient.execute((HttpUriRequest) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);
    streamingService.setTrustedHttpClient(httpClient);

    final File mediaPackageRoot = new File(getClass().getResource("/mediapackage.xml").toURI()).getParentFile();

    final Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        final URI uri = (URI) EasyMock.getCurrentArguments()[0];
        final String[] pathElems = uri.getPath().split("/");
        final String file = pathElems[pathElems.length - 1];
        return new File(mediaPackageRoot, file);
      }
    }).anyTimes();
    EasyMock.replay(workspace);
    streamingService.setWorkspace(workspace);

    serviceRegistry = new ServiceRegistryInMemoryImpl(streamingService, securityService, userDirectoryService,
            orgDirectoryService, EasyMock.createNiceMock(IncidentService.class));
    streamingService.setServiceRegistry(serviceRegistry);
  }

  public void setUpDefault() {
    List<Organization> orgList = new ArrayList<>();
    orgList.add(new DefaultOrganization());

    expect(orgDirectoryService.getOrganizations()).andReturn(orgList).anyTimes();
    EasyMock.replay(orgDirectoryService);
  }

  public void setUpMultiTenant() throws Exception {

    // testing multiple tenants
    List<Organization> orgListMultiple = new ArrayList();
    org1 = EasyMock.createMock(JaxbOrganization.class);
    orgListMultiple.add(org1);
    expect(org1.getId()).andReturn(tenant1).anyTimes();
    replay(org1);

    org2 = EasyMock.createMock(JaxbOrganization.class);
    orgListMultiple.add(org2);
    expect(org2.getId()).andReturn(tenant2).anyTimes();
    replay(org2);

    org3 = EasyMock.createMock(JaxbOrganization.class);
    orgListMultiple.add(org3);
    expect(org3.getId()).andReturn(tenant3).anyTimes();
    replay(org3);

    expect(orgDirectoryService.getOrganizations()).andReturn(orgListMultiple).anyTimes();
    expect(orgDirectoryService.getOrganization(tenant1)).andReturn(org1).anyTimes();
    EasyMock.replay(orgDirectoryService);

    map.put(property1, tenant1Url);
    map.put(property2, tenant2Url);
  }

  @Test
  public void testInitialization() throws Exception {
    setUpDefault();

    map.put(defaultUrlProperty, streamingURL);
    map.put(defaultPortProperty, port);
    streamingService.activate(bundleContext, map);

    assertTrue(streamingService.streamingUrls.containsKey(defaultTenant));
    assertEquals(1, streamingService.streamingUrls.size());
    assertEquals(completeStreamingUrl, streamingService.streamingUrls.get(defaultTenant).toString());
  }

  @Test
  public void testNoPort() throws Exception {
    setUpDefault();

    map.put(defaultUrlProperty, streamingURL);
    streamingService.activate(bundleContext, map);

    assertTrue(streamingService.streamingUrls.containsKey(defaultTenant));
    assertEquals(1, streamingService.streamingUrls.size());
    assertEquals(streamingURL, streamingService.streamingUrls.get(defaultTenant).toString());
  }

  @Test
  public void testNoStreamingURL() throws Exception {
    setUpDefault();

    streamingService.activate(bundleContext, new HashMap());

    assertFalse(streamingService.streamingUrls.containsKey(defaultTenant));
    assertEquals(0, streamingService.streamingUrls.size());
  }

  @Test
  public void testIncorrectStreamingURLs() throws Exception {
    setUpDefault();

    final String[] inputStreamingUrls = new String[] {
            "incorrect url",
            "noschema.myserver.com/my/path/to/server",
            "http://withhttp.example.com/path",
            "https://withhttps.testing.com/this/is/a/path",
            "rtmp://withrtmp.test.ext/another/path",
            "rtmps://withrtmps.anothertest.test/path/to/server",
            "other://withotherschema.test/mypath"
    };

    final String[] outputStreamingUrls = new String[] {
            null,
            "http://noschema.myserver.com:10/my/path/to/server",
            "http://withhttp.example.com:10/path",
            "https://withhttps.testing.com:10/this/is/a/path",
            null,
            null,
            null
    };

    map.put(defaultPortProperty, port);
    streamingService.activate(bundleContext, map);

    // Test service instance
    for (int i = 0; i < inputStreamingUrls.length; i++) {

      map.put(defaultUrlProperty, inputStreamingUrls[i]);

      if (outputStreamingUrls[i] == null) {
        try {
          streamingService.modified(bundleContext, map);
          fail("Should fail on invalid streaming URL");
        } catch (ComponentException e) {
          // expected
        }
      } else {
        streamingService.activate(bundleContext, map);
        assertEquals(new URI(outputStreamingUrls[i]), streamingService.streamingUrls.get(defaultTenant));
      }
    }
  }

  @Test
  public void testMultipleTenants() throws Exception {
    setUpMultiTenant();

    streamingService.activate(bundleContext, map);
    assertEquals(2, streamingService.streamingUrls.size());

    assertTrue(streamingService.streamingUrls.containsKey(tenant1));
    assertTrue(streamingService.streamingUrls.containsKey(tenant2));
    assertFalse(streamingService.streamingUrls.containsKey(tenant3));

    assertEquals(tenant1Url, streamingService.streamingUrls.get(tenant1).toString());
    assertEquals(tenant2Url, streamingService.streamingUrls.get(tenant2).toString());
  }

  @Test
  public void testPublishToStreaming() throws Exception {
    setUpMultiTenant();

    expect(securityService.getOrganization()).andReturn(org1).once();
    expect(securityService.getOrganization()).andReturn(org2).once();
    expect(securityService.getOrganization()).andReturn(org3).once();
    replay(securityService);

    streamingService.activate(bundleContext, map);
    assertTrue(streamingService.publishToStreaming());
    assertTrue(streamingService.publishToStreaming());
    assertFalse(streamingService.publishToStreaming());
  }

  @Test
  public void testDistribute() throws Exception {
    setUpMultiTenant();

    expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    replay(securityService);

    streamingService.activate(bundleContext, map);

    MediaPackage mp = MediaPackageParser.getFromXml(
            IOUtils.toString(getClass().getResourceAsStream("/mediapackage.xml"), "UTF-8"));

    Job job = streamingService.distribute("channel", mp, "blah");
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 500, job);
    jobBarrier.waitForJobs();
  }
}
