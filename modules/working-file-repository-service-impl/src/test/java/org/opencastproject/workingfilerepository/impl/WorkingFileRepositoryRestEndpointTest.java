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


package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

public class WorkingFileRepositoryRestEndpointTest {

  private WorkingFileRepositoryRestEndpoint endpoint = null;

  @Before
  public void setUp() throws Exception {
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.expect(organization.getId()).andReturn("org1").anyTimes();
    Map<String, String> orgProps = new HashMap<String, String>();
    orgProps.put(OpencastConstants.WFR_URL_ORG_PROPERTY, UrlSupport.DEFAULT_BASE_URL);
    EasyMock.expect(organization.getProperties()).andReturn(orgProps).anyTimes();
    EasyMock.replay(organization);

    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);

    endpoint = new WorkingFileRepositoryRestEndpoint();
    endpoint.setSecurityService(securityService);
    endpoint.rootDirectory = "target/endpointroot";
    FileUtils.forceMkdir(new File(endpoint.rootDirectory));
    endpoint.serverUrl = UrlSupport.DEFAULT_BASE_URL;
    endpoint.servicePath = WorkingFileRepositoryImpl.URI_PREFIX;
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(new File(endpoint.rootDirectory));
  }

  @Test
  public void testExtractImageContentType() throws Exception {
    String mediaPackageId = "mp";
    String image = "element1";
    InputStream in = null;
    InputStream responseIn = null;

    try {
      in = getClass().getResourceAsStream("/opencast_header.gif");
      endpoint.put(mediaPackageId, image, "opencast_header.gif", in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // execute gets, and ensure that the content types are correct
    Response response = endpoint.restGet(mediaPackageId, image, null);

    Assert.assertEquals("Gif content type", "image/gif", response.getMetadata().getFirst("Content-Type"));

    // Make sure the image byte stream was not modified by the content type detection
    try {
      in = getClass().getResourceAsStream("/opencast_header.gif");
      byte[] bytesFromClasspath = IOUtils.toByteArray(in);
      responseIn = (InputStream) response.getEntity();
      byte[] bytesFromRepo = IOUtils.toByteArray(responseIn);
      Assert.assertTrue(Arrays.equals(bytesFromClasspath, bytesFromRepo));
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(responseIn);
    }
  }


  @Test
  public void testExtractImageContentTypeFromCollection() throws Exception {
    InputStream in = null;

    try {
      in = getClass().getResourceAsStream("/opencast_header.gif");
      endpoint.putInCollection("collection-2", "opencast_header.gif", in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // execute gets, and ensure that the content types are correct
    Response response = endpoint.restGetFromCollection("collection-2", "opencast_header.gif");

    Assert.assertEquals("Gif content type", "image/gif", response.getMetadata().getFirst("Content-Type"));
  }


  @Test
  public void testExtractXmlContentType() throws Exception {
    String mediaPackageId = "mp";
    String dc = "element1";
    try (InputStream in = getClass().getResourceAsStream("/dublincore.xml")) {
      endpoint.put(mediaPackageId, dc, "dublincore.xml", in);
    }

    // execute gets, and ensure that the content types are correct
    Response response = endpoint.restGet(mediaPackageId, dc, null);

    Assert.assertEquals("DC content type", "text/xml", response.getMetadata().getFirst("Content-Type"));

    // Make sure the image byte stream was not modified by the content type detection
    try (InputStream in = getClass().getResourceAsStream("/dublincore.xml")) {
      byte[] imageBytesFromClasspath = IOUtils.toByteArray(in);
      try (InputStream responseIn = (InputStream) response.getEntity()) {
        byte[] imageBytesFromRepo = IOUtils.toByteArray(responseIn);
        Assert.assertTrue(Arrays.equals(imageBytesFromClasspath, imageBytesFromRepo));
      }
    }
  }

}
