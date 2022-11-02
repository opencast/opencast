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

import com.auth0.jwk.JwkException;

import org.junit.Before;

import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests {@link JWTQueryParameterAuthenticationFilter}.
 */
public class JWTQueryParameterAuthenticationFilterTest extends JWTLoginTest {
  private JWTQueryParameterAuthenticationFilter parameterAuthenticationFilter =
          new JWTQueryParameterAuthenticationFilter();

  @Override @Before
  public void setUp() throws NoSuchAlgorithmException, JwkException {
    super.setUp();

    // Prepare authentication filter
    parameterAuthenticationFilter.setLoginHandler(loginHandler);
    parameterAuthenticationFilter.setParameterName("jwt");

    authFilter = new TestAbstractPreAuthenticatedProcessingFilter() {
      @Override
      public Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        return parameterAuthenticationFilter.getPreAuthenticatedPrincipal(request);
      }
    };
  }

  @Override
  protected HttpServletRequest mockRequest(String content) {
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    expect(request.getQueryString())
            .andReturn("jwt=" + content)
            .atLeastOnce();
    replay(request);
    return request;
  }
}
