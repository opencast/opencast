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
package org.opencastproject.kernel.security;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests the {@link RemoteUserAndOrganizationFilter}
 */
public class RemoteUserAndOrganizationFilterTest {

  private RemoteUserAndOrganizationFilter filter;

  private FilterChain chain;

  private Responder<User> userResponder;

  private Responder<User> switchingUserResponder;

  private User switchingUser;

  private User defaultUser;

  private static class Responder<A> implements IAnswer<A> {
    private A response;

    Responder(A response) {
      this.response = response;
    }

    public void setResponse(A response) {
      this.response = response;
    }

    @Override
    public A answer() throws Throwable {
      return response;
    }
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    defaultUser = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    switchingUser = new JaxbUser("switch", "test", new DefaultOrganization(), new JaxbRole("ROLE_USER",
            new DefaultOrganization()));
    userResponder = new Responder<User>(defaultUser);

    chain = EasyMock.createNiceMock(FilterChain.class);
    EasyMock.replay(chain);

    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    switchingUserResponder = new Responder<User>(switchingUser);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject(String.class))).andAnswer(switchingUserResponder)
            .anyTimes();
    EasyMock.replay(userDirectoryService);

    OrganizationDirectoryService organizationDirectoryService = EasyMock
            .createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization(EasyMock.anyObject(String.class)))
            .andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(organizationDirectoryService);

    filter = new RemoteUserAndOrganizationFilter();
    filter.setOrganizationDirectoryService(organizationDirectoryService);
    filter.setUserDirectoryService(userDirectoryService);
  }

  @Test
  public void testOrganizationSwitchingForbidden() throws IOException {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    filter.setSecurityService(securityService);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.replay(securityService);

    userResponder.setResponse(switchingUser);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(SecurityConstants.ORGANIZATION_HEADER)).andReturn("mh_default_org").anyTimes();
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    response.sendError(EasyMock.anyInt());
    EasyMock.expectLastCall().times(1);
    EasyMock.replay(response);

    try {
      filter.doFilter(request, response, chain);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    EasyMock.verify(response);
  }

  @Test
  public void testOrganizationSwitching() throws IOException {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    filter.setSecurityService(securityService);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    securityService.setOrganization(EasyMock.anyObject(Organization.class));
    EasyMock.expectLastCall().times(2);
    EasyMock.replay(securityService);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(SecurityConstants.ORGANIZATION_HEADER)).andReturn("mh_default_org").anyTimes();
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    EasyMock.replay(response);

    try {
      filter.doFilter(request, response, chain);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    EasyMock.verify(securityService);
  }

  @Test
  public void testUserSwitchingForbidden() throws IOException {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    filter.setSecurityService(securityService);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.replay(securityService);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(SecurityConstants.USER_HEADER)).andReturn("joe").anyTimes();
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    response.sendError(EasyMock.anyInt());
    EasyMock.expectLastCall().times(1);
    EasyMock.replay(response);

    try {
      filter.doFilter(request, response, chain);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    EasyMock.verify(response);
  }

  @Test
  public void testUserSwitchingToAdminForbidden() throws IOException {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    filter.setSecurityService(securityService);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.replay(securityService);

    User defaultUser = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_SUDO_ROLE, new DefaultOrganization()));
    userResponder.setResponse(defaultUser);
    switchingUserResponder.setResponse(defaultUser);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(SecurityConstants.USER_HEADER)).andReturn("admin").anyTimes();
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    response.sendError(EasyMock.anyInt());
    EasyMock.expectLastCall().times(1);
    EasyMock.replay(response);

    try {
      filter.doFilter(request, response, chain);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    EasyMock.verify(response);
  }

  @Test
  public void testUserSwitching() throws IOException {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    filter.setSecurityService(securityService);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    securityService.setUser(EasyMock.anyObject(User.class));
    EasyMock.expectLastCall().times(2);
    EasyMock.replay(securityService);

    User defaultUser = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_SUDO_ROLE, new DefaultOrganization()));
    userResponder.setResponse(defaultUser);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(SecurityConstants.USER_HEADER)).andReturn("joe").anyTimes();
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    EasyMock.replay(response);

    try {
      filter.doFilter(request, response, chain);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    EasyMock.verify(securityService);
  }

  @Test
  public void testRolesSwitchingForbidden() throws IOException {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    filter.setSecurityService(securityService);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.replay(securityService);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(SecurityConstants.ROLES_HEADER)).andReturn("ROLE_TEST").anyTimes();
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    response.sendError(EasyMock.anyInt());
    EasyMock.expectLastCall().times(1);
    EasyMock.replay(response);

    try {
      filter.doFilter(request, response, chain);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    EasyMock.verify(response);
  }

  @Test
  public void testRolesSwitchingForbiddenAdmin() throws IOException {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    filter.setSecurityService(securityService);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.replay(securityService);

    User defaultUser = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_SUDO_ROLE, new DefaultOrganization()));
    userResponder.setResponse(defaultUser);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(SecurityConstants.ROLES_HEADER)).andReturn("ROLE_TEST,ROLE_ADMIN").anyTimes();
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    response.sendError(EasyMock.anyInt());
    EasyMock.expectLastCall().times(1);
    EasyMock.replay(response);

    try {
      filter.doFilter(request, response, chain);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    EasyMock.verify(response);
  }

  @Test
  public void testRolesSwitching() throws IOException {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    filter.setSecurityService(securityService);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    securityService.setUser(EasyMock.anyObject(User.class));
    EasyMock.expectLastCall().times(2);
    EasyMock.replay(securityService);

    User defaultUser = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_SUDO_ROLE, new DefaultOrganization()));
    userResponder.setResponse(defaultUser);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(SecurityConstants.ROLES_HEADER)).andReturn("ROLE_TEST,ROLE_USER").anyTimes();
    EasyMock.replay(request);

    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    EasyMock.replay(response);

    try {
      filter.doFilter(request, response, chain);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    EasyMock.verify(securityService);
  }

}
