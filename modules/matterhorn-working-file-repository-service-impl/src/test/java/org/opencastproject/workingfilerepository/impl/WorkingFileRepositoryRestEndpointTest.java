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

package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.util.UrlSupport;

import junit.framework.Assert;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

import javax.ws.rs.core.Response;

public class WorkingFileRepositoryRestEndpointTest {

  private WorkingFileRepositoryRestEndpoint endpoint = null;

  @Before
  public void setUp() throws Exception {
    endpoint = new WorkingFileRepositoryRestEndpoint();
    endpoint.rootDirectory = "target/endpointroot";
    FileUtils.forceMkdir(new File(endpoint.rootDirectory));
    endpoint.serverUrl = UrlSupport.DEFAULT_BASE_URL;
    endpoint.serviceUrl = new URI("http://localhost/files");
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
  public void testExtractXmlContentType() throws Exception {
    String mediaPackageId = "mp";
    String dc = "element1";
    InputStream in = null;
    InputStream responseIn = null;
    try {
      in = getClass().getResourceAsStream("/dublincore.xml");
      endpoint.put(mediaPackageId, dc, "dublincore.xml", in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // execute gets, and ensure that the content types are correct
    Response response = endpoint.restGet(mediaPackageId, dc, null);

    Assert.assertEquals("Gif content type", "application/xml", response.getMetadata().getFirst("Content-Type"));

    // Make sure the image byte stream was not modified by the content type detection
    try {
      in = getClass().getResourceAsStream("/dublincore.xml");
      byte[] imageBytesFromClasspath = IOUtils.toByteArray(in);
      responseIn = (InputStream) response.getEntity();
      byte[] imageBytesFromRepo = IOUtils.toByteArray(responseIn);
      Assert.assertTrue(Arrays.equals(imageBytesFromClasspath, imageBytesFromRepo));
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(responseIn);
    }
  }

  public void testEtag() throws Exception {
    String mediaPackageId = "mp";
    String dc = "element1";
    InputStream in = null;
    InputStream responseIn = null;
    try {
      in = getClass().getResourceAsStream("/dublincore.xml");
      endpoint.put(mediaPackageId, dc, "dublincore.xml", in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    try {
      in = getClass().getResourceAsStream("/dublincore.xml");
      String md5 = DigestUtils.md5Hex(in);
      Response response = endpoint.restGet(mediaPackageId, dc, md5);
      Assert.assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
      responseIn = (InputStream) response.getEntity();
      Assert.assertNull(responseIn);
      response = endpoint.restGet(mediaPackageId, dc, "foo");
      Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      responseIn = (InputStream) response.getEntity();
      Assert.assertNotNull(responseIn);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(responseIn);
    }

  }

}
