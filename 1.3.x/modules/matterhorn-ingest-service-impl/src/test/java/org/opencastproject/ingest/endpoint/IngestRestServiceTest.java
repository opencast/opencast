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
package org.opencastproject.ingest.endpoint;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.util.NotFoundException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import junit.framework.Assert;

import org.apache.commons.fileupload.MockHttpServletRequest;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class IngestRestServiceTest {
  protected IngestRestService restService;
  private ComboPooledDataSource pooledDataSource = null;

  @Before
  public void setUp() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    restService = new IngestRestService();
    restService.setPersistenceProvider(new PersistenceProvider());
    restService.setPersistenceProperties(props);
    
    // Create a mock ingest service
    IngestService ingestService = EasyMock.createNiceMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage()).andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addAttachment(
            (URI)EasyMock.anyObject(), (MediaPackageElementFlavor)EasyMock.anyObject(), (MediaPackage)EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addCatalog(
            (URI)EasyMock.anyObject(), (MediaPackageElementFlavor)EasyMock.anyObject(), (MediaPackage)EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addTrack(
            (URI)EasyMock.anyObject(), (MediaPackageElementFlavor)EasyMock.anyObject(), (MediaPackage)EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addAttachment(
            (InputStream)EasyMock.anyObject(), (String)EasyMock.anyObject(), (MediaPackageElementFlavor)EasyMock.anyObject(), (MediaPackage)EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addCatalog(
            (InputStream)EasyMock.anyObject(), (String)EasyMock.anyObject(), (MediaPackageElementFlavor)EasyMock.anyObject(), (MediaPackage)EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.expect(ingestService.addTrack(
            (InputStream)EasyMock.anyObject(), (String)EasyMock.anyObject(), (MediaPackageElementFlavor)EasyMock.anyObject(), (MediaPackage)EasyMock.anyObject()))
            .andReturn(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    EasyMock.replay(ingestService);

    // Set the service, and activate the rest endpoint
    restService.setIngestService(ingestService);
    restService.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    pooledDataSource.close();
  }

  @Test
  public void testCreateMediaPackage() throws Exception {
    Response response = restService.createMediaPackage();
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    MediaPackage mp = (MediaPackage)response.getEntity();
    Assert.assertNotNull(mp);
  }

  @Test
  public void testAddMediaPackageTrack() throws Exception {
    Response response = restService.addMediaPackageTrack("http://foo/av.mov", "presenter/source",
            MediaPackageParser.getAsXml(((MediaPackage)restService.createMediaPackage().getEntity())));
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testAddMediaPackageCatalog() throws Exception {
    Response response = restService.addMediaPackageCatalog("http://foo/dc.xml", "dublincore/episode",
            MediaPackageParser.getAsXml(((MediaPackage)restService.createMediaPackage().getEntity())));
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testAddMediaPackageAttachment() throws Exception {
    Response response = restService.addMediaPackageAttachment("http://foo/cover.png", "image/cover",
            MediaPackageParser.getAsXml(((MediaPackage)restService.createMediaPackage().getEntity())));
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
  public void testUploadJobPersistence() throws Exception {
    // Create the job (this is done in a browser on the initial page load)
    UploadJob job = restService.createUploadJob();

    // Upload the mediapackage with its new element
    Response postResponse = restService.addElementMonitored(job.getId(), newMockRequest());
    Assert.assertEquals(Status.OK.getStatusCode(), postResponse.getStatus());

    // Check on the job status
    Response progressResponse = restService.getProgress(job.getId());
    Assert.assertEquals(Status.OK.getStatusCode(), progressResponse.getStatus());
    Assert.assertTrue(progressResponse.getEntity().toString().startsWith("{\"total\":"));
    
    // Check on a job that doesn't exist
    try {
      restService.getProgress("This ID does not exist");
      Assert.fail("The rest service should throw");
    } catch (NotFoundException e) {
      // expected
    }
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
    return new MockHttpServletRequest(requestBody.toString().getBytes("UTF-8"),"multipart/form-data; boundary=---1234");
  }
}
