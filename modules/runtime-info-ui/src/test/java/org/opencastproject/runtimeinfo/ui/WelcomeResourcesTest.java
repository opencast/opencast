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

package org.opencastproject.runtimeinfo.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

class WelcomeResourcesTest {

  @Test
  void staticResourcesReturnsNotFoundForNonExistentResource() {
    WelcomeResources welcomeResources = new WelcomeResources();
    try (Response response = welcomeResources.staticResources("nonexistent.js")) {
      assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void staticResourcesReturnsOkForExistingResource() {
    WelcomeResources welcomeResources = new WelcomeResources();
    try (Response response = welcomeResources.staticResources("existing.js")) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void staticResourcesSetsCorrectMediaTypeForJs() {
    WelcomeResources welcomeResources = new WelcomeResources();
    try (Response response = welcomeResources.staticResources("existing.js")) {
      assertEquals("text/javascript;charset=UTF-8", response.getMediaType().toString());
    }
  }

  @Test
  void staticResourcesSetsCorrectMediaTypeForSvg() {
    WelcomeResources welcomeResources = new WelcomeResources();
    try (Response response = welcomeResources.staticResources("image.svg")) {
      assertEquals("image/svg+xml", response.getMediaType().toString());
    }
  }

  @Test
  void staticResourcesSetsCorrectMediaTypeForPng() {
    WelcomeResources welcomeResources = new WelcomeResources();
    try (Response response = welcomeResources.staticResources("logo.png")) {
      assertEquals("image/png", response.getMediaType().toString());
    }
  }

  @Test
  void staticResourcesSetsCorrectMediaTypeForCss() {
    WelcomeResources welcomeResources = new WelcomeResources();
    try (Response response = welcomeResources.staticResources("styles.css")) {
      assertEquals("text/css", response.getMediaType().toString());
    }
  }

  @Test
  void staticResourcesReturnsOkForResourceWithoutExtension() {
    WelcomeResources welcomeResources = new WelcomeResources();
    try (Response response = welcomeResources.staticResources("noextension")) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
  }


  @Test
  void staticResourcesReturnsOkForResourceEndsWithDot() {
    WelcomeResources welcomeResources = new WelcomeResources();
    try (Response response = welcomeResources.staticResources("dot.")) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
  }


}
