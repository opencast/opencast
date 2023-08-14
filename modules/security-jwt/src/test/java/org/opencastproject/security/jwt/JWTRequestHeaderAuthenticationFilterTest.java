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

package org.opencastproject.security.jwt;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.auth0.jwk.JwkException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;

import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests {@link JWTRequestHeaderAuthenticationFilter}.
 */
public class JWTRequestHeaderAuthenticationFilterTest extends JWTLoginTest {
  private JWTRequestHeaderAuthenticationFilter headerAuthFilter = new JWTRequestHeaderAuthenticationFilter();

  @Override @Before
  public void setUp() throws NoSuchAlgorithmException, JwkException {
    super.setUp();

    // Prepare authentication filter
    headerAuthFilter.setLoginHandler(loginHandler);
    headerAuthFilter.setPrincipalRequestHeader("Authorization");

    authFilter = new TestAbstractPreAuthenticatedProcessingFilter() {
      @Override
      public Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        return headerAuthFilter.getPreAuthenticatedPrincipal(request);
      }
    };
  }

  @Override
  protected HttpServletRequest mockRequest(String content) {
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    expect(request.getHeader("Authorization"))
            .andReturn(content)
            .atLeastOnce();
    replay(request);
    return request;
  }

  @Test
  public void testLoginWithPrincipalPrefix() {
    Object username;
    String prefix = "Bearer ";
    headerAuthFilter.setPrincipalPrefix(prefix);

    // Existing principal prefix
    loginHandler.setUserDetailsService(userDetailsServiceNewUser);
    username = authFilter.getPreAuthenticatedPrincipal(
            mockRequest(prefix + generator.generateValidSymmetricJWT())
    );
    assertEquals(generator.getUsername(), username);

    // Missing principal prefix
    username = authFilter.getPreAuthenticatedPrincipal(
            mockRequest(generator.generateValidSymmetricJWT())
    );
    assertNull(username);
  }

  @Test
  public void testLoginWithInvalidRequestHeader() {
    headerAuthFilter.setPrincipalRequestHeader("XY");
    assertThrows(
            PreAuthenticatedCredentialsNotFoundException.class,
            () -> authFilter.getPreAuthenticatedPrincipal(
                    mockRequest(generator.generateExpiredSymmetricJWT())
            )
    );
  }
}
