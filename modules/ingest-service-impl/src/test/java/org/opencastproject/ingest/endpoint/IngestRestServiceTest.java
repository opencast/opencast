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

package org.opencastproject.ingest.endpoint;

import static org.junit.Assert.assertEquals;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.ingest.impl.IngestServiceImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;

import org.apache.commons.fileupload.MockHttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class IngestRestServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(IngestRestServiceTest.class);
  protected IngestRestService restService;
  private File testDir = null;
  private LimitVerifier limitVerifier;

  @Before
  public void setUp() throws Exception {
    testDir = new File("./target", "ingest-rest-service-test");
    if (testDir.exists()) {
      FileUtils.deleteQuietly(testDir);
      logger.info("Removing  " + testDir.getAbsolutePath());
    } else {
      logger.info("Didn't Delete " + testDir.getAbsolutePath());
    }
    testDir.mkdir();

    restService = new IngestRestService();

    // Create a mock ingest service
    IngestService ingestService = EasyMock.createNiceMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage())
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.createMediaPackage("1a6f70ab-4262-4523-9f8e-babce22a1ea8"))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                    .createNew(new UUIDIdBuilderImpl().fromString("1a6f70ab-4262-4523-9f8e-babce22a1ea8")));
    EasyMock.expect(ingestService.addAttachment((URI) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addCatalog((URI) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addTrack((URI) EasyMock.anyObject(), (MediaPackageElementFlavor) EasyMock.anyObject(),
            (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addTrack((URI) EasyMock.anyObject(), (MediaPackageElementFlavor) EasyMock.anyObject(),
            (String[]) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addAttachment((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addAttachment((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (String[]) EasyMock.anyObject(),
            (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addCatalog((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addCatalog((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (String[]) EasyMock.anyObject(),
            (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addTrack((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addTrack((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (String[]) EasyMock.anyObject(),
            (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addPartialTrack((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), EasyMock.anyLong(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.replay(ingestService);

    // Set the service, and activate the rest endpoint
    restService.setIngestService(ingestService);
    restService.activate(null);
  }

  @Test
  public void testNoIngestLimit() {
    setupAndTestLimit(null, -1, false);
  }

  @Test
  public void testIngestLimitOfNegativeOne() {
    setupAndTestLimit("-1", -1, false);
  }

  @Test
  public void testIngestLimitOfZero() {
    setupAndTestLimit("0", -1, false);
  }

  @Test
  public void testIngestLimitOfOne() {
    setupAndTestLimit("1", 1, true);
  }

  @Test
  public void testIngestLimitOfTen() {
    setupAndTestLimit("10", 10, true);
  }

  @Test
  public void testIngestLimitOfThousand() {
    setupAndTestLimit("1000", 1000, true);
  }

  @Test
  public void testInvalidLimitAddZippedMediaPackage() {
    setupAndTestLimit("This is not a number", -1, false);
  }

  public void setupAndTestLimit(String limit, int expectedLimit, boolean expectedEnabled) {
    restService = new IngestRestService();

    // Create a mock ingest service
    IngestService ingestService = EasyMock.createNiceMock(IngestService.class);
    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(cc);
    EasyMock.expect(bc.getProperty(IngestRestService.DEFAULT_WORKFLOW_DEFINITION)).andReturn("full").anyTimes();
    if (StringUtils.trimToNull(limit) != null) {
      EasyMock.expect(bc.getProperty(IngestRestService.MAX_INGESTS_KEY)).andReturn(limit).anyTimes();
    }
    EasyMock.replay(bc);
    restService.setIngestService(ingestService);
    restService.activate(cc);
    Assert.assertEquals(expectedLimit, restService.getIngestLimit());
    Assert.assertEquals(expectedEnabled, restService.isIngestLimitEnabled());
  }

  @Test
  public void testLimitOfOneToAddZippedMediaPackage() {
    setupAndTestIngestingLimit("1", 1, 1, 0);
  }

  @Test
  public void testLimitOfOneWithTwoIngestsToAddZippedMediaPackage() {
    setupAndTestIngestingLimit("1", 2, 1, 1);
  }

  @Test
  public void testLimitOfOneWithTenIngestsToAddZippedMediaPackage() {
    setupAndTestIngestingLimit("1", 10, 1, 9);
  }

  @Test
  public void testLimitOfTwoWithTenIngestsToAddZippedMediaPackage() {
    setupAndTestIngestingLimit("2", 10, 2, 8);
  }

  @Test
  public void testLimitOfFiveWithTenIngestsToAddZippedMediaPackage() {
    setupAndTestIngestingLimit("5", 10, 5, 5);
  }

  @Test
  public void testLimitOfTenWithOneHundredIngestsToAddZippedMediaPackage() {
    setupAndTestIngestingLimit("10", 100, 10, 90);
  }

  @Test
  public void testLimitOfZeroWithTenIngestsToAddZippedMediaPackage() {
    setupAndTestIngestingLimit("0", 10, 10, 0);
  }

  @Test
  public void testLegacyMediaPackageIdPropertyUsingZippedIngest() throws Exception {
    // Create a mock ingest service
    Capture<Map<String, String>> workflowConfigCapture = EasyMock.newCapture();

    IngestService ingestService = EasyMock.createNiceMock(IngestService.class);
    EasyMock.expect(ingestService.addZippedMediaPackage(EasyMock.anyObject(InputStream.class), EasyMock.anyString(),
            EasyMock.capture(workflowConfigCapture))).andReturn(new WorkflowInstanceImpl());
    EasyMock.replay(ingestService);
    restService.setIngestService(ingestService);

    String mpId = "6f7a7850-3232-4719-9064-24c9bad2832f";
    Response response = restService.addZippedMediaPackage(setupAddZippedMediaPackageHttpServletRequest(), "test", mpId);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Map<String, String> config = workflowConfigCapture.getValue();
    Assert.assertFalse(config.isEmpty());
    Assert.assertEquals(mpId, config.get(IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY));
  }

  @Test
  public void testLegacyMediaPackageIdPropertyUsingIngest() throws Exception {
    // Create a mock ingest service
    Capture<Map<String, String>> workflowConfigCapture = EasyMock.newCapture();

    IngestService ingestService = EasyMock.createNiceMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage())
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.ingest(EasyMock.anyObject(MediaPackage.class), EasyMock.anyString(),
            EasyMock.capture(workflowConfigCapture))).andReturn(new WorkflowInstanceImpl());
    EasyMock.replay(ingestService);
    restService.setIngestService(ingestService);

    String mpId = "6f7a7850-3232-4719-9064-24c9bad2832f";

    MultivaluedMap<String, String> metadataMap = new MetadataMap<>();
    Response createMediaPackage = restService.createMediaPackage();
    MediaPackage mp = (MediaPackage) createMediaPackage.getEntity();
    metadataMap.add("mediaPackage", MediaPackageParser.getAsXml(mp));
    metadataMap.add(IngestRestService.WORKFLOW_INSTANCE_ID_PARAM, mpId);
    Response response = restService.ingest(metadataMap);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Map<String, String> config = workflowConfigCapture.getValue();
    Assert.assertFalse(config.isEmpty());
    Assert.assertEquals(mpId, config.get(IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY));
  }

  public void setupAndTestIngestingLimit(String limit, int numberOfIngests, int expectedOK, int expectedBusy) {
    restService = new IngestRestService();
    restService.setIngestService(setupAddZippedMediaPackageIngestService());
    restService.activate(setupAddZippedMediaPackageComponentContext(limit));

    limitVerifier = new LimitVerifier(numberOfIngests);
    limitVerifier.start();

    Assert.assertEquals("There should be no errors when making requests.", 0, limitVerifier.getError());
    Assert.assertEquals("There should have been the same number of successful ingests finished as expected.",
            expectedOK, limitVerifier.getOk());
    Assert.assertEquals("The extra ingests beyond the limit should have received a server unavailable error. ",
            expectedBusy, limitVerifier.getUnavailable());
  }

  private class LimitVerifier {
    private int numberOfIngests;
    private int current;

    private int unavailable = 0;
    private int ok = 0;
    private int error = 0;

    LimitVerifier(int numberOfIngests) {
      this.numberOfIngests = numberOfIngests;
    }

    public void start() {
      getResponse();
    }

    private void getResponse() {
      current++;
      if (current > numberOfIngests) {
        return;
      } else {
        Response response = restService.addZippedMediaPackage(setupAddZippedMediaPackageHttpServletRequest());
        if (response.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
          unavailable++;
          // Because there is no mock that gets called if the service is unavailable we will have to do the next request
          // here.
          getResponse();
        } else if (response.getStatus() == Status.OK.getStatusCode()) {
          ok++;
        } else {
          error++;
        }
      }
    }

    public void callback() {
      getResponse();
    }

    private synchronized int getUnavailable() {
      return unavailable;
    }

    private synchronized int getOk() {
      return ok;
    }

    private synchronized int getError() {
      return error;
    }
  }

  private ServletInputStream setupAddZippedMediaPackageServletInputStream() {
    ServletInputStream servletInputStream = new ServletInputStream() {
      @Override
      public int read() throws IOException {
        return 0;
      }

      @Override
      public boolean isFinished() {
        return false;
      }

      @Override
      public boolean isReady() {
        return false;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
      }
    };
    return servletInputStream;
  }

  private HttpServletRequest setupAddZippedMediaPackageHttpServletRequest() {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andReturn("post");
    try {
      EasyMock.expect(request.getInputStream()).andReturn(setupAddZippedMediaPackageServletInputStream());
    } catch (IOException e) {
      Assert.fail("Failed due to exception " + e.getMessage());
    }
    EasyMock.replay(request);
    return request;
  }

  private BundleContext setupAddZippedMediaPackageBundleContext(String limit) {
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(IngestRestService.DEFAULT_WORKFLOW_DEFINITION)).andReturn("full").anyTimes();
    if (StringUtils.trimToNull(limit) != null) {
      EasyMock.expect(bc.getProperty(IngestRestService.MAX_INGESTS_KEY)).andReturn(limit).anyTimes();
    }
    EasyMock.replay(bc);
    return bc;
  }

  private ComponentContext setupAddZippedMediaPackageComponentContext(String limit) {
    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(setupAddZippedMediaPackageBundleContext(limit)).anyTimes();
    EasyMock.replay(cc);
    return cc;
  }

  @SuppressWarnings({ "unchecked", "deprecation" })
  private IngestService setupAddZippedMediaPackageIngestService() {
    // Create a mock ingest service
    IngestService ingestService = EasyMock.createNiceMock(IngestService.class);
    try {
      EasyMock.expect(ingestService.addZippedMediaPackage(EasyMock.anyObject(InputStream.class), EasyMock.anyString(),
              EasyMock.anyObject(Map.class), EasyMock.anyLong()))
              .andAnswer(() -> {
                limitVerifier.callback();
                return new WorkflowInstanceImpl();
              }).anyTimes();
      EasyMock.expect(ingestService.addZippedMediaPackage(EasyMock.anyObject(InputStream.class), EasyMock.anyString(),
              EasyMock.anyObject(Map.class)))
              .andAnswer(() -> {
                limitVerifier.callback();
                return new WorkflowInstanceImpl();
              }).anyTimes();
    } catch (Exception e) {
      Assert.fail("Threw exception " + e.getMessage());
    }
    EasyMock.replay(ingestService);
    return ingestService;
  }

  @Test
  public void testCreateMediaPackage() throws Exception {
    Response response = restService.createMediaPackage();
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    MediaPackage mp = (MediaPackage) response.getEntity();
    Assert.assertNotNull(mp);

    response = restService.createMediaPackage("1a6f70ab-4262-4523-9f8e-babce22a1ea8");
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    mp = (MediaPackage) response.getEntity();
    Assert.assertNotNull(mp);

  }

  @Test
  public void testAddMediaPackageTrack() throws Exception {
    Response response = restService.addMediaPackageTrack("http://foo/av.mov", "presenter/source", "testtag",
            MediaPackageParser.getAsXml(((MediaPackage) restService.createMediaPackage().getEntity())));
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testAddMediaPackageCatalog() throws Exception {
    Response response = restService.addMediaPackageCatalog("http://foo/dc.xml", "dublincore/episode",
            MediaPackageParser.getAsXml(((MediaPackage) restService.createMediaPackage().getEntity())));
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testAddMediaPackageAttachment() throws Exception {
    Response response = restService.addMediaPackageAttachment("http://foo/cover.png", "image/cover",
            MediaPackageParser.getAsXml(((MediaPackage) restService.createMediaPackage().getEntity())));
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testAddMediaPackageAttachmentFromRequest() throws Exception {
    // Upload the mediapackage with its new element
    Response postResponse = restService.addMediaPackageAttachment(newMockRequest());
    Assert.assertEquals(Status.OK.getStatusCode(), postResponse.getStatus());
  }

  @Test
  public void testAddMediaPackageCatalogFromRequest() throws Exception {
    // Upload the mediapackage with its new element
    Response postResponse = restService.addMediaPackageCatalog(newMockRequest());
    Assert.assertEquals(Status.OK.getStatusCode(), postResponse.getStatus());
  }

  @Test
  public void testAddMediaPackageTrackFromRequest() throws Exception {
    // Upload the mediapackage with its new element
    Response postResponse = restService.addMediaPackageTrack(newMockRequest());
    Assert.assertEquals(Status.OK.getStatusCode(), postResponse.getStatus());
  }

  @Test
  public void testAddMediaPackagePartialTrack() throws Exception {
    String mediaPackage = MediaPackageParser.getAsXml(((MediaPackage) restService.createMediaPackage().getEntity()));

    Response response = restService.addMediaPackagePartialTrack("http://foo/av.mov", "presenter/source+partial", 1000L,
            mediaPackage);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());

    response = restService.addMediaPackagePartialTrack(newPartialMockRequest());
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testAddMediaPackageTrackWithStartTime() throws Exception {
    IngestService ingestService = EasyMock.createNiceMock(IngestService.class);
    EasyMock.expect(ingestService.addPartialTrack((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), EasyMock.anyLong(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()).once();
    EasyMock.expect(ingestService.addTrack((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (String[]) EasyMock.anyObject(),
            (MediaPackage) EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()).once();
    EasyMock.replay(ingestService);
    restService.setIngestService(ingestService);

    MockHttpServletRequest request = newPartialMockRequest();
    request.setPathInfo("/addTrack");
    Response response = restService.addMediaPackageTrack(request);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    request = newPartialMockRequest();
    request.setPathInfo("/addPartialTrack");
    response = restService.addMediaPackageTrack(request);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    EasyMock.verify(ingestService);
  }

  private HttpServletRequest newMockRequest() throws Exception {
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    StringBuilder requestBody = new StringBuilder();
    requestBody.append("-----1234\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"flavor\"\r\n");
    requestBody.append("\r\ntest/flavor\r\n");
    requestBody.append("-----1234\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"mediaPackage\"\r\n");
    requestBody.append("\r\n");
    requestBody.append(MediaPackageParser.getAsXml(mp));
    requestBody.append("\r\n");
    requestBody.append("-----1234\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"catalog.txt\"\r\n");
    requestBody.append("Content-Type: text/whatever\r\n");
    requestBody.append("\r\n");
    requestBody.append("This is the content of the file\n");
    requestBody.append("\r\n");
    requestBody.append("-----1234");
    return new MockHttpServletRequest(requestBody.toString().getBytes("UTF-8"),
            "multipart/form-data; boundary=---1234");
  }

  private MockHttpServletRequest newPartialMockRequest() throws Exception {
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    StringBuilder requestBody = new StringBuilder();
    requestBody.append("-----1234\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"flavor\"\r\n");
    requestBody.append("\r\ntest/flavor\r\n");
    requestBody.append("-----1234\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"mediaPackage\"\r\n");
    requestBody.append("\r\n");
    requestBody.append(MediaPackageParser.getAsXml(mp));
    requestBody.append("\r\n");
    requestBody.append("-----1234\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"startTime\"\r\n");
    requestBody.append("\r\n2000\r\n");
    requestBody.append("-----1234\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"catalog.txt\"\r\n");
    requestBody.append("Content-Type: text/whatever\r\n");
    requestBody.append("\r\n");
    requestBody.append("This is the content of the file\n");
    requestBody.append("\r\n");
    requestBody.append("-----1234");
    return new MockHttpServletRequest(requestBody.toString().getBytes("UTF-8"),
            "multipart/form-data; boundary=---1234");
  }

}
