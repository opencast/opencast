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
package org.opencastproject.security.urlsigning.verifier.impl;

import static org.junit.Assert.assertEquals;

import org.opencastproject.urlsigning.common.Policy;
import org.opencastproject.urlsigning.common.ResourceRequest;
import org.opencastproject.urlsigning.common.ResourceRequest.Status;
import org.opencastproject.urlsigning.utils.ResourceRequestUtil;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Dictionary;
import java.util.Hashtable;

public class UrlSigningVerifierImplTest {
  private static final String CLIENT_IP = "10.0.0.1";
  private static final String URL = "http://testurl.com";

  @Test
  public void testVerifiesWithSigningProviders() throws Exception {
    String keyId = "theKeyId";
    String key = "TheKeyIsThis";
    DateTime future = new DateTime(4749125399000L);
    Policy policy = Policy.mkSimplePolicy(URL, future);
    String queryString = ResourceRequestUtil.policyToResourceRequestQueryString(policy, keyId, key);

    // Test with no configured keys
    UrlSigningVerifierImpl urlSigningVerifierImpl = new UrlSigningVerifierImpl();
    ResourceRequest result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true);
    assertEquals(Status.Forbidden, result.getStatus());

    // Test no matching key
    urlSigningVerifierImpl = new UrlSigningVerifierImpl();
    Dictionary<String, String> keys = new Hashtable<>();
    keys.put(UrlSigningVerifierImpl.KEY_PREFIX + "otherKey", "ThisIsTheOtherKey");
    urlSigningVerifierImpl.updated(keys);
    result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true);
    assertEquals(Status.Forbidden, result.getStatus());

    // Test only matching keys
    urlSigningVerifierImpl = new UrlSigningVerifierImpl();
    keys = new Hashtable<>();
    keys.put(UrlSigningVerifierImpl.KEY_PREFIX + keyId, key);
    urlSigningVerifierImpl.updated(keys);
    result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true);
    assertEquals(Status.Ok, result.getStatus());

    // Test matching and non-matching keys
    urlSigningVerifierImpl = new UrlSigningVerifierImpl();
    keys = new Hashtable<>();
    keys.put(UrlSigningVerifierImpl.KEY_PREFIX + "otherKey", "ThisIsTheOtherKey");
    keys.put(UrlSigningVerifierImpl.KEY_PREFIX + keyId, key);
    urlSigningVerifierImpl.updated(keys);
    result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true);
    assertEquals(Status.Ok, result.getStatus());

    // Test correct key id and wrong key
    urlSigningVerifierImpl = new UrlSigningVerifierImpl();
    keys = new Hashtable<>();
    keys.put(UrlSigningVerifierImpl.KEY_PREFIX + "otherKey", "ThisIsTheOtherKey");
    keys.put(UrlSigningVerifierImpl.KEY_PREFIX + keyId, "The Wrong Key");
    urlSigningVerifierImpl.updated(keys);
    result = urlSigningVerifierImpl.verify(queryString, CLIENT_IP, URL, true);
    assertEquals(Status.Forbidden, result.getStatus());
  }
}
