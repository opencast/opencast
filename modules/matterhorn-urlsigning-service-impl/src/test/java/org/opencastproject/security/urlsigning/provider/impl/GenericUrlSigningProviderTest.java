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

import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.urlsigning.common.Policy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class GenericUrlSigningProviderTest {
  private static final Logger logger = LoggerFactory.getLogger(GenericUrlSigningProviderTest.class);
  private static final String RTMP_MATCHER = "rtmp";
  private static final String KEY = "0123456789abcdef";
  private static final String KEY_ID = "theId";
  private static final String MATCHING_URI = "http://www.entwinemedia.com";
  private static final String NON_MATCHING_URI = "http://hello.com";
  private static final String NON_MATCHING_KEY = "abcdef0123456789";
  private static final String RESOURCE_PATH = "http://www.entwinemedia.com/path/to/resource.mp4";

  @Test
  public void testUpdate() throws ConfigurationException {
    Properties properties = new Properties();
    GenericUrlSigningProvider signer = new GenericUrlSigningProvider();
    // Handles empty properties
    signer.updated(properties);
    assertFalse(signer.accepts(RESOURCE_PATH));
    assertEquals(0, signer.getUris().size());

    // Incomplete entries
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    signer.updated(properties);
    assertFalse(signer.accepts(RESOURCE_PATH));
    assertEquals(0, signer.getUris().size());

    // Non-Matching key
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", NON_MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", NON_MATCHING_KEY);
    signer.updated(properties);
    assertFalse(signer.accepts(RESOURCE_PATH));
    assertEquals(1, signer.getUris().size());

    // Matching Key
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);
    signer.updated(properties);
    assertTrue(signer.accepts(RESOURCE_PATH));
    assertEquals(1, signer.getUris().size());

    // Matching Key and Unrelated Key
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);

    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".2", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".2", NON_MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".2", NON_MATCHING_KEY);
    signer.updated(properties);
    assertTrue(signer.accepts(RESOURCE_PATH));
    assertEquals(2, signer.getUris().size());
  }

  @Test
  public void testSign() throws UrlSigningException, ConfigurationException {
    Properties properties = new Properties();
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", MATCHING_URI);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);

    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".2", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".2", RTMP_MATCHER);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".2", KEY);

    GenericUrlSigningProvider signer = new GenericUrlSigningProvider();
    signer.updated(properties);

    DateTime before = new DateTime(2020, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    // Handles a policy without query parameters.
    Policy withoutQuery = Policy.mkSimplePolicy(RESOURCE_PATH, before);
    String result = signer.sign(withoutQuery);
    logger.info(result);
    assertEquals(
            "http://www.entwinemedia.com/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3LmVudHdpbmVtZWRpYS5jb21cL3BhdGhcL3RvXC9yZXNvdXJjZS5tcDQifX0&keyId=theId&signature=a20e1e5891266c9c2f25da99930a821808725971e5dd2ab386e4482de743ecb9",
            result);
    // Handles a policy with additional query parameters.
    Policy withQuery = Policy.mkSimplePolicy("http://www.entwinemedia.com/path/to/resource.mp4?queryparam=this", before);
    result = signer.sign(withQuery);
    logger.info(result);
    assertEquals(
            "http://www.entwinemedia.com/path/to/resource.mp4?queryparam=this&policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoiaHR0cDpcL1wvd3d3LmVudHdpbmVtZWRpYS5jb21cL3BhdGhcL3RvXC9yZXNvdXJjZS5tcDQ_cXVlcnlwYXJhbT10aGlzIn19&keyId=theId&signature=e20bdc3c82f167c3b71f71d05d224bc6307ddb9a4ead8e04428e7c60f2959f8e",
            result);
    // Handles rtmp protocol
    Policy withRtmp = Policy.mkSimplePolicy("rtmp://www.entwinemedia.com/path/to/resource.mp4", before);
    result = signer.sign(withRtmp);
    logger.info(result);
    assertEquals(
            "rtmp://www.entwinemedia.com/path/to/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTU4MzAyMzU3NzAwMH0sIlJlc291cmNlIjoicnRtcDpcL1wvd3d3LmVudHdpbmVtZWRpYS5jb21cL3BhdGhcL3RvXC9yZXNvdXJjZS5tcDQifX0&keyId=theId&signature=773bb6c0170a5faba3a1c3bb49a75ad0ce66a6748fc19afdb037d11de654dec5",
            result);
  }

  @Test
  public void testSignUrlWithPort() throws Exception {
    Properties properties = new Properties();
    properties.put(GenericUrlSigningProvider.ID_PREFIX + ".1", KEY_ID);
    properties.put(GenericUrlSigningProvider.URL_PREFIX + ".1", RTMP_MATCHER);
    properties.put(GenericUrlSigningProvider.KEY_PREFIX + ".1", KEY);

    GenericUrlSigningProvider signer = new GenericUrlSigningProvider();
    signer.updated(properties);

    assertTrue(signer.sign(Policy.mkSimplePolicy("rtmp://myhost.com:1935/vod/mp4:movie.mp4", new DateTime()))
            .startsWith(
                    "rtmp://myhost.com:1935/vod/mp4:movie.mp4"));
  }
}
