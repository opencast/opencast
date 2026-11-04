/*
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

package org.opencastproject.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.net.URI;

public class UrlSupportTest {
  @Test
  public void testConcat() {
    assertEquals("first/second", UrlSupport.concat("first", "second"));
    assertEquals("first/second", UrlSupport.concat("first", "/second"));
    assertEquals("first/second", UrlSupport.concat("first/", "//second"));
    assertEquals("/first/second", UrlSupport.concat("/first/", "//second"));
    assertEquals("/first", UrlSupport.concat("/", "first"));
    assertEquals("/first/", UrlSupport.concat("/", "first/", true));
    assertEquals("/first/", UrlSupport.concat("/", "first", true));
    assertEquals("first/second/", UrlSupport.concat("first", "second", true));
    assertEquals("first/second/", UrlSupport.concat("first", "second//", true));
    assertEquals("first/second/third", UrlSupport.concat("first", "second//", "third"));
    assertEquals("http://localhost:8080/first/second", UrlSupport.concat("http://localhost:8080", "first", "second"));
  }

  private static String withoutQueryAndFragment(String uri) {
    return UrlSupport.withoutQueryAndFragment(URI.create(uri));
  }

  @Test
  public void testWithoutQueryAndFragmentRemovesQuery() {
    // The signature of stream security
    assertEquals("http://localhost:8080/static/mh_default_org/engage-player/mp1/el1/video.mp4",
        withoutQueryAndFragment("http://localhost:8080/static/mh_default_org/engage-player/mp1/el1/video.mp4"
            + "?policy=abc&signature=def"));
    assertEquals("http://localhost:8080/video.mp4",
        withoutQueryAndFragment("http://localhost:8080/video.mp4?a=1&b=2"));
    assertEquals("http://localhost:8080/video.mp4", withoutQueryAndFragment("http://localhost:8080/video.mp4?"));
  }

  @Test
  public void testWithoutQueryAndFragmentRemovesFragment() {
    assertEquals("http://localhost:8080/video.mp4", withoutQueryAndFragment("http://localhost:8080/video.mp4#t=10"));
    assertEquals("http://localhost:8080/video.mp4",
        withoutQueryAndFragment("http://localhost:8080/video.mp4?sig=1#t=10"));
    // A question mark in the fragment is not a query
    assertEquals("http://localhost:8080/video.mp4", withoutQueryAndFragment("http://localhost:8080/video.mp4#a?b=1"));
    assertEquals("http://localhost:8080/video.mp4",
        withoutQueryAndFragment("http://localhost:8080/video.mp4?sig=1#a?b=1"));
  }

  @Test
  public void testWithoutQueryAndFragmentOfUriWithoutBoth() {
    assertEquals("http://localhost:8080/video.mp4", withoutQueryAndFragment("http://localhost:8080/video.mp4"));
    assertEquals("file:///var/lib/video.mp4", withoutQueryAndFragment("file:///var/lib/video.mp4"));
  }

  @Test
  public void testWithoutQueryAndFragmentDoesNotDecodeOrNormalize() {
    assertEquals("http://localhost:8080/my%20video%2Fpart.mp4",
        withoutQueryAndFragment("http://localhost:8080/my%20video%2Fpart.mp4?sig=1"));
    assertEquals("http://localhost:8080/a%3Fb.mp4", withoutQueryAndFragment("http://localhost:8080/a%3Fb.mp4?sig=1"));
    assertEquals("http://localhost:8080/vid%C3%A9o.mp4",
        withoutQueryAndFragment("http://localhost:8080/vid%C3%A9o.mp4?sig=1"));
    assertEquals("http://user:pw@[::1]:8080/video.mp4",
        withoutQueryAndFragment("http://user:pw@[::1]:8080/video.mp4?sig=1"));
    assertEquals("file:///var/lib/video.mp4", withoutQueryAndFragment("file:///var/lib/video.mp4?sig=1"));
  }

  @Test
  public void testWithoutQueryAndFragmentOfOpaqueUri() {
    assertEquals("urn:matterhorn:mp1:v1:el1:video.mp4",
        withoutQueryAndFragment("urn:matterhorn:mp1:v1:el1:video.mp4"));
    assertEquals("urn:matterhorn:mp1:v1:el1:video.mp4",
        withoutQueryAndFragment("urn:matterhorn:mp1:v1:el1:video.mp4#t=10"));
  }
}
