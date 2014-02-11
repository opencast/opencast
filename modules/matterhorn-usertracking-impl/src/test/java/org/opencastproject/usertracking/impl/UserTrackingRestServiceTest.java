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
package org.opencastproject.usertracking.impl;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.systems.MatterhornConstans;
import org.opencastproject.usertracking.api.UserAction;
import org.opencastproject.usertracking.api.UserTrackingException;
import org.opencastproject.usertracking.api.UserTrackingService;
import org.opencastproject.usertracking.endpoint.UserTrackingRestService;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

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
                    new JaxbUser(MOCK_USER, new DefaultOrganization(), new JaxbRole("ROLE_USER",
                            new DefaultOrganization()))).anyTimes();

    BundleContext bc = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(MatterhornConstans.SERVER_URL_PROPERTY)).andReturn("http://www.example.org:8080")
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
    EasyMock.expect(usertracking.addUserFootprint(EasyMock.isA(UserAction.class))).andReturn(ua).anyTimes();

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

  private HttpServletRequest getMockHttpSessionWithProxy() {
    HttpServletRequest request = getMockHttpSession();
    EasyMock.expect(request.getHeader("X-FORWARDED-FOR")).andReturn(PROXY_IP).anyTimes();
    EasyMock.replay(request);
    return request;
  }

  @Test
  @Ignore
  public void testNullContext() {
    service.activate(null);
    // This is broken, the Response object generated in the REST service can't find the appropriate class :(
    HttpServletRequest request = getMockHttpSession();
    Response r = service.addFootprint("test", "0", "10", "FOOTPRINT", "true", request);
    UserAction a = (UserAction) r.getEntity();
    Assert.assertEquals(0, a.getInpoint());
    Assert.assertEquals(10, a.getOutpoint());
    Assert.assertEquals(10, a.getLength());
    Assert.assertEquals("FOOTPRINT", a.getType());
    Assert.assertTrue(a.getIsPlaying());
    Assert.assertEquals("test", a.getMediapackageId());
    Assert.assertEquals(MOCK_SESSION_ID, a.getSessionId());
    Assert.assertEquals(REMOTE_IP, a.getUserIp());
    Assert.assertEquals(MOCK_USER, a.getUserId());

    request = getMockHttpSessionWithProxy();
    r = service.addFootprint("test", "20", "30", "FOOTPRINT", "true", request);
    a = (UserAction) r.getEntity();
    Assert.assertEquals(20, a.getInpoint());
    Assert.assertEquals(30, a.getOutpoint());
    Assert.assertEquals(10, a.getLength());
    Assert.assertEquals("FOOTPRINT", a.getType());
    Assert.assertTrue(a.getIsPlaying());
    Assert.assertEquals("test", a.getMediapackageId());
    Assert.assertEquals(MOCK_SESSION_ID, a.getSessionId());
    Assert.assertEquals(REMOTE_IP, a.getUserIp());
    Assert.assertEquals(MOCK_USER, a.getUserId());
  }
}
