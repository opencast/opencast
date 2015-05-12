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
package org.opencastproject.staticfiles.endpoint;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.anyObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.staticfiles.api.StaticFileService;
//import org.opencastproject.staticfiles.impl.StaticFileServiceImpl;
//import org.opencastproject.staticfiles.impl.StaticFileServiceImplTest;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.MockHttpServletRequest;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class StaticFileRestServiceTest {

  private static final String MOCK_FILE_CONTENT = "This is the content of the file\n";

  private static final Logger logger = LoggerFactory.getLogger(StaticFileRestServiceTest.class);

  private static final String SERVER_URL = "http://localhost:8080";

  private static final String WEBSERVER_URL = "http://localhost/staticfiles";

  private static String videoFilename = "av.mov";
  private static String imageFilename = "image.jpg";

  /** The File object that is an example image */
  private static File imageFile;
  /** Location where the files are copied to */
  private static File rootDir;
  /** The File object that is an example video */
  private static File videoFile;
  /** The org to use for the tests */
  private static Organization org = new DefaultOrganization();
  /** The test root directory */
  private static URI baseDir;

  @BeforeClass
  public static void beforeClass() throws URISyntaxException {
    // baseDir = StaticFileServiceImplTest.class.getResource("/").toURI();
    // rootDir = new File(new File(baseDir), "ingest-temp");
    // imageFile = new File(StaticFileServiceImplTest.class.getResource("/" + imageFilename).getPath());
    // videoFile = new File(StaticFileServiceImplTest.class.getResource("/" + videoFilename).getPath());
  }

  @Before
  public void setUp() throws IOException {
    // FileUtils.forceMkdir(rootDir);
  }

  @After
  public void tearDown() {
    // FileUtils.deleteQuietly(rootDir);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static ComponentContext getComponentContext(String useWebserver, long maxSize) {
    // Create BundleContext
    BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(MatterhornConstants.SERVER_URL_PROPERTY)).andReturn(SERVER_URL);
    // EasyMock.expect(bundleContext.getProperty(StaticFileServiceImpl.STATICFILES_ROOT_DIRECTORY_KEY)).andReturn(
    // rootDir.getAbsolutePath());
    EasyMock.expect(bundleContext.getProperty(StaticFileRestService.STATICFILES_UPLOAD_MAX_SIZE_KEY)).andReturn(
            Long.toString(maxSize));
    EasyMock.replay(bundleContext);
    // Create ComponentContext
    Dictionary properties = new Properties();
    if (useWebserver != null) {
      properties.put(StaticFileRestService.STATICFILES_WEBSERVER_ENABLED_KEY, useWebserver);
    }
    properties.put(StaticFileRestService.STATICFILES_WEBSERVER_URL_KEY, WEBSERVER_URL);
    ComponentContext cc = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(cc.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc);
    return cc;
  }

  private static SecurityService getSecurityService() {
    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.replay(securityService);
    return securityService;
  }

  private MockHttpServletRequest newMockRequest() throws Exception {
    StringBuilder requestBody = new StringBuilder();
    requestBody.append("-----1234\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"other.mov\"\r\n");
    requestBody.append("Content-Type: text/whatever\r\n");
    requestBody.append("\r\n");
    requestBody.append(MOCK_FILE_CONTENT);
    requestBody.append("\r\n");
    requestBody.append("-----1234");
    return new MockHttpServletRequest(requestBody.toString().getBytes("UTF-8"), "multipart/form-data; boundary=---1234");
  }

  private MockHttpServletRequest newUnsizedMockRequest() throws Exception {
    StringBuilder requestBody = new StringBuilder();
    requestBody.append("-----1234\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"other.mov\"\r\n");
    requestBody.append("Content-Type: text/whatever\r\n");
    requestBody.append("\r\n");
    requestBody.append(MOCK_FILE_CONTENT);
    requestBody.append("\r\n");
    requestBody.append("-----1234");
    return new MockHttpServletRequest(new ByteArrayInputStream(requestBody.toString().getBytes("UTF-8")), -1,
            "multipart/form-data; boundary=---1234");
  }

  @Test
  public void testUseWebserver() throws ConfigurationException {
    StaticFileRestService staticFileRestService = new StaticFileRestService();
    staticFileRestService.activate(getComponentContext(null, 100000000L));
    assertFalse(staticFileRestService.useWebserver);

    staticFileRestService.activate(getComponentContext("", 100000000L));
    assertFalse(staticFileRestService.useWebserver);

    staticFileRestService.activate(getComponentContext("false", 100000000L));
    assertFalse(staticFileRestService.useWebserver);

    staticFileRestService.activate(getComponentContext("other", 100000000L));
    assertFalse(staticFileRestService.useWebserver);

    staticFileRestService.activate(getComponentContext("true", 100000000L));
    assertTrue(staticFileRestService.useWebserver);
  }

  @Test
  public void testStoreStaticFileInputHttpServletRequest() throws FileUploadException, Exception {
    // Setup static file service.
    StaticFileService fileService = EasyMock.createMock(StaticFileService.class);
    String fileUuid = "12345";
    String fileName = "other.mov";
    EasyMock.expect(fileService.storeFile(eq(fileName), anyObject(InputStream.class))).andReturn(fileUuid);
    EasyMock.expect(fileService.getFileName(fileUuid)).andReturn(fileName);
    EasyMock.replay(fileService);

    // Run the test
    StaticFileRestService staticFileRestService = new StaticFileRestService();
    staticFileRestService.activate(getComponentContext(null, 100000000L));
    staticFileRestService.setSecurityService(getSecurityService());
    staticFileRestService.setStaticFileService(fileService);

    // Test a good store request
    Response result = staticFileRestService.postStaticFile(newMockRequest());
    assertEquals(Status.CREATED.getStatusCode(), result.getStatus());
    assertTrue(result.getMetadata().size() > 0);
    assertTrue(result.getMetadata().get("location").size() > 0);
    String location = result.getMetadata().get("location").get(0).toString();
    String uuid = location.substring(location.lastIndexOf("/") + 1);

    // assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(MOCK_FILE_CONTENT.getBytes("UTF-8")),
    // staticFileServiceImpl.getFile(uuid)));

    // Test a request with too large of an input stream
    HttpServletRequest tooLargeRequest = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(tooLargeRequest.getContentLength()).andReturn(1000000000).anyTimes();
    EasyMock.replay(tooLargeRequest);
    result = staticFileRestService.postStaticFile(tooLargeRequest);
    assertEquals(Status.BAD_REQUEST.getStatusCode(), result.getStatus());

    staticFileRestService.activate(getComponentContext("true", 100000000L));
    URI staticFileURL = staticFileRestService.getStaticFileURL(uuid);
    assertEquals("http://localhost/staticfiles/mh_default_org/" + uuid + "/other.mov", staticFileURL.toString());
  }

  @Test
  public void testUploadMaxSizeReached() throws FileUploadException, Exception {
    // Setup static file service.
    StaticFileService fileService = EasyMock.createMock(StaticFileService.class);
    EasyMock.expect(fileService.storeFile(eq("other.mov"), anyObject(InputStream.class))).andReturn("12345");
    EasyMock.replay(fileService);

    // Run the test
    StaticFileRestService staticFileRestService = new StaticFileRestService();
    staticFileRestService.activate(getComponentContext(null, 10L));
    staticFileRestService.setSecurityService(getSecurityService());
    staticFileRestService.setStaticFileService(fileService);

    // Test a sized mock request
    Response result = staticFileRestService.postStaticFile(newMockRequest());
    assertEquals(Status.BAD_REQUEST.getStatusCode(), result.getStatus());
  }

  @Test
  public void testDeleteStaticFile() throws FileUploadException, Exception {
    // Setup static file service.
    StaticFileService fileService = EasyMock.createMock(StaticFileService.class);
    final String fileUuid = "12345";
    EasyMock.expect(fileService.storeFile(anyObject(String.class), anyObject(InputStream.class))).andReturn(fileUuid);
    fileService.deleteFile(fileUuid);
    EasyMock.expectLastCall();
    EasyMock.expect(fileService.getFile(fileUuid)).andThrow(new NotFoundException());
    EasyMock.replay(fileService);

    // Run the test
    StaticFileRestService staticFileRestService = new StaticFileRestService();
    staticFileRestService.activate(getComponentContext(null, 100000000L));
    staticFileRestService.setSecurityService(getSecurityService());
    staticFileRestService.setStaticFileService(fileService);

    // Test a good store request
    Response result = staticFileRestService.postStaticFile(newMockRequest());
    assertEquals(Status.CREATED.getStatusCode(), result.getStatus());

    String location = result.getMetadata().get("location").get(0).toString();
    String uuid = location.substring(location.lastIndexOf("/") + 1);

    // staticFileServiceImpl.getFile(uuid);

    Response response = staticFileRestService.deleteStaticFile(uuid);
    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

    try {
      staticFileRestService.getStaticFile(uuid);
      fail("NotFoundException must be passed on");
    } catch (NotFoundException e) {
      // expected
    }
  }
}
