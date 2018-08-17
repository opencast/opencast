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
package org.opencastproject.security.urlsigning.provider.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.urlsigning.common.Policy;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class GenericUrlSigningProviderTest {
  private static final Logger logger = LoggerFactory.getLogger(GenericUrlSigningProviderTest.class);

  private static final String RTMP_MATCHER = "rtmp";
  private static final String RESOURCE_URL = "/path/to/resource.mp4";
  private static final String NON_MATCHING_URI = "http://hello.com";
  private static final String NON_MATCHING_KEY = "abcdef0123456789";

  private static final String KEY_ID = "theId";
  private static final String MATCHING_URI = "http://www.opencast.org";
  private static final String KEY = "0123456789abcdef";
  private static final String RESOURCE_PATH = MATCHING_URI + RESOURCE_URL;

  private static final String ORGANIZATION_A_ID = "organization-a";
  private static final String ORGANIZATION_A_MATCHING_URI = "http://organization-a.opencast.org";
  private static final String ORGANIZATION_A_KEY_ID = "key-id-organization-a";
  private static final String ORGANIZATION_A_KEY = "key-organization-a";
  private static final String ORGANIZATION_A_RESOURCE_PATH = ORGANIZATION_A_MATCHING_URI + RESOURCE_URL;

  private static final String ORGANIZATION_B_ID = "organization-b";
  private static final String ORGANIZATION_B_MATCHING_URI = "http://organization-b.opencast.org";
  private static final String ORGANIZATION_B_KEY_ID = "key-id-organizatino-b";
  private static final String ORGANIZATION_B_KEY = "key-organization-b";
  private static final String ORGANIZATION_B_RESOURCE_PATH = ORGANIZATION_B_MATCHING_URI + RESOURCE_URL;

  private static GenericUrlSigningProvider signer;
  private static GenericUrlSigningProvider signerA;
  private static GenericUrlSigningProvider signerB;

  private static Properties properties;

  @BeforeClass
  public static void setUpClass() throws Exception {

    /* Create mockup for organization a */
    JaxbOrganization organizationA = EasyMock.createNiceMock(JaxbOrganization.class);
    EasyMock.expect(organizationA.getId()).andReturn(ORGANIZATION_A_ID).anyTimes();
    EasyMock.replay(organizationA);
    SecurityService securityServiceA = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityServiceA.getOrganization()).andReturn(organizationA).anyTimes();
    EasyMock.replay(securityServiceA);
    signerA = new GenericUrlSigningProvider();
    signerA.setSecurityService(securityServiceA);

    /* Create mockup for organization b */
    JaxbOrganization organizationB = EasyMock.createNiceMock(JaxbOrganization.class);
    EasyMock.expect(organizationB.getId()).andReturn(ORGANIZATION_B_ID).anyTimes();
    EasyMock.replay(organizationB);
    SecurityService securityServiceB = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityServiceB.getOrganization()).andReturn(organizationB).anyTimes();
    EasyMock.replay(securityServiceB);
    signerB = new GenericUrlSigningProvider();
    signerB.setSecurityService(securityServiceB);

    properties = new Properties();

    /* To indicate that specific tests are not supposed to test multi-tenant capabilities, we just set an alias */
    signer = signerA;
  }

  @Before
  public void setUp() throws Exception {
    properties.clear();
    signerA.updated(properties);
    signerB.updated(properties);
  }

  @Test
  public void testPropertiesUpdated() throws ConfigurationException {
    // Handles empty properties
    assertFalse(signer.accepts(RESOURCE_PATH));
    assertEquals(0, signer.getUris().size());

    // Incomplete entries
    properties.clear();
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    signer.updated(properties);
    assertFalse(signer.accepts(RESOURCE_PATH));
    assertEquals(0, signer.getUris().size());

    // Non-Matching key
    properties.clear();
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", NON_MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", NON_MATCHING_KEY);
    signer.updated(properties);
    assertFalse(signer.accepts(RESOURCE_PATH));
    assertEquals(1, signer.getUris().size());

    // Matching Key
    properties.clear();
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);
    signer.updated(properties);
    assertTrue(signer.accepts(RESOURCE_PATH));
    assertEquals(1, signer.getUris().size());

    // Matching Key and Unrelated Key
    properties.clear();
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);

    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".2", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".2", NON_MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".2", NON_MATCHING_KEY);
    signer.updated(properties);
    assertTrue(signer.accepts(RESOURCE_PATH));
    assertEquals(2, signer.getUris().size());

    // Organization set to "any" organization works
    properties.clear();
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);
    properties.put(GenericUrlSigningProvider.ORGANIZATION_PREFIX + ".1", GenericUrlSigningProvider.ANY_ORGANIZATION);
    signer.updated(properties);
    assertTrue(signer.accepts(RESOURCE_PATH));
    assertEquals(1, signer.getUris().size());
  }

  @Test
  public void testSign() throws UrlSigningException, ConfigurationException {
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);

    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".2", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".2", RTMP_MATCHER);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".2", KEY);

    signer.updated(properties);

    DateTime before = new DateTime(2020, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    // Handles a policy without query parameters.
    Policy withoutQuery = Policy.mkSimplePolicy(RESOURCE_PATH, before);
    String result = signer.sign(withoutQuery);
    logger.info(result);
    assertEquals(
            "http://www.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wNCJ9fQ&keyId=theId&signature=5b45e678275e6bc7b06a579f7f42e9a7ea5c58f1da130701db532f121e363e98",
            result);
    // Handles a policy with additional query parameters.
    Policy withQuery = Policy.mkSimplePolicy(RESOURCE_PATH + "?queryparam=this", before);
    result = signer.sign(withQuery);
    logger.info(result);
    assertEquals(
            "http://www.opencast.org/path/to/resource.mp4?queryparam=this&policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wND9xdWVyeXBhcmFtPXRoaXMifX0&keyId=theId&signature=0f9cab4393a5d0ebe2683124a92094c3ccf06ed07e87b8f60fa2bab3963bd462",
            result);
    // Handles rtmp protocol
    Policy withRtmp = Policy.mkSimplePolicy("rtmp://www.opencast.org/path/to/resource.mp4", before);
    result = signer.sign(withRtmp);
    logger.info(result);
    assertEquals(
            "rtmp://www.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoicnRtcDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wNCJ9fQ&keyId=theId&signature=0464d9672fa5cb62ed82e7a6c46db5552dcd76590cf11efa5ba5222f53f5bbaa",
            result);
  }

  @Test
  public void testSignUrlWithPort() throws Exception {
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", RTMP_MATCHER);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);

    signer.updated(properties);

    assertTrue(signer.sign(Policy.mkSimplePolicy("rtmp://myhost.com:1935/vod/mp4:movie.mp4", new DateTime()))
            .startsWith(
                    "rtmp://myhost.com:1935/vod/mp4:movie.mp4"));
  }

  @Test
  public void testMultitenantSign() throws UrlSigningException, ConfigurationException {

    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);

    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".2", ORGANIZATION_A_KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".2", ORGANIZATION_A_MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".2", ORGANIZATION_A_KEY);
    properties.put(GenericUrlSigningProvider.ORGANIZATION_PREFIX + ".2", ORGANIZATION_A_ID);

    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".3", ORGANIZATION_B_KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".3", ORGANIZATION_B_MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".3", ORGANIZATION_B_KEY);
    properties.put(GenericUrlSigningProvider.ORGANIZATION_PREFIX + ".3", ORGANIZATION_B_ID);

    signerA.updated(properties);
    signerB.updated(properties);

    DateTime before = new DateTime(2020, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    Policy policy;
    String result;
    boolean exceptionThrown;

    // Organization A can sign its URLs using its key
    policy = Policy.mkSimplePolicy(ORGANIZATION_A_RESOURCE_PATH, before);
    result = signerA.sign(policy);
    logger.info(result);
    assertEquals(
            "http://organization-a.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvb3JnYW5pemF0aW9uLWEub3BlbmNhc3Qub3JnXC9wYXRoXC90b1wvcmVzb3VyY2UubXA0In19&keyId=key-id-organization-a&signature=ea715d6ff561f49bb6e2cb8cc3e925029e139f14eeda62fc92573f362c694423",
            result);

    // Organization A can sign URLs not specific to any organization
    policy = Policy.mkSimplePolicy(RESOURCE_PATH, before);
    result = signerA.sign(policy);
    logger.info(result);
    assertEquals(
            "http://www.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wNCJ9fQ&keyId=theId&signature=5b45e678275e6bc7b06a579f7f42e9a7ea5c58f1da130701db532f121e363e98",
            result);

    // Organization A cannot sign URLs of organization B
    exceptionThrown = false;
    policy = Policy.mkSimplePolicy(ORGANIZATION_B_RESOURCE_PATH, before);
    try {
      result = signerA.sign(policy);
    } catch (UrlSigningException e) {
      exceptionThrown = true;
    }
    assertTrue(exceptionThrown);

    // Organization B can sign its URLs using its key
    policy = Policy.mkSimplePolicy(ORGANIZATION_B_RESOURCE_PATH, before);
    result = signerB.sign(policy);
    logger.info(result);
    assertEquals(
            "http://organization-b.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvb3JnYW5pemF0aW9uLWIub3BlbmNhc3Qub3JnXC9wYXRoXC90b1wvcmVzb3VyY2UubXA0In19&keyId=key-id-organizatino-b&signature=a6d803d4766f808bf2eaabdcf2e9114f85513d3d5596b4a01cb8d2488de816e8",
            result);

    // Organization B can sign URLs not specific to any organization
    policy = Policy.mkSimplePolicy(RESOURCE_PATH, before);
    result = signerB.sign(policy);
    logger.info(result);
    assertEquals(
            "http://www.opencast.org/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3Lm9wZW5jYXN0Lm9yZ1wvcGF0aFwvdG9cL3Jlc291cmNlLm1wNCJ9fQ&keyId=theId&signature=5b45e678275e6bc7b06a579f7f42e9a7ea5c58f1da130701db532f121e363e98",
            result);

    // Organization B cannot sign URLs of organization B
    exceptionThrown = false;
    policy = Policy.mkSimplePolicy(ORGANIZATION_A_RESOURCE_PATH, before);
    try {
      result = signerB.sign(policy);
    } catch (UrlSigningException e) {
      exceptionThrown = true;
    }
    assertTrue(exceptionThrown);
  }

}
