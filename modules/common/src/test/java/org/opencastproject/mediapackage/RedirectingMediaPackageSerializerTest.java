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

package org.opencastproject.mediapackage;

import static org.junit.Assert.assertEquals;

import org.opencastproject.util.UrlSupport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

/**
 * Test case used to make sure the media package serializer works as expected.
 */
public class RedirectingMediaPackageSerializerTest {

  /** Test source prefix */
  private static final String SOURCE_URI_PREFIX = "http://www.test-a.com";

  /** Test destination prefix */
  private static final String DESTINATION_URI_PREFIX = "http://www.test-b.com/path";

  /** Test final destination prefix */
  private static final String FINAL_DESTINATION_URI_PREFIX = "http://www.test-c.com/path";

  /** The URI to replace */
  private URI sourceURIPrefix = null;

  /** The replacement URI */
  private URI destinationURIPrefix = null;

  /** The replacement URI */
  private URI finalDestinationURIPrefix = null;

  /** The serializer */
  private RedirectingMediaPackageSerializer serializer = null;

  @Before
  public void setUp() throws Exception {
    sourceURIPrefix = new URI(SOURCE_URI_PREFIX);
    destinationURIPrefix = new URI(DESTINATION_URI_PREFIX);
    finalDestinationURIPrefix = new URI(FINAL_DESTINATION_URI_PREFIX);
    serializer = new RedirectingMediaPackageSerializer(destinationURIPrefix, sourceURIPrefix);
  }

  @Test
  public void testEncodeMatchingElement() throws Exception {
    String path = "dc.xml";
    URI elementURI = new URI(UrlSupport.concat(SOURCE_URI_PREFIX, path));
    URI encodedElementURI = serializer.encodeURI(elementURI);
    assertEquals(UrlSupport.concat(SOURCE_URI_PREFIX, path), encodedElementURI.toString());
  }

  @Test
  public void testEncodeDecodeNonMatchingElement() throws Exception {
    String path = "dc.xml";
    URI elementURI = new URI(UrlSupport.concat("http://not-matching.com", path));
    URI encodedElementURI = serializer.encodeURI(elementURI);
    assertEquals(elementURI, encodedElementURI);
    URI decodedElementURI = serializer.decodeURI(elementURI);
    assertEquals(elementURI, decodedElementURI);
  }

  @Test
  public void testDecodeMatchingElement() throws Exception {
    String path = "dc.xml";
    URI elementURI = new URI(UrlSupport.concat(DESTINATION_URI_PREFIX, path));
    URI encodedElementURI = serializer.decodeURI(elementURI);
    assertEquals(UrlSupport.concat(DESTINATION_URI_PREFIX, path), encodedElementURI.toString());
  }

  @Test
  public void testMultipleRedirects() throws Exception {
    String path = "dc.xml";
    URI elementURI = new URI(UrlSupport.concat(FINAL_DESTINATION_URI_PREFIX, path));
    serializer.addRedirect(finalDestinationURIPrefix, destinationURIPrefix);
    URI encodedElementURI = serializer.decodeURI(elementURI);
    assertEquals(UrlSupport.concat(FINAL_DESTINATION_URI_PREFIX, path), encodedElementURI.toString());
  }

  @Test
  public void testCircularRedirects() throws Exception {
    String path = "dc.xml";
    URI elementURI = new URI(UrlSupport.concat(DESTINATION_URI_PREFIX, path));
    try {
      serializer.addRedirect(destinationURIPrefix, sourceURIPrefix);
      URI encodedElementURI = serializer.decodeURI(elementURI);
      Assert.fail(encodedElementURI + " has been the original uri, rewriter allows for cycles");
    } catch (IllegalStateException e) {
      // that's expected
    }
  }
}
