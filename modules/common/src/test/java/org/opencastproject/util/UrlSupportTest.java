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
    assertEquals("/first/", UrlSupport.concat("/", "first/"));
    assertEquals("first/second/third", UrlSupport.concat("first", "second//", "third"));
    assertEquals("http://localhost:8080/first/second", UrlSupport.concat("http://localhost:8080", "first", "second"));
    assertEquals("http://localhost:8080/files", UrlSupport.concat("http://localhost:8080/", "/files"));
    assertEquals("http://localhost:8080/", UrlSupport.concat("http://localhost:8080", "/"));
    assertEquals("first", UrlSupport.concat("first"));
  }

  @Test
  public void testConcatIgnoresEmptyParts() {
    assertEquals("a", UrlSupport.concat("", "a"));
    assertEquals("a", UrlSupport.concat("a", ""));
    assertEquals("a/b", UrlSupport.concat("a", "", "b"));
    assertEquals("a/b", UrlSupport.concat("a", null, "b"));
    assertEquals("a", UrlSupport.concat(null, "a"));
    assertEquals("", UrlSupport.concat("", ""));
    assertEquals("", UrlSupport.concat());
  }

  @Test
  public void testConcatOnlyTouchesBoundaries() {
    assertEquals("http://a//b/c", UrlSupport.concat("http://a//b", "c"));
    assertEquals("http://host/p/a//b", UrlSupport.concat("http://host/p", "a//b"));
    assertEquals("http://host/p/x?u=http://other//path", UrlSupport.concat("http://host/p", "x?u=http://other//path"));
    // Leading slashes of the first and trailing slashes of the last part are kept
    assertEquals("//a/b", UrlSupport.concat("//a", "b"));
    assertEquals("a/b//", UrlSupport.concat("a", "b//"));
  }

  @Test
  public void testUri() {
    assertEquals(URI.create("http://localhost:8080/events/5/comment"),
        UrlSupport.uri("http://localhost:8080/", "/events", 5, "comment"));
    assertEquals(URI.create("http://localhost:8080/events"), UrlSupport.uri("http://localhost:8080", null, "events"));
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
