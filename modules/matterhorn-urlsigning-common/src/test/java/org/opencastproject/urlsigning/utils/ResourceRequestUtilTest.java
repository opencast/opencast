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
package org.opencastproject.urlsigning.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.urlsigning.common.Policy;
import org.opencastproject.urlsigning.common.ResourceRequest;
import org.opencastproject.urlsigning.common.ResourceRequest.Status;

import org.apache.http.NameValuePair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

public class ResourceRequestUtilTest {
  private static final String keyId = "default";
  private static final String key = "0123456789abcdef";
  private static final String clientIp = "10.0.0.1";
  private Properties properties;

  @Before
  public void setUp() {
    properties = new Properties();
    properties.put(keyId, key);
  }

  @Test
  public void testQueryStringParsing() {
    String policyValue = "{policy:'value'}";
    String signatureValue = "randomString";

    String queryString = "?" + ResourceRequest.POLICY_KEY + "=" + policyValue + "&" + ResourceRequest.SIGNATURE_KEY
            + "=" + signatureValue + "&" + ResourceRequest.ENCRYPTION_ID_KEY + "=" + keyId;

    List<NameValuePair> parameters = ResourceRequestUtil.parseQueryString(queryString);

    boolean foundOrg = false;
    boolean foundPolicy = false;
    boolean foundSignature = false;

    for (NameValuePair nameValuePair : parameters) {
      if (ResourceRequest.ENCRYPTION_ID_KEY.equals(nameValuePair.getName())) {
        assertEquals(keyId, nameValuePair.getValue());
        foundOrg = true;
      }
      if (ResourceRequest.POLICY_KEY.equals(nameValuePair.getName())) {
        assertEquals(policyValue, nameValuePair.getValue());
        foundPolicy = true;
      }
      if (ResourceRequest.SIGNATURE_KEY.equals(nameValuePair.getName())) {
        assertEquals(signatureValue, nameValuePair.getValue());
        foundSignature = true;
      }
    }

    assertTrue("Didn't find the organization value.", foundOrg);
    assertTrue("Didn't find the policy value.", foundPolicy);
    assertTrue("Didn't find the signature value.", foundSignature);
  }

  @Test
  public void testAuthenticateDuplicateProperties() {
    // Test duplicate query properties.
    String twoOrgs = ResourceRequest.ENCRYPTION_ID_KEY + "=org1&" + ResourceRequest.ENCRYPTION_ID_KEY + "=org2";

    assertEquals(Status.BadRequest,
            ResourceRequestUtil.resourceRequestFromQueryString(twoOrgs, clientIp, null, properties, true).getStatus());

    String twoPolicies = ResourceRequest.POLICY_KEY + "=policy1&" + ResourceRequest.POLICY_KEY + "=policy2";
    assertEquals(Status.BadRequest, ResourceRequestUtil
            .resourceRequestFromQueryString(twoPolicies, clientIp, null, properties, true).getStatus());

    String twoSignatures = ResourceRequest.SIGNATURE_KEY + "=signature1&" + ResourceRequest.SIGNATURE_KEY
            + "=signature1";
    assertEquals(Status.BadRequest, ResourceRequestUtil
            .resourceRequestFromQueryString(twoSignatures, clientIp, null, properties, true).getStatus());
  }

  @Test
  public void testAuthenticateMissingProperties() {
    // Test Missing query properties
    String missingOrg = ResourceRequest.POLICY_KEY + "=policy&" + ResourceRequest.SIGNATURE_KEY + "=signature";
    assertEquals(Status.BadRequest, ResourceRequestUtil
            .resourceRequestFromQueryString(missingOrg, clientIp, null, properties, true).getStatus());

    String missingPolicy = ResourceRequest.ENCRYPTION_ID_KEY + "=organization&" + ResourceRequest.SIGNATURE_KEY
            + "=signature";
    assertEquals(Status.BadRequest, ResourceRequestUtil
            .resourceRequestFromQueryString(missingPolicy, clientIp, null, properties, true).getStatus());

    String missingSignature = ResourceRequest.ENCRYPTION_ID_KEY + "=organization&" + ResourceRequest.POLICY_KEY
            + "=policy";
    assertEquals(Status.BadRequest, ResourceRequestUtil
            .resourceRequestFromQueryString(missingSignature, clientIp, null, properties, true).getStatus());
  }

  @Test
  public void testAuthenticatePolicyMatchesSignature() throws Exception {
    DateTime after = new DateTime(DateTimeZone.UTC);
    after = after.minus(2 * 60 * 60 * 1000L);
    DateTime before = new DateTime(DateTimeZone.UTC);
    before = before.plus(2 * 60 * 60 * 1000L);
    String nonMatchingResource = "http://other.com";
    Policy nonMatchingPolicy = Policy.mkSimplePolicy(nonMatchingResource, before);
    String matchingResource = "http://mh-allinone/";
    Policy matchingPolicy = Policy.mkSimplePolicy(matchingResource, before);
    String signature = PolicyUtils.getPolicySignature(matchingPolicy, key);

    // Test non-existant encryption key is forbidden.
    String wrongEncryptionKeyId = ResourceRequest.ENCRYPTION_ID_KEY + "=" + "WrongId" + "&" + ResourceRequest.POLICY_KEY
            + "=" + PolicyUtils.toBase64EncodedPolicy(matchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "="
            + signature;
    assertEquals(Status.Forbidden,
            ResourceRequestUtil
                    .resourceRequestFromQueryString(wrongEncryptionKeyId, clientIp, matchingResource, properties, true)
                    .getStatus());

    // Test non matching resource results is forbidden.
    String nonMatching = ResourceRequest.ENCRYPTION_ID_KEY + "=organization&" + ResourceRequest.POLICY_KEY + "="
            + PolicyUtils.toBase64EncodedPolicy(nonMatchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "="
            + signature;
    assertEquals(Status.Forbidden, ResourceRequestUtil
            .resourceRequestFromQueryString(nonMatching, clientIp, matchingResource, properties, true).getStatus());

    // Test non-matching client ip results in forbidden.
    Policy wrongClientPolicy = Policy.mkPolicyValidWithIP(matchingResource, before, "10.0.0.255");
    String wrongClient = ResourceRequestUtil.policyToResourceRequestQueryString(wrongClientPolicy, keyId, key);
    assertEquals(Status.Forbidden, ResourceRequestUtil
            .resourceRequestFromQueryString(wrongClient, clientIp, matchingResource, properties, true).getStatus());

    // Test matching client ip results in ok.
    Policy rightClientPolicy = Policy.mkPolicyValidWithIP(matchingResource, before, clientIp);
    String rightClient = ResourceRequestUtil.policyToResourceRequestQueryString(rightClientPolicy, keyId, key);
    assertEquals(Status.Ok, ResourceRequestUtil
            .resourceRequestFromQueryString(rightClient, clientIp, matchingResource, properties, true).getStatus());

    // Test not yet DateGreaterThan results in gone
    Policy wrongDateGreaterThanPolicy = Policy.mkPolicyValidFrom(matchingResource, before, before);
    String wrongDateGreaterThan = ResourceRequestUtil.policyToResourceRequestQueryString(wrongDateGreaterThanPolicy,
            keyId, key);
    assertEquals(Status.Gone,
            ResourceRequestUtil
                    .resourceRequestFromQueryString(wrongDateGreaterThan, clientIp, matchingResource, properties, true)
                    .getStatus());

    // Test after DateGreaterThan results in ok
    Policy rightDateGreaterThanPolicy = Policy.mkPolicyValidFrom(matchingResource, before, after);
    String rightDateGreaterThan = ResourceRequestUtil.policyToResourceRequestQueryString(rightDateGreaterThanPolicy,
            keyId, key);
    assertEquals(Status.Ok,
            ResourceRequestUtil
                    .resourceRequestFromQueryString(rightDateGreaterThan, clientIp, matchingResource, properties, true)
                    .getStatus());

    // Test before DateLessThan results in gone
    Policy wrongDateLessThanPolicy = Policy.mkSimplePolicy(matchingResource, after);
    String wrongDateLessThan = ResourceRequestUtil.policyToResourceRequestQueryString(wrongDateLessThanPolicy, keyId,
            key);
    assertEquals(Status.Gone,
            ResourceRequestUtil
                    .resourceRequestFromQueryString(wrongDateLessThan, clientIp, matchingResource, properties, true)
                    .getStatus());

    // Test matching results in ok.
    String matching = ResourceRequest.ENCRYPTION_ID_KEY + "=" + keyId + "&" + ResourceRequest.POLICY_KEY + "="
            + PolicyUtils.toBase64EncodedPolicy(matchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "=" + signature;
    assertEquals(Status.Ok, ResourceRequestUtil
            .resourceRequestFromQueryString(matching, clientIp, matchingResource, properties, true).getStatus());
  }

  @Test
  public void testIsSigned() throws URISyntaxException {
    String noQueryString = "http://notsigned.com";
    String wrongQueryString = "http://notsigned.com?irrelevant=value";
    String signed = "http://notsigned.com?signature=theSignature&keyId=theKey&policy=thePolicy";
    assertFalse(ResourceRequestUtil.isSigned(new URI(noQueryString)));
    assertFalse(ResourceRequestUtil.isSigned(new URI(wrongQueryString)));
    assertTrue(ResourceRequestUtil.isSigned(new URI(signed)));
  }

  @Test
  public void testNonStrictResourceChecking() throws Exception {
    DateTime before = new DateTime(DateTimeZone.UTC);
    before = before.plus(2 * 60 * 60 * 1000L);
    String hostname = "signed.host.com";
    String path = "/path/to/resource";
    String rtmpResource = "rtmp://" + hostname + path;
    String httpResource = "http://" + hostname + path;
    String portResource = "rtmp://" + hostname + ":8080" + path;
    String differentHostnameResource = "rtmp://different.host.com" + path;

    Policy differentScheme = Policy.mkSimplePolicy(rtmpResource, before);
    String signature = PolicyUtils.getPolicySignature(differentScheme, key);
    String differentSchemeQueryString = ResourceRequest.ENCRYPTION_ID_KEY + "=default&" + ResourceRequest.POLICY_KEY + "="
            + PolicyUtils.toBase64EncodedPolicy(differentScheme) + "&" + ResourceRequest.SIGNATURE_KEY + "="
            + signature;

    assertEquals(Status.Ok, ResourceRequestUtil
            .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, httpResource, properties, false).getStatus());
    assertEquals(Status.Forbidden, ResourceRequestUtil
            .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, httpResource, properties, true).getStatus());

    assertEquals(Status.Ok, ResourceRequestUtil
            .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, portResource, properties, false).getStatus());
    assertEquals(Status.Forbidden, ResourceRequestUtil
            .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, portResource, properties, true).getStatus());

    assertEquals(Status.Ok, ResourceRequestUtil
            .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, differentHostnameResource, properties, false).getStatus());
    assertEquals(Status.Forbidden, ResourceRequestUtil
            .resourceRequestFromQueryString(differentSchemeQueryString, clientIp, differentHostnameResource, properties, true).getStatus());
  }
}
