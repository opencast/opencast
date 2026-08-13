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
package org.opencastproject.kernel.filter.https;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.kernel.filter.proxy.TransparentProxyFilter;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests the {@link HttpsFilter}
 */
public class HttpsFilterTest {

  private HttpsFilter filter;

  /** The request the filter chain was called with */
  private HttpServletRequest forwarded;

  private FilterChain chain;

  @Before
  public void setUp() {
    filter = new HttpsFilter();
    forwarded = null;
    chain = new FilterChain() {
      @Override
      public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        forwarded = (HttpServletRequest) request;
      }
    };
  }

  private void run(boolean trusted, String header, String value) throws Exception {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getAttribute(TransparentProxyFilter.FORWARDED_HEADERS_TRUSTED))
        .andStubReturn(trusted ? Boolean.TRUE : null);
    EasyMock.expect(request.getHeader(header)).andStubReturn(value);
    EasyMock.expect(request.getScheme()).andStubReturn("http");
    EasyMock.expect(request.isSecure()).andStubReturn(false);
    EasyMock.expect(request.getRequestURL()).andStubReturn(new StringBuffer("http://example.org/test"));
    EasyMock.replay(request);

    filter.doFilter(request, EasyMock.createNiceMock(HttpServletResponse.class), chain);
  }

  @Test
  public void testForwardedSslFromTrustedProxy() throws Exception {
    run(true, "X-Forwarded-SSL", "on");
    assertEquals("https", forwarded.getScheme());
    assertTrue(forwarded.isSecure());
    assertEquals("https://example.org/test", forwarded.getRequestURL().toString());
  }

  @Test
  public void testForwardedProtoFromTrustedProxy() throws Exception {
    run(true, "X-Forwarded-Proto", "https");
    assertEquals("https", forwarded.getScheme());
    assertTrue(forwarded.isSecure());
  }

  @Test
  public void testForwardedSslFromUntrustedPeerIsIgnored() throws Exception {
    run(false, "X-Forwarded-SSL", "on");
    assertEquals("http", forwarded.getScheme());
    assertFalse(forwarded.isSecure());
  }

  @Test
  public void testForwardedProtoFromUntrustedPeerIsIgnored() throws Exception {
    run(false, "X-Forwarded-Proto", "https");
    assertEquals("http", forwarded.getScheme());
    assertFalse(forwarded.isSecure());
  }

  @Test
  public void testPlainRequestFromTrustedProxy() throws Exception {
    run(true, "X-Forwarded-Proto", "http");
    assertEquals("http", forwarded.getScheme());
    assertFalse(forwarded.isSecure());
  }

}
