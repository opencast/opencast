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

import org.opencastproject.urlsigning.common.Policy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class PolicyUtilsTest {
  private static final String EXAMPLE_IP = "10.0.0.1";

  @Test
  public void testToJson() {
    DateTime before = new DateTime(2015, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    Policy policy = Policy.mkSimplePolicy("http://mh-allinone/", before);
    assertEquals("{\"Statement\":{\"Condition\":{\"DateLessThan\":" + before.getMillis()
            + "},\"Resource\":\"http:\\/\\/mh-allinone\\/\"}}", PolicyUtils.toJson(policy).toJSONString());
    // With optional parameters
    policy = Policy.mkPolicyValidFromWithIP("http://mh-allinone/", before, new DateTime(2015, 02, 28, 00, 46, 19, 0,
            DateTimeZone.UTC), EXAMPLE_IP);
    assertEquals(
            "{\"Statement\":{\"Condition\":{\"DateGreaterThan\":1425084379000,\"DateLessThan\":1425170777000,\"IpAddress\":\"10.0.0.1\"},\"Resource\":\"http:\\/\\/mh-allinone\\/\"}}",
            PolicyUtils.toJson(policy).toJSONString());
  }

  @Test
  public void testFromJson() throws UnsupportedEncodingException {
    String policyJson = "{\"Statement\": {\"Resource\":\"http://mh-allinone/engage/url/to/resource.mp4\",\"Condition\":{\"DateLessThan\":1425170777000,\"DateGreaterThan\":1425084379000,\"IpAddress\": \"10.0.0.1\"}}}";
    Policy policy = PolicyUtils.fromJson(policyJson);
    assertEquals("http://mh-allinone/engage/url/to/resource.mp4", policy.getBaseUrl());
    assertEquals(EXAMPLE_IP, policy.getClientIpAddress().get().getHostAddress());

    DateTime after = new DateTime(2015, 02, 28, 00, 46, 19, 0, DateTimeZone.UTC);
    after = after.withSecondOfMinute(19);
    assertEquals(after, policy.getValidFrom().get());

    DateTime before = new DateTime(2015, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    assertEquals(before, policy.getValidUntil());
  }

  @Test
  public void testBase64Decoding() throws UnsupportedEncodingException {
    String policyValue = "{policy:'The Policy'}";
    String result = PolicyUtils.base64Decode(PolicyUtils.base64Encode(policyValue));
    assertEquals(policyValue, result);
  }

  @Test
  public void testFromBase64EncodedPolicy() throws UnsupportedEncodingException {
    String examplePolicy = "{\"Statement\": {\"Resource\":\"http://mh-allinone/engage/url/to/resource.mp4\",\"Condition\":{\"DateLessThan\":1425170777000,\"DateGreaterThan\":1425084379000,\"IpAddress\": \"10.0.0.1\"}}}";
    Policy policy = PolicyUtils.fromBase64EncodedPolicy(PolicyUtils.base64Encode(examplePolicy));
    assertEquals("http://mh-allinone/engage/url/to/resource.mp4", policy.getBaseUrl());
    assertEquals(EXAMPLE_IP, policy.getClientIpAddress().get().getHostAddress());

    DateTime after = new DateTime(2015, 02, 28, 00, 46, 19, 0, DateTimeZone.UTC);
    after = after.withSecondOfMinute(19);
    assertEquals(after, policy.getValidFrom().get());

    DateTime before = new DateTime(2015, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    assertEquals(before, policy.getValidUntil());
  }

  @Test
  public void testToBase64EncodedPolicy() throws UnsupportedEncodingException {
    String resource = "http://mh-allinone/";
    DateTime before = new DateTime(2015, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    Policy policy = Policy.mkSimplePolicy("http://mh-allinone/", before);

    Policy result = PolicyUtils.fromBase64EncodedPolicy(PolicyUtils.toBase64EncodedPolicy(policy));
    assertEquals(resource, result.getBaseUrl());
    assertEquals(before, result.getValidUntil());
  }
}
