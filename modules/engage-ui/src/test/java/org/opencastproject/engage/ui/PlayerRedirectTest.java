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

package org.opencastproject.engage.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

public class PlayerRedirectTest extends EasyMockSupport {

  private SecurityService securityService;
  private Organization organization;
  private PlayerRedirect playerRedirect;

  @BeforeEach
  public void setup() {
    securityService = createMock(SecurityService.class);
    organization = createMock(Organization.class);
    playerRedirect = new PlayerRedirect();
    playerRedirect.setSecurityService(securityService);
  }

  @Test
  public void shouldRedirectToDefaultPlayerWhenNoPlayerIsConfigured() {
    String eventId = "event1";
    EasyMock.expect(securityService.getOrganization()).andReturn(organization);
    EasyMock.expect(organization.getProperties()).andReturn(new HashMap<>());
    replayAll();

    try (Response response = playerRedirect.redirect(eventId)) {
      assertEquals(Response.Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
      assertEquals("/paella7/ui/watch.html?id=event1", response.getHeaderString("location"));
    }

    verifyAll();
  }

  @Test
  public void shouldRedirectToConfiguredPlayer() {
    String eventId = "event2";
    Map<String, String> properties = new HashMap<>();
    properties.put("player", "/custom/ui/watch.html?id=#{id}");
    EasyMock.expect(securityService.getOrganization()).andReturn(organization);
    EasyMock.expect(organization.getProperties()).andReturn(properties);
    replayAll();

    try (Response response = playerRedirect.redirect(eventId)) {
      assertEquals(Response.Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
      assertEquals("/custom/ui/watch.html?id=event2", response.getHeaderString("location"));
    }

    verifyAll();
  }

  @Test
  public void shouldHandleSpecialCharactersInEventId() {
    String eventId = "event%20with%20spaces";
    EasyMock.expect(securityService.getOrganization()).andReturn(organization);
    EasyMock.expect(organization.getProperties()).andReturn(new HashMap<>());
    replayAll();

    try (Response response = playerRedirect.redirect(eventId)) {
      assertEquals(Response.Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
      assertEquals("/paella7/ui/watch.html?id=event%2520with%2520spaces", response.getHeaderString("location"));
    }

    verifyAll();
  }

  @Test
  public void shouldHandleUTF8EventId() {
    String eventId = "üäöß$%&/()=?!§";
    EasyMock.expect(securityService.getOrganization()).andReturn(organization);
    EasyMock.expect(organization.getProperties()).andReturn(new HashMap<>());
    replayAll();

    try (Response response = playerRedirect.redirect(eventId)) {
      assertEquals(Response.Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
      assertEquals("/paella7/ui/watch.html?id=%C3%BC%C3%A4%C3%B6%C3%9F%24%25%26%2F%28%29%3D%3F%21%C2%A7",
          response.getHeaderString("location"));
    }

    verifyAll();
  }
}
