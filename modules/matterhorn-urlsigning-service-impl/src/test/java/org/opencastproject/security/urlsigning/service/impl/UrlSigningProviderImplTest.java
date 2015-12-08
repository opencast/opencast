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
package org.opencastproject.security.urlsigning.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.provider.UrlSigningProvider;
import org.opencastproject.urlsigning.common.Policy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;


public class UrlSigningProviderImplTest {
  private static final String SIGNED_URL = "signedUrl";
  private static final String URL = "http://testurl.com";
  private DateTime before = new DateTime(2020, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);;

  @Test
  public void testFindsSigningProviders() throws UrlSigningException {
    // Test no signing providers
    UrlSigningServiceImpl urlSigningServiceImpl = new UrlSigningServiceImpl();
    try {
      urlSigningServiceImpl.sign(URL, before, null, null);
      fail("There are no signing services, this should fail.");
    } catch (UrlSigningException e) {
      // This test should have a UrlSigningException as there are no supporting providers.
    }

    // Test no accepting signing providers
    urlSigningServiceImpl = new UrlSigningServiceImpl();
    urlSigningServiceImpl.registerSigningProvider(new TestRejectingSigningProvider());
    try {
      urlSigningServiceImpl.sign(URL, before, null, null);
      fail("There are no signing services, this should fail.");
    } catch (UrlSigningException e) {
      // This test should have a UrlSigningException as there are no supporting providers.
    }

    // Test only accepting signing providers
    urlSigningServiceImpl = new UrlSigningServiceImpl();
    urlSigningServiceImpl.registerSigningProvider(new TestAcceptingSigningProvider());
    String result = urlSigningServiceImpl.sign(URL, before, null, null);
    assertEquals(SIGNED_URL, result);

    // Test accepting signing provider with non-accepting
    urlSigningServiceImpl = new UrlSigningServiceImpl();
    urlSigningServiceImpl.registerSigningProvider(new TestRejectingSigningProvider());
    urlSigningServiceImpl.registerSigningProvider(new TestAcceptingSigningProvider());
    result = urlSigningServiceImpl.sign(URL, before, null, null);
    assertEquals(SIGNED_URL, result);
  }

  private class TestRejectingSigningProvider implements UrlSigningProvider {
    @Override
    public boolean accepts(String baseUrl) {
      return false;
    }

    @Override
    public String sign(Policy policy) throws UrlSigningException {
      return SIGNED_URL;
    }
  }

  private class TestAcceptingSigningProvider implements UrlSigningProvider {
    @Override
    public boolean accepts(String baseUrl) {
      return true;
    }

    @Override
    public String sign(Policy policy) throws UrlSigningException {
      return SIGNED_URL;
    }
  }
}
