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

package org.opencastproject.usertracking.impl;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.usertracking.api.UserAction;
import org.opencastproject.usertracking.api.UserSession;
import org.opencastproject.usertracking.api.UserTrackingException;
import org.opencastproject.usertracking.api.UserTrackingService;
import org.opencastproject.usertracking.endpoint.UserTrackingRestService;

import org.easymock.EasyMock;
import org.junit.Before;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class UserTrackingRestServiceTest {

  public static final String MOCK_SESSION_ID = "mock session id";
  public static final String REMOTE_IP = "127.0.0.1";
  public static final String PROXY_IP = "127.0.0.2";
  public static final String MOCK_USER = "Mock User";

  private UserTrackingRestService service;

  @Before
  public void setUp() throws UserTrackingException {
    SecurityService security = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(security.getUser())
            .andReturn(
                    new JaxbUser(MOCK_USER, "test", new DefaultOrganization(), new JaxbRole("ROLE_USER",
                            new DefaultOrganization()))).anyTimes();

    BundleContext bc = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(OpencastConstants.SERVER_URL_PROPERTY)).andReturn("http://www.example.org:8080")
            .anyTimes();

    @SuppressWarnings("rawtypes")
    Dictionary dict = EasyMock.createMock(Dictionary.class);
    EasyMock.expect(dict.get(RestConstants.SERVICE_PATH_PROPERTY)).andReturn("/usertracking").anyTimes();

    ComponentContext context = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(context.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.expect(context.getProperties()).andReturn(dict).anyTimes();

    UserActionImpl ua = EasyMock.createMock(UserActionImpl.class);
    EasyMock.expect(ua.getId()).andReturn(4L).anyTimes();

    UserTrackingService usertracking = EasyMock.createMock(UserTrackingService.class);
    EasyMock.expect(usertracking.addUserFootprint(EasyMock.isA(UserAction.class), EasyMock.isA(UserSession.class))).andReturn(ua).anyTimes();

    EasyMock.replay(security, bc, dict, context, ua, usertracking);

    service = new UserTrackingRestService();
    service.setSecurityService(security);
    service.setService(usertracking);
    service.activate(context);
  }

  private HttpServletRequest getMockHttpSession() {
    HttpSession session = EasyMock.createMock(HttpSession.class);
    EasyMock.expect(session.getId()).andReturn(MOCK_SESSION_ID).anyTimes();
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getSession()).andReturn(session).anyTimes();
    EasyMock.expect(request.getHeader("X-FORWARDED-FOR")).andReturn(null).anyTimes();
    EasyMock.expect(request.getRemoteAddr()).andReturn(REMOTE_IP).anyTimes();
    EasyMock.replay(session, request);
    return request;
  }

}
