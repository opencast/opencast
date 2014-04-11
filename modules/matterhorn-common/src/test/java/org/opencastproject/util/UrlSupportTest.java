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
package org.opencastproject.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
